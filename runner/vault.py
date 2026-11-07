"""AES-256-GCM vault for secret values: ``enc:v1:`` + base64(12-byte nonce + ciphertext + 16-byte tag).

Key: RUNNER_SECRET_KEY = base64 of 32 random bytes. Java counterpart: com.taf.core.utils.dataReader.SecretVault
(same format), used by LocalCredentials, so an ``enc:v1:`` value works in config/credentials.local.properties,
in an environment variable or typed in the run form.

The runner never stores passwords (AGENTS.md); this module only encrypts/decrypts values the owner manages.

    python runner/vault.py --new-key          print a new key
    python runner/vault.py --encrypt          read a value from stdin, print enc:v1:...  (needs RUNNER_SECRET_KEY)
"""

import argparse
import base64
import getpass
import os
import secrets
import sys

PREFIX = "enc:v1:"
NONCE_BYTES = 12


class VaultError(Exception):
    pass


def _aesgcm(key=None):
    try:
        from cryptography.hazmat.primitives.ciphers.aead import AESGCM
    except ImportError as error:  # optional dependency
        raise VaultError("The 'cryptography' package is required (pip install -r runner/requirements.txt)") from error
    raw = key if key is not None else os.environ.get("RUNNER_SECRET_KEY", "")
    if not raw:
        raise VaultError("RUNNER_SECRET_KEY is not set")
    try:
        material = base64.b64decode(raw, validate=True)
    except ValueError as error:
        raise VaultError("RUNNER_SECRET_KEY is not valid base64") from error
    if len(material) != 32:
        raise VaultError("RUNNER_SECRET_KEY must decode to 32 bytes")
    return AESGCM(material)


def available():
    try:
        _aesgcm()
        return True
    except VaultError:
        return False


def new_key():
    return base64.b64encode(secrets.token_bytes(32)).decode("ascii")


def is_encrypted(value):
    return isinstance(value, str) and value.startswith(PREFIX)


def encrypt(plain, key=None):
    nonce = secrets.token_bytes(NONCE_BYTES)
    sealed = _aesgcm(key).encrypt(nonce, plain.encode("utf-8"), None)
    return PREFIX + base64.b64encode(nonce + sealed).decode("ascii")


def decrypt(value, key=None):
    """Plain text of an enc:v1: value; any other value is returned unchanged."""
    if not is_encrypted(value):
        return value
    try:
        blob = base64.b64decode(value[len(PREFIX):], validate=True)
    except ValueError as error:
        raise VaultError("Encrypted value is not valid base64") from error
    if len(blob) < NONCE_BYTES + 16:
        raise VaultError("Encrypted value is too short")
    try:
        return _aesgcm(key).decrypt(blob[:NONCE_BYTES], blob[NONCE_BYTES:], None).decode("utf-8")
    except VaultError:
        raise
    except Exception as error:  # InvalidTag: wrong key or tampered value
        raise VaultError("Cannot decrypt: wrong RUNNER_SECRET_KEY or damaged value") from error


def main():
    parser = argparse.ArgumentParser(description="AES-256-GCM vault for runner secrets")
    parser.add_argument("--new-key", action="store_true", help="print a new base64 key for RUNNER_SECRET_KEY")
    parser.add_argument("--encrypt", action="store_true", help="encrypt a value read from stdin (or a hidden prompt)")
    args = parser.parse_args()
    if args.new_key:
        print(new_key())
        return 0
    if args.encrypt:
        plain = sys.stdin.readline().rstrip("\r\n") if not sys.stdin.isatty() else getpass.getpass("Value: ")
        try:
            print(encrypt(plain))
        except VaultError as error:
            print("error: " + str(error), file=sys.stderr)
            return 1
        return 0
    parser.print_help()
    return 0


if __name__ == "__main__":
    sys.exit(main())
