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
    void lateNightStartingGardenPeriodDropsItsFirstDayInIcs() {
        // 2026-08-06: the Fruit->Root boundary is at 22:25, so the Root period only touches Aug 6 for
        // ~1.5h. Like the PDF, the ICS must not mark Aug 6 as a root day — the period starts on Aug 7.
        ZonedDateTime start = ZonedDateTime.of(2026, 8, 6, 22, 25, 0, 0, ZoneOffset.UTC);
        ZonedDateTime end   = ZonedDateTime.of(2026, 8, 9, 0, 20, 0, 0, ZoneOffset.UTC);
        EventInstance root = new EventInstance(start, end,
                "Root Day", "Root", "desc", ZoneOffset.UTC, "garden-biodynamic-root");

        String ics = mapper().map(List.of(root), 0, Lang.forCode("en"));

        assertTrue(ics.contains("DTSTART;VALUE=DATE:20260807"), "root should start Aug 7 (Aug 6 trimmed) in:\n" + ics);
        // End 00:20 < 06:00 trims Aug 9 too; last covered day Aug 8 -> exclusive DTEND Aug 9.
        assertTrue(ics.contains("DTEND;VALUE=DATE:20260809"), "expected DTEND 20260809 in:\n" + ics);
    }

    /** A mapper whose messages echo "&lt;key&gt;|&lt;args...&gt;" so tests see which key and values were used. */
    private static CalendarMapper echoMapper() {
        MessagesApi echo = mock(MessagesApi.class, inv -> {
            Object[] a = inv.getArguments();
            if (a.length <= 1) {
                return "x";
            }
            StringBuilder s = new StringBuilder(String.valueOf(a[1]));
            for (int i = 2; i < a.length; i++) {
                s.append('|').append(a[i]);
            }
            return s.toString();
        });
        return new CalendarMapper(echo);
    }

    /** ical4j folds long DESCRIPTION lines and escapes newlines; unfold so assertions see the raw text. */
    private static String unfold(String ics) {
        return ics.replace("\r\n ", "").replace("\n ", "");
    }

    @Test
    void multiDayGardenEventSpellsOutTheExactPeriodInTheDescription() {
        ZonedDateTime start = ZonedDateTime.of(2026, 8, 6, 22, 25, 0, 0, ZoneOffset.UTC);
        ZonedDateTime end   = ZonedDateTime.of(2026, 8, 9, 5, 12, 0, 0, ZoneOffset.UTC);
        EventInstance root = new EventInstance(start, end,
                "Root Day", "Root", "guidance", ZoneOffset.UTC, "garden-biodynamic-root");

        String ics = unfold(echoMapper().map(List.of(root), 0, Lang.forCode("de")));

        // German locale formats the date as DD.MM.YYYY and the time as HH:mm; both edges shown as a range.
        assertTrue(ics.contains("garden.ics.period"), "expected the range key in:\n" + ics);
        assertTrue(ics.contains("06.08.2026") && ics.contains("22:25"), "localized start missing in:\n" + ics);
        assertTrue(ics.contains("09.08.2026") && ics.contains("05:12"), "localized end missing in:\n" + ics);
        assertTrue(ics.contains("guidance"), "original guidance text must be kept in:\n" + ics);
    }

    @Test
    void singleDayPeriodTrimmedAtEndShowsOnlyItsStartTime() {
        // The leaf period on 2026-08-11: starts 01:16, cut at 23:59:59 because Aug 12 is a bad day.
        ZonedDateTime start = ZonedDateTime.of(2026, 8, 11, 1, 16, 0, 0, ZoneOffset.UTC);
        ZonedDateTime end   = ZonedDateTime.of(2026, 8, 11, 23, 59, 59, 0, ZoneOffset.UTC);
        EventInstance leaf = new EventInstance(start, end,
                "Leaf Day", "Leaf", "guidance", ZoneOffset.UTC, "garden-biodynamic-leaf");

        String ics = unfold(echoMapper().map(List.of(leaf), 0, Lang.forCode("de")));

        assertTrue(ics.contains("garden.ics.starttime"), "expected the start-only key in:\n" + ics);
        assertTrue(ics.contains("11.08.2026") && ics.contains("01:16"), "start time missing in:\n" + ics);
        assertFalse(ics.contains("23:59"), "the midnight artefact end must not be shown in:\n" + ics);
    }

    @Test
    void singleDayPeriodTrimmedAtStartShowsOnlyItsEndTime() {
        // A period that starts at midnight (previous day was a bad day) and ends at a real boundary time.
        ZonedDateTime start = ZonedDateTime.of(2026, 8, 13, 0, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime end   = ZonedDateTime.of(2026, 8, 13, 14, 30, 0, 0, ZoneOffset.UTC);
        EventInstance leaf = new EventInstance(start, end,
                "Leaf Day", "Leaf", "guidance", ZoneOffset.UTC, "garden-biodynamic-leaf");

        String ics = unfold(echoMapper().map(List.of(leaf), 0, Lang.forCode("de")));

        assertTrue(ics.contains("garden.ics.endtime"), "expected the end-only key in:\n" + ics);
        assertTrue(ics.contains("13.08.2026") && ics.contains("14:30"), "end time missing in:\n" + ics);
        assertFalse(ics.contains("00:00"), "the midnight artefact start must not be shown in:\n" + ics);
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
