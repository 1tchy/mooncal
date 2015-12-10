package models;

import org.jetbrains.annotations.NotNull;
import play.data.validation.Constraints;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RequestForm {
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

    public void setFrom(String from) {
        this.from = parseDate(from);
    }

    public void setFrom2(ZonedDateTime from) {
        this.from = from;
    }

    public ZonedDateTime getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = parseDate(to);
    }

    public void setTo2(ZonedDateTime to) {
        this.to = to;
    }

    private ZonedDateTime parseDate(String date) {
        return ZonedDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSX"));
    }

    public boolean includePhase(@NotNull String name) {
        return phases.contains(name);
    }

    public boolean includeEvent(@NotNull String name) {
        return events.contains(name);
    }

}
