package logics.calculation;

import logics.astronomy.Ephemeris;
import org.simpleastronomy.lib.MoonPhaseFinder;
import models.*;
import org.jetbrains.annotations.NotNull;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import javax.inject.Inject;
import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MoonPhasesCalculation extends Calculation {

    private final DateTimeFormatter DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("u-M-d H:m");
    public static final String FULLMOON_EVENT_TYPE_ID = "fullmoon";
    public static final String NEWMOON_EVENT_TYPE_ID = "newmoon";
    public static final String FIRST_QUARTER_EVENT_TYPE_ID = "first-quarter";
    public static final String LAST_QUARTER_EVENT_TYPE_ID = "last-quarter";
    // Public so the PDF renderer can draw them: its fonts have no arrow glyphs.
    public static final char MOONRISE_SYMBOL = '↑';
    public static final char MOONSET_SYMBOL = '↓';
    private final EnumMap<Hemisphere, EnumMap<EventStyle, ArrayList<EventTemplate>>> moonPhases1700to2100 = new EnumMap<>(Arrays.stream(Hemisphere.values()).collect(Collectors.toMap(
            Function.identity(),
            hemisphere -> new EnumMap<>(Arrays.stream(EventStyle.values()).collect(Collectors.toMap(
                    Function.identity(),
                    style -> new ArrayList<>()
            )))
    )));

    private final Ephemeris ephemeris;

    @Inject
    public MoonPhasesCalculation(MessagesApi messagesApi, Ephemeris ephemeris) {
        super(messagesApi);
        this.ephemeris = ephemeris;
        initializeMoonPhases();
    }

    private void initializeMoonPhases() {
        CSVUtil.load(Objects.requireNonNull(getClass().getResource("moon-phases/moon-phases.csv")).getFile(), rows -> {
            ZonedDateTime date = LocalDateTime.parse(rows[1], DATE_TIME_PATTERN).atZone(ZoneOffset.UTC);
            MoonPhase phase = switch (rows[0]) {
                case "1" -> MoonPhase.NEWMOON;
                case "2" -> MoonPhase.FIRST_QUARTER;
                case "3" -> MoonPhase.FULLMOON;
                case "4" -> MoonPhase.LAST_QUARTER;
                default -> throw new IllegalArgumentException("Unexpected value: " + rows[1]);
            };
            String eventTypeId = switch (phase) {
                case NEWMOON -> NEWMOON_EVENT_TYPE_ID;
                case FIRST_QUARTER -> FIRST_QUARTER_EVENT_TYPE_ID;
                case FULLMOON -> FULLMOON_EVENT_TYPE_ID;
                case LAST_QUARTER -> LAST_QUARTER_EVENT_TYPE_ID;
            };
            for (Hemisphere hemisphere : Hemisphere.values()) {
                for (EventStyle style : EventStyle.values()) {
                    moonPhases1700to2100.get(hemisphere).get(style).add(new EventTemplate.WithZoneId(
                            date,
                            (zoneId, lang) -> phase.getTitle(messagesApi, lang, date, style, hemisphere),
                            (zoneId, lang) -> phase.getPdfTitle(messagesApi, lang, date, style, hemisphere),
                            (zoneId, lang) -> eventAt(date, phase.getSimpleName(messagesApi, lang), zoneId, lang),
                            eventTypeId)
                    );
                }
            }
        });
        moonPhases1700to2100.values().forEach(byStyle -> byStyle.values().forEach(ArrayList::trimToSize));
    }

    @Override
    public void calculate(RequestForm requestForm, Collection<EventInstance> eventCollection) {
        final ZonedDateTime fromMorning = requestForm.getFrom().withHour(0).withMinute(0).withSecond(0);
        final ZonedDateTime toNight = requestForm.getTo().withHour(23).withMinute(59).withSecond(59);
        final Hemisphere hemisphere = requestForm.getHemisphere();
        if (requestForm.includePhase(MoonPhaseType.FULLMOON)) {
            load(fromMorning, toNight, MoonPhaseFinder::findFullMoonFollowing, MoonPhase.FULLMOON, eventCollection, requestForm.getLang(), requestForm.getStyle(), hemisphere, FULLMOON_EVENT_TYPE_ID);
        }
        if (requestForm.includePhase(MoonPhaseType.NEWMOON)) {
            load(fromMorning, toNight, MoonPhaseFinder::findNewMoonFollowing, MoonPhase.NEWMOON, eventCollection, requestForm.getLang(), requestForm.getStyle(), hemisphere, NEWMOON_EVENT_TYPE_ID);
        }
        if (requestForm.includePhase(MoonPhaseType.QUARTER)) {
            load(fromMorning, toNight, MoonPhaseFinder::findFirsQuarterFollowing, MoonPhase.FIRST_QUARTER, eventCollection, requestForm.getLang(), requestForm.getStyle(), hemisphere, FIRST_QUARTER_EVENT_TYPE_ID);
            load(fromMorning, toNight, MoonPhaseFinder::findLastQuarterFollowing, MoonPhase.LAST_QUARTER, eventCollection, requestForm.getLang(), requestForm.getStyle(), hemisphere, LAST_QUARTER_EVENT_TYPE_ID);
        }
        if (requestForm.includePhase(MoonPhaseType.DAILY)) {
            calculateDailyEvents(requestForm, eventCollection);
        }
    }

    private void load(ZonedDateTime from, ZonedDateTime to, Function<ZonedDateTime, ZonedDateTime> moonCalculation, MoonPhase phase, Collection<EventInstance> eventCollection, Lang lang, EventStyle style, Hemisphere hemisphere, String eventTypeId) {
        if (from.toLocalDate().isAfter(LocalDate.of(1970, 1, 1)) &&
                to.toLocalDate().isBefore(LocalDate.of(2100, 12, 31))) {
            fromCSV(from, to, eventCollection, lang, style, hemisphere, eventTypeId);
        } else {
            calculate(from, to, moonCalculation, phase, eventCollection, lang, style, hemisphere, eventTypeId);
        }
    }

    private void fromCSV(ZonedDateTime from, ZonedDateTime to, Collection<EventInstance> eventCollection, Lang lang, EventStyle style, Hemisphere hemisphere, String eventTypeId) {
        ZonedDateTime utcFrom = from.withZoneSameInstant(ZoneOffset.UTC);
        ZonedDateTime utcTo = to.withZoneSameInstant(ZoneOffset.UTC);
        moonPhases1700to2100.get(hemisphere).get(style == null ? EventStyle.FULLMOON : style).stream()
                .dropWhile(eventTemplate -> eventTemplate.getDateTime().isBefore(utcFrom))
                .takeWhile(eventTemplate -> eventTemplate.getDateTime().isBefore(utcTo))
                .filter(eventTemplate -> eventTemplate.getEventTypeId().equals(eventTypeId))
                .map(eventTemplate -> new EventInstance(eventTemplate, from.getZone(), lang))
                .forEach(eventCollection::add);
    }

    private void calculate(ZonedDateTime from, ZonedDateTime to, Function<ZonedDateTime, ZonedDateTime> moonCalculation, MoonPhase phase, Collection<EventInstance> eventCollection, Lang lang, EventStyle style, Hemisphere hemisphere, String eventTypeId) {
        while (true) {
            final ZonedDateTime moonHappening = moonCalculation.apply(from);
            if (moonHappening.isAfter(to)) {
                break;
            }
            eventCollection.add(new EventInstance(
                    moonHappening,
                    phase.getTitle(messagesApi, lang, moonHappening, style, hemisphere),
                    phase.getPdfTitle(messagesApi, lang, moonHappening, style, hemisphere),
                    eventAt(moonHappening, phase.getSimpleName(messagesApi, lang), from.getZone(), lang),
                    from.getZone(),
                    eventTypeId));
            from = moonHappening.plusDays((int) Math.floor(MoonPhase.MOON_CYCLE_DAYS) - 1);
        }
    }

    private void calculateDailyEvents(RequestForm requestForm, Collection<EventInstance> eventCollection) {
        ZoneId at = requestForm.getZone();
        LocalDate to = requestForm.getTo().toLocalDate();
        for (LocalDate day = requestForm.getFrom().toLocalDate(); !day.isAfter(to); day = day.plusDays(1)) {
            eventCollection.add(calculateDailyEvent(requestForm, day, at));
        }
    }

    @NotNull
    private EventInstance calculateDailyEvent(RequestForm requestForm, LocalDate day, ZoneId at) {
        Lang lang = requestForm.getLang();
        String title = messagesApi.get(lang, "phases.daily.visibility", getMoonVisiblePercent(day.atTime(12, 0).atZone(at), new DecimalFormat("0")));
        final DecimalFormat precise = new DecimalFormat("0.0");
        final String moonVisiblePercentMorning = getMoonVisiblePercent(day.atTime(6, 0).atZone(at), precise);
        final String moonVisiblePercentAtNoon = getMoonVisiblePercent(day.atTime(12, 0).atZone(at), precise);
        final String moonVisiblePercentEvening = getMoonVisiblePercent(day.atTime(18, 0).atZone(at), precise);
        final String moonVisiblePercentMidnight = getMoonVisiblePercent(day.plusDays(1).atTime(0, 0).atZone(at), precise);
        String description = messagesApi.get(lang, "phases.daily.visibility.morning6", moonVisiblePercentMorning) + "\n" +
                messagesApi.get(lang, "phases.daily.visibility.noon12", moonVisiblePercentAtNoon) + "\n" +
                messagesApi.get(lang, "phases.daily.visibility.evening6", moonVisiblePercentEvening) + "\n" +
                messagesApi.get(lang, "phases.daily.visibility.midnight", moonVisiblePercentMidnight);
        if (requestForm.hasLocation()) {
            RiseSetTexts riseSet = calculateRiseSet(requestForm, day);
            if (!riseSet.titleSuffix().isEmpty()) {
                title += " " + riseSet.titleSuffix();
            }
            description += "\n\n" + riseSet.description();
        }
        return new EventInstance(day.atTime(12, 0).atZone(at), title, title, description, at, "daily");
    }

    private record RiseSetTexts(String titleSuffix, String description) {
    }

    private record HorizonCrossing(Instant when, char symbol, String messageKey) {
    }

    private RiseSetTexts calculateRiseSet(RequestForm requestForm, LocalDate day) {
        Lang lang = requestForm.getLang();
        ZoneId zone = requestForm.getZone();
        Ephemeris.RiseSet riseSet = ephemeris.moonRiseSet(
                day.atStartOfDay(zone).toInstant(), day.plusDays(1).atStartOfDay(zone).toInstant(),
                requestForm.getLat(), requestForm.getLon());
        List<HorizonCrossing> crossings = new ArrayList<>();
        riseSet.rise().ifPresent(rise -> crossings.add(new HorizonCrossing(rise, MOONRISE_SYMBOL, "phases.daily.moonrise")));
        riseSet.set().ifPresent(set -> crossings.add(new HorizonCrossing(set, MOONSET_SYMBOL, "phases.daily.moonset")));
        crossings.sort(Comparator.comparing(HorizonCrossing::when));
        StringJoiner titleSuffix = new StringJoiner(" ");
        StringJoiner description = new StringJoiner("\n");
        for (HorizonCrossing crossing : crossings) {
            String time = formatToMinute(crossing.when(), zone);
            titleSuffix.add(crossing.symbol() + time);
            description.add(messagesApi.get(lang, crossing.messageKey(), time));
        }
        if (crossings.isEmpty()) {
            String key = riseSet.upAtStart() ? "phases.daily.alwaysUp" : "phases.daily.alwaysDown";
            description.add(messagesApi.get(lang, key));
        } else if (riseSet.rise().isEmpty()) {
            description.add(messagesApi.get(lang, "phases.daily.noMoonrise"));
        } else if (riseSet.set().isEmpty()) {
            description.add(messagesApi.get(lang, "phases.daily.noMoonset"));
        }
        description.add(messagesApi.get(lang, "phases.daily.location", formatLocation(requestForm.getLat(), requestForm.getLon())));
        return new RiseSetTexts(titleSuffix.toString(), description.toString());
    }

    private static String formatToMinute(Instant instant, ZoneId zone) {
        return instant.plusSeconds(30).truncatedTo(ChronoUnit.MINUTES).atZone(zone).format(TIME_FORMATTER);
    }

    private static String formatLocation(double lat, double lon) {
        return String.format(Locale.ROOT, "%.1f° %s, %.1f° %s", Math.abs(lat), lat < 0 ? "S" : "N", Math.abs(lon), lon < 0 ? "W" : "E");
    }

    private String getMoonVisiblePercent(ZonedDateTime dateTime, DecimalFormat format) {
        return format.format(MoonPhaseFinder.getMoonVisiblePercent(dateTime) * 100);
    }

    public CurrentMoonPhase getCurrentMoonPhase() {
        ZonedDateTime now = ZonedDateTime.now();
        int moonAngle = (int) Math.round(MoonPhaseFinder.getMoonAngle(now));
        int moonVisiblePercent = (int) Math.round(MoonPhaseFinder.getMoonVisiblePercent(now) * 100);
        return new CurrentMoonPhase(moonVisiblePercent, moonAngle < 180);
    }

    public record CurrentMoonPhase(int visibility, boolean isWaxing) {
        /**
         * @return A value from 0% (new moon) to 100% (full moon) to 200% (new moon again)
         */
        public int getPhaseVisibilityPercentage() {
            return isWaxing ? visibility : 200 - visibility;
        }
    }
}
