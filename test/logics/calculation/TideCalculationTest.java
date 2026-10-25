package logics.calculation;

import io.openwaters.slackwater.ExtremeKind;
import logics.tides.TideStation;
import logics.tides.TideStations;
import models.EventInstance;
import models.MoonPhaseType;
import models.RequestForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TideCalculationTest {

    private static final TideStations STATIONS = new TideStations();
    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final Pattern TIME = Pattern.compile("\\d{1,2}:\\d{2}");
    private TideCalculation cut;

    @BeforeEach
    void setup() {
        final MessagesApi messagesApi = mock(MessagesApi.class);
        when(messagesApi.get(any(Lang.class), anyString(), any(Object[].class))).thenAnswer(invocation -> {
            final Object[] arguments = invocation.getArguments();
            return String.valueOf(arguments[arguments.length - 1]);
        });
        cut = new TideCalculation(messagesApi, STATIONS);
    }

    @Test
    void oneEntryPerDayAtCuxhaven() {
        List<EventInstance> events = calculate(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), BERLIN, 53.9, 8.7);
        assertThat(events, hasSize(31));
        for (EventInstance event : events) {
            assertEquals(TideCalculation.TIDE_EVENT_TYPE_ID, event.getEventTypeId());
            assertThat(event.getTitle(), allOf(containsString("↑"), containsString("↓")));
            assertThat(event.getPDFTitle(), startsWith("≈"));
        }
    }

    @Test
    void sanFranciscoTimesInTheRequestedZone() {
        // nearest is NOAA 9414275 Ocean Beach; NOAA's prediction in local time: 14 January H 21:48, 15 January L 1:58, H 7:39, L 15:12, H 22:24
        List<EventInstance> events = calculate(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 15), ZoneId.of("America/Los_Angeles"), 37.8, -122.5);
        assertThat(events, hasSize(1));
        List<LocalTime> times = times(events.get(0).getDescription());
        assertThat(times, hasSize(4));
        assertContainsAbout(times, LocalTime.of(1, 58));
        assertContainsAbout(times, LocalTime.of(7, 39));
        assertContainsAbout(times, LocalTime.of(15, 12));
        assertContainsAbout(times, LocalTime.of(22, 24));
    }

    @Test
    void cuxhavenTimesLikeBsh() {
        // BSH (Bundesamt für Seeschifffahrt und Hydrographie) astronomical prediction for Cuxhaven Steubenhöft, 2026-10-26 MEZ:
        // H 0:41, L 7:26, H 12:53, L 19:47; low waters in the shallow estuary are less precise
        List<EventInstance> events = calculate(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 26), BERLIN, 53.9, 8.7);
        assertThat(events, hasSize(1));
        List<LocalTime> times = times(events.get(0).getDescription());
        assertThat(times, hasSize(4));
        assertContainsAbout(times, LocalTime.of(0, 41), 8);
        assertContainsAbout(times, LocalTime.of(7, 26), 8);
        assertContainsAbout(times, LocalTime.of(12, 53), 8);
        assertContainsAbout(times, LocalTime.of(19, 47), 8);
    }

    @Test
    void everyEventEndsWithTheAttribution() {
        List<EventInstance> events = calculate(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3), BERLIN, 53.9, 8.7);
        String attribution = STATIONS.nearestWithin(53.9, 8.7, 50).orElseThrow().station().attribution();
        for (EventInstance event : events) {
            assertThat(event.getDescription(), endsWith("\n" + attribution));
        }
        assertSame(events.get(0).getAttribution(), events.get(1).getAttribution());
    }

    @Test
    void stationAtTheLocationHasNoDistance() {
        // the location of a station clicked in the hint is the station's position, rounded like any location
        TideStation clicked = cut.stationsToChoose(47.4, 8.5, 1).getFirst().station();
        String description = calculate(keyEchoingMessages(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), BERLIN, clicked.lat(), clicked.lon()).getFirst().getDescription();
        assertThat(description, containsString("phases.tide.stationAtLocation " + clicked.label() + "\n"));
    }

    @Test
    void stationNearTheLocationHasItsDistance() {
        // Hamburg: the nearest station, St. Pauli, is rounded to 53.5
        String description = calculate(keyEchoingMessages(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), BERLIN, 53.6, 10.0).getFirst().getDescription();
        assertThat(description, matchesPattern("(?s).*phases\\.tide\\.station .+ \\d+\n.*"));
    }

    @Test
    void everyStationToChooseIsShownAtItsLocation() {
        // the stations to choose near any station, including those sharing their rounded position with another one
        Map<String, TideStations.StationDistance> toChoose = new HashMap<>();
        for (TideStations.StationDistance station : STATIONS.nearest(0, 0, STATIONS.size())) {
            cut.stationsToChoose(station.station().lat(), station.station().lon(), 3).forEach(choice -> toChoose.put(choice.station().id(), choice));
        }
        TideCalculation keyEchoing = new TideCalculation(keyEchoingMessages(), STATIONS);
        List<String> notAtTheLocation = new ArrayList<>();
        for (TideStations.StationDistance station : toChoose.values()) {
            String description = calculate(keyEchoing, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), BERLIN, station.station().lat(), station.station().lon()).getFirst().getDescription();
            if (!description.contains("phases.tide.stationAtLocation " + station.station().label() + "\n")) {
                notAtTheLocation.add(station.station().label() + ": " + description.lines().filter(line -> line.startsWith("phases.tide.station")).findFirst().orElseThrow());
            }
        }
        assertEquals(List.of(), notAtTheLocation);
    }

    @Test
    void stationsToChooseAreDistinctAndNearestFirst() {
        List<TideStations.StationDistance> stations = cut.stationsToChoose(47.4, 8.5, 3);
        assertThat(stations, hasSize(3));
        assertEquals(3, stations.stream().map(station -> station.station().id()).distinct().count());
        assertThat(stations.get(0).km(), lessThanOrEqualTo(stations.get(1).km()));
        assertThat(stations.get(1).km(), lessThanOrEqualTo(stations.get(2).km()));
    }

    @Test
    void stationsToChooseAreTheNearestOfAllShownStations() {
        // a shown station can be farther away than the one it hides, so a nearer one must not be left out for it
        Set<TideStation> shown = new HashSet<>();
        for (TideStations.StationDistance station : STATIONS.nearest(0, 0, STATIONS.size())) {
            shown.add(cut.station(station.station().lat(), station.station().lon()).orElseThrow().station());
        }
        // the Loire estuary and the Oste near Hamburg
        for (double[] location : new double[][]{{47.6, -1.7}, {53.4, 9.1}}) {
            List<String> expected = shown.stream()
                    .sorted(Comparator.comparingDouble(station -> TideStations.distanceKm(location[0], location[1], station.lat(), station.lon())))
                    .limit(3).map(TideStation::id).toList();
            assertEquals(expected, cut.stationsToChoose(location[0], location[1], 3).stream().map(station -> station.station().id()).toList());
        }
    }

    @Test
    void stationHiddenByAnotherAtItsRoundedPositionIsNotToChoose() {
        List<TideStation> hidden = STATIONS.nearest(0, 0, STATIONS.size()).stream()
                .map(TideStations.StationDistance::station)
                .filter(station -> cut.station(station.lat(), station.lon()).orElseThrow().station() != station)
                .toList();
        assertThat(hidden, is(not(empty())));
        for (TideStation station : hidden) {
            assertThat(cut.stationsToChoose(station.lat(), station.lon(), 3).stream().map(TideStations.StationDistance::station).toList(), not(hasItem(station)));
        }
    }

    @Test
    void noTideEventsFarFromTheSea() {
        assertThat(calculate(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), BERLIN, 47.4, 8.5), is(empty()));
    }

    @Test
    void noLocationNoTideEvents() {
        assertThat(calculate(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), BERLIN, null, null), is(empty()));
    }

    @Test
    void onlyDaysFrom1900To2100GetTides() {
        List<EventInstance> events = calculate(LocalDate.of(2095, 1, 1), LocalDate.of(2105, 12, 31), BERLIN, 53.9, 8.7);
        assertEquals(LocalDate.of(2095, 1, 1), events.get(0).getLocalDate());
        assertEquals(LocalDate.of(2100, 12, 31), events.get(events.size() - 1).getLocalDate());
        assertThat(calculate(LocalDate.of(1800, 1, 1), LocalDate.of(1801, 1, 1), BERLIN, 53.9, 8.7), is(empty()));
    }

    @Test
    void dayWithOnlyOneHighAndOneLowHasNoEmptyPart() {
        // Pensacola has a diurnal tide: mostly one high and one low per day
        List<EventInstance> events = calculate(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), ZoneId.of("America/Chicago"), 30.4, -87.2);
        assertThat(events, is(not(empty())));
        assertTrue(events.stream().anyMatch(event -> times(event.getTitle()).size() < 4), "no day with fewer than 4 extremes");
        for (EventInstance event : events) {
            for (String title : List.of(event.getTitle(), event.getPDFTitle())) {
                assertThat(title, not(anyOf(endsWith("↑"), endsWith("↓"), endsWith("·"), containsString("↑ ·"), containsString("↓ ·"), containsString("  "))));
            }
        }
    }

    @Test
    void extremesAroundNewYearAndDstAreNeitherLostNorDuplicated() {
        assertSameExtremesAsOnePrediction(LocalDate.of(2025, 12, 30), LocalDate.of(2026, 1, 2));
        assertSameExtremesAsOnePrediction(LocalDate.of(2026, 3, 28), LocalDate.of(2026, 3, 30));
    }

    private void assertSameExtremesAsOnePrediction(LocalDate from, LocalDate to) {
        List<String> fromEvents = new ArrayList<>();
        for (EventInstance event : calculate(from, to, BERLIN, 53.9, 8.7)) {
            fromEvents.addAll(extremesOfTitle(event));
        }
        TideStation station = STATIONS.nearestWithin(53.9, 8.7, 50).orElseThrow().station();
        Instant start = from.atStartOfDay(BERLIN).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(BERLIN).toInstant();
        List<String> expected = station.prediction().extremes(start.minus(Duration.ofHours(1)), end.plus(Duration.ofHours(1))).stream()
                .map(extreme -> Map.entry(extreme.getKind(), extreme.getTime().plusSeconds(30).truncatedTo(ChronoUnit.MINUTES)))
                .filter(extreme -> !extreme.getValue().isBefore(start) && extreme.getValue().isBefore(end))
                .map(extreme -> describe(extreme.getKey(), extreme.getValue().atZone(BERLIN).toLocalDate(), extreme.getValue().atZone(BERLIN).toLocalTime()))
                .sorted().toList();
        assertEquals(expected, fromEvents.stream().sorted().toList());
    }

    private static List<String> extremesOfTitle(EventInstance event) {
        List<String> extremes = new ArrayList<>();
        for (String part : event.getTitle().split(" · ")) {
            ExtremeKind kind = part.contains("↑") ? ExtremeKind.HIGH : ExtremeKind.LOW;
            times(part).forEach(time -> extremes.add(describe(kind, event.getLocalDate(), time)));
        }
        return extremes;
    }

    private static String describe(ExtremeKind kind, LocalDate day, LocalTime time) {
        return day + " " + time.truncatedTo(ChronoUnit.MINUTES) + " " + kind;
    }

    private static List<LocalTime> times(String text) {
        List<LocalTime> times = new ArrayList<>();
        Matcher matcher = TIME.matcher(text);
        while (matcher.find()) {
            String[] hourMinute = matcher.group().split(":");
            times.add(LocalTime.of(Integer.parseInt(hourMinute[0]), Integer.parseInt(hourMinute[1])));
        }
        return times;
    }

    /** @return messages that are their key followed by their arguments */
    private static MessagesApi keyEchoingMessages() {
        final MessagesApi messagesApi = mock(MessagesApi.class);
        when(messagesApi.get(any(Lang.class), anyString(), any(Object[].class))).thenAnswer(invocation -> {
            StringJoiner message = new StringJoiner(" ");
            Arrays.stream(invocation.getArguments()).skip(1).forEach(argument -> message.add(String.valueOf(argument)));
            return message.toString();
        });
        return messagesApi;
    }

    private static void assertContainsAbout(List<LocalTime> times, LocalTime expected) {
        assertContainsAbout(times, expected, 5);
    }

    private static void assertContainsAbout(List<LocalTime> times, LocalTime expected, int toleranceMinutes) {
        assertTrue(times.stream().anyMatch(time -> Math.abs(Duration.between(expected, time).toMinutes()) <= toleranceMinutes), () -> expected + " not in " + times);
    }

    private List<EventInstance> calculate(LocalDate from, LocalDate to, ZoneId zone, Double lat, Double lon) {
        return calculate(cut, from, to, zone, lat, lon);
    }

    private List<EventInstance> calculate(MessagesApi messagesApi, LocalDate from, LocalDate to, ZoneId zone, Double lat, Double lon) {
        return calculate(new TideCalculation(messagesApi, STATIONS), from, to, zone, lat, lon);
    }

    private static List<EventInstance> calculate(TideCalculation cut, LocalDate from, LocalDate to, ZoneId zone, Double lat, Double lon) {
        RequestForm requestForm = new RequestForm();
        requestForm.getPhases().put(MoonPhaseType.TIDE, true);
        requestForm.setFrom(from.atStartOfDay(zone));
        requestForm.setTo(to.atTime(23, 59, 59).atZone(zone));
        requestForm.setLang(Lang.forCode("de"));
        requestForm.setLat(lat);
        requestForm.setLon(lon);
        Collection<EventInstance> events = new TreeSet<>();
        cut.calculate(requestForm, events);
        return List.copyOf(events);
    }
}
