package logics.calculation;

import play.i18n.Lang;
import play.i18n.MessagesApi;

public enum MoonPhase {

    NEWMOON("phases.new", "🌑"),
    FIRST_QUARTER("phases.quarter.first", "🌓"),
    FULLMOON("phases.full", "🌕"),
    LAST_QUARTER("phases.quarter.last", "🌗");

    public static final double MOON_CYCLE_DAYS = 29.530588853;

    private final String name;
    private final String emoticon;

    MoonPhase(String name, String emoticon) {
        this.name = name;
        this.emoticon = emoticon;
    }

    public String getName(MessagesApi messagesApi, Lang lang) {
        return messagesApi.get(lang, name);
    }

    public String getEmoticon() {
        return emoticon;
    }
}
