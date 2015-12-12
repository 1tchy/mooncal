package models;

import org.jetbrains.annotations.NotNull;
import play.data.format.Formatters;
import play.data.validation.Constraints;

import java.text.ParseException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RequestForm {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSX");

    static {
        Formatters.register(ZonedDateTime.class, new Formatters.SimpleFormatter<ZonedDateTime>() {
            @Override
            public ZonedDateTime parse(String input, Locale l) throws ParseException {
                return ZonedDateTime.parse(input, DATE_TIME_FORMATTER);
            }

            @Override
            public String print(ZonedDateTime input, Locale l) {
                return DATE_TIME_FORMATTER.format(input);
            }
        });
    }

    private Map<String, Boolean> phases = new HashMap<>();
    private Map<String, Boolean> events = new HashMap<>();
    @Constraints.Required
    private ZonedDateTime from;
    @Constraints.Required
    private ZonedDateTime to;

    public Map<String, Boolean> getPhases() {
        return phases;
    }

    public void setPhases(Map<String, Boolean> phases) {
        if (phases != null) {
            this.phases = phases;
        }
    }

    public Map<String, Boolean> getEvents() {
        return events;
    }

    public void setEvents(Map<String, Boolean> events) {
        if (events != null) {
            this.events = events;
        }
    }

    public ZonedDateTime getFrom() {
        return from;
    }

    public void setFrom(ZonedDateTime from) {
        this.from = from;
    }

    public ZonedDateTime getTo() {
        return to;
    }

    public void setTo(ZonedDateTime to) {
        this.to = to;
    }

    public boolean includePhase(@NotNull String name) {
        return phases.get(name) == Boolean.TRUE;
    }

    public boolean includeEvent(@NotNull String name) {
        return events.get(name) == Boolean.TRUE;
    }

}
