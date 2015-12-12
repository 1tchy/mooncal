package logics.calendar;

import models.Event;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.util.UidGenerator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.GregorianCalendar;

public class CalendarMapper {

    private static final TimeZone UTC_ZONE = TimeZoneRegistryFactory.getInstance().createRegistry().getTimeZone("Europe/London");
    private final CalendarOutputter calendarOutputter = new CalendarOutputter();
    private final UidGenerator uidGenerator = new UidGenerator(null, "1");

    public String map(Collection<Event> events) {
        final Calendar calendar = createCalendar();
        for (Event event : events) {
            addEvent(calendar, event);
        }
        return getICalendarString(calendar);
    }

    @NotNull
    private Calendar createCalendar() {
        final Calendar calendar = new Calendar();
        calendar.getProperties().add(new ProdId("-//Mondkalender 1.0//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);
        return calendar;
    }

    private void addEvent(Calendar calendar, Event event) {
        final VEvent calEvent = new VEvent(toDate(event.getDateTime()), event.getTitle());
        calEvent.getProperties().add(uidGenerator.generateUid());
        calendar.getComponents().add(calEvent);
    }

    private String getICalendarString(Calendar calendar) {
        try {
            final StringWriter stringWriter = new StringWriter();
            calendarOutputter.output(calendar, stringWriter);
            return stringWriter.toString();
        } catch (IOException | ValidationException e) {
            throw new RuntimeException(e);
        }
    }

    private Date toDate(ZonedDateTime in) {
        ZonedDateTime utcTime = in.withZoneSameInstant(ZoneOffset.UTC);
        final GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeZone(UTC_ZONE);
        //noinspection MagicConstant
        gregorianCalendar.set(utcTime.getYear(), utcTime.getMonthValue() - 1, utcTime.getDayOfMonth(), utcTime.getHour(), utcTime.getMinute(), utcTime.getSecond());
        return new Date(gregorianCalendar.getTime());
    }
}
