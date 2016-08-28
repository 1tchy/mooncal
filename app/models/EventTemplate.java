package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import play.i18n.Lang;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.function.BiFunction;

public class EventTemplate implements Comparable<EventInstance> {
    @NotNull
    protected ZonedDateTime dateTime;
    @Nullable
    protected final BiFunction<ZoneId, Lang, String> descriptionTemplate;
    @NotNull
    protected BiFunction<ZoneId, Lang, String> titleTemplate;
    @NotNull
    protected final String eventTypeId;

    public EventTemplate(@NotNull ZonedDateTime dateTime, @NotNull BiFunction<ZoneId, Lang, String> titleTemplate, @Nullable BiFunction<ZoneId, Lang, String> descriptionTemplate, @NotNull String eventTypeId) {
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
    public String getTitle(ZoneId timezone, Lang lang) {
        return titleTemplate.apply(timezone, lang);
    }

    @Nullable
    public String getDescription(ZoneId timezone, Lang lang) {
        return descriptionTemplate == null ? null : descriptionTemplate.apply(timezone, lang);
    }

    @NotNull
    public String getEventTypeId() {
        return eventTypeId;
    }

    @Override
    public int compareTo(@NotNull EventInstance other) {
        return this.dateTime.compareTo(other.dateTime);
    }

    @Override
    public String toString() {
        return titleTemplate.apply(ZoneOffset.UTC, Lang.forCode("en")) + "@" + dateTime;
    }
}
