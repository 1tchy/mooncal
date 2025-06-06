package models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import play.i18n.Lang;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class EventInstance implements Comparable<EventInstance> {
    @NotNull
    private final ZonedDateTime dateTime;
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
        this.dateTime = dateTime.withZoneSameInstant(timezone);
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

    public String getDate() {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public LocalDate getLocalDate() {
        return dateTime.toLocalDate();
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

    public int compareTo(@NotNull EventInstance other) {
        return dateTime.compareTo(other.getDateTime());
    }

    public String toString() {
        return title + "@" + dateTime;
    }
}
