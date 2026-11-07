package com.taf.systems.zakaadash.exceptions;

/** Required preprocessing rule is not exposed in the live UI. */
public class ProductCapabilityBlockedException extends RuntimeException {

    public ProductCapabilityBlockedException(String message) {
        super(message);
    }
}
