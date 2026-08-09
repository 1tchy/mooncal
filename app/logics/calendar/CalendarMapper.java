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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collection;

public class CalendarMapper {

    // A single-day garden period trimmed against a bad day ends one second before midnight (see
    // GardenBiodynamicCalculation); that edge is an artefact, not a meaningful time.
    private static final LocalTime SEGMENT_DAY_END = LocalTime.of(23, 59, 59);

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
        boolean hasGarden = events.stream().anyMatch(EventInstance::isGardenEvent);
        if (hasGarden) {
            calendar.add(new XProperty("X-WR-CALDESC", messagesApi.get(language, "garden.disclaimer")));
        }
        Url thankUrl = new Url(URI.create("https://mooncal.ch/" + getThankUrl(language) + "?c=ics"));
        for (EventInstance event : events) {
            addEvent(calendar, event, thankUrl, language);
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

    private void addEvent(Calendar calendar, EventInstance event, Url thankUrl, Lang language) {
        final VEvent calEvent = event.isMultiDay()
                ? new VEvent(event.getDisplayStartLocalDate(), event.getDisplayEndLocalDate().plusDays(1), event.getTitle())
                : new VEvent(event.getDateTime().toLocalDate(), event.getTitle());
        String description = buildDescription(event, language);
        if (description != null) {
            calEvent.add(new Description(description));
        }
        calEvent.add(calculateUid(event));
        calEvent.add(thankUrl);
        calendar.add(calEvent);
    }

    /**
     * The event's description, prefixed for garden periods with their exact start/end time — which the
     * all-day ICS event itself cannot convey.
     */
    @Nullable
    private String buildDescription(EventInstance event, Lang language) {
        String description = event.getDescription();
        String period = gardenPeriodLine(event, language);
        if (period == null) {
            return description;
        }
        return description == null ? period : period + "\n" + description;
    }

    /**
     * The exact-time line prefixed to a garden event's description, or {@code null} when there is no
     * meaningful time (all-day bad days, or a full day sandwiched between two of them).
     *
     * <p>Multi-day periods show the full "start – end" range. A single-day period usually has one edge that
     * is only a midnight artefact of the day trimming, so we show just the other, meaningful boundary
     * (as a "starts"/"ends" line); if both edges are real times we still show the full range.
     */
    @Nullable
    private String gardenPeriodLine(EventInstance event, Lang language) {
        if (!event.isGardenEvent() || event.getEndDateTime() == null) {
            return null;
        }
        // Locale-aware date+time (e.g. de: "06.08.2026, 22:25"), driven by the calendar's language.
        DateTimeFormatter format = DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(language.toLocale());
        ZonedDateTime start = event.getDateTime();
        ZonedDateTime end = event.getEndDateTime();
        if (event.isMultiDay()) {
            return messagesApi.get(language, "garden.ics.period", start.format(format), end.format(format));
        }
        boolean startIsMidnight = start.toLocalTime().equals(LocalTime.MIDNIGHT);
        boolean endIsMidnight = end.toLocalTime().equals(SEGMENT_DAY_END)
                || end.toLocalTime().equals(LocalTime.MIDNIGHT);
        if (startIsMidnight && endIsMidnight) {
            return null; // whole day, both edges are artefacts
        }
        if (startIsMidnight) {
            return messagesApi.get(language, "garden.ics.endtime", end.format(format));
        }
        if (endIsMidnight) {
            return messagesApi.get(language, "garden.ics.starttime", start.format(format));
        }
        return messagesApi.get(language, "garden.ics.period", start.format(format), end.format(format));
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
