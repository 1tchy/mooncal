package logics.astronomy;

import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class MeeusEphemerisTest {
    private final MeeusEphemeris e = new MeeusEphemeris();

    private static Instant utc(int y, int mo, int d, int h) {
        return ZonedDateTime.of(y, mo, d, h, 0, 0, 0, ZoneOffset.UTC).toInstant();
    }

    @Test
    void longitudeIsNormalized() {
        for (int day = 0; day < 60; day++) {
            double lon = e.moonEclipticLongitude(utc(2025, 1, 1, 0).plus(Duration.ofDays(day)));
            assertTrue(lon >= 0 && lon < 360, "lon out of range: " + lon);
        }
    }

    @Test
    void moonCompletesRoughlyOneZodiacPerMonth() {
        // ~13.2°/day mean motion; over 1 day longitude advances 11..15 deg (mod 360)
        double l0 = e.moonEclipticLongitude(utc(2025, 6, 1, 0));
        double l1 = e.moonEclipticLongitude(utc(2025, 6, 2, 0));
        double delta = ((l1 - l0) % 360 + 360) % 360;
        assertTrue(delta > 10 && delta < 16, "daily motion: " + delta);
    }

    @Test
    void declinationWithinObliquityPlusLatitude() {
        for (int day = 0; day < 30; day++) {
            double dec = e.moonDeclination(utc(2025, 1, 1, 0).plus(Duration.ofDays(day)));
            assertTrue(Math.abs(dec) < 29, "declination too large: " + dec);
        }
    }

    @Test
    void anchorLongitudeApril2015() {
        double lon = e.moonEclipticLongitude(utc(2015, 4, 4, 12));
        assertEquals(194.5, lon, 1.5); // tighten against NASA Horizons during implementation
    }

    @Test
    void siderealIsTropicalMinusAyanamsha() {
        Instant t = utc(2025, 1, 1, 0);
        double trop = e.moonEclipticLongitude(t);
        double jd = toJd(t);
        // Lahiri formula: 23.853 + 0.0139697 * (jd - J2000) / 365.25
        double lahiri = 23.853 + 0.0139697 * (jd - 2451545.0) / 365.25;
        double sid = e.moonSiderealLongitude(t);
        double expected = ((trop - lahiri) % 360 + 360) % 360;
        assertEquals(expected, sid, 1e-6);
    }

    private static double toJd(Instant t) {
        return t.toEpochMilli() / 86400000.0 + 2440587.5;
    }
}
