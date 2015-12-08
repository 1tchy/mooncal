package logics;

import org.jetbrains.annotations.Nullable;

import java.time.*;

public class SunMoonCalculatorWrapper {

	private final LocalDate date;
	private final ZoneId offset;
	private final double obsLon;
	private final double obsLat;
	private final SunMoonCalculator calculationAtNoon;
	private final SunMoonCalculator calculationAtStartOfDay;
	private final SunMoonCalculator calculationAtEndOfDay;

	public SunMoonCalculatorWrapper(LocalDate date, ZoneId offset, double obsLon, double obsLat) {
		ZonedDateTime startOfDay = ZonedDateTime.of(date, LocalTime.MIN, offset).withZoneSameInstant(ZoneOffset.UTC);
		ZonedDateTime endOfDay = ZonedDateTime.of(date, LocalTime.MAX, offset).withZoneSameInstant(ZoneOffset.UTC);
		ZonedDateTime noon = ZonedDateTime.of(date, LocalTime.NOON, offset).withZoneSameInstant(ZoneOffset.UTC);
		this.date = date;
		this.offset = offset;
		this.obsLon = obsLon;
		this.obsLat = obsLat;
		calculationAtStartOfDay = new SunMoonCalculator(startOfDay.getYear(), startOfDay.getMonthValue(), startOfDay.getDayOfMonth(), startOfDay.getHour(), startOfDay.getMinute(), startOfDay.getSecond(), obsLon, obsLat);
		calculationAtStartOfDay.calcSunAndMoon();
		calculationAtNoon = new SunMoonCalculator(noon.getYear(), noon.getMonthValue(), noon.getDayOfMonth(), noon.getHour(), noon.getMinute(), noon.getSecond(), obsLon, obsLat);
		calculationAtNoon.calcSunAndMoon();
		calculationAtEndOfDay = new SunMoonCalculator(endOfDay.getYear(), endOfDay.getMonthValue(), endOfDay.getDayOfMonth(), endOfDay.getHour(), endOfDay.getMinute(), endOfDay.getSecond(), obsLon, obsLat);
		calculationAtEndOfDay.calcSunAndMoon();
	}

	@Nullable
	public LocalTime getTimeOf(MoonPhase phase) {
		return phase.getTimeByMoonAge(calculationAtStartOfDay.moonAge,calculationAtEndOfDay.moonAge);
	}

}

