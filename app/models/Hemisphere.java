package models;

import org.jetbrains.annotations.Nullable;

public enum Hemisphere {
    NORTHERN("northern"),
    SOUTHERN("southern");

    private final String key;

    Hemisphere(String key) {
        this.key = key;
    }

    public static Hemisphere of(@Nullable String key) {
        if (key == null) {
            return NORTHERN;
        }
        for (Hemisphere hemisphere : values()) {
            if (hemisphere.key.equals(key)) {
                return hemisphere;
            }
        }
        return NORTHERN;
    }

    public String getKey() {
        return key;
    }
}
