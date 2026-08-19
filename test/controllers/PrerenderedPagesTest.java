package controllers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.route;

@SuppressWarnings("JUnitMixedFramework")
class PrerenderedPagesTest extends WithApplication {

    @BeforeEach
    void setUpPlay() {
        startPlay();
    }

    @AfterEach
    void tearDownPlay() {
        stopPlay();
    }

    @Test
    void servesThePrerenderedGermanHomePageAtTheRoot() {
        final Result result = get("/");
        assertEquals(200, result.status());
        final String html = contentOf(result);
        assertThat(html, containsString("lang=\"de\""));
        assertThat(html, containsString("Mondkalender"));
    }

    @Test
    void servesThePrerenderedEnglishCalendarPage() {
        final Result result = get("/en/calendar");
        assertEquals(200, result.status());
        final String html = contentOf(result);
        assertThat(html, containsString("lang=\"en\""));
        assertThat(html, containsString("Moon Calendar"));
    }

    @Test
    void servesThePrerenderedGermanAboutPage() {
        final Result result = get("/ueber");
        assertEquals(200, result.status());
        final String html = contentOf(result);
        assertThat(html, containsString("lang=\"de\""));
        assertThat(html, containsString("https://mooncal.ch/ueber"));
    }

    @Test
    void fallsBackToTheRootPageForUnknownPaths() {
        final Result result = get("/does-not/exist");
        assertEquals(200, result.status());
        assertThat(contentOf(result), containsString("<app-root"));
    }

    private Result get(String uri) {
        return route(app, new Http.RequestBuilder().method("GET").uri(uri));
    }

    private String contentOf(Result result) {
        return contentAsString(result, app.asScala().materializer());
    }
}
