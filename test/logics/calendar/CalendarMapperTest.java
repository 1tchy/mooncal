package logics.calendar;

import logics.Randomizer;
import models.EventInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.i18n.Lang;
import play.test.WithApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.TimeZone;

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
