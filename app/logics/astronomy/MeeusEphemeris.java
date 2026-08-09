package logics.astronomy;

import jakarta.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;

@Singleton
public class MeeusEphemeris implements Ephemeris {

    private static final double DEG = Math.PI / 180.0;

    static double julianDay(Instant t) {
        return t.toEpochMilli() / 86400000.0 + 2440587.5;
    }

    private static double norm360(double x) {
        return ((x % 360) + 360) % 360;
    }

    /** {coeff(1e-6 deg), D, M, M', F} — principal longitude terms. */
    private static final double[][] LON = {
            {6288774, 0, 0, 1, 0}, {1274027, 2, 0, -1, 0}, {658314, 2, 0, 0, 0},
            {213618, 0, 0, 2, 0}, {-185116, 0, 1, 0, 0}, {-114332, 0, 0, 0, 2},
            {58793, 2, 0, -2, 0}, {57066, 2, -1, -1, 0}, {53322, 2, 0, 1, 0},
            {45758, 2, -1, 0, 0}, {-40923, 0, 1, -1, 0}, {-34720, 1, 0, 0, 0},
            {-30383, 0, 1, 1, 0}, {15327, 2, 0, 0, -2}, {-12528, 0, 0, 1, 2},
            {10980, 0, 0, 1, -2}, {10675, 4, 0, -1, 0}, {10034, 0, 0, 3, 0},
            {8548, 4, 0, -2, 0}, {-7888, 2, 1, -1, 0}, {-6766, 2, 1, 0, 0},
            {-5163, 0, 0, 1, -2}, {4987, 1, 1, 0, 0}, {4036, 2, -1, 1, 0},
            {3994, 2, 0, 2, 0}, {3861, 4, 0, 0, 0}, {3665, 2, 0, -3, 0},
            {-2689, 0, 1, -2, 0}, {2602, 2, 0, -1, 2}, {2390, 2, -1, -2, 0},
            {-2348, 1, 0, 1, 0}, {2236, 2, -2, 0, 0}, {-2120, 0, 1, 2, 0},
            {-2069, 0, 2, 0, 0}, {2048, 2, -2, -1, 0}, {-1773, 2, 0, 1, -2},
            {-1595, 2, 0, 0, 2}, {1215, 4, -1, -1, 0}, {-1110, 0, 0, 2, 2},
            {-892, 3, 0, -1, 0}, {-810, 2, 1, 1, 0}, {759, 4, -1, -2, 0},
            {-713, 0, 2, -1, 0}, {-700, 2, 2, -1, 0}, {644, 4, -1, 1, 0},
            {-598, 2, -1, 0, 2}, {-566, 2, 0, 0, -2}, {-529, 4, 0, 1, 0},
            {-502, 3, 0, -2, 0}, {-439, 0, 0, 3, 2}, {-423, 2, 1, -1, 2},
            {-383, 1, 1, -1, 0}, {331, 4, 0, -3, 0}, {-317, 2, 1, 1, -2},
            {299, 2, -1, 2, 0}, {294, 2, 0, 3, 0}
    };

    /** {coeff(1e-6 deg), D, M, M', F} — principal latitude terms. */
    private static final double[][] LAT = {
            {5128122, 0, 0, 0, 1}, {280602, 0, 0, 1, 1}, {277693, 0, 0, 1, -1},
            {173237, 2, 0, 0, -1}, {55413, 2, 0, -1, 1}, {46271, 2, 0, -1, -1},
            {32573, 2, 0, 0, 1}, {17198, 0, 0, 2, 1}, {9266, 2, 0, 1, -1},
            {8822, 0, 0, 2, -1}, {8216, 2, -1, 0, -1}, {4324, 2, 0, -2, -1},
            {4200, 2, 0, 1, 1}, {-3359, 2, 1, 0, -1}, {2463, 2, -1, -1, 1},
            {2211, 2, -1, 0, 1}, {2065, 2, -1, -1, -1}, {-1870, 0, 1, -1, -1},
            {1828, 4, 0, -1, -1}, {-1794, 0, 1, 0, 1}, {-1749, 0, 0, 0, 3},
            {-1565, 0, 1, -1, 1}, {-1491, 1, 0, 0, 1}, {-1475, 0, 1, 1, 1},
            {-1410, 0, 1, 1, -1}, {-1344, 0, 1, 0, -1}, {-1335, 1, 0, 0, -1},
            {1107, 0, 0, 3, 1}, {1021, 4, 0, 0, -1}, {833, 4, 0, -1, 1}
    };

    /** {coeff(1e-3 km), D, M, M', F} — principal distance terms (added to 385000.56 km). */
    private static final double[][] DIST = {
            {-20905355, 0, 0, 1, 0}, {-3699111, 2, 0, -1, 0}, {-2955968, 2, 0, 0, 0},
            {-569925, 0, 0, 2, 0}, {48888, 0, 1, 0, 0}, {-3149, 0, 0, 0, 2},
            {246158, 2, 0, -2, 0}, {-152138, 2, -1, -1, 0}, {-170733, 2, 0, 1, 0},
            {-204586, 2, -1, 0, 0}, {-129620, 0, 1, -1, 0}, {108743, 1, 0, 0, 0},
            {104755, 0, 1, 1, 0}, {10321, 2, 0, 0, -2}, {79661, 0, 0, 1, -2}
    };

    private double[] fundamentals(double jd) {
        double t = (jd - 2451545.0) / 36525.0;
        double Lp = 218.3164477 + 481267.88123421 * t - 0.0015786 * t * t + t * t * t / 538841 - t * t * t * t / 65194000;
        double D = 297.8501921 + 445267.1114034 * t - 0.0018819 * t * t + t * t * t / 545868 - t * t * t * t / 113065000;
        double M = 357.5291092 + 35999.0502909 * t - 0.0001536 * t * t + t * t * t / 24490000;
        double Mp = 134.9633964 + 477198.8675055 * t + 0.0087414 * t * t + t * t * t / 69699 - t * t * t * t / 14712000;
        double F = 93.272095 + 483202.0175233 * t - 0.0036539 * t * t - t * t * t / 3526000 + t * t * t * t / 863310000;
        return new double[]{Lp, D, M, Mp, F, t};
    }

    private double sumSeries(double[][] terms, double[] f, boolean sine) {
        double D = f[1], M = f[2], Mp = f[3], F = f[4];
        double t = f[5];
        // Solar-anomaly correction factor (eccentricity of Earth's orbit)
        double E = 1 - 0.002516 * t - 0.0000074 * t * t;
        double sum = 0;
        for (double[] term : terms) {
            double arg = (term[1] * D + term[2] * M + term[3] * Mp + term[4] * F) * DEG;
            double coeff = term[0];
            // Apply E correction for terms with |M| = 1 or 2
            int mAbs = (int) Math.abs(term[2]);
            if (mAbs == 1) coeff *= E;
            else if (mAbs == 2) coeff *= E * E;
            sum += coeff * (sine ? Math.sin(arg) : Math.cos(arg));
        }
        return sum;
    }

    // Additional additive terms for longitude (Venus, Jupiter, flattening) per Meeus ch.47
    private static double additionalLon(double[] f) {
        double t = f[5];
        double F = f[4];
        double A1 = 119.75 + 131.849 * t;          // Venus
        double A2 = 53.09 + 479264.290 * t;         // Jupiter
        return 3958 * Math.sin(A1 * DEG)
             + 1962 * Math.sin((f[0] - F) * DEG)   // Lp - F
             + 318 * Math.sin(A2 * DEG);
    }

    // Additional additive terms for latitude per Meeus ch.47
    private static double additionalLat(double[] f) {
        double t = f[5];
        double Mp = f[3], F = f[4], Lp = f[0];
        double A1 = 119.75 + 131.849 * t;
        double A3 = 313.45 + 481266.484 * t;
        return -2235 * Math.sin(Lp * DEG)
             +  382 * Math.sin(A3 * DEG)
             +  175 * Math.sin((A1 - F) * DEG)
             +  175 * Math.sin((A1 + F) * DEG)
             +  127 * Math.sin((Lp - Mp) * DEG)
             -  115 * Math.sin((Lp + Mp) * DEG);
    }

    @Override
    public double moonEclipticLongitude(Instant t) {
        double[] f = fundamentals(julianDay(t));
        double lon = f[0] + (sumSeries(LON, f, true) + additionalLon(f)) / 1_000_000.0;
        return norm360(lon);
    }

    @Override
    public double moonEclipticLatitude(Instant t) {
        double[] f = fundamentals(julianDay(t));
        return (sumSeries(LAT, f, true) + additionalLat(f)) / 1_000_000.0;
    }

    @Override
    public double earthMoonDistanceKm(Instant t) {
        double[] f = fundamentals(julianDay(t));
        return 385000.56 + sumSeries(DIST, f, false) / 1000.0;
    }

    @Override
    public double moonDeclination(Instant t) {
        double lon = moonEclipticLongitude(t) * DEG;
        double lat = moonEclipticLatitude(t) * DEG;
        double jd = julianDay(t);
        double tc = (jd - 2451545.0) / 36525.0;
        double eps = (23.4392911 - 0.0130042 * tc) * DEG; // obliquity of the ecliptic
        double dec = Math.asin(Math.sin(lat) * Math.cos(eps) + Math.cos(lat) * Math.sin(eps) * Math.sin(lon));
        return dec / DEG;
    }

    /** Lahiri ayanamsha in degrees: linear precession anchored at J2000.0 (~50.29"/yr). */
    private static double lahiriAyanamshaDeg(double julianDay) {
        return 23.853 + 0.0139697 * (julianDay - 2451545.0) / 365.25;
    }

    @Override
    public double moonSiderealLongitude(Instant t) {
        return norm360(moonEclipticLongitude(t) - lahiriAyanamshaDeg(julianDay(t)));
    }

    private static final Duration STEP = Duration.ofHours(1);

    private static int signIndex(double siderealLongitudeDeg) {
        return (int) Math.floor(norm360(siderealLongitudeDeg) / 30.0);
    }

    @Override
    public Instant nextSignBoundaryCrossing(Instant from, Instant until) {
        int startSign = signIndex(moonSiderealLongitude(from));
        Instant lo = from;
        for (Instant hi = from.plus(STEP); !hi.isAfter(until); hi = hi.plus(STEP)) {
            if (signIndex(moonSiderealLongitude(hi)) != startSign) {
                return refine(lo, hi, t -> signIndex(moonSiderealLongitude(t)) != startSign);
            }
            lo = hi;
        }
        return null;
    }

    @Override
    public Instant nextNodeCrossing(Instant from, Instant until) {
        double prev = moonEclipticLatitude(from);
        Instant lo = from;
        for (Instant hi = from.plus(STEP); !hi.isAfter(until); hi = hi.plus(STEP)) {
            double cur = moonEclipticLatitude(hi);
            if (prev != 0 && Math.signum(cur) != Math.signum(prev)) {
                final double latAtLo = prev; // sign at lo
                return refine(lo, hi, t -> Math.signum(moonEclipticLatitude(t)) != Math.signum(latAtLo));
            }
            prev = cur;
            lo = hi;
        }
        return null;
    }

    @Override
    public Apsis nextApsis(Instant from, Instant until) {
        Extreme e = nextExtreme(from, until, this::earthMoonDistanceKm);
        return e == null ? null : new Apsis(e.when(), e.minimum()); // perigee = closest = minimum distance
    }

    @Override
    public Instant nextDeclinationExtreme(Instant from, Instant until) {
        Extreme e = nextExtreme(from, until, this::moonDeclination);
        return e == null ? null : e.when();
    }

    private record Extreme(Instant when, boolean minimum) {}

    /** Next local extremum of {@code series} in (from, until], refined to ~1 minute, or null. */
    private Extreme nextExtreme(Instant from, Instant until, java.util.function.Function<Instant, Double> series) {
        double curr = series.apply(from.plus(STEP));
        double prevSlope = curr - series.apply(from);
        Instant lo = from.plus(STEP);
        for (Instant hi = lo.plus(STEP); !hi.isAfter(until); hi = hi.plus(STEP)) {
            double prev = curr;
            curr = series.apply(hi);
            double slope = curr - prev;
            if (Math.signum(slope) != Math.signum(prevSlope)) {
                boolean minimum = prevSlope < 0; // value was decreasing, now increasing -> minimum
                // Bisect [lo, hi] on the slope sign until the bracket is under a minute wide.
                final Duration DELTA = Duration.ofMinutes(1);
                Instant elo = lo, ehi = hi;
                while (Duration.between(elo, ehi).toSeconds() > 60) {
                    Instant mid = midpoint(elo, ehi);
                    double slopeMid = series.apply(mid) - series.apply(mid.minus(DELTA));
                    if (Math.signum(slopeMid) == Math.signum(prevSlope)) {
                        elo = mid;
                    } else {
                        ehi = mid;
                    }
                }
                return new Extreme(midpoint(elo, ehi), minimum);
            }
            prevSlope = slope;
            lo = hi;
        }
        return null;
    }

    private Instant refine(Instant lo, Instant hi, Predicate<Instant> hiCondition) {
        // bisection: invariant lo=false, hi=true, until <1 min apart
        while (Duration.between(lo, hi).toSeconds() > 60) {
            Instant mid = midpoint(lo, hi);
            if (hiCondition.test(mid)) {
                hi = mid;
            } else {
                lo = mid;
            }
        }
        return hi;
    }

    private static Instant midpoint(Instant lo, Instant hi) {
        return lo.plus(Duration.between(lo, hi).dividedBy(2));
    }
}
