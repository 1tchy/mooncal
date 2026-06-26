package logics.calendar;

import models.EventInstance;
import org.junit.jupiter.api.Test;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CalendarMapperMultiDayTest {

    private final MessagesApi messages = mock(MessagesApi.class, inv ->
            inv.getArguments().length > 1 ? "DISCLAIMER" : "thank");

    private CalendarMapper mapper() {
        return new CalendarMapper(messages);
    }

    @Test
    void multiDayGardenEventProducesDtend() {
        ZonedDateTime start = ZonedDateTime.of(2026, 1, 5, 10, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime end   = ZonedDateTime.of(2026, 1, 7, 18, 0, 0, 0, ZoneOffset.UTC);
        EventInstance event = new EventInstance(start, end,
                "Root Day", "Root", "desc", ZoneOffset.UTC, "garden-biodynamic-root");

        String ics = mapper().map(List.of(event), 0, Lang.forCode("en"));

        // Multi-day events must have DTSTART and DTEND
        assertTrue(ics.contains("DTSTART;VALUE=DATE:20260105"), "Expected DTSTART 20260105 in:\n" + ics);
        // DTEND is exclusive: last covered day (Jan 7) + 1 = Jan 8
        assertTrue(ics.contains("DTEND;VALUE=DATE:20260108"), "Expected DTEND 20260108 (exclusive) in:\n" + ics);
    }

    @Test
    void singleDayEventProducesNoDtend() {
        ZonedDateTime start = ZonedDateTime.of(2026, 3, 10, 8, 0, 0, 0, ZoneOffset.UTC);
        EventInstance event = new EventInstance(start,
                "Full Moon", "Full Moon", null, ZoneOffset.UTC, "fullmoon");

        String ics = mapper().map(List.of(event), 0, Lang.forCode("en"));

        assertFalse(ics.contains("DTEND"), "Expected NO DTEND for single-day event in:\n" + ics);
        assertTrue(ics.contains("DTSTART;VALUE=DATE:20260310"), "Expected DTSTART 20260310 in:\n" + ics);
    }

    @Test
    void multiDayUidStillUsesStartDate() {
        ZonedDateTime start = ZonedDateTime.of(2026, 1, 5, 10, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime end   = ZonedDateTime.of(2026, 1, 7, 18, 0, 0, 0, ZoneOffset.UTC);
        EventInstance event = new EventInstance(start, end,
                "Root Day", "Root", "desc", ZoneOffset.UTC, "garden-biodynamic-root");

        String ics = mapper().map(List.of(event), 0, Lang.forCode("en"));

        assertTrue(ics.contains("UID:mooncal-20260105Z-garden-biodynamic-root"),
                "Expected UID based on start date in:\n" + ics);
    }
}
