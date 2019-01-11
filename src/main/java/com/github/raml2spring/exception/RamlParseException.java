package com.github.raml2spring.exception;

public class RamlParseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RamlParseException(String message) {
        super(message);
    }

    public RamlParseException(String message, Throwable cause) {
        super(message, cause);
    }

}
