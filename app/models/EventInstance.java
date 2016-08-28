package models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import play.i18n.Lang;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BiFunction;

public class EventInstance extends EventTemplate {
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
        super(dateTime, (zoneId, langIgnored) -> title, descriptionTemplate, eventTypeId);
        this.timezone = timezone;
        this.lang = lang;
    }

    public String getTitle() {
        return getTitle(timezone, lang);
    }

    @JsonProperty("date")
    public String getDateString() {
        return dateTime.withZoneSameInstant(timezone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    @Nullable
    public String getDescription() {
        return getDescription(timezone, lang);
    }

}
