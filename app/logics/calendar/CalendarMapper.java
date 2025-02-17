package logics.calendar;

import jakarta.inject.Inject;
import logics.Randomizer;
import models.EventInstance;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.GregorianCalendar;

public class CalendarMapper {

    private static final TimeZone UTC_ZONE = TimeZoneRegistryFactory.getInstance().createRegistry().getTimeZone("Europe/London");
    private final CalendarOutputter calendarOutputter = new CalendarOutputter();
    private final MessagesApi messagesApi;

    @Inject
    public CalendarMapper(MessagesApi messagesApi) {
        this.messagesApi = messagesApi;
    }

    /**
     * @param events          to map
     * @param updateFrequency How often this calendar should be updated (in days)
     * @return the ical-file
     */
    public String map(Collection<EventInstance> events, long updateFrequency, Lang language) {
        final Calendar calendar = createCalendar(updateFrequency);
        Url thankUrl = new Url(URI.create("https://mooncal.ch/" + getThankUrl(language) + "?c=ics"));
        for (EventInstance event : events) {
            addEvent(calendar, event, thankUrl);
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

    private void addEvent(Calendar calendar, EventInstance event, Url thankUrl) {
        final VEvent calEvent = new VEvent(toDate(event.getDateTime()), event.getTitle());
        if (event.getDescription() != null) {
            calEvent.getProperties().add(new Description(event.getDescription()));
        }
        calEvent.getProperties().add(calculateUid(event));
        calEvent.getProperties().add(thankUrl);
        calendar.getComponents().add(calEvent);
    }

    @VisibleForTesting
    String getThankUrl(Lang lang) {
        return messagesApi.get(lang, Randomizer.chooseRandom("navigation.thank", "navigation.donate"));
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Date toDate(ZonedDateTime in) {
        final GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeZone(UTC_ZONE);
        //noinspection MagicConstant
        gregorianCalendar.set(in.getYear(), in.getMonthValue() - 1, in.getDayOfMonth(), in.getHour(), in.getMinute(), in.getSecond());
        return new Date(gregorianCalendar.getTime());
    }
}
