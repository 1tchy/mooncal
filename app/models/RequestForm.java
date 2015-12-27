package models;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import play.data.format.Formatters;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;
import play.i18n.Lang;
import play.i18n.Messages;

import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RequestForm {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssVV");
    private static final int MAX_YEARS_FOR_DAILY_PHASES = 200;

    static {
        Formatters.register(ZonedDateTime.class, new Formatters.SimpleFormatter<ZonedDateTime>() {
            @Override
            public ZonedDateTime parse(String input, Locale l) throws ParseException {
                return ZonedDateTime.parse(input.replaceAll(" ", "+"), DATE_TIME_FORMATTER);
            }

            @Override
            public String print(ZonedDateTime input, Locale l) {
                return DATE_TIME_FORMATTER.format(input);
            }
        });
        Formatters.register(MoonPhaseType.class, new Formatters.SimpleFormatter<MoonPhaseType>() {
            @Override
            public MoonPhaseType parse(String input, Locale l) throws ParseException {
                return MoonPhaseType.read(input);
            }

            @Override
            public String print(MoonPhaseType input, Locale l) {
                return input.getKey();
            }
        });
        Formatters.register(EventType.class, new Formatters.SimpleFormatter<EventType>() {
            @Override
            public EventType parse(String input, Locale l) throws ParseException {
                return EventType.read(input);
            }

            @Override
            public String print(EventType input, Locale l) {
                return input.getKey();
            }
        });
        Formatters.register(Period.class, new Formatters.SimpleFormatter<Period>() {
            @Override
            public Period parse(String input, Locale l) throws ParseException {
                return Period.parse(input);
            }

            @Override
            public String print(Period input, Locale l) {
                return input.toString();
            }
        });
        Formatters.register(Lang.class, new Formatters.SimpleFormatter<Lang>() {
            @Override
            public Lang parse(String input, Locale l) throws ParseException {
                return Lang.forCode(input);
            }

            @Override
            public String print(Lang input, Locale l) {
                return Messages.get(input, "lang.current");
            }
        });
    }

    private Map<MoonPhaseType, Boolean> phases = new HashMap<>();
    private Map<EventType, Boolean> events = new HashMap<>();
    @Constraints.Required
    private LocalDateTime from;
    @Constraints.Required
    private LocalDateTime to;
    @Constraints.Required
    private ZoneId zone;
    private Lang lang;

    public Map<MoonPhaseType, Boolean> getPhases() {
        return phases;
    }

    public void setPhases(Map<MoonPhaseType, Boolean> phases) {
        if (phases != null) {
            this.phases = phases;
        }
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

    /**
     * Sets the "from" to a date before today to form a floating timeframe around today for subscriptions
     */
    public void setBefore(Period before) {
        from = LocalDate.now(ZoneOffset.UTC).minus(before).atStartOfDay();
    }

    /**
     * Sets the "to" to a date after today to form a floating timeframe around today for subscriptions
     */
    public void setAfter(Period after) {
        to = LocalDate.now(ZoneOffset.UTC).plus(after).plusDays(1).atStartOfDay();
    }

    public Lang getLang() {
        return lang;
    }

    public void setLang(Lang lang) {
        this.lang = lang;
    }

    public boolean includePhase(@NotNull MoonPhaseType moonPhaseType) {
        return phases.get(moonPhaseType) == Boolean.TRUE;
    }

    public boolean includeEvent(@NotNull EventType eventType) {
        return events.get(eventType) == Boolean.TRUE;
    }

    @SuppressWarnings("unused") //used by Play
    public List<ValidationError> validate() {
        if (includePhase(MoonPhaseType.DAILY) && from.until(to, ChronoUnit.YEARS) > MAX_YEARS_FOR_DAILY_PHASES) {
            return Lists.newArrayList(new ValidationError(MAX_YEARS_FOR_DAILY_PHASES + "", "error.fromTo.tolargefordaily"));
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
        sb.append(from.toEpochSecond(ZoneOffset.UTC) / 3600 / 24);
        sb.append("x");
        sb.append(to.toEpochSecond(ZoneOffset.UTC) / 3600 / 24);
        if (zone != null) {
            sb.append(zone.getId());
        }
        return sb.toString();
    }

}
