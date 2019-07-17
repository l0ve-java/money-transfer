package org.syuzhakov.moneytranfer.error;

public class UnexpectedException extends ExpectedException {
    public UnexpectedException(String message) {
        super(message, new ErrorResponse(500, 500001, message));
    }

    public UnexpectedException(String message, Throwable cause) {
        super(message, cause, new ErrorResponse(500, 500001, message));
    }

    public UnexpectedException(Throwable cause) {
        super(cause, new ErrorResponse(500, 500001, cause.getMessage()));
    }
}
