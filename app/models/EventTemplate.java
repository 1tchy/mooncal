package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Function;

public class EventTemplate implements Comparable<ZonedEvent> {
    @NotNull
    protected ZonedDateTime dateTime;
    @Nullable
    protected final Function<ZoneId, String> descriptionTemplate;
    @NotNull
    protected String title;

    public EventTemplate(@NotNull ZonedDateTime dateTime, @NotNull String title, @Nullable Function<ZoneId, String> descriptionTemplate) {
        this.title = title;
        this.dateTime = dateTime;
        this.descriptionTemplate = descriptionTemplate;
    }

    @NotNull
    @JsonIgnore
    public ZonedDateTime getDateTime() {
        return dateTime;
    }

    @NotNull
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getDescription(ZoneId timezone) {
        return descriptionTemplate == null ? null : descriptionTemplate.apply(timezone);
    }

    @Override
    public int compareTo(@NotNull ZonedEvent other) {
        return this.dateTime.compareTo(other.dateTime);
    }

    @Override
    public String toString() {
        return title + "@" + dateTime;
    }
}
