package logics.calendar;

import models.EventInstance;
import org.junit.Test;
import play.i18n.Lang;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;

public class CalendarMapperTest {

    private final CalendarMapper cut = new CalendarMapper();

    @Test
    public void testSingleEventCalendar() {
        EventInstance event = new EventInstance(ZonedDateTime.of(2015, 12, 24, 18, 0, 0, 0, ZoneOffset.UTC), "Christmas", "6pm at christmas in UTC", ZoneOffset.UTC, Lang.forCode("en"), "test");
        String actual = cut.map(Collections.singletonList(event), 123);
        assertThat(actual, startsWith("BEGIN:VCALENDAR\r\nPRODID:-//Mooncal 1.0//EN\r\nVERSION:2.0\r\nCALSCALE:GREGORIAN\r\nX-PUBLISHED-TTL:P123D\r\nBEGIN:VEVENT\r\nDTSTAMP:"));
        assertThat(actual, endsWith("\r\nDTSTART;VALUE=DATE:20151224\r\nSUMMARY:Christmas\r\nDESCRIPTION:6pm at christmas in UTC\r\nUID:mooncal-20151224Z-test\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"));
    }

}
