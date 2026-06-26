package logics.calculation;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Singleton
public class MoonPhaseData {

    private static final DateTimeFormatter DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("u-M-d H:m");
    private final NavigableMap<ZonedDateTime, MoonPhase> phases = new TreeMap<>();

    @Inject
    public MoonPhaseData() {
        CSVUtil.load(Objects.requireNonNull(getClass().getResource("moon-phases/moon-phases.csv")).getFile(), rows -> {
            ZonedDateTime date = LocalDateTime.parse(rows[1], DATE_TIME_PATTERN).atZone(ZoneOffset.UTC);
            MoonPhase phase = switch (rows[0]) {
                case "1" -> MoonPhase.NEWMOON;
                case "2" -> MoonPhase.FIRST_QUARTER;
                case "3" -> MoonPhase.FULLMOON;
                case "4" -> MoonPhase.LAST_QUARTER;
                default -> throw new IllegalArgumentException("Unexpected value: " + rows[0]);
            };
            phases.put(date, phase);
        });
    }

    public NavigableMap<ZonedDateTime, MoonPhase> phases() {
        return Collections.unmodifiableNavigableMap(phases);
    }
}
