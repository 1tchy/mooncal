package models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

public class ZonedEvent extends EventTemplate {
    @NotNull
    private final ZoneId timezone;

    public ZonedEvent(@NotNull EventTemplate eventTemplate, @NotNull ZoneId timezone) {
        this(eventTemplate.dateTime, eventTemplate.getTitle(timezone), eventTemplate.descriptionTemplate, timezone, eventTemplate.eventTypeId);
    }

    public ZonedEvent(@NotNull ZonedDateTime dateTime, @NotNull String title, @Nullable String description, @NotNull ZoneId timezone, String eventTypeId) {
        this(dateTime, title, zoneId -> description, timezone, eventTypeId);
    }

    private ZonedEvent(@NotNull ZonedDateTime dateTime, @NotNull String title, @Nullable Function<ZoneId, String> descriptionTemplate, @NotNull ZoneId timezone, String eventTypeId) {
        super(dateTime, zoneId -> title, descriptionTemplate, eventTypeId);
        this.timezone = timezone;
    }

    public String getTitle() {
        return getTitle(timezone);
    }

    @JsonProperty("date")
    public String getDateString() {
        return dateTime.withZoneSameInstant(timezone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    @Nullable
    public String getDescription() {
        return getDescription(timezone);
    }

}
