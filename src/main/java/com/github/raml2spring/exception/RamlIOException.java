package com.github.raml2spring.exception;

public class RamlIOException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RamlIOException(String message) {
        super(message);
    }

    public RamlIOException(String message, Throwable cause) {
        super(message, cause);
    }

}
