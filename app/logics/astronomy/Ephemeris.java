package logics.astronomy;

import java.time.Instant;

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

    record Apsis(Instant when, boolean perigee) {}
}
