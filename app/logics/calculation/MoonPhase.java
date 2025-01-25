package logics.calculation;

import models.EventStyle;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import java.time.ZonedDateTime;

public enum MoonPhase {

    NEWMOON("phases.new", "🌑"),
    FIRST_QUARTER("phases.quarter.first", "🌓"),
    FULLMOON("phases.full", "🌕") {
        @Override
        public String getTitle(MessagesApi messagesApi, Lang lang, ZonedDateTime date, EventStyle style) {
            if (style == EventStyle.WITH_DESCRIPTION) {
                int month = date.getMonthValue();
                if (isBlueMoon(date)) {
                    month = 13;
                }
                String fullMoonName = messagesApi.get(lang, "phases.full." + month);
                return super.getTitle(messagesApi, lang, date, style) + (fullMoonName.isEmpty() ? "" : " (" + fullMoonName + ")");
            } else {
                return super.getTitle(messagesApi, lang, date, style);
            }
        }

        @Override
        public String getPdfTitle(MessagesApi messagesApi, Lang lang, ZonedDateTime date, EventStyle style) {
            if (style == EventStyle.WITH_DESCRIPTION) {
                int month = date.getMonthValue();
                if (isBlueMoon(date)) {
                    month = 13;
                }
                String fullMoonName = messagesApi.get(lang, "phases.full." + month);
                if (!fullMoonName.isEmpty()) {
                    return fullMoonName;
                }
            }
            return super.getPdfTitle(messagesApi, lang, date, style);
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

    public String getTitle(MessagesApi messagesApi, Lang lang, ZonedDateTime date, EventStyle style) {
        if (style == EventStyle.ICON_ONLY) {
            return emoticon;
        }
        return emoticon + " " + getSimpleName(messagesApi, lang);
    }

    public String getPdfTitle(MessagesApi messagesApi, Lang lang, ZonedDateTime date, EventStyle style) {
        if (style == EventStyle.ICON_ONLY) {
            return "";
        }
        return getSimpleName(messagesApi, lang);
    }
}
