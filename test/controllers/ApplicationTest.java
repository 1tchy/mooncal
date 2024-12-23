package controllers;

import models.EventStyle;
import org.junit.Test;
import play.api.libs.json.*;
import play.api.mvc.Call;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import scala.collection.Seq;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.route;

public class ApplicationTest extends WithApplication {

    @Test
    public void testDefaultQueryCall() {
        final Seq<JsValue> resultList = query("de", true, false, false, false, EventStyle.WITH_DESCRIPTION, true, true, true, "from=2015-01-01T00:00:00CET", "to=2015-12-31T00:00:00CET");
        assertEquals(17, resultList.size());
        JsObject firstResult = (JsObject) resultList.head();
        assertEquals("🌕 Vollmond (Hartung)", ((JsString) firstResult.value().get("title").get()).value());
        assertEquals("2015-01-05", ((JsString) firstResult.value().get("date").get()).value());
    }

    @Test
    public void testNewPhasesQueryCall() {
        final Seq<JsValue> resultList = query("en", false, true, false, false, EventStyle.WITH_DESCRIPTION, false, false, false, "from=2015-01-01T00:00:00CET", "to=2015-12-31T00:00:00CET");
        assertEquals(12, resultList.size());
        JsObject firstResult = (JsObject) resultList.head();
        assertEquals("🌑 new moon", ((JsString) firstResult.value().get("title").get()).value());
        assertEquals("2015-01-20", ((JsString) firstResult.value().get("date").get()).value());
    }

    @Test
    public void testQuarterPhasesQueryCall() {
        final Seq<JsValue> resultList = query("en", false, false, true, false, EventStyle.WITH_DESCRIPTION, false, false, false, "from=2015-01-01T00:00:00CET", "to=2015-12-31T00:00:00CET");
        assertEquals(24, resultList.size());
        JsObject firstResult = (JsObject) resultList.head();
        assertEquals("🌗 third quarter", ((JsString) firstResult.value().get("title").get()).value());
        assertEquals("2015-01-13", ((JsString) firstResult.value().get("date").get()).value());
    }

    @Test
    public void testDailyPhasesQueryCall() {
        final Seq<JsValue> resultList = query("en", false, false, false, true, EventStyle.ICON_ONLY, false, false, false, "from=2015-01-01T00:00:00CET", "to=2015-01-31T00:00:00CET");
        assertEquals(31, resultList.size());
        JsObject firstResult = (JsObject) resultList.head();
        assertEquals("moon 87% visible", ((JsString) firstResult.value().get("title").get()).value());
        assertEquals("2015-01-01", ((JsString) firstResult.value().get("date").get()).value());
    }

    @Test
    public void testWithoutDescriptionQueryCall() {
        final Seq<JsValue> resultList = query("de", true, false, false, false, EventStyle.FULLMOON, true, true, true, "from=2015-01-01T00:00:00CET", "to=2015-12-31T00:00:00CET");
        assertEquals(17, resultList.size());
        JsObject firstResult = (JsObject) resultList.head();
        assertEquals("🌕 Vollmond", ((JsString) firstResult.value().get("title").get()).value());
        assertEquals("2015-01-05", ((JsString) firstResult.value().get("date").get()).value());
    }

    @Test
    public void testIconOnlyQueryCall() {
        final Seq<JsValue> resultList = query("de", true, false, false, false, EventStyle.ICON_ONLY, true, true, true, "from=2015-01-01T00:00:00CET", "to=2015-12-31T00:00:00CET");
        assertEquals(17, resultList.size());
        JsObject firstResult = (JsObject) resultList.head();
        assertEquals("🌕", ((JsString) firstResult.value().get("title").get()).value());
        assertEquals("2015-01-05", ((JsString) firstResult.value().get("date").get()).value());
    }

    @Test
    public void testEclipsesQueryCall() {
        final Seq<JsValue> resultList = query("en", false, false, false, false, EventStyle.WITH_DESCRIPTION, true, true, false, "from=2015-01-01T00:00:00CET", "to=2015-12-31T00:00:00CET");
        assertEquals(4, resultList.size());
        JsObject firstResult = (JsObject) resultList.head();
        assertEquals("total solar eclipse", ((JsString) firstResult.value().get("title").get()).value());
        assertEquals("2015-03-20", ((JsString) firstResult.value().get("date").get()).value());
    }

    @Test
    public void testMoonLandingQueryCall() {
        final Seq<JsValue> resultList = query("en", false, false, false, false, EventStyle.WITH_DESCRIPTION, false, false, true, "from=2000-01-01T00:00:00CET", "to=2015-12-31T00:00:00CET");
        assertEquals(7, resultList.size());
        JsObject firstResult = (JsObject) resultList.head();
        assertEquals("🚀 SMART-1 (ESA)", ((JsString) firstResult.value().get("title").get()).value());
        assertEquals("2006-09-03", ((JsString) firstResult.value().get("date").get()).value());
    }

    @Test
    public void testFloatingQuery() {
        final Seq<JsValue> resultList = query("de", true, false, false, false, EventStyle.WITH_DESCRIPTION, false, false, false, "before=P1M", "after=P11M", "zone=CET");
        assertThat(resultList.size(), greaterThanOrEqualTo(12));
        assertThat(resultList.size(), lessThanOrEqualTo(13));
    }

    @Test
    public void testICalendarDefaultQuery() {
        final String result = queryAsICalendar("de", true, false, false, false, true, true, true, "from=2015-12-01T00:00:00CET", "to=2015-12-31T00:00:00CET");
        assertThat(result, containsString("Vollmond"));
    }

    @Test
    public void testICalendarQueryInverted() {
        final String result = queryAsICalendar("nl", false, true, true, true, false, false, false, "from=2015-12-01T00:00:00CET", "to=2015-12-31T00:00:00CET");
        assertThat(result, containsString("Nieuwe maan"));
    }

    private String queryAsICalendar(String lang, boolean fullPhases, boolean newPhases, boolean quarterQuases, boolean dailyPhases, boolean lunarEclipses, boolean solarEclipses, boolean moonLandings, String... extraParams) {
        final Call query = routes.Application.queryAsICalendar();
        final Result result = paramQuery(query.method(), query.url() + "?", lang, fullPhases, newPhases, quarterQuases, dailyPhases, EventStyle.WITH_DESCRIPTION, lunarEclipses, solarEclipses, moonLandings, extraParams);
        return contentAsString(result);
    }

    private Seq<JsValue> query(String lang, boolean fullPhases, boolean newPhases, boolean quarterQuases, boolean dailyPhases, EventStyle style, boolean lunarEclipses, boolean solarEclipses, boolean moonLandings, String... extraParams) {
        final Result result = paramQuery("GET", routes.Application.query().url() + "?", lang, fullPhases, newPhases, quarterQuases, dailyPhases, style, lunarEclipses, solarEclipses, moonLandings, extraParams);
        return ((JsArray) Json.parse(contentAsString(result))).value();
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
