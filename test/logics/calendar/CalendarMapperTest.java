package logics.calendar;

import models.EventInstance;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import play.test.WithApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class CalendarMapperTest extends WithApplication {

    private CalendarMapper cut;

    @Before
    public void setUp() {
        cut = app.injector().instanceOf(CalendarMapper.class);
    }

    @Test
    public void testSingleEventCalendar() {
        EventInstance event = new EventInstance(ZonedDateTime.of(2015, 12, 24, 18, 0, 0, 0, ZoneOffset.UTC), "Christmas", "6pm at christmas in UTC", ZoneOffset.UTC, "test");
        String actual = cut.map(Collections.singletonList(event), 123, Lang.forCode("en"));
        assertThat(actual, startsWith("BEGIN:VCALENDAR\r\nPRODID:-//Mooncal 1.0//EN\r\nVERSION:2.0\r\nCALSCALE:GREGORIAN\r\nX-PUBLISHED-TTL:P123D\r\nBEGIN:VEVENT\r\nDTSTAMP:"));
        assertThat(actual, endsWith("\r\nDTSTART;VALUE=DATE:20151224\r\nSUMMARY:Christmas\r\nDESCRIPTION:6pm at christmas in UTC\r\nUID:mooncal-20151224Z-test\r\nURL:https://mooncal.ch/en/thank\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"));
    }

    @Test
    public void testAllThankUrlsAreCorrect() throws IOException {
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
