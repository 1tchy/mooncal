package logics.calendar;

import models.EventInstance;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.GregorianCalendar;

public class CalendarMapper {

    private static final TimeZone UTC_ZONE = TimeZoneRegistryFactory.getInstance().createRegistry().getTimeZone("Europe/London");
    private final CalendarOutputter calendarOutputter = new CalendarOutputter();

    /**
     * @param events          to map
     * @param updateFrequency How often this calendar should be updated (in days)
     * @return the ical-file
     */
    public String map(Collection<EventInstance> events, long updateFrequency) {
        final Calendar calendar = createCalendar(updateFrequency);
        for (EventInstance event : events) {
            addEvent(calendar, event);
        }
        return getICalendarString(calendar);
    }

    @NotNull
    private Calendar createCalendar(long updateFrequency) {
        final Calendar calendar = new Calendar();
        calendar.getProperties().add(new ProdId("-//Mooncal 1.0//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);
        if (updateFrequency > 0) {
            calendar.getProperties().add(new XProperty("X-PUBLISHED-TTL", "P" + updateFrequency + "D"));
        }
        return calendar;
    }

    private void addEvent(Calendar calendar, EventInstance event) {
        final VEvent calEvent = new VEvent(toDate(event.getDateTime()), event.getTitle());
        if (event.getDescription() != null) {
            calEvent.getProperties().add(new Description(event.getDescription()));
        }
        calEvent.getProperties().add(calculateUid(event));
        calendar.getComponents().add(calEvent);
    }

    private Uid calculateUid(EventInstance event) {
        return new Uid("mooncal-" + getStandardDate(event) + "-" + event.getEventTypeId());
    }

    @NotNull
    private String getStandardDate(EventInstance event) {
        return event.getDateTime().withZoneSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
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
