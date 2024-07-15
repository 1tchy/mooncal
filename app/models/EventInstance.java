package models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import play.i18n.Lang;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class EventInstance implements Comparable<EventInstance> {
    @NotNull
    private final ZonedDateTime dateTime;
    @NotNull
    private final String title;
    @Nullable
    private final String description;
    @NotNull
    private final ZoneId timezone;
    @NotNull
    private final String eventTypeId;

    public EventInstance(@NotNull EventTemplate eventTemplate, @NotNull ZoneId timezone, Lang lang) {
        this(eventTemplate.getDateTime(), eventTemplate.getTitle(timezone, lang), eventTemplate.getDescription(timezone, lang), timezone, eventTemplate.getEventTypeId());
    }

    public EventInstance(@NotNull ZonedDateTime dateTime, @NotNull String title, @Nullable String description, @NotNull ZoneId timezone, @NotNull String eventTypeId) {
        this.dateTime = dateTime;
        this.title = title;
        this.description = description;
        this.timezone = timezone;
        this.eventTypeId = eventTypeId;
    }

    @NotNull
    public String getTitle() {
        return title;
    }

    public String getDate() {
        return dateTime.withZoneSameInstant(timezone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
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
