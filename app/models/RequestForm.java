package models;

import org.jetbrains.annotations.NotNull;
import play.data.format.Formatters;
import play.data.validation.Constraints;

import java.text.ParseException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    private List<String> phases = new ArrayList<>();
    private List<String> events = new ArrayList<>();
    @Constraints.Required
    private ZonedDateTime from;
    @Constraints.Required
    private ZonedDateTime to;

    public List<String> getPhases() {
        return phases;
    }

    public void setPhases(List<String> phases) {
        this.phases = phases;
    }

    public List<String> getEvents() {
        return events;
    }

    public void setEvents(List<String> events) {
        this.events = events;
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
        return phases.contains(name);
    }

    public boolean includeEvent(@NotNull String name) {
        return events.contains(name);
    }

}
