package models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import play.i18n.Lang;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class EventInstance implements Comparable<EventInstance> {
    private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @NotNull
    private final ZonedDateTime dateTime;
    @Nullable
    private final ZonedDateTime endDateTime;
    @NotNull
    private final String title;
    @NotNull
    private final String pdfTitle;
    @Nullable
    private final String description;
    @NotNull
    private final String eventTypeId;

    public EventInstance(@NotNull EventTemplate eventTemplate, @NotNull ZoneId timezone, Lang lang) {
        this(eventTemplate.getDateTime(), eventTemplate.getTitle(timezone, lang), eventTemplate.getPdfTitle(timezone, lang), eventTemplate.getDescription(timezone, lang), timezone, eventTemplate.getEventTypeId());
    }

    public EventInstance(@NotNull ZonedDateTime dateTime, @NotNull String title, @NotNull String pdfTitle, @Nullable String description, @NotNull ZoneId timezone, @NotNull String eventTypeId) {
        this(dateTime, null, title, pdfTitle, description, timezone, eventTypeId);
    }

    public EventInstance(@NotNull ZonedDateTime dateTime, @Nullable ZonedDateTime endDateTime, @NotNull String title, @NotNull String pdfTitle, @Nullable String description, @NotNull ZoneId timezone, @NotNull String eventTypeId) {
        this.dateTime = dateTime.withZoneSameInstant(timezone);
        this.endDateTime = endDateTime == null ? null : endDateTime.withZoneSameInstant(timezone);
        this.title = title;
        this.pdfTitle = pdfTitle;
        this.description = description;
        this.eventTypeId = eventTypeId;
    }

    @NotNull
    public String getTitle() {
        return title;
    }

    @NotNull
    public String getPDFTitle() {
        return pdfTitle;
    }

    /**
     * @return the local wall-clock start ("yyyy-MM-dd'T'HH:mm") in the event's timezone. The frontend
     * localises the display format; it shows only the date for single-day events and the full date+time
     * for multi-day ranges.
     */
    public String getDate() {
        return dateTime.toLocalDateTime().format(LOCAL_DATE_TIME_FORMATTER);
    }

    /**
     * @return the local wall-clock end ("yyyy-MM-dd'T'HH:mm") for a multi-day event, or {@code null} for a
     * single-day event. Its presence is what tells the calendar preview to render a "from – to" range.
     */
    @Nullable
    public String getEndDate() {
        return isMultiDay() ? endDateTime.toLocalDateTime().format(LOCAL_DATE_TIME_FORMATTER) : null;
    }

    public LocalDate getLocalDate() {
        return dateTime.toLocalDate();
    }

    @Nullable
    public ZonedDateTime getEndDateTime() {
        return endDateTime;
    }

    public LocalDate getEndLocalDate() {
        return endDateTime != null ? endDateTime.toLocalDate() : getLocalDate();
    }

    public boolean isMultiDay() {
        return endDateTime != null && getEndLocalDate().isAfter(getLocalDate());
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @NotNull
    public ZonedDateTime getDateTime() {
        return dateTime;
    }

    @NotNull
    public String getEventTypeId() {
        return eventTypeId;
    }

    public boolean isGardenEvent() {
        return eventTypeId.startsWith("garden-");
    }

    public int compareTo(@NotNull EventInstance other) {
        int byTime = dateTime.compareTo(other.getDateTime());
        return byTime != 0 ? byTime : eventTypeId.compareTo(other.eventTypeId);
    }

    public String toString() {
        return title + "@" + dateTime;
    }
}
