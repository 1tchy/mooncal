package models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import play.i18n.Lang;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BiFunction;

public class EventInstance implements Comparable<EventInstance> {
    @NotNull
    private final EventTemplate template;
    @NotNull
    private final ZoneId timezone;
    @NotNull
    private final Lang lang;

    public EventInstance(@NotNull EventTemplate eventTemplate, @NotNull ZoneId timezone, Lang lang) {
        this(eventTemplate.dateTime, eventTemplate.getTitle(timezone, lang), eventTemplate.descriptionTemplate, timezone, lang, eventTemplate.eventTypeId);
    }

    public EventInstance(@NotNull ZonedDateTime dateTime, @NotNull String title, @Nullable String description, @NotNull ZoneId timezone, Lang lang, String eventTypeId) {
        this(dateTime, title, (zoneId, langIgnored) -> description, timezone, lang, eventTypeId);
    }

    private EventInstance(@NotNull ZonedDateTime dateTime, @NotNull String title, @Nullable BiFunction<ZoneId, Lang, String> descriptionTemplate, @NotNull ZoneId timezone, Lang lang, String eventTypeId) {
        template = new EventTemplate(dateTime, (zoneId, langIgnored) -> title, descriptionTemplate, eventTypeId);
        this.timezone = timezone;
        this.lang = lang;
    }

    public String getTitle() {
        return template.getTitle(timezone, lang);
    }

    public String getDate() {
        return template.dateTime.withZoneSameInstant(timezone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    @Nullable
    public String getDescription() {
        return template.getDescription(timezone, lang);
    }

    @NotNull
    public ZonedDateTime getDateTime() {
        return template.getDateTime();
    }

    @NotNull
    public String getEventTypeId() {
        return template.getEventTypeId();
    }

    public int compareTo(@NotNull EventInstance other) {
        return template.getDateTime().compareTo(other.template.getDateTime());
    }

    public String toString() {
        return template.titleTemplate.apply(timezone, lang) + "@" + template.getDateTime();
    }
}
