package logics;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;

public class SunMoonCalculationWrapperTest {

	@Test
	public void testGetNewmoonOnNormalDay() {
		final LocalDate before = LocalDate.of(2015, 11, 30);
		SunMoonCalculatorWrapper cut = new SunMoonCalculatorWrapper(before, ZoneOffset.UTC, 0, 0);
		assertNull(cut.getTimeOf(MoonPhase.NEWMOON));
	}

	@Test
	public void testGetNewmoonOnNewmoon() {
		final LocalDate before = LocalDate.of(2015, 11, 11);
		SunMoonCalculatorWrapper cut = new SunMoonCalculatorWrapper(before, ZoneOffset.UTC, 0, 0);
		assertThat(cut.getTimeOf(MoonPhase.NEWMOON), is(LocalTime.of(17, 47, 30)));
	}

	@Test
	public void testGetNewmoonBeforeWithNewMoonDate() {
		final LocalDate before = LocalDate.of(2015, 11, 11);
		final LocalDate lastNewMoon = PhasesCalculation2.getNewmoonBefore(before);
		assertThat(lastNewMoon, is(LocalDate.of(2015, 11, 11)));
	}

	@Test
	public void testGetNewmoonBeforeWithDayBeforeNextNewmoonDate() {
		final LocalDate before = LocalDate.of(2015, 12, 10);
		final LocalDate lastNewMoon = PhasesCalculation2.getNewmoonBefore(before);
		assertThat(lastNewMoon, is(LocalDate.of(2015, 11, 11)));
	}

	@Test
	public void t() {
		final SunMoonCalculator sunMoonCalculator = new SunMoonCalculator(2015, 11, 11, 17, 18, 19, 0, 0);
		sunMoonCalculator.calcSunAndMoon();
		assertThat(sunMoonCalculator.moonAge, is(0));
	}

}
