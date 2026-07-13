package logics.calendar;

import models.EventInstance;
import org.junit.jupiter.api.Test;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CalendarMapperDisclaimerTest {
    private final MessagesApi messages = mock(MessagesApi.class, inv ->
            inv.getArguments().length > 1 ? "DISCLAIMER_TEXT" : "thank");

    @Test
    void addsCalendarDescriptionWhenGardenEventPresent() {
        CalendarMapper mapper = new CalendarMapper(messages);
        EventInstance garden = new EventInstance(ZonedDateTime.now(ZoneOffset.UTC),
                "t", "t", "d", ZoneOffset.UTC, "garden-biodynamic-fruit");
        String ics = mapper.map(List.of(garden), 0, Lang.forCode("en"));
        assertTrue(ics.contains("X-WR-CALDESC"), "Expected X-WR-CALDESC in ICS for garden event");
        assertTrue(ics.contains("DISCLAIMER_TEXT"), "Expected disclaimer text in ICS for garden event");
    }

    @Test
    void noCalendarDescriptionWithoutGardenEvents() {
        CalendarMapper mapper = new CalendarMapper(messages);
        EventInstance full = new EventInstance(ZonedDateTime.now(ZoneOffset.UTC),
                "t", "t", "d", ZoneOffset.UTC, "fullmoon");
        String ics = mapper.map(List.of(full), 0, Lang.forCode("en"));
        assertFalse(ics.contains("X-WR-CALDESC"), "Expected NO X-WR-CALDESC in ICS for non-garden event");
    }
}
