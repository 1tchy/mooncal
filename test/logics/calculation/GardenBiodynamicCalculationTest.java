package logics.calculation;

import logics.astronomy.*;
import models.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.i18n.Lang;
import play.i18n.MessagesApi;
import play.test.WithApplication;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GardenBiodynamicCalculationTest extends WithApplication {

    private final MessagesApi messages = mock(MessagesApi.class, inv ->
            inv.getArguments().length > 1 ? inv.getArguments()[1].toString() : "");

    @BeforeEach
    void setUpPlay() {
        startPlay();
    }

    @AfterEach
    void tearDownPlay() {
        stopPlay();
    }

    private RequestForm form() {
        RequestForm f = new RequestForm();
        f.setLang(Lang.forCode("en"));
        f.setStyle("withDescription");
        f.setHemisphere("northern");
        f.setFrom(ZonedDateTime.parse("2025-06-01T00:00:00Z"));
        f.setTo(ZonedDateTime.parse("2025-06-30T00:00:00Z"));
        f.setEvents(Map.of(EventType.GARDEN_BIODYNAMIC, true));
        return f;
    }

    @Test
    void emitsPlantPartTransitionsEachAboutTwoToThreeDays() {
        GardenBiodynamicCalculation calc = new GardenBiodynamicCalculation(new MeeusEphemeris(), messages);
        Collection<EventInstance> out = new TreeSet<>();
        calc.calculate(form(), out);
        // ~12 sign changes in a 29-day window, plus the leading-edge event, plus node/eclipse
        // bad-day events and the period splits they introduce.
        assertTrue(out.size() >= 9 && out.size() <= 22, "transitions: " + out.size());
        out.forEach(e -> assertTrue(e.getEventTypeId().startsWith("garden-biodynamic-")));
    }

    @Test
    void periodNotInDescription() {
        GardenBiodynamicCalculation calc = new GardenBiodynamicCalculation(new MeeusEphemeris(), messages);
        Collection<EventInstance> out = new TreeSet<>();
        calc.calculate(form(), out);
        assertFalse(out.isEmpty());
        // garden.period was moved to the date column (getDate/getEndDate wall-clock datetimes)
        out.forEach(e -> assertFalse(
                e.getDescription() != null && e.getDescription().contains("garden.period"),
                "description must not contain period sentence: " + e.getDescription()));
    }

    @Test
    void descriptionsDoNotContainTheUntilLine() {
        GardenBiodynamicCalculation calc = new GardenBiodynamicCalculation(new MeeusEphemeris(), messages);
        Collection<EventInstance> out = new TreeSet<>();
        calc.calculate(form(), out);
        assertFalse(out.isEmpty());
        // The "Gilt bis ..." line was removed in favour of the from–to range in the date column.
        out.forEach(e -> assertFalse(e.getDescription().contains("garden.biodynamic.until"),
                "description should not contain the until line: " + e.getDescription()));
    }

    @Test
    void sameDateWorldwideRegardlessOfHemisphere() {
        GardenBiodynamicCalculation calc = new GardenBiodynamicCalculation(new MeeusEphemeris(), messages);
        Collection<EventInstance> north = new TreeSet<>();
        calc.calculate(form(), north);
        RequestForm south = form();
        south.setHemisphere("southern");
        Collection<EventInstance> southOut = new TreeSet<>();
        calc.calculate(south, southOut);

        // Assert date strings are identical (original assertion)
        List<String> nd = north.stream().map(EventInstance::getDate).sorted().toList();
        List<String> sd = southOut.stream().map(EventInstance::getDate).sorted().toList();
        assertEquals(nd, sd);

        // Assert event times and types are identical (hemisphere-independent identity)
        // Events are from TreeSet so already time-ordered; build deterministic comparison strings
        List<String> northInstants = north.stream()
            .map(e -> e.getDateTime().toInstant() + "|" + e.getEventTypeId())
            .sorted()
            .toList();
        List<String> southInstants = southOut.stream()
            .map(e -> e.getDateTime().toInstant() + "|" + e.getEventTypeId())
            .sorted()
            .toList();
        assertEquals(northInstants, southInstants,
            "Hemisphere must not affect event instants or types");
    }

    @Test
    void disabledEmitsNothing() {
        RequestForm f = form();
        f.setEvents(Map.of(EventType.GARDEN_BIODYNAMIC, false));
        Collection<EventInstance> out = new TreeSet<>();
        new GardenBiodynamicCalculation(new MeeusEphemeris(), messages).calculate(f, out);
        assertTrue(out.isEmpty());
    }

    // --- Node-day blackout: a lunar node crossing overrides the plant-part classification. ---

    /** Verified 2026 node fixtures (UT): date, and the plant-part event type id it would otherwise carry. */
    private RequestForm utcWindow(String fromIso, String toIso) {
        RequestForm f = new RequestForm();
        f.setLang(Lang.forCode("en"));
        f.setStyle("withDescription");
        f.setHemisphere("northern");
        f.setFrom(ZonedDateTime.parse(fromIso)); // ...Z -> UTC zone, so day boundaries are deterministic
        f.setTo(ZonedDateTime.parse(toIso));
        f.setEvents(Map.of(EventType.GARDEN_BIODYNAMIC, true));
        return f;
    }

    private Collection<EventInstance> run(RequestForm f) {
        Collection<EventInstance> out = new TreeSet<>();
        new GardenBiodynamicCalculation(new MeeusEphemeris(), messages).calculate(f, out);
        return out;
    }

    private static boolean covers(EventInstance e, LocalDate d) {
        return !d.isBefore(e.getLocalDate()) && !d.isAfter(e.getEndLocalDate());
    }

    private static boolean hasBadDayOn(Collection<EventInstance> out, LocalDate d) {
        return out.stream().anyMatch(e -> e.getEventTypeId().equals("garden-biodynamic-badday") && e.getLocalDate().equals(d));
    }

    private static boolean anyPlantPartCovers(Collection<EventInstance> out, LocalDate d, String typeIdSuffix) {
        return out.stream()
                .filter(e -> e.getEventTypeId().equals("garden-biodynamic-" + typeIdSuffix))
                .anyMatch(e -> covers(e, d));
    }

    @Test
    void descendingNodeInLeoBecomesBadDayNotFruit() {
        Collection<EventInstance> out = run(utcWindow("2026-01-02T00:00:00Z", "2026-01-12T00:00:00Z"));
        LocalDate node = LocalDate.of(2026, 1, 7);
        assertTrue(hasBadDayOn(out, node), "Jan 7 must be flagged as a bad garden day");
        assertFalse(anyPlantPartCovers(out, node, "fruit"), "Jan 7 must not be a fruit day");
    }

    @Test
    void secondDescendingNodeInLeoBecomesBadDayNotFruit() {
        Collection<EventInstance> out = run(utcWindow("2026-04-21T00:00:00Z", "2026-05-01T00:00:00Z"));
        LocalDate node = LocalDate.of(2026, 4, 26);
        assertTrue(hasBadDayOn(out, node), "Apr 26 must be flagged as a bad garden day");
        assertFalse(anyPlantPartCovers(out, node, "fruit"), "Apr 26 must not be a fruit day");
    }

    @Test
    void ascendingNodeInAquariusBecomesBadDayNotFlower() {
        Collection<EventInstance> out = run(utcWindow("2026-03-12T00:00:00Z", "2026-03-22T00:00:00Z"));
        LocalDate node = LocalDate.of(2026, 3, 17);
        assertTrue(hasBadDayOn(out, node), "Mar 17 must be flagged as a bad garden day");
        assertFalse(anyPlantPartCovers(out, node, "flower"), "Mar 17 must not be a flower day");
    }

    @Test
    void ascendingNodeInCapricornBecomesBadDayNotRoot() {
        Collection<EventInstance> out = run(utcWindow("2026-12-09T00:00:00Z", "2026-12-19T00:00:00Z"));
        LocalDate node = LocalDate.of(2026, 12, 14);
        assertTrue(hasBadDayOn(out, node), "Dec 14 must be flagged as a bad garden day");
        assertFalse(anyPlantPartCovers(out, node, "root"), "Dec 14 must not be a root day");
    }

    @Test
    void controlDayKeepsItsPlantPartAndIsNotBad() {
        Collection<EventInstance> out = run(utcWindow("2026-01-02T00:00:00Z", "2026-01-20T00:00:00Z"));
        LocalDate control = LocalDate.of(2026, 1, 15);
        assertFalse(hasBadDayOn(out, control), "Jan 15 is not a node day and must not be marked bad");
        boolean anyPlantPart = out.stream()
                .filter(e -> !e.getEventTypeId().equals("garden-biodynamic-badday"))
                .anyMatch(e -> covers(e, control));
        assertTrue(anyPlantPart, "Jan 15 must still carry a normal plant-part label");
    }

    @Test
    void badDayUsesRequestZoneLocalDateNotUtc() {
        // A node near UTC midnight lands on the previous calendar day in zones west of UTC.
        ZoneId ny = ZoneId.of("America/New_York");
        RequestForm f = new RequestForm();
        f.setLang(Lang.forCode("en"));
        f.setStyle("withDescription");
        f.setHemisphere("northern");
        f.setFrom(ZonedDateTime.of(2026, 1, 18, 0, 0, 0, 0, ny));
        f.setTo(ZonedDateTime.of(2026, 1, 26, 0, 0, 0, 0, ny));
        f.setEvents(Map.of(EventType.GARDEN_BIODYNAMIC, true));

        MeeusEphemeris ephemeris = new MeeusEphemeris();
        Collection<EventInstance> out = new TreeSet<>();
        new GardenBiodynamicCalculation(ephemeris, messages).calculate(f, out);

        // The bad day must equal the node instant rendered in the request zone, not in UTC.
        Instant node = ephemeris.nextNodeCrossing(f.getFrom().toInstant(), f.getTo().toInstant());
        assertNotNull(node);
        LocalDate expected = node.atZone(ny).toLocalDate();
        assertTrue(hasBadDayOn(out, expected),
                "Bad day must use the request-zone local date " + expected
                        + " (UTC would be " + node.atZone(java.time.ZoneOffset.UTC).toLocalDate() + ")");
    }

    @Test
    void biodynamicEventsAreMultiDay() {
        GardenBiodynamicCalculation calc = new GardenBiodynamicCalculation(new MeeusEphemeris(), messages);
        Collection<EventInstance> out = new TreeSet<>();
        calc.calculate(form(), out);
        // All but the last event should have a non-null end that is after the start
        // (the last period in range may have periodEnd==null and be single-day)
        long multiDayCount = out.stream().filter(EventInstance::isMultiDay).count();
        assertTrue(multiDayCount >= 1,
                "Expected at least one multi-day biodynamic event, got: " + multiDayCount
                + " out of " + out.size());
        // Every multi-day event must have endLocalDate strictly after localDate
        out.stream().filter(EventInstance::isMultiDay).forEach(e ->
                assertTrue(e.getEndLocalDate().isAfter(e.getLocalDate()),
                        "endLocalDate must be after localDate for: " + e));
    }
}
