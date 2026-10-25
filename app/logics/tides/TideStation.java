package logics.tides;

import io.openwaters.slackwater.TideExtreme;

import java.time.Instant;
import java.util.List;

/**
 * A tide gauge with the attribution its data requires, which is shared among all stations of the same source.
 */
public record TideStation(String id, String label, double lat, double lon, String attribution, TidePrediction prediction) {

    @FunctionalInterface
    public interface TidePrediction {
        List<TideExtreme> extremes(Instant from, Instant until);
    }
}
