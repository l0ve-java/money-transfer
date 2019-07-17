package org.syuzhakov.moneytranfer.error;

public class Require {
    public static void notNull(Object target, String targetDescription) {
        if (target == null) {
            throw new ValidationException(String.format("Validation error: '%s' cannot be null", targetDescription));
        }
    }
}
