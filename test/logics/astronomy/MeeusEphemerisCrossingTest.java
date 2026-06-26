package logics.astronomy;

import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class MeeusEphemerisCrossingTest {
    private final MeeusEphemeris e = new MeeusEphemeris();

    private static Instant utc(int y, int mo, int d) {
        return ZonedDateTime.of(y, mo, d, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
    }

    private static int signIndex(double siderealLongitudeDeg) {
        return (int) Math.floor((((siderealLongitudeDeg % 360) + 360) % 360) / 30.0);
    }

    @Test
    void signBoundaryWithinAFewDays() {
        Instant from = utc(2025, 6, 1);
        Instant until = from.plus(Duration.ofDays(5));
        Instant crossing = e.nextSignBoundaryCrossing(from, until);
        assertNotNull(crossing);
        // at the crossing the sign index changes within a minute
        int before = signIndex(e.moonSiderealLongitude(crossing.minusSeconds(60)));
        int after  = signIndex(e.moonSiderealLongitude(crossing.plusSeconds(60)));
        assertNotEquals(before, after);
    }

    @Test
    void nodeCrossingHasZeroLatitude() {
        Instant from = utc(2025, 1, 1);
        Instant until = from.plus(Duration.ofDays(16)); // at least one node within half a draconic month
        Instant nc = e.nextNodeCrossing(from, until);
        assertNotNull(nc);
        assertEquals(0.0, e.moonEclipticLatitude(nc), 0.05);
    }

    @Test
    void apsisIsDistanceExtremum() {
        Instant from = utc(2025, 1, 1);
        Instant until = from.plus(Duration.ofDays(16)); // at least one perigee/apogee within half an anomalistic month
        Ephemeris.Apsis ap = e.nextApsis(from, until);
        assertNotNull(ap);
        double at = e.earthMoonDistanceKm(ap.when());
        double before = e.earthMoonDistanceKm(ap.when().minus(Duration.ofHours(6)));
        double after = e.earthMoonDistanceKm(ap.when().plus(Duration.ofHours(6)));
        if (ap.perigee()) {
            assertTrue(at <= before && at <= after);
        } else {
            assertTrue(at >= before && at >= after);
        }
    }
}
