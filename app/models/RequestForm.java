package models;

import org.jetbrains.annotations.NotNull;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;
import play.i18n.Lang;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Constraints.Validate
public class RequestForm implements Constraints.Validatable<ValidationError> {

    private static final int MAX_YEARS_FOR_DAILY_PHASES = 200;

    private Map<MoonPhaseType, Boolean> phases = new HashMap<>();
    private EventStyle style;
    private Map<EventType, Boolean> events = new HashMap<>();
    @Constraints.Required
    private LocalDateTime from;
    @Constraints.Required
    private LocalDateTime to;
    @Constraints.Required
    private ZoneId zone;
    @Constraints.Required
    private Lang lang;
    private Long created;

    public Map<MoonPhaseType, Boolean> getPhases() {
        return phases;
    }

    public void setPhases(Map<MoonPhaseType, Boolean> phases) {
        if (phases != null) {
            this.phases = phases;
        }
    }

    public EventStyle getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = EventStyle.of(style);
    }

    public Map<EventType, Boolean> getEvents() {
        return events;
    }

    public void setEvents(Map<EventType, Boolean> events) {
        if (events != null) {
            this.events = events;
        }
    }

    public ZonedDateTime getFrom() {
        return from == null ? null : from.atZone(zone);
    }

    public void setFrom(ZonedDateTime from) {
        this.from = from.toLocalDateTime();
        setZone(from.getZone());
    }

    public ZonedDateTime getTo() {
        return to == null ? null : to.atZone(zone);
    }

    public void setTo(ZonedDateTime to) {
        this.to = to.toLocalDateTime();
        setZone(to.getZone());
    }

    public ZoneId getZone() {
        return zone;
    }

    public void setZone(ZoneId zone) {
        this.zone = zone;
    }

    public Lang getLang() {
        return lang;
    }

    public void setLang(Lang lang) {
        this.lang = lang;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    /**
     * Sets the "from" to a date before today to form a floating timeframe around today for subscriptions
     */
    public void setBefore(Period before) {
        from = getToday().minus(before).atStartOfDay();
    }

    /**
     * Sets the "to" to a date after today to form a floating timeframe around today for subscriptions
     */
    public void setAfter(Period after) {
        to = getToday().plus(after).plusDays(1).atStartOfDay();
    }

    private static LocalDate getToday() {
        if (System.getProperty("today") != null) {
            return LocalDate.parse(System.getProperty("today"));
        }
        return LocalDate.now(ZoneOffset.UTC);
    }

    public boolean includePhase(@NotNull MoonPhaseType moonPhaseType) {
        return phases.get(moonPhaseType) == Boolean.TRUE;
    }

    public boolean includeEvent(@NotNull EventType eventType) {
        return events.get(eventType) == Boolean.TRUE;
    }

    @SuppressWarnings("unused") //used by Play
    public ValidationError validate() {
        if (includePhase(MoonPhaseType.DAILY) && from.until(to, ChronoUnit.YEARS) > MAX_YEARS_FOR_DAILY_PHASES) {
            return new ValidationError(MAX_YEARS_FOR_DAILY_PHASES + "", "error.fromTo.tolargefordaily");
        }
        return null;//form is fine
    }

    public String calculateETag() {
        int eventsPart = 0;
        for (MoonPhaseType moonPhaseType : MoonPhaseType.values()) {
            eventsPart = eventsPart * 2 + (includePhase(moonPhaseType) ? 1 : 0);
        }
        for (EventType eventType : EventType.values()) {
            eventsPart = eventsPart * 2 + (includeEvent(eventType) ? 1 : 0);
        }
        StringBuilder sb = new StringBuilder("W/");
        sb.append(eventsPart);
        sb.append("x");
        sb.append(style.getStyle());
        sb.append("x");
        sb.append(from.toEpochSecond(ZoneOffset.UTC) / 3600 / 24);
        sb.append("x");
        sb.append(to.toEpochSecond(ZoneOffset.UTC) / 3600 / 24);
        if (zone != null) {
            sb.append(zone.getId());
        }
        sb.append("x");
        sb.append(lang.code());
        return sb.toString();
    }

    public String getForLog() {
        StringBuilder sb = new StringBuilder();
        for (MoonPhaseType moonPhaseType : MoonPhaseType.values()) {
            if (includePhase(moonPhaseType)) {
                sb.append(moonPhaseType.getKey());
                sb.append(" ");
            }
        }
        sb.append(style.getStyle()).append(" ");
        for (EventType eventType : EventType.values()) {
            if (includeEvent(eventType)) {
                sb.append(eventType.getKey());
                sb.append(" ");
            }
        }
        sb.append(from.atZone(ZoneOffset.UTC).toLocalDate().toString());
        sb.append(" to ");
        sb.append(to.atZone(ZoneOffset.UTC).toLocalDate().toString());
        if (zone != null) {
            sb.append(" ");
            sb.append(zone.getId());
        }
        sb.append(" (");
        sb.append(lang.code());
        sb.append(")");
        if (created != null) {
            sb.append(" #");
            sb.append(created);
        }
        return sb.toString();
    }
}
