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
import javax.inject.Singleton;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static logics.calculation.MoonPhasesCalculation.RISE_SYMBOL;
import static logics.calculation.MoonPhasesCalculation.SET_SYMBOL;

/**
 * One entry per day with the high and low tides at the nearest tide station.
 */
@Singleton
public class TideCalculation extends Calculation {

    public static final String TIDE_EVENT_TYPE_ID = "tide";
    private static final char TIDE_SYMBOL = '≈';
    // Farther away, the tides of a station can be hours off.
    public static final double MAX_STATION_DISTANCE_KM = 50;
    // The constants of today's tide gauges say little about the tides of centuries away.
    static final int FIRST_YEAR = 1900;
    static final int LAST_YEAR = 2100;
    // The extremes of each chunk are searched a bit beyond it, so that none at its border gets lost.
    private static final Duration CHUNK_MARGIN = Duration.ofHours(1);

    private final TideStations tideStations;
    // the one shown at each rounded position, as another station at the same rounded position hides a station
    private final List<TideStation> shownStations;

    @Inject
    public TideCalculation(MessagesApi messagesApi, TideStations tideStations) {
        super(messagesApi);
        this.tideStations = tideStations;
        this.shownStations = shownStations(tideStations);
    }

    @Override
    public void calculate(RequestForm requestForm, Collection<EventInstance> eventCollection) {
        if (!requestForm.includePhase(MoonPhaseType.TIDE) || !requestForm.hasLocation()) {
            return;
        }
        station(requestForm.getLat(), requestForm.getLon()).ifPresent(nearest -> {
            String stationText = stationText(requestForm, nearest);
            LocalDate from = max(requestForm.getFrom().toLocalDate(), LocalDate.of(FIRST_YEAR, 1, 1));
            LocalDate to = min(requestForm.getTo().toLocalDate(), LocalDate.of(LAST_YEAR, 12, 31));
            // a year at a time, so the extremes of a long time frame are never all in memory at once
            for (LocalDate chunkStart = from; !chunkStart.isAfter(to); chunkStart = chunkStart.plusYears(1).withDayOfYear(1)) {
                LocalDate chunkEnd = min(chunkStart.withMonth(12).withDayOfMonth(31), to);
                calculateChunk(requestForm, chunkStart, chunkEnd, nearest.station(), stationText, eventCollection);
            }
        });
    }

    /**
     * @return the station whose tides are shown at a location: one at the location if there is one, so that a station chosen
     * in the hint is the one shown, even when another one is nearer to its rounded position
     */
    public Optional<TideStations.StationDistance> station(double lat, double lon) {
        double roundedLat = RequestForm.roundLocation(lat);
        double roundedLon = RequestForm.roundLocation(lon);
        return tideStations.nearest(roundedLat, roundedLon, station -> isAtLocation(station, roundedLat, roundedLon))
                .or(() -> tideStations.nearestWithin(roundedLat, roundedLon, MAX_STATION_DISTANCE_KM));
    }

    /**
     * @return the nearest stations whose tides are shown at their own position, to choose from in the hint
     */
    public List<TideStations.StationDistance> stationsToChoose(double lat, double lon, int count) {
        double roundedLat = RequestForm.roundLocation(lat);
        double roundedLon = RequestForm.roundLocation(lon);
        return shownStations.stream()
                .map(station -> new TideStations.StationDistance(station, TideStations.distanceKm(roundedLat, roundedLon, station.lat(), station.lon())))
                .sorted(Comparator.comparingDouble(TideStations.StationDistance::km))
                .limit(count)
                .toList();
    }

    /**
     * @return at each rounded position, the station that {@link #station(double, double)} shows there
     */
    private static List<TideStation> shownStations(TideStations tideStations) {
        Map<List<Double>, TideStation> shown = new LinkedHashMap<>();
        for (TideStation station : tideStations.stations()) {
            double lat = RequestForm.roundLocation(station.lat());
            double lon = RequestForm.roundLocation(station.lon());
            shown.merge(List.of(lat, lon), station, (kept, other) ->
                    TideStations.distanceKm(lat, lon, other.lat(), other.lon()) < TideStations.distanceKm(lat, lon, kept.lat(), kept.lon()) ? other : kept);
        }
        return List.copyOf(shown.values());
    }

    private static boolean isAtLocation(TideStation station, double lat, double lon) {
        return RequestForm.roundLocation(station.lat()) == lat && RequestForm.roundLocation(station.lon()) == lon;
    }

    private String stationText(RequestForm requestForm, TideStations.StationDistance nearest) {
        TideStation station = nearest.station();
        // its distance would only be that of the rounding
        if (isAtLocation(station, requestForm.getLat(), requestForm.getLon())) {
            return messagesApi.get(requestForm.getLang(), "phases.tide.stationAtLocation", station.label());
        }
        return messagesApi.get(requestForm.getLang(), "phases.tide.station", station.label(), Math.round(nearest.km()));
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
