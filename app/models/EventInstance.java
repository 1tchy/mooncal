package models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import play.i18n.Lang;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class EventInstance implements Comparable<EventInstance> {
    private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    // A garden period that only just touches an edge day is not counted on that day: it is dropped from
    // its first day when it starts after this time, and from its last day when it ends before the other.
    private static final LocalTime GARDEN_EDGE_LATE_START = LocalTime.of(22, 0);
    private static final LocalTime GARDEN_EDGE_EARLY_END = LocalTime.of(6, 0);

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

    /**
     * First local day on which this event should be shown. For a multi-day garden period that starts late
     * at night (after 22:00) the first day is dropped, since the period barely touches it. Used by both the
     * PDF and the iCal renderer so they agree on which days a period covers.
     */
    public LocalDate getDisplayStartLocalDate() {
        if (isGardenEvent() && isMultiDay() && dateTime.toLocalTime().isAfter(GARDEN_EDGE_LATE_START)) {
            LocalDate trimmed = getLocalDate().plusDays(1);
            if (!trimmed.isAfter(getDisplayEndLocalDateRaw())) {
                return trimmed;
            }
        }
        return getLocalDate();
    }

    /**
     * Last local day on which this event should be shown. For a multi-day garden period that ends early in
     * the morning (before 06:00) the last day is dropped. Mirror of {@link #getDisplayStartLocalDate()}.
     */
    public LocalDate getDisplayEndLocalDate() {
        LocalDate trimmed = getDisplayEndLocalDateRaw();
        return trimmed.isBefore(getLocalDate()) ? getEndLocalDate() : trimmed;
    }

    private LocalDate getDisplayEndLocalDateRaw() {
        if (isGardenEvent() && isMultiDay() && endDateTime.toLocalTime().isBefore(GARDEN_EDGE_EARLY_END)) {
            return getEndLocalDate().minusDays(1);
        }
        return getEndLocalDate();
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
