package org.syuzhakov.moneytranfer.error;

public class OperationImpossibleException extends ExpectedException {

    public OperationImpossibleException(String message) {
        super(message, new ErrorResponse(500, 500010, message));
    }

    public OperationImpossibleException(String message, Throwable cause) {
        super(message, cause, new ErrorResponse(500, 500010, message));
    }

    public OperationImpossibleException(Throwable cause) {
        super(cause, new ErrorResponse(500, 500010, cause.getMessage()));
    }
}
