package org.syuzhakov.moneytranfer.model;

public enum AccountStatus {
    ACTIVE(0), BLOCKED(1);
    private int value;

    AccountStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static AccountStatus fromValue(int value) {
        switch (value) {
            case 0:
                return ACTIVE;
            case 1:
                return BLOCKED;
            default:
                return null;
        }
    }
}
