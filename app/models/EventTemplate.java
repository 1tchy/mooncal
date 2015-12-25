package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.function.Function;

public class EventTemplate implements Comparable<ZonedEvent> {
    @NotNull
    protected ZonedDateTime dateTime;
    @Nullable
    protected final Function<ZoneId, String> descriptionTemplate;
    @NotNull
    protected Function<ZoneId, String> titleTemplate;
    @NotNull
    protected final String eventTypeId;

    public EventTemplate(@NotNull ZonedDateTime dateTime, @NotNull Function<ZoneId, String> titleTemplate, @Nullable Function<ZoneId, String> descriptionTemplate, @NotNull String eventTypeId) {
        this.titleTemplate = titleTemplate;
        this.dateTime = dateTime;
        this.descriptionTemplate = descriptionTemplate;
        this.eventTypeId = eventTypeId;
    }

    @NotNull
    @JsonIgnore
    public ZonedDateTime getDateTime() {
        return dateTime;
    }

    @NotNull
    public String getTitle(ZoneId timezone) {
        return titleTemplate.apply(timezone);
    }

    @Nullable
    public String getDescription(ZoneId timezone) {
        return descriptionTemplate == null ? null : descriptionTemplate.apply(timezone);
    }

    @NotNull
    public String getEventTypeId() {
        return eventTypeId;
    }

    @Override
    public int compareTo(@NotNull ZonedEvent other) {
        return this.dateTime.compareTo(other.dateTime);
    }

    @Override
    public String toString() {
        return titleTemplate.apply(ZoneOffset.UTC) + "@" + dateTime;
    }
}
