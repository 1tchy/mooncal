package logics.astronomy;

import java.time.Instant;
import java.util.Optional;

public interface Ephemeris {
    double moonEclipticLongitude(Instant t);
    double moonEclipticLatitude(Instant t);
    double moonDeclination(Instant t);
    double moonSiderealLongitude(Instant t);
    double earthMoonDistanceKm(Instant t);

    Instant nextSignBoundaryCrossing(Instant from, Instant until);
    Instant nextNodeCrossing(Instant from, Instant until);
    Instant nextDeclinationExtreme(Instant from, Instant until);
    Apsis nextApsis(Instant from, Instant until);

    /** First moonrise and first moonset in [from, until); empty where there is none. */
    RiseSet moonRiseSet(Instant from, Instant until, double latitude, double longitude);

    record Apsis(Instant when, boolean perigee) {}

    /**
     * @param upAtStart whether the moon is above the horizon at the start of the interval, which tells
     *                  "up all the time" from "down all the time" when there is neither rise nor set
     */
    record RiseSet(Optional<Instant> rise, Optional<Instant> set, boolean upAtStart) {}
}
