package controllers;

import logics.calculation.TotalCalculation;
import models.EventInstance;
import models.MoonPhaseType;
import models.RequestForm;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.util.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.http.HttpEntity;
import play.i18n.Lang;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static play.test.Helpers.route;

@SuppressWarnings("JUnitMixedFramework")
class ApplicationJsonTest extends WithApplication {

    @BeforeEach
    void setUp() {
        startPlay();
    }

    @AfterEach
    void tearDown() {
        stopPlay();
    }

    @Test
    void largeCalendarIsStreamedInSmallChunks() throws Exception {
        // the descriptions of tide events end with a long attribution: neither a JSON tree nor the whole JSON is held at once
        ZoneId zone = ZoneId.of("Europe/Berlin");
        RequestForm requestForm = new RequestForm();
        requestForm.setPhases(Map.of(MoonPhaseType.DAILY, true, MoonPhaseType.SUN, true, MoonPhaseType.TIDE, true));
        requestForm.setFrom(LocalDate.of(2000, 1, 1).atStartOfDay(zone));
        requestForm.setTo(LocalDate.of(2099, 12, 31).atTime(23, 59).atZone(zone));
        requestForm.setLang(Lang.forCode("de"));
        requestForm.setLat(53.9);
        requestForm.setLon(8.7);
        Collection<EventInstance> events = app.injector().instanceOf(TotalCalculation.class).calculate(requestForm);

        List<ByteString> chunks = stream(events);
        int largest = chunks.stream().mapToInt(ByteString::size).max().orElseThrow();
        ByteString json = chunks.stream().reduce(ByteString.emptyByteString(), ByteString::concat);
        System.out.println("JSON of " + events.size() + " events: " + (json.size() >> 20) + " MB in " + chunks.size() + " chunks, the largest " + (largest >> 10) + " KB");
        assertArrayEquals(Json.mapper().writeValueAsBytes(events), json.toArray());
        assertThat(largest, lessThan(1 << 20));
    }

    @Test
    void emptyCalendarIsAnEmptyArray() throws Exception {
        assertEquals("[]", stream(List.of()).stream().map(ByteString::utf8String).reduce("", String::concat));
    }

    @Test
    void calendarResponseIsStreamed() {
        Result result = route(app, new Http.RequestBuilder().method("GET").uri("/mooncal?lang=de&phases[full]=true&style=withDescription&from=2026-01-01T00:00:00Europe/Zurich&to=2026-12-31T23:59:59Europe/Zurich"));
        assertEquals(200, result.status());
        assertEquals("application/json", result.contentType().orElseThrow());
        assertInstanceOf(HttpEntity.Chunked.class, result.body());
    }

    private List<ByteString> stream(Collection<EventInstance> events) throws Exception {
        return Application.toJson(events).runWith(Sink.seq(), app.injector().instanceOf(Materializer.class)).toCompletableFuture().get();
    }
}
