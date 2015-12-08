package logics;

import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

@Ignore
public class PhasesCalculation1Test {

	@Test
	public void testGetNewmoonBeforeWithRecentDate() {
//		PhasesCalculation cut = new PhasesCalculation(true, true, true, true, ZonedDateTime.of(2015, 12, 31, 0, 0, 0, 0, ZoneOffset.ofHours(1), ZonedDateTime.now()));
		final ZonedDateTime before = ZonedDateTime.of(2015, 12, 31, 0, 0, 0, 0, ZoneOffset.ofHours(1));
		final ZonedDateTime actualNewmoon = PhasesCalculation1.getNewmoonBefore(before);
		assertThat(actualNewmoon, is(ZonedDateTime.of(2015, 12, 11, 10, 29, 59, 0, ZoneOffset.UTC)));
	}

	@Test
	public void testGetNewmoonBeforeWithAncientDate() {
		final ZonedDateTime before = ZonedDateTime.of(1900, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(1));
		final ZonedDateTime actualNewmoon = PhasesCalculation1.getNewmoonBefore(before);
		assertThat(actualNewmoon, is(ZonedDateTime.of(1899, 12, 3, 0, 46, 3, 0, ZoneOffset.UTC)));
	}

	@Test
	public void testGetNewmoonBeforeWithAncientDate2() {
		final ZonedDateTime before = ZonedDateTime.of(1970, 1, 9, 0, 0, 0, 0, ZoneOffset.ofHours(1));
		final ZonedDateTime actualNewmoon = PhasesCalculation1.getNewmoonBefore(before);
		final Duration offset = Duration.between(actualNewmoon, ZonedDateTime.of(1970, 1, 7, 20, 35, 0, 0, ZoneOffset.UTC));
		assertThat("offset (" + offset + ") in seconds to actualNewmoon " + actualNewmoon, offset.abs().toNanos() / 1000, is(lessThan(60L)));
	}

	@Test
	public void testGetNewmoonBeforeWithAncientDate3() {
		final ZonedDateTime before = ZonedDateTime.of(1999, 1, 20, 0, 0, 0, 0, ZoneOffset.ofHours(1));
		final ZonedDateTime actualNewmoon = PhasesCalculation1.getNewmoonBefore(before);
		final Duration offset = Duration.between(actualNewmoon, ZonedDateTime.of(1999, 1, 17, 15, 46, 0, 0, ZoneOffset.UTC));
		assertThat("offset (" + offset + ") in seconds to actualNewmoon " + actualNewmoon, offset.abs().toNanos() / 1000, is(lessThan(60L)));
	}
}
