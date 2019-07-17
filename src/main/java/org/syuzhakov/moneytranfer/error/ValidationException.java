package org.syuzhakov.moneytranfer.error;

public class ValidationException extends ExpectedException {

    public ValidationException(String message) {
        super(message, new ErrorResponse(400, 400001, message));
    }
}
