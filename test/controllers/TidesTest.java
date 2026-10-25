package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.route;

@SuppressWarnings("JUnitMixedFramework")
class TidesTest extends WithApplication {

    @BeforeEach
    void setup() {
        startPlay();
    }

    @AfterEach
    void tearDown() {
        stopPlay();
    }

    @Test
    void nearestStationsOfZurich() {
        Result result = get("/tideStations?lat=47.4&lon=8.5");
        assertEquals(200, result.status());
        assertEquals("max-age=21600", result.header("Cache-Control").orElseThrow());
        JsonNode json = Json.parse(contentAsString(result));
        assertEquals(50, json.get("maxDistanceKm").asInt());
        JsonNode nearest = json.get("nearest");
        assertEquals(3, nearest.size());
        assertThat(nearest.get(0).get("km").asInt(), greaterThan(50));
        assertThat(nearest.get(0).get("km").asInt(), lessThanOrEqualTo(nearest.get(1).get("km").asInt()));
        assertThat(nearest.get(1).get("km").asInt(), lessThanOrEqualTo(nearest.get(2).get("km").asInt()));
        assertThat(nearest.get(0).get("label").asText(), not(emptyString()));
        // station coordinates are rounded like any location
        double lat = nearest.get(0).get("lat").asDouble();
        assertEquals(Math.round(lat * 10) / 10.0, lat);
    }

    @Test
    void nearestStationsOfCuxhaven() {
        JsonNode json = Json.parse(contentAsString(get("/tideStations?lat=53.9&lon=8.7")));
        assertThat(json.get("nearest").get(0).get("km").asInt(), lessThan(5));
    }

    @Test
    void tellsWhetherTheNearestStationIsWithinTheLimit() {
        assertEquals(true, Json.parse(contentAsString(get("/tideStations?lat=53.9&lon=8.7"))).get("withinMaxDistance").asBoolean());
        assertEquals(false, Json.parse(contentAsString(get("/tideStations?lat=47.4&lon=8.5"))).get("withinMaxDistance").asBoolean());
    }

    @Test
    void locationIsRoundedLikeTheCalendar() {
        assertEquals(contentAsString(get("/tideStations?lat=53.9&lon=8.7")), contentAsString(get("/tideStations?lat=53.94&lon=8.66")));
    }

    @Test
    void invalidLocationIsRejected() {
        assertEquals(400, get("/tideStations?lat=95&lon=8").status());
        assertEquals(400, get("/tideStations?lat=47&lon=181").status());
        assertEquals(400, get("/tideStations?lat=NaN&lon=8").status());
        assertEquals(400, get("/tideStations?lon=8").status());
    }

    private Result get(String uri) {
        return route(app, new Http.RequestBuilder().method("GET").uri(uri));
    }
}
