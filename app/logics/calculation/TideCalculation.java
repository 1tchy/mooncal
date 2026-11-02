package logics.calculation;

import io.openwaters.slackwater.ExtremeKind;
import io.openwaters.slackwater.TideExtreme;
import logics.tides.TideStation;
import logics.tides.TideStations;
import models.EventInstance;
import models.MoonPhaseType;
import models.RequestForm;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import javax.inject.Inject;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static logics.calculation.MoonPhasesCalculation.RISE_SYMBOL;
import static logics.calculation.MoonPhasesCalculation.SET_SYMBOL;

/**
 * One entry per day with the high and low tides at the nearest tide station, or at the one chosen when none is near.
 */
public class TideCalculation extends Calculation {

    public static final String TIDE_EVENT_TYPE_ID = "tide";
    private static final char TIDE_SYMBOL = '≈';
    // Farther away, the tides of a station can be hours off.
    public static final double MAX_STATION_DISTANCE_KM = 50;
    // Nearer, the distance says less than the rounding of the location to 0.1° (~11 km).
    static final double MIN_SHOWN_DISTANCE_KM = Math.min(1.5 * TideStations.KM_PER_DEGREE / RequestForm.LOCATION_PRECISION, MAX_STATION_DISTANCE_KM);
    // The constants of today's tide gauges say little about the tides of centuries away.
    static final int FIRST_YEAR = 1900;
    static final int LAST_YEAR = 2100;
    // The extremes of each chunk are searched a bit beyond it, so that none at its border gets lost.
    private static final Duration CHUNK_MARGIN = Duration.ofHours(1);

    private final TideStations tideStations;

    @Inject
    public TideCalculation(MessagesApi messagesApi, TideStations tideStations) {
        super(messagesApi);
        this.tideStations = tideStations;
    }

    @Override
    public void calculate(RequestForm requestForm, Collection<EventInstance> eventCollection) {
        if (!requestForm.includePhase(MoonPhaseType.TIDE)) {
            return;
        }
        // unless it is no longer in the data, e.g. for a subscription
        Optional<TideStation> chosen = Optional.ofNullable(requestForm.getTideStation()).flatMap(tideStations::byId);
        if (chosen.isPresent()) {
            // without its distance, as the location is only sent when other entries need it
            calculate(requestForm, chosen.get(), messagesApi.get(requestForm.getLang(), "phases.tide.stationWithoutDistance", chosen.get().label()), eventCollection);
        } else if (requestForm.hasLocation()) {
            tideStations.nearestWithin(requestForm.getLat(), requestForm.getLon(), MAX_STATION_DISTANCE_KM).ifPresent(nearest -> {
                String stationText = nearest.km() < MIN_SHOWN_DISTANCE_KM
                        ? messagesApi.get(requestForm.getLang(), "phases.tide.stationWithoutDistance", nearest.station().label())
                        : messagesApi.get(requestForm.getLang(), "phases.tide.station", nearest.station().label(), Math.round(nearest.km()));
                calculate(requestForm, nearest.station(), stationText, eventCollection);
            });
        }
    }

    private void calculate(RequestForm requestForm, TideStation station, String stationText, Collection<EventInstance> eventCollection) {
        LocalDate from = max(requestForm.getFrom().toLocalDate(), LocalDate.of(FIRST_YEAR, 1, 1));
        LocalDate to = min(requestForm.getTo().toLocalDate(), LocalDate.of(LAST_YEAR, 12, 31));
        // a year at a time, so the extremes of a long time frame are never all in memory at once
        for (LocalDate chunkStart = from; !chunkStart.isAfter(to); chunkStart = chunkStart.plusYears(1).withDayOfYear(1)) {
            LocalDate chunkEnd = min(chunkStart.withMonth(12).withDayOfMonth(31), to);
            calculateChunk(requestForm, chunkStart, chunkEnd, station, stationText, eventCollection);
        }
    }

    private void calculateChunk(RequestForm requestForm, LocalDate from, LocalDate to, TideStation station, String stationText, Collection<EventInstance> eventCollection) {
        ZoneId zone = requestForm.getZone();
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();
        // grouped by the minute shown, so an extreme at 23:59:40 is at 0:00 of the next day
        SortedMap<LocalDate, List<TideExtreme>> byDay = new TreeMap<>();
        for (TideExtreme extreme : station.prediction().extremes(start.minus(CHUNK_MARGIN), end.plus(CHUNK_MARGIN))) {
            Instant shown = toMinute(extreme.getTime());
            if (!shown.isBefore(start) && shown.isBefore(end)) {
                byDay.computeIfAbsent(shown.atZone(zone).toLocalDate(), day -> new ArrayList<>()).add(extreme);
            }
        }
        byDay.forEach((day, extremes) -> eventCollection.add(dayEvent(requestForm.getLang(), zone, day, extremes, station.attribution(), stationText)));
    }

    private EventInstance dayEvent(Lang lang, ZoneId zone, LocalDate day, List<TideExtreme> extremes, String attribution, String stationText) {
        StringJoiner highs = new StringJoiner(" ");
        StringJoiner lows = new StringJoiner(" ");
        StringJoiner description = new StringJoiner("\n");
        for (TideExtreme extreme : extremes) {
            String time = toMinute(extreme.getTime()).atZone(zone).format(TIME_FORMATTER);
            boolean high = extreme.getKind() == ExtremeKind.HIGH;
            (high ? highs : lows).add(time);
            description.add(messagesApi.get(lang, high ? "phases.tide.highAt" : "phases.tide.lowAt", time));
        }
        description.add(stationText);
        description.add(messagesApi.get(lang, "phases.tide.disclaimer"));
        StringJoiner title = new StringJoiner(" · ");
        // the PDF's day cells are narrow, so tides are a symbol there
        StringJoiner pdfTitle = new StringJoiner(" ", String.valueOf(TIDE_SYMBOL), "");
        if (highs.length() > 0) {
            title.add(messagesApi.get(lang, "phases.tide.high") + " " + RISE_SYMBOL + highs);
            pdfTitle.add(RISE_SYMBOL + highs.toString());
        }
        if (lows.length() > 0) {
            title.add(messagesApi.get(lang, "phases.tide.low") + " " + SET_SYMBOL + lows);
            pdfTitle.add(SET_SYMBOL + lows.toString());
        }
        return new EventInstance(day.atTime(12, 0).atZone(zone), title.toString(), pdfTitle.toString(), description.toString(), zone, TIDE_EVENT_TYPE_ID, attribution);
    }

    private static Instant toMinute(Instant instant) {
        return instant.plusSeconds(30).truncatedTo(ChronoUnit.MINUTES);
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
