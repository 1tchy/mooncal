package logics;

import java.time.Duration;

public enum MoonPhase {


	NEWMOON("Neumond"), FIRST_QUARTER("Zunehmender Halbmond"), FULLMOON("Vollmond"), LAST_QUARTER("Abnehmender Halbmond");

	public static final double MOON_CYCLE_DAYS = 29.530588853;
	public static final Duration MOON_CYCLE = Duration.ofDays(29).plusHours(12).plusMinutes(44).plusSeconds(2).plusMillis(803);

	private final String name;

	MoonPhase(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
