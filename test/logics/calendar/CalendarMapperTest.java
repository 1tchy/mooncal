package logics.calendar;

import logics.Randomizer;
import logics.calculation.MoonPhasesCalculation;
import logics.calculation.TideCalculation;
import models.EventInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.i18n.Lang;
import play.test.WithApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SuppressWarnings("JUnitMixedFramework")
class CalendarMapperTest extends WithApplication {

    private CalendarMapper cut;

    @BeforeEach
    void setUp() {
        startPlay();
        cut = app.injector().instanceOf(CalendarMapper.class);
        Randomizer.reseed();
    }

    @AfterEach
    void tearDown() {
        stopPlay();
    }

    @Test
    void testSingleEventCalendar() {
        EventInstance event = new EventInstance(ZonedDateTime.of(2015, 12, 24, 18, 0, 0, 0, ZoneOffset.UTC), "Christmas", "title visible only in PDF", "6pm at christmas in UTC", ZoneOffset.UTC, "test");
        String actual = cut.map(Collections.singletonList(event), 123, Lang.forCode("en"));
        assertThat(actual, startsWith("BEGIN:VCALENDAR\r\nPRODID:-//Mooncal 1.0//EN\r\nVERSION:2.0\r\nCALSCALE:GREGORIAN\r\nX-PUBLISHED-TTL:P123D\r\nBEGIN:VEVENT\r\nDTSTAMP:"));
        assertThat(actual, endsWith("\r\nDTSTART;VALUE=DATE:20151224\r\nSUMMARY:Christmas\r\nDESCRIPTION:6pm at christmas in UTC\r\nUID:mooncal-20151224Z-test\r\nURL:https://mooncal.ch/en/donate?c=ics\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"));
    }

    @Test
    void testTimezoneOffset() {
        ZoneId zoneId = TimeZone.getTimeZone("Europe/Zurich").toZoneId();
        EventInstance event = new EventInstance(ZonedDateTime.of(2025, 12, 5, 0, 22, 0, 0, zoneId), "Vollmond (Julmond)", "title visible only in PDF", "Vollmond um 0:22", ZoneOffset.UTC, "test");
        String actual = cut.map(Collections.singletonList(event), 123, Lang.forCode("en"));
        assertThat(actual, startsWith("BEGIN:VCALENDAR\r\nPRODID:-//Mooncal 1.0//EN\r\nVERSION:2.0\r\nCALSCALE:GREGORIAN\r\nX-PUBLISHED-TTL:P123D\r\nBEGIN:VEVENT\r\nDTSTAMP:"));
        assertThat(actual, endsWith("\r\nDTSTART;VALUE=DATE:20251204\r\nSUMMARY:Vollmond (Julmond)\r\nDESCRIPTION:Vollmond um 0:22\r\nUID:mooncal-20251204Z-test\r\nURL:https://mooncal.ch/en/donate?c=ics\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"));
    }

    @Test
    void dayEventsAroundTheSwitchToSummerTimeInNewZealandHaveTheirOwnUid() {
        // noon of the Saturday before the switch is 0:00 UTC, noon of the Sunday 23:00 UTC of the Saturday
        ZoneId auckland = ZoneId.of("Pacific/Auckland");
        for (String eventTypeId : List.of(MoonPhasesCalculation.DAILY_EVENT_TYPE_ID, MoonPhasesCalculation.SUN_EVENT_TYPE_ID, TideCalculation.TIDE_EVENT_TYPE_ID)) {
            List<EventInstance> events = Stream.of(26, 27)
                    .map(day -> new EventInstance(LocalDate.of(2026, 9, day).atTime(12, 0).atZone(auckland), "title", "title", null, auckland, eventTypeId))
                    .toList();
            String actual = cut.map(events, 1, Lang.forCode("en"));
            assertThat(actual, containsString("UID:mooncal-20260926Z-" + eventTypeId + "\r\n"));
            assertThat(actual, containsString("UID:mooncal-20260927Z-" + eventTypeId + "\r\n"));
        }
    }

    @Test
    void uidOfADayEventInEuropeIsUnchanged() {
        ZoneId zurich = ZoneId.of("Europe/Zurich");
        EventInstance event = new EventInstance(LocalDate.of(2026, 7, 15).atTime(12, 0).atZone(zurich), "title", "title", null, zurich, MoonPhasesCalculation.SUN_EVENT_TYPE_ID);
        assertThat(cut.map(List.of(event), 1, Lang.forCode("en")), containsString("UID:mooncal-20260715Z-sun\r\n"));
    }

    @Test
    void testAllThankUrlsAreCorrect() throws IOException {
        String routesCompiled = Files.readString(Path.of("ui/src/app/app.routes.compiled.spec.ts"));
        for (Lang lang : Lang.availables(app)) {
            String thankUrl = cut.getThankUrl(lang);
            if (!lang.code().equals("de")) {
                assertThat(thankUrl, startsWith(lang.code() + "/"));
            }
            assertThat(routesCompiled, containsString(thankUrl));
        }
    }
}
