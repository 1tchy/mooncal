package logics.tides;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openwaters.slackwater.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;

/**
 * The tide stations exported from the Slackwater database (see tools/export-tide-stations.sh), loaded once.
 */
@Singleton
public class TideStations {

    static final String RESOURCE = "/logics/calculation/tides/tide-stations.json.gz";
    private static final double EARTH_RADIUS_KM = 6371;
    private final List<TideStation> stations;

    public record StationDistance(TideStation station, double km) {
    }

    @Inject
    public TideStations() {
        this(load());
    }

    TideStations(List<TideStation> stations) {
        this.stations = List.copyOf(stations);
    }

    public List<StationDistance> nearest(double lat, double lon, int count) {
        return stations.stream()
                .map(station -> new StationDistance(station, distanceKm(lat, lon, station.lat(), station.lon())))
                .sorted(Comparator.comparingDouble(StationDistance::km))
                .limit(count)
                .toList();
    }

    public Optional<StationDistance> nearestWithin(double lat, double lon, double maxKm) {
        return nearest(lat, lon, 1).stream().filter(nearest -> nearest.km() <= maxKm).findFirst();
    }

    public Optional<StationDistance> nearest(double lat, double lon, Predicate<TideStation> filter) {
        return stations.stream()
                .filter(filter)
                .map(station -> new StationDistance(station, distanceKm(lat, lon, station.lat(), station.lon())))
                .min(Comparator.comparingDouble(StationDistance::km));
    }

    public List<TideStation> stations() {
        return stations;
    }

    public int size() {
        return stations.size();
    }

    Optional<TideStation> byId(String id) {
        return stations.stream().filter(station -> station.id().equals(id)).findFirst();
    }

    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    private static List<TideStation> load() {
        JsonNode root;
        try (InputStream in = new GZIPInputStream(Objects.requireNonNull(TideStations.class.getResourceAsStream(RESOURCE)))) {
            root = new ObjectMapper().readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<String> attributions = new ArrayList<>();
        root.get("attributions").forEach(attribution -> attributions.add(attribution.asText()));
        // reference stations first, subordinate stations predict from them
        Map<String, Station> references = new HashMap<>();
        for (JsonNode station : root.get("stations")) {
            if (station.has("constituents")) {
                references.put(station.get("id").asText(), reference(station));
            }
        }
        List<TideStation> stations = new ArrayList<>();
        for (JsonNode station : root.get("stations")) {
            String id = station.get("id").asText();
            TideStation.TidePrediction prediction = station.has("constituents") ? references.get(id)::extremes : subordinate(station, references)::extremes;
            stations.add(new TideStation(id, station.get("label").asText(), station.get("lat").asDouble(), station.get("lon").asDouble(),
                    attributions.get(station.get("attribution").asInt()), prediction));
        }
        return stations;
    }

    private static Station reference(JsonNode station) {
        List<HarmonicConstituent> constituents = new ArrayList<>();
        for (JsonNode constituent : station.get("constituents")) {
            String name = constituent.get(0).asText();
            if (ConstituentAstronomyKt.constituentAstronomy(name, Instant.EPOCH) == null) {
                throw new IllegalStateException("Unknown constituent " + name + " of tide station " + station.get("id").asText());
            }
            constituents.add(new HarmonicConstituent(name, constituent.get(1).asDouble(), constituent.get(2).asDouble()));
        }
        return new Station(constituents, 0);
    }

    private static SubordinateTideStation subordinate(JsonNode station, Map<String, Station> references) {
        Station reference = Objects.requireNonNull(references.get(station.get("reference").asText()), () -> "Missing reference of tide station " + station.get("id").asText());
        double high = station.get("heightHigh").asDouble();
        double low = station.get("heightLow").asDouble();
        HeightOffset height = "ratio".equals(station.get("heightType").asText()) ? new HeightOffset.Ratio(high, low) : new HeightOffset.Fixed(high, low);
        return new SubordinateTideStation(reference, station.get("highMinutes").asDouble() * 60, station.get("lowMinutes").asDouble() * 60, height);
    }
}
