package logics.calendar;

import models.EventInstance;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PDFMapperDayOrderTest {

    private static final LocalDate DAY = LocalDate.of(2026, 1, 10);

    private static EventInstance event(ZonedDateTime start, ZonedDateTime end, String eventTypeId) {
        return new EventInstance(start, end, eventTypeId, eventTypeId, null, ZoneOffset.UTC, eventTypeId);
    }

    private static ZonedDateTime utc(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneOffset.UTC);
    }

    private static List<String> sortedIds(List<EventInstance> events) {
        List<EventInstance> copy = new ArrayList<>(events);
        copy.sort(PDFMapper.dayEventOrder(DAY));
        return copy.stream().map(EventInstance::getEventTypeId).toList();
    }

    @Test
    void gardenEventsAreOrderedByTimeOnTheDayNotByTypeId() {
        // A "root" period that began the previous day and ends at 14:00 — ongoing since 00:00 on DAY.
        EventInstance ongoingRoot = event(utc(2026, 1, 8, 10, 0), utc(2026, 1, 10, 14, 0), "garden-biodynamic-root");
        // A "leaf" period that starts at 14:00 on DAY.
        EventInstance newLeaf = event(utc(2026, 1, 10, 14, 0), utc(2026, 1, 12, 9, 0), "garden-biodynamic-leaf");

        // By type id alone, "leaf" < "root" would put the later-starting leaf first; by time the
        // ongoing root (00:00) must come first.
        assertEquals(List.of("garden-biodynamic-root", "garden-biodynamic-leaf"),
                sortedIds(List.of(newLeaf, ongoingRoot)));
    }

    @Test
    void allDayBadDayOrdersWithOngoingPeriodsAtStartOfDay() {
        EventInstance badDay = event(utc(2026, 1, 10, 0, 0), null, "garden-biodynamic-badday");
        EventInstance ongoingLeaf = event(utc(2026, 1, 6, 18, 0), utc(2026, 1, 18, 5, 0), "garden-biodynamic-leaf");
        EventInstance newFruit = event(utc(2026, 1, 10, 9, 30), utc(2026, 1, 12, 3, 0), "garden-biodynamic-fruit");

        // 00:00 events (bad day + ongoing leaf period) come before the 09:30 fruit; the two 00:00 events
        // tie-break by type id ("garden-biodynamic-badday" < "garden-biodynamic-leaf").
        assertEquals(List.of("garden-biodynamic-badday", "garden-biodynamic-leaf", "garden-biodynamic-fruit"),
                sortedIds(List.of(newFruit, ongoingLeaf, badDay)));
    }

    @Test
    void nonGardenEventsKeepTypeIdOrderRegardlessOfTime() {
        // A lunar eclipse at 18:00 and a full moon at 20:00 on the same day: order stays by type id
        // ("fullmoon" < "lunareclipse"), i.e. unchanged from before, so existing PDF baselines hold.
        EventInstance fullmoon = event(utc(2026, 1, 10, 20, 0), null, "fullmoon");
        EventInstance lunarEclipse = event(utc(2026, 1, 10, 18, 0), null, "lunareclipse");

        assertEquals(List.of("fullmoon", "lunareclipse"),
                sortedIds(List.of(lunarEclipse, fullmoon)));
    }

    @Test
    void gardenBlockStaysContiguousBetweenNonGardenEvents() {
        EventInstance firstQuarter = event(utc(2026, 1, 10, 8, 0), null, "first-quarter"); // 'f' < "garden-"
        EventInstance lastQuarter = event(utc(2026, 1, 10, 6, 0), null, "last-quarter");   // 'l' > "garden-"
        EventInstance gardenLeaf = event(utc(2026, 1, 10, 14, 0), utc(2026, 1, 12, 9, 0), "garden-biodynamic-leaf");
        EventInstance gardenRoot = event(utc(2026, 1, 8, 10, 0), utc(2026, 1, 10, 14, 0), "garden-biodynamic-root");

        // Non-garden events bracket the garden block; garden events inside it are time-ordered.
        assertEquals(List.of("first-quarter", "garden-biodynamic-root", "garden-biodynamic-leaf", "last-quarter"),
                sortedIds(List.of(lastQuarter, gardenLeaf, firstQuarter, gardenRoot)));
    }
}
