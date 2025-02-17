package logics.calendar;

import jakarta.inject.Inject;
import logics.Randomizer;
import models.EventInstance;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.model.property.immutable.ImmutableCalScale;
import net.fortuna.ical4j.model.property.immutable.ImmutableVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

public class CalendarMapper {

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
        calendar.add(new ProdId("-//Mooncal 1.0//EN"));
        calendar.add(ImmutableVersion.VERSION_2_0);
        calendar.add(ImmutableCalScale.GREGORIAN);
        if (updateFrequency > 0) {
            calendar.add(new XProperty("X-PUBLISHED-TTL", "P" + updateFrequency + "D"));
        }
        return calendar;
    }

    private void addEvent(Calendar calendar, EventInstance event, Url thankUrl) {
        final VEvent calEvent = new VEvent(event.getDateTime().toLocalDate(), event.getTitle());
        if (event.getDescription() != null) {
            calEvent.add(new Description(event.getDescription()));
        }
        calEvent.add(calculateUid(event));
        calEvent.add(thankUrl);
        calendar.add(calEvent);
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
}
