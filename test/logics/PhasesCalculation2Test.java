package logics;

import org.junit.Ignore;
import org.junit.Test;

import java.time.LocalDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Ignore
public class PhasesCalculation2Test {

	@Test
	public void testGetNewmoonBeforeWithRecentDate() {
		final LocalDate before = LocalDate.of(2015, 11, 30);
		final LocalDate lastNewMoon = PhasesCalculation2.getNewmoonBefore(before);
		assertThat(lastNewMoon, is(LocalDate.of(2015, 11, 11)));
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

}
