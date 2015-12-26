package controllers;

import org.junit.Test;
import play.api.libs.json.*;
import play.api.mvc.Call;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import scala.collection.Seq;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.route;

public class ApplicationTest extends WithApplication {

    @Test
    public void testSimpleQueryCall() {
        final Seq<JsValue> resultList = query(true, false, false, false, true, true, true, "from=2015-01-01T00:00:00CET", "to=2015-12-31T00:00:00CET");
        assertEquals(resultList.size(), 17);
        JsObject firstResult = (JsObject) resultList.head();
        assertEquals(((JsString) firstResult.value().get("title").get()).value(), "Vollmond");
        assertEquals(((JsString) firstResult.value().get("date").get()).value(), "2015-01-05");
    }

    @Test
    public void testFloatingQuery() {
        final Seq<JsValue> resultList = query(true, false, false, false, false, false, false, "before=P1M", "after=P11M", "zone=CET");
        assertThat(resultList.size(), greaterThanOrEqualTo(12));
        assertThat(resultList.size(), lessThanOrEqualTo(13));
    }

    private Seq<JsValue> query(boolean fullPhases, boolean newPhases, boolean quarterQuases, boolean dailyPhases, boolean lunarEclipses, boolean solarEclipses, boolean moonLandings, String... dateParams) {
        final Call query = routes.Application.query();
        String params = "phases[full]=" + fullPhases + "&phases[new]=" + newPhases + "&phases[quarter]=" + quarterQuases + "&phases[daily]=" + dailyPhases + "&events[lunareclipse]=" + lunarEclipses + "&events[solareclipse]=" + solarEclipses + "&events[moonlanding]=" + moonLandings;
        for (String dateParam : dateParams) {
            params += "&" + dateParam;
        }
        final Result result = route(new Http.RequestBuilder().method(query.method()).uri(query.url() + "?" + params));
        return ((JsArray) Json.parse(contentAsString(result))).value();
    }
}
