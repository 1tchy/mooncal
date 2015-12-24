package models;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import play.data.format.Formatters;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;

import java.text.ParseException;
import java.time.ZonedDateTime;
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
    }

    private Map<MoonPhaseType, Boolean> phases = new HashMap<>();
    private Map<String, Boolean> events = new HashMap<>();
    @Constraints.Required
    private ZonedDateTime from;
    @Constraints.Required
    private ZonedDateTime to;

    public Map<MoonPhaseType, Boolean> getPhases() {
        return phases;
    }

    public void setPhases(Map<MoonPhaseType, Boolean> phases) {
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

    public boolean includePhase(@NotNull MoonPhaseType moonPhaseType) {
        return phases.get(moonPhaseType) == Boolean.TRUE;
    }

    public boolean includeEvent(@NotNull String name) {
        return events.get(name) == Boolean.TRUE;
    }

    @SuppressWarnings("unused") //used by Play
    public List<ValidationError> validate() {
        if (includePhase(MoonPhaseType.DAILY) && from.until(to, ChronoUnit.YEARS) > MAX_YEARS_FOR_DAILY_PHASES) {
            return Lists.newArrayList(new ValidationError(MAX_YEARS_FOR_DAILY_PHASES + "", "error.fromTo.toolargefordaily"));
        }
        return null;//form is fine
    }

}
