package logics.calculation;

import play.i18n.Messages;

import java.time.Duration;

public enum MoonPhase {


	NEWMOON("phases.new"), FIRST_QUARTER("phases.quarter.first"), FULLMOON("phases.full"), LAST_QUARTER("phases.quarter.last");

	public static final double MOON_CYCLE_DAYS = 29.530588853;
	public static final Duration MOON_CYCLE = Duration.ofDays(29).plusHours(12).plusMinutes(44).plusSeconds(2).plusMillis(803);

	private final String name;

	MoonPhase(String name) {
		this.name = name;
	}

	public String getName() {
		return Messages.get(name);
	}
}
