package logics.tides;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openwaters.slackwater.ConstituentAstronomyKt;
import io.openwaters.slackwater.TideExtreme;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.zip.GZIPInputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class TideStationsTest {

    private static final TideStations STATIONS = new TideStations();
    private static final Instant JAN_15 = Instant.parse("2026-01-15T00:00:00Z");
    private static final Instant JAN_16 = Instant.parse("2026-01-16T00:00:00Z");

    @Test
    void loadsAllExportedStations() {
        assertThat(STATIONS.size(), greaterThan(4000));
    }

    @Test
    void allConstituentsAreKnownToTheEngine() throws IOException {
        Instant time = Instant.parse("2026-01-01T00:00:00Z");
        try (InputStream in = new GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream(TideStations.RESOURCE)))) {
            Set<String> names = new TreeSet<>();
            new ObjectMapper().readTree(in).get("stations").forEach(station -> {
                if (station.has("constituents")) station.get("constituents").forEach(c -> names.add(c.get(0).asText()));
            });
            assertThat(names.size(), greaterThan(100));
            for (String name : names) {
                assertNotNull(ConstituentAstronomyKt.constituentAstronomy(name, time), name);
            }
        }
    }

    @Test
    void predictsSanFranciscoLikeNoaa() {
        // NOAA 9414290, 2026-01-15 GMT
        List<TideExtreme> extremes = station("noaa/9414290").prediction().extremes(JAN_15, JAN_16);
        assertExtremes(extremes, Duration.ofMinutes(5), "06:37 HIGH", "10:33 LOW", "16:28 HIGH", "23:47 LOW");
    }

    @Test
    void predictsSubordinateStationLikeNoaa() {
        // NOAA 8410875 Birch Islands (subordinate of 8410140), 2026-01-15 GMT
        List<TideExtreme> extremes = station("noaa/8410875").prediction().extremes(JAN_15, JAN_16);
        assertExtremes(extremes, Duration.ofMinutes(10), "01:59 HIGH", "08:22 LOW", "14:16 HIGH", "20:57 LOW");
    }

    @Test
    void nearestWithin50KmOfCuxhaven() {
        Optional<TideStations.StationDistance> nearest = STATIONS.nearestWithin(53.9, 8.7, 50);
        assertTrue(nearest.isPresent());
        assertThat(nearest.get().km(), lessThan(5.0));
        assertThat(nearest.get().station().label(), containsString("Cuxhaven"));
    }

    @Test
    void noStationWithin50KmOfZurich() {
        assertTrue(STATIONS.nearestWithin(47.4, 8.5, 50).isEmpty());
        List<TideStations.StationDistance> nearest = STATIONS.nearest(47.4, 8.5, 3);
        assertEquals(3, nearest.size());
        assertThat(nearest.get(0).km(), greaterThan(50.0));
        assertThat(nearest.get(0).km(), lessThanOrEqualTo(nearest.get(1).km()));
        assertThat(nearest.get(1).km(), lessThanOrEqualTo(nearest.get(2).km()));
    }

    @Test
    void stationsWithBarelyAnyTideAreLeftOut() {
        // Palma de Mallorca: the station there has a mean range of a few centimetres
        assertTrue(STATIONS.nearestWithin(39.6, 2.6, 50).isEmpty());
    }

    @Test
    void duplicateOfABetterGaugeIsLeftOut() {
        // TICON's Fort Point (data from 1858-1877) is 300 m from NOAA's Golden Gate gauge and 25 minutes off
        assertTrue(STATIONS.byId("ticon/sanfrancisco_fort_point-9414920-usa-da_sat").isEmpty());
        assertTrue(STATIONS.byId("noaa/9414290").isPresent());
    }

    @Test
    void nearestWorksAcrossTheAntimeridian() {
        TideStation east = new TideStation("east", "East", -17.0, -179.9, "A", (from, until) -> List.of());
        TideStation west = new TideStation("west", "West", -17.0, 170.0, "A", (from, until) -> List.of());
        List<TideStations.StationDistance> nearest = new TideStations(List.of(west, east)).nearest(-17.0, 179.9, 1);
        assertSame(east, nearest.get(0).station());
        assertThat(nearest.get(0).km(), lessThan(25.0));
    }

    @Test
    void stationsShareTheirAttributionInstance() {
        TideStation cuxhaven = station("ticon/cuxhaven-825-deu-uhslc_fd");
        TideStation brest = station("ticon/brest-822-fra-uhslc_fd");
        assertThat(cuxhaven.attribution(), containsString("TICON-4"));
        assertSame(cuxhaven.attribution(), brest.attribution());
    }

    private static TideStation station(String id) {
        return STATIONS.byId(id).orElseThrow(() -> new AssertionError("missing station " + id));
    }

    private static void assertExtremes(List<TideExtreme> extremes, Duration tolerance, String... expected) {
        assertEquals(expected.length, extremes.size(), extremes::toString);
        for (int i = 0; i < expected.length; i++) {
            String[] timeAndKind = expected[i].split(" ");
            Instant expectedTime = JAN_15.atZone(ZoneOffset.UTC).with(LocalTime.parse(timeAndKind[0])).toInstant();
            TideExtreme actual = extremes.get(i);
            assertEquals(timeAndKind[1], actual.getKind().name(), actual::toString);
            assertThat(actual + " vs " + expectedTime, Duration.between(expectedTime, actual.getTime()).abs(), lessThanOrEqualTo(tolerance));
        }
    }
}
