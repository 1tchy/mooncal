import io.fluentlenium.core.domain.FluentWebElement;
import io.fluentlenium.core.filter.AttributeFilter;
import org.junit.Test;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
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
import java.util.function.Supplier;

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
        System.setProperty("today", "2024-06-09");
//        return Helpers.testBrowser(new ChromeDriver(new ChromeOptions()
//                .addArguments("--lang=de-CH")
//                .addArguments("-headless")
//        ), port);
        return Helpers.testBrowser(new FirefoxDriver(new FirefoxOptions()
                .addArguments("-headless")
                .addArguments("-width=1920")
                .addArguments("-height=1080")
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
            click(browser.$("button", withText("Zum Kalender hinzufügen")).first());
            String iCalSubscribeLink = browser.$("#icalLink").first().value();
            assertThat(iCalSubscribeLink, matchesPattern("http.*/mooncal.ics\\?created=\\d+&lang=de&phases\\[full]=false&phases\\[new]=true&phases\\[quarter]=false&phases\\[daily]=false&events\\[lunareclipse]=false&events\\[solareclipse]=false&events\\[moonlanding]=false&before=P6M&after=P2Y&zone=Europe/Zurich"));
            assertThat(getText("body"), containsString("Wähle dein Kalenderprogramm aus, um dafür eine Kurzanleitung zu sehen"));
            assertIcsEquals(load("IntegrationTest_newmoon_subscribe.ics"), download(iCalSubscribeLink));
            click(browser.$("button", new AttributeFilter("aria-label", "Close")).first());
            click(browser.$("button", withText("Zum Kalender hinzufügen")).first());
            awaitClickable(() -> browser.$("a", withText("Datei herunterladen")).first());
            String iCalDownloadLink = browser.$("a", withText("Datei herunterladen")).attributes("href").getFirst();
            assertThat(iCalDownloadLink, endsWith("&from=2024-02-01T00:00:00Europe/Zurich&to=2024-07-31T23:59:59Europe/Zurich&manualDownload"));
            assertEquals(iCalSubscribeLink
                            .replaceFirst("created=\\d+&", "")
                            .replaceFirst("&before=.*", ""),
                    iCalDownloadLink
                            .replaceFirst("&from=.*", ""));
            assertIcsEquals(load("IntegrationTest_newmoon_download.ics"), download(iCalDownloadLink));
            click(browser.$("button", new AttributeFilter("aria-label", "Close")).first());

            assertThat(getText("body"), not(containsString("English")));
            scrollAndAwait(() -> browser.$("a", containingText("Sprache ändern")).first());
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

    private void scrollAndAwait(Supplier<FluentWebElement> element) throws InterruptedException {
        try {
            element.get().scrollIntoView(true);
        } catch (NoSuchElementException | StaleElementReferenceException ignored) {
            Thread.sleep(200);
            element.get().scrollIntoView(true);
        }
        browser.fluentWait().until(ExpectedConditions.visibilityOf(element.get().getElement()));
    }

    private void awaitClickable(Supplier<FluentWebElement> element) throws InterruptedException {
        int attempts = 0;
        while (attempts++ < 20) {
            if (element.get().clickable()) {
                return;
            } else {
                Thread.sleep(50);
            }
        }
    }

    private String getText(String selector) {
        return browser.$(selector).first().text();
    }
}
