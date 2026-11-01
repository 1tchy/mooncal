package controllers;

import logics.tides.TideStation;
import logics.tides.TideStations;
import models.EventStyle;
import org.apache.pekko.stream.Materializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.api.libs.json.*;
import play.api.mvc.Call;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import scala.collection.Seq;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.route;

@SuppressWarnings("JUnitMixedFramework")
class ApplicationTest extends WithApplication {

    @BeforeEach
    void setUpPlay() {
        startPlay();
    }

    @AfterEach
    void tearDownPlay() {
        stopPlay();
    }

    @Test
    void testDefaultQueryCall() {
        final Seq<JsValue> resultList = query("de", true, false, false, false, EventStyle.WITH_DESCRIPTION, true, true, true, "from=2015-01-01T00:00:00CET", "to=2015-12-31T00:00:00CET");
        assertEquals(17, resultList.size());
        JsObject firstResult = (JsObject) resultList.head();
        assertEquals("🌕 Vollmond (Hartung)", ((JsString) firstResult.value().get("title").get()).value());
        assertThat(((JsString) firstResult.value().get("date").get()).value(), startsWith("2015-01-05"));
    }

    @Test
    void testNewPhasesQueryCall() {
        final Seq<JsValue> resultList = query("en", false, true, false, false, EventStyle.WITH_DESCRIPTION, false, false, false, "from=2015-01-01T00:00:00CET", "to=2015-12-31T00:00:00CET");
        assertEquals(12, resultList.size());
        JsObject firstResult = (JsObject) resultList.head();
        assertEquals("🌑 New Moon", ((JsString) firstResult.value().get("title").get()).value());
        assertThat(((JsString) firstResult.value().get("date").get()).value(), startsWith("2015-01-20"));
    }

    @Test
    void testQuarterPhasesQueryCall() {
        final Seq<JsValue> resultList = query("en", false, false, true, false, EventStyle.WITH_DESCRIPTION, false, false, false, "from=2015-01-01T00:00:00CET", "to=2015-12-31T00:00:00CET");
        assertEquals(24, resultList.size());
        JsObject firstResult = (JsObject) resultList.head();
        assertEquals("🌗 Waning crescent Moon", ((JsString) firstResult.value().get("title").get()).value());
        assertThat(((JsString) firstResult.value().get("date").get()).value(), startsWith("2015-01-13"));
    }

    @Test
    void testDailyPhasesQueryCall() {
        final Seq<JsValue> resultList = query("en", false, false, false, true, EventStyle.ICON_ONLY, false, false, false, "from=2015-01-01T00:00:00CET", "to=2015-01-31T00:00:00CET");
        assertEquals(31, resultList.size());
        JsObject firstResult = (JsObject) resultList.head();
        assertEquals("Moon 87% visible", ((JsString) firstResult.value().get("title").get()).value());
        assertThat(((JsString) firstResult.value().get("date").get()).value(), startsWith("2015-01-01"));
    }

    @Test
    void testWithoutDescriptionQueryCall() {
        final Seq<JsValue> resultList = query("de", true, false, false, false, EventStyle.FULLMOON, true, true, true, "from=2015-01-01T00:00:00CET", "to=2015-12-31T00:00:00CET");
        assertEquals(17, resultList.size());
        JsObject firstResult = (JsObject) resultList.head();
        assertEquals("🌕 Vollmond", ((JsString) firstResult.value().get("title").get()).value());
        assertThat(((JsString) firstResult.value().get("date").get()).value(), startsWith("2015-01-05"));
    }

    @Test
    void testIconOnlyQueryCall() {
        final Seq<JsValue> resultList = query("de", true, false, false, false, EventStyle.ICON_ONLY, true, true, true, "from=2015-01-01T00:00:00CET", "to=2015-12-31T00:00:00CET");
        assertEquals(17, resultList.size());
        JsObject firstResult = (JsObject) resultList.head();
        assertEquals("🌕", ((JsString) firstResult.value().get("title").get()).value());
        assertThat(((JsString) firstResult.value().get("date").get()).value(), startsWith("2015-01-05"));
    }

    @Test
    void testEclipsesQueryCall() {
        final Seq<JsValue> resultList = query("en", false, false, false, false, EventStyle.WITH_DESCRIPTION, true, true, false, "from=2015-01-01T00:00:00CET", "to=2015-12-31T00:00:00CET");
        assertEquals(4, resultList.size());
        JsObject firstResult = (JsObject) resultList.head();
        assertEquals("Total Solar Eclipse", ((JsString) firstResult.value().get("title").get()).value());
        assertThat(((JsString) firstResult.value().get("date").get()).value(), startsWith("2015-03-20"));
    }

    @Test
    void testMoonLandingQueryCall() {
        final Seq<JsValue> resultList = query("en", false, false, false, false, EventStyle.WITH_DESCRIPTION, false, false, true, "from=2000-01-01T00:00:00CET", "to=2015-12-31T00:00:00CET");
        assertEquals(7, resultList.size());
        JsObject firstResult = (JsObject) resultList.head();
        assertEquals("🚀 SMART-1 (ESA)", ((JsString) firstResult.value().get("title").get()).value());
        assertThat(((JsString) firstResult.value().get("date").get()).value(), startsWith("2006-09-03"));
    }

    @Test
    void testFloatingQuery() {
        final Seq<JsValue> resultList = query("de", true, false, false, false, EventStyle.WITH_DESCRIPTION, false, false, false, "before=P1M", "after=P11M", "zone=CET");
        assertThat(resultList.size(), greaterThanOrEqualTo(12));
        assertThat(resultList.size(), lessThanOrEqualTo(13));
    }

    @Test
    void testICalendarDefaultQuery() {
        final String result = queryAsICalendar("de", true, false, false, false, true, true, true, "from=2015-12-01T00:00:00CET", "to=2015-12-31T00:00:00CET");
        assertThat(result, containsString("Vollmond"));
    }

    @Test
    void testICalendarQueryInverted() {
        final String result = queryAsICalendar("nl", false, true, true, true, false, false, false, "from=2015-12-01T00:00:00CET", "to=2015-12-31T00:00:00CET");
        assertThat(result, containsString("Nieuwe maan"));
    }

    @Test
    void testICalendarDailyWithLocation() {
        String result = queryAsICalendar("de", false, false, false, true, false, false, false, "from=2026-01-15T00:00:00Europe/Zurich", "to=2026-01-15T23:59:59Europe/Zurich", "lat=47.38", "lon=8.54");
        assertThat(result, containsString("↑5:5"));
        assertThat(result, containsString("Mondaufgang um 5:5"));
        assertThat(result, containsString("47.4° N"));
    }

    @Test
    void testICalendarSunWithLocation() {
        String result = queryAsICalendar("de", false, false, false, false, false, false, false, "phases[sun]=true", "from=2026-01-15T00:00:00Europe/Zurich", "to=2026-01-15T23:59:59Europe/Zurich", "lat=47.38", "lon=8.54");
        assertThat(result, containsString("Sonne ↑8:"));
        assertThat(result, containsString("Sonnenuntergang um 17:0"));
        assertThat(result, containsString("47.4° N"));
    }

    @Test
    void testTideQueryAtTheCoast() {
        final Seq<JsValue> resultList = query("de", false, false, false, false, EventStyle.WITH_DESCRIPTION, false, false, false, "phases[tide]=true", "from=2026-01-15T00:00:00Europe/Berlin", "to=2026-01-15T23:59:59Europe/Berlin", "lat=53.9", "lon=8.7");
        assertEquals(1, resultList.size());
        JsObject result = (JsObject) resultList.head();
        assertThat(((JsString) result.value().get("title").get()).value(), allOf(containsString("Flut ↑"), containsString("Ebbe ↓")));
        assertThat(((JsString) result.value().get("description").get()).value(), allOf(containsString("Hochwasser um"), containsString("TICON-4")));
    }

    @Test
    void testTideQueryAtAChosenStation() {
        // none within the limit of Zurich, so the user chose one of the nearest in the hint
        TideStation chosen = app.injector().instanceOf(TideStations.class).nearest(47.4, 8.5, 1).getFirst().station();
        final Seq<JsValue> resultList = query("de", false, false, false, false, EventStyle.WITH_DESCRIPTION, false, false, false, "phases[tide]=true", "tideStation=" + URLEncoder.encode(chosen.id(), StandardCharsets.UTF_8), "from=2026-01-15T00:00:00Europe/Zurich", "to=2026-01-15T23:59:59Europe/Zurich", "lat=47.4", "lon=8.5");
        assertEquals(1, resultList.size());
        String description = ((JsString) ((JsObject) resultList.head()).value().get("description").get()).value();
        assertThat(description, allOf(containsString("Hochwasser um"), containsString(chosen.label())));
    }

    @Test
    void testICalendarTide() {
        // long lines are folded in iCal
        String result = queryAsICalendar("de", false, false, false, false, false, false, false, "phases[tide]=true", "from=2026-01-15T00:00:00Europe/Berlin", "to=2026-01-15T23:59:59Europe/Berlin", "lat=53.9", "lon=8.7").replace("\r\n ", "");
        assertThat(result, containsString("Hochwasser um"));
        assertThat(result, containsString("CC BY 4.0"));
    }

    @Test
    void testQueryWithInvalidLocationIsRejected() {
        Call query = routes.Application.query();
        Result result = paramQuery("GET", query.url() + "?", "de", false, false, false, true, EventStyle.WITH_DESCRIPTION, false, false, false, "from=2026-01-15T00:00:00Europe/Zurich", "to=2026-01-15T23:59:59Europe/Zurich", "lat=95", "lon=8.5");
        assertEquals(400, result.status());
        assertThat(contentAsString(result), containsString("Ungültiger Standort"));
    }

    @Test
    void testDailyQueryWithTooLargeTimeframeIsRejected() {
        Call query = routes.Application.query();
        Result result = paramQuery("GET", query.url() + "?", "de", false, false, false, true, EventStyle.WITH_DESCRIPTION, false, false, false, "from=2026-01-01T00:00:00Europe/Zurich", "to=2198-03-31T23:59:59Europe/Zurich");
        assertEquals(400, result.status());
        assertEquals("Bei der Anzeige von täglichen Phasen sind maximal 100 Jahre möglich.", contentAsString(result));
    }

    @Test
    void testTruncatedDailyICalendarQueryIsRejected() {
        Result result = route(app, new Http.RequestBuilder().method("GET").uri("/mooncal.ics?created=20077059936&lang=en&phases%5Bfull%5D=false&phases%5Bnew%5D=false&phases%5Bquarter%5D=false&phases%5Bdaily%5D=true&style=withDescription&events%5Blunareclipse]"));
        assertEquals(400, result.status());
    }

    private String queryAsICalendar(String lang, boolean fullPhases, boolean newPhases, boolean quarterQuases, boolean dailyPhases, boolean lunarEclipses, boolean solarEclipses, boolean moonLandings, String... extraParams) {
        final Call query = routes.Application.queryAsICalendar();
        final Result result = paramQuery(query.method(), query.url() + "?", lang, fullPhases, newPhases, quarterQuases, dailyPhases, EventStyle.WITH_DESCRIPTION, lunarEclipses, solarEclipses, moonLandings, extraParams);
        return contentAsString(result);
    }

    private Seq<JsValue> query(String lang, boolean fullPhases, boolean newPhases, boolean quarterQuases, boolean dailyPhases, EventStyle style, boolean lunarEclipses, boolean solarEclipses, boolean moonLandings, String... extraParams) {
        final Result result = paramQuery("GET", routes.Application.query().url() + "?", lang, fullPhases, newPhases, quarterQuases, dailyPhases, style, lunarEclipses, solarEclipses, moonLandings, extraParams);
        // the JSON is streamed
        return ((JsArray) Json.parse(contentAsString(result, app.injector().instanceOf(Materializer.class)))).value();
    }

    @SuppressWarnings("StringConcatenationInLoop") //it's just in the tests
    private Result paramQuery(String method, String urlStart, String lang, boolean fullPhases, boolean newPhases, boolean quarterQuases, boolean dailyPhases, EventStyle style, boolean lunarEclipses, boolean solarEclipses, boolean moonLandings, String... extraParams) {
        String params = "lang=" + lang + "&phases[full]=" + fullPhases + "&phases[new]=" + newPhases + "&phases[quarter]=" + quarterQuases + "&phases[daily]=" + dailyPhases + "&style=" + style.getStyle() + "&events[lunareclipse]=" + lunarEclipses + "&events[solareclipse]=" + solarEclipses + "&events[moonlanding]=" + moonLandings;
        for (String extraParam : extraParams) {
            params += "&" + extraParam;
        }
        return route(app, new Http.RequestBuilder().method(method).uri(urlStart + params));
    }
}
