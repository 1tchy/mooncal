package logics;

import org.junit.Ignore;
import org.junit.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Ignore
public class SunCalcTest {

	@Test
	public void testCalculationOfJulianCreation() {
		assertThat(SunCalc.toJulian(ZonedDateTime.of(1970, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC)), is(SunCalc.J1970));
	}
}
