package controllers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import logics.calculation.TideCalculation;
import logics.tides.TideStations;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.List;

/**
 * The nearest tide stations, for the hint shown when none is close enough to a location. The location is not logged.
 */
public class Tides extends Controller {

    private static final int NEAREST_STATIONS = 3;
    private final TideStations tideStations;

    @Inject
    public Tides(TideStations tideStations) {
        this.tideStations = tideStations;
    }

    public Result nearestStations(Double lat, Double lon) {
        if (!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) {
            return badRequest();
        }
        // rounded like the location of the calendar, so that both find the same stations
        List<TideStations.StationDistance> stations = tideStations.nearest(roundLocation(lat), roundLocation(lon), NEAREST_STATIONS);
        ObjectNode result = Json.newObject();
        result.put("maxDistanceKm", Math.round(TideCalculation.MAX_STATION_DISTANCE_KM));
        // the rounded distances can't tell, e.g. for a station 50.3 km away
        result.put("withinMaxDistance", !stations.isEmpty() && stations.getFirst().km() <= TideCalculation.MAX_STATION_DISTANCE_KM);
        ArrayNode nearest = result.putArray("nearest");
        stations.forEach(station -> nearest.addObject()
                .put("id", station.station().id())
                .put("label", station.station().label())
                .put("lat", roundLocation(station.station().lat()))
                .put("lon", roundLocation(station.station().lon()))
                .put("km", Math.round(station.km())));
        return ok(result).withHeader(CACHE_CONTROL, "max-age=21600"); //=6h
    }

    private static double roundLocation(double degrees) {
        return Math.round(degrees * 10) / 10.0;
    }
}
