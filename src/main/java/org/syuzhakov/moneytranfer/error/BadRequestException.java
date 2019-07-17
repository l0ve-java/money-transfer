package org.syuzhakov.moneytranfer.error;

public class BadRequestException extends ExpectedException {

    public BadRequestException(String message) {
        super(message, new ErrorResponse(400, 400002, message));
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause, new ErrorResponse(400, 400002, message));
    }

    public BadRequestException(Throwable cause) {
        super(cause, new ErrorResponse(400, 400002, cause.getMessage()));
    }
}
