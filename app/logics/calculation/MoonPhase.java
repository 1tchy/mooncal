package logics.calculation;

import play.i18n.Lang;
import play.i18n.MessagesApi;

import java.time.ZonedDateTime;

public enum MoonPhase {

    NEWMOON("phases.new", "🌑"),
    FIRST_QUARTER("phases.quarter.first", "🌓"),
    FULLMOON("phases.full", "🌕") {
        @Override
        public String getTitle(MessagesApi messagesApi, Lang lang, ZonedDateTime date) {
            int month = date.getMonthValue();
            if (isBlueMoon(date)) {
                month = 13;
            }
            return super.getTitle(messagesApi, lang, date) + " (" + messagesApi.get(lang, "phases.full." + month) + ")";
        }

        private static boolean isBlueMoon(ZonedDateTime date) {
            ZonedDateTime previousMoon = date.minusSeconds((long) (MOON_CYCLE_DAYS * 24 * 3600));
            return previousMoon.getMonth() == date.getMonth();
        }
    },
    LAST_QUARTER("phases.quarter.last", "🌗");

    public static final double MOON_CYCLE_DAYS = 29.530588853;

    private final String name;
    private final String emoticon;

    MoonPhase(String name, String emoticon) {
        this.name = name;
        this.emoticon = emoticon;
    }

    public String getSimpleName(MessagesApi messagesApi, Lang lang) {
        return messagesApi.get(lang, name);
    }

    public String getTitle(MessagesApi messagesApi, Lang lang, ZonedDateTime date) {
        return getSimpleName(messagesApi, lang);
    }

    public String getEmoticon() {
        return emoticon;
    }
}
