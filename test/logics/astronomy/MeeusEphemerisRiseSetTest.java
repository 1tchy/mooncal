package logics.astronomy;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reference times from the US Naval Observatory (https://aa.usno.navy.mil/data/RS_OneDay), in UTC.
 */
class MeeusEphemerisRiseSetTest {
    private static final double ZURICH_LAT = 47.38, ZURICH_LON = 8.54;
    private static final double TROMSO_LAT = 69.65, TROMSO_LON = 18.96;
    private static final double SYDNEY_LAT = -33.87, SYDNEY_LON = 151.21;
    private static final long TOLERANCE_SECONDS = 60;

    private final MeeusEphemeris e = new MeeusEphemeris();

    private Ephemeris.RiseSet riseSetOnUtcDay(LocalDate day, double lat, double lon) {
        Instant start = day.atStartOfDay(ZoneOffset.UTC).toInstant();
        return e.moonRiseSet(start, start.plus(Duration.ofDays(1)), lat, lon);
    }

    private static void assertNear(LocalDate day, String expectedUtc, Optional<Instant> actual) {
        assertTrue(actual.isPresent(), "expected an event at " + expectedUtc);
        Instant expected = day.atTime(LocalTime.parse(expectedUtc)).toInstant(ZoneOffset.UTC);
        long diff = Math.abs(Duration.between(expected, actual.get()).toSeconds());
        assertTrue(diff <= TOLERANCE_SECONDS, "expected " + expected + " but was " + actual);
    }

    @Test
    void zurichRiseAndSet() {
        LocalDate day = LocalDate.of(2026, 1, 15);
        Ephemeris.RiseSet actual = riseSetOnUtcDay(day, ZURICH_LAT, ZURICH_LON);
        assertNear(day, "04:57", actual.rise());
        assertNear(day, "12:31", actual.set());
    }

    @Test
    void zurichSummerRiseShortlyAfterMidnight() {
        LocalDate day = LocalDate.of(2025, 6, 21);
        Ephemeris.RiseSet actual = riseSetOnUtcDay(day, ZURICH_LAT, ZURICH_LON);
        assertNear(day, "00:10", actual.rise());
        assertNear(day, "15:03", actual.set());
    }

    @Test
    void zurichDayWithoutMoonrise() {
        LocalDate day = LocalDate.of(2026, 10, 5);
        Ephemeris.RiseSet actual = riseSetOnUtcDay(day, ZURICH_LAT, ZURICH_LON);
        assertTrue(actual.rise().isEmpty());
        assertNear(day, "14:48", actual.set());
    }

    @Test
    void sydneySetBeforeRise() {
        LocalDate day = LocalDate.of(2026, 3, 10);
        Ephemeris.RiseSet actual = riseSetOnUtcDay(day, SYDNEY_LAT, SYDNEY_LON);
        assertNear(day, "02:10", actual.set());
        assertNear(day, "11:53", actual.rise());
    }

    @Test
    void tromsoMoonAboveHorizonAllDay() {
        Ephemeris.RiseSet actual = riseSetOnUtcDay(LocalDate.of(2026, 1, 2), TROMSO_LAT, TROMSO_LON);
        assertTrue(actual.rise().isEmpty());
        assertTrue(actual.set().isEmpty());
        assertTrue(actual.upAtStart());
    }

    @Test
    void tromsoMoonBelowHorizonAllDay() {
        Ephemeris.RiseSet actual = riseSetOnUtcDay(LocalDate.of(2026, 1, 16), TROMSO_LAT, TROMSO_LON);
        assertTrue(actual.rise().isEmpty());
        assertTrue(actual.set().isEmpty());
        assertFalse(actual.upAtStart());
    }

    @Test
    void tromsoRiseAndSet() {
        LocalDate day = LocalDate.of(2026, 10, 5);
        Ephemeris.RiseSet actual = riseSetOnUtcDay(day, TROMSO_LAT, TROMSO_LON);
        assertNear(day, "17:13", actual.set());
        assertNear(day, "20:36", actual.rise());
    }
}
