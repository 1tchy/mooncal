package models;

public enum EventStyle {
    ICON_ONLY("iconOnly"),
    FULLMOON("fullmoon"),
    WITH_DESCRIPTION("withDescription");

    private final String style;

    EventStyle(String style) {
        this.style = style;
    }

    public static EventStyle of(String style) {
        for (EventStyle eventStyle : values()) {
            if (eventStyle.style.equals(style)) {
                return eventStyle;
            }
        }
        throw new IllegalArgumentException("Unknown style: " + style);
    }

    public String getStyle() {
        return style;
    }
}
