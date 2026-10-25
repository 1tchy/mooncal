package models;

public enum MoonPhaseType {

    NEWMOON("new"), QUARTER("quarter"), FULLMOON("full"), DAILY("daily"),
    /** Daily sunrise and sunset, which need a location. */
    SUN("sun"),
    /** Daily high and low tides at the nearest tide station, which need a location. */
    TIDE("tide");

    private final String key;

    MoonPhaseType(String key) {
        this.key = key;
    }

    public static MoonPhaseType read(String key) {
        for (MoonPhaseType moonPhaseType : values()) {
            if (moonPhaseType.key.equals(key)) {
                return moonPhaseType;
            }
        }
        return null;
    }

    public String getKey() {
        return key;
    }
}
