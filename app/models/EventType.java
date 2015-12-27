package models;

public enum EventType {

    LUNARECLIPSE("lunareclipse"), SOLARECLIPSE("solareclipse"), MOONLANDING("moonlanding");

    private final String key;

    EventType(String key) {
        this.key = key;
    }

    public static EventType read(String key) {
        for (EventType moonPhaseType : values()) {
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
