package com.taf.systems.zakaadash.exceptions;

/** Navigation succeeded but application output cannot be read comprehensively for oracle comparison. */
public class DataCorrectnessBlockedException extends RuntimeException {

    public DataCorrectnessBlockedException(String message) {
        super(message);
    }

    public DataCorrectnessBlockedException(String message, Throwable cause) {
        super(message, cause);
    }
}
