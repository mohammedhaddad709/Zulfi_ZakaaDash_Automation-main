# Automation Control Center (Zulfi + ZakaaDash TAF runner) - spec section 16.
# Build from the project root:  docker compose -f runner/docker-compose.yml up -d --build
FROM maven:3.9-eclipse-temurin-21

ENV DEBIAN_FRONTEND=noninteractive \
    PYTHONUNBUFFERED=1 \
    RUNNER_HOST=0.0.0.0 \
    RUNNER_PORT=8080 \
    RUNNER_EXECUTION=LocalHeadless \
    MVN=mvn

# Google Chrome (headless runs), Python 3, git (Claude request copies), Node.js (Claude Code CLI)
RUN apt-get update \
 && apt-get install -y --no-install-recommends wget gnupg ca-certificates python3 python3-pip python3-venv git curl fonts-noto-core fonts-noto-color-emoji \
 && wget -q -O /tmp/chrome.deb https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
 && apt-get install -y --no-install-recommends /tmp/chrome.deb \
 && rm /tmp/chrome.deb \
 && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
 && apt-get install -y --no-install-recommends nodejs \
 && npm install -g @anthropic-ai/claude-code \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY runner/requirements.txt runner/requirements.txt
RUN python3 -m venv /opt/runner-venv \
 && /opt/runner-venv/bin/pip install --no-cache-dir -r runner/requirements.txt
ENV PATH="/opt/runner-venv/bin:${PATH}"

# Dependencies first (cached layer), then the sources; pre-compile so the first run starts fast.
COPY pom.xml pom.xml
RUN mvn -B -q dependency:go-offline || true
COPY src src
COPY suites suites
COPY tools/*.py tools/
COPY config/credentials.example.properties config/credentials.example.properties
COPY runner runner
RUN mvn -B -q test-compile

EXPOSE 8080
CMD ["python3", "runner/server.py"]
