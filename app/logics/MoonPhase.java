package logics;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalTime;

public enum MoonPhase {


	NEWMOON(0, "Neumond"), FIRST_QUARTER(1 / 4, "Zunehmender Halbmond"), FULLMOON(1 / 2, "Vollmond"), LAST_QUARTER(3 / 4, "Abnehmender Halbmond");

	public static final double MOON_CYCLE_DAYS = 29.530588853;
	private static final Duration MOON_CYCLE = Duration.ofDays(29).plusHours(12).plusMinutes(44).plusSeconds(2).plusMillis(803);

	private final double phaseStartingAge;
	private final String name;

	MoonPhase(double phaseStartingAgePercentage, String name) {
		this.name = name;
		this.phaseStartingAge = MOON_CYCLE_DAYS * phaseStartingAgePercentage;
	}

	@Nullable
	public LocalTime getTimeByMoonAge(double moonAgeAtStartOfDay, double moonAgeAtEndOfDay) {
		if (moonAgeAtEndOfDay > MOON_CYCLE_DAYS) {
			moonAgeAtEndOfDay -= MOON_CYCLE_DAYS;
		}
		if (moonAgeAtEndOfDay >= 0 + phaseStartingAge && moonAgeAtEndOfDay < 1 + phaseStartingAge) {
			return LocalTime.ofSecondOfDay((long) (24 * 60 * 60 * (1 - (moonAgeAtEndOfDay - phaseStartingAge))));
		}
		return null;
	}

	public String getName() {
		return name;
	}
}
