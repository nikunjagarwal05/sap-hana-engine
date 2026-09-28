package com.sap.engine.core.exception;

/**
 * Standard runtime exception thrown when SAP connector operations fail.
 */
public class ConnectorException extends RuntimeException {
    public ConnectorException(String message) {
        super(message);
    }

    public ConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
