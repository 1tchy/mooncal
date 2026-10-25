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

import static models.RequestForm.roundLocation;

/**
 * The nearest tide stations, for the hint shown when none is close enough to a location. The location is not logged.
 */
public class Tides extends Controller {

    private static final int NEAREST_STATIONS = 3;
    private final TideCalculation tideCalculation;

    @Inject
    public Tides(TideCalculation tideCalculation) {
        this.tideCalculation = tideCalculation;
    }

    public Result nearestStations(Double lat, Double lon) {
        if (!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) {
            return badRequest();
        }
        // the same stations as the calendar, so that each one is shown when chosen
        List<TideStations.StationDistance> stations = tideCalculation.stationsToChoose(lat, lon, NEAREST_STATIONS);
        ObjectNode result = Json.newObject();
        result.put("maxDistanceKm", Math.round(TideCalculation.MAX_STATION_DISTANCE_KM));
        // the rounded distances can't tell, e.g. for a station 50.3 km away
        result.put("withinMaxDistance", tideCalculation.station(lat, lon).isPresent());
        ArrayNode nearest = result.putArray("nearest");
        stations.forEach(station -> nearest.addObject()
                .put("label", station.station().label())
                .put("lat", roundLocation(station.station().lat()))
                .put("lon", roundLocation(station.station().lon()))
                .put("km", Math.round(station.km())));
        return ok(result).withHeader(CACHE_CONTROL, "max-age=21600"); //=6h
    }
}
