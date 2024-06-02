import io.fluentlenium.core.domain.FluentWebElement;
import org.junit.Test;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import play.test.Helpers;
import play.test.TestBrowser;
import play.test.WithBrowser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

import static io.fluentlenium.core.filter.FilterConstructor.containingText;
import static io.fluentlenium.core.filter.FilterConstructor.withText;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IntegrationTest extends WithBrowser {
    @Override
    protected TestBrowser provideBrowser(int port) {
//        return Helpers.testBrowser(new ChromeDriver(new ChromeOptions()
//                .addArguments("--lang=de-CH")
//                .addArguments("-headless")
//        ), port);
        return Helpers.testBrowser(new FirefoxDriver(new FirefoxOptions()
                .addArguments("-headless")
        ), port);
    }

    @Test
    public void integrationTest() throws InterruptedException, IOException {
        try {
            browser.goTo("/");
            assertEquals("Mondkalender", getText("h1"));
            browser.waitUntil(webDriver -> getText("#calendar").contains("Vollmond um"));

            browser.$("#phases label", withText("Vollmond")).click();
            browser.$("#events label").click();
            browser.$("#interval #from").fill().with("2024-02-01");
            browser.$("#interval #to").fill().with("2024-07-31");
            browser.$("#interval #zone").fillSelect().withText("Europe/Zurich");
            browser.$("#phases label", withText("Neumond")).click();
            browser.waitUntil(webDriver -> getText("#calendar").contains("Neumond"));
            assertEquals(load("IntegrationTest_newmoon_body.txt"), getText("#calendar"));

            assertTrue(browser.$("button", withText("iCalendar-Feed abonnieren")).isEmpty());
            scrollAndAwait(browser.$("button", withText("In Kalender exportieren")).first());
            click(browser.$("button", withText("In Kalender exportieren")).first());
            browser.$("button", withText("iCalendar-Feed abonnieren")).click();
            String iCalSubscribeLink = browser.$("#icalLink").first().value();
            assertThat(iCalSubscribeLink, matchesPattern("http.*/mooncal.ics\\?created=\\d+&lang=de&phases\\[full]=false&phases\\[new]=true&phases\\[quarter]=false&phases\\[daily]=false&events\\[lunareclipse]=false&events\\[solareclipse]=false&events\\[moonlanding]=false&before=P6M&after=P2Y&zone=Europe/Zurich"));
            assertThat(getText("body"), containsString("Kopiere folgenden Link und füge ihn als Abonnement in deinem Kalenderprogramm hinzu:"));
            assertIcsEquals(load("IntegrationTest_newmoon_subscribe.ics"), download(iCalSubscribeLink));
            click(browser.$("button", withText("Schliessen")).first());
            click(browser.$("button", withText("In Kalender exportieren")).first());
            String iCalDownloadLink = browser.$("a", withText("iCalendar-Datei herunterladen")).attributes("href").getFirst();
            assertThat(iCalDownloadLink, endsWith("&from=2024-02-01T01:00:00Europe/Zurich&to=2024-07-31T02:00:00Europe/Zurich"));
            assertEquals(iCalSubscribeLink
                            .replaceFirst("created=\\d+&", "")
                            .replaceFirst("&before=.*", ""),
                    iCalDownloadLink
                            .replaceFirst("&from=.*", ""));
            assertIcsEquals(load("IntegrationTest_newmoon_download.ics"), download(iCalDownloadLink));

            assertThat(getText("body"), not(containsString("English")));
            scrollAndAwait(browser.$("a", containingText("Sprache ändern")).first());
            click(browser.$("a", containingText("Sprache ändern")).first());
            click(browser.$("a", withText("English")).first());
            assertEquals("Moon Calendar", getText("h1"));
            click(browser.$("a", withText("About")).first());
            assertEquals("About this site", getText("h1"));
        } catch (Error | RuntimeException e) {
            browser.takeScreenshot("target/integration-test-failure-" + System.currentTimeMillis() + ".png");
            Thread.sleep(2000);
            throw e;
        }
    }

    private void assertIcsEquals(String expected, String actual) {
        assertEquals(expected
                        .replaceAll("DTSTAMP:.*", "DTSTAMP:ignored"),
                actual
                        .replaceAll("\\r", "")
                        .replaceAll("DTSTAMP:.*", "DTSTAMP:ignored"));
    }

    private static String load(String file) throws IOException {
        try (InputStream stream = IntegrationTest.class.getResourceAsStream(file)) {
            return new String(Objects.requireNonNull(stream).readAllBytes());
        }
    }

    private static String download(String iCalDownloadLink) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(iCalDownloadLink)).build();
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            return response.body();
        }
    }

    private static void click(FluentWebElement element) {
        if (!element.clickable()) {
            throw new RuntimeException("Element not clickable: " + element);
        }
        element.executeScript("arguments[0].click();", element);
    }

    private void scrollAndAwait(FluentWebElement element) throws InterruptedException {
        try {
            element.scrollIntoView(true);
        } catch (NoSuchElementException ignored) {
            Thread.sleep(200);
            element.scrollIntoView(true);
        }
        browser.fluentWait().until(ExpectedConditions.visibilityOf(element.getElement()));
    }

    private String getText(String selector) {
        return browser.$(selector).first().text();
    }
}
