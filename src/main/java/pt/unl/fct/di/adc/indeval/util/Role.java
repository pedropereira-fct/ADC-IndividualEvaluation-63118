package pt.unl.fct.di.adc.indeval.util;

public enum Role {

    USER (0),
    BOFFICER (1),
    ADMIN (2);

    public final int level;

    Role(int level) {
        this.level = level;
    }

    public static Role fromString(String role) {
        try {
            return Role.valueOf(role);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid role.");
        }
    }
}