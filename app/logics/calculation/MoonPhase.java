package logics.calculation;

import play.i18n.Lang;
import play.i18n.Messages;

public enum MoonPhase {


    NEWMOON("phases.new"), FIRST_QUARTER("phases.quarter.first"), FULLMOON("phases.full"), LAST_QUARTER("phases.quarter.last");

    public static final double MOON_CYCLE_DAYS = 29.530588853;

    private final String name;

    MoonPhase(String name) {
        this.name = name;
    }

    public String getName(Lang lang) {
        return Messages.get(lang, name);
    }
}