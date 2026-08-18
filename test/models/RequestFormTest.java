package models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestFormTest {
    private static RequestForm newMinimalForm() {
        RequestForm f = new RequestForm();
        f.setStyle("withDescription");
        f.setHemisphere("northern");
        f.setLang(play.i18n.Lang.forCode("en"));
        f.setFrom(java.time.ZonedDateTime.parse("2025-01-01T00:00:00Z"));
        f.setTo(java.time.ZonedDateTime.parse("2025-12-31T00:00:00Z"));
        return f;
    }

    @Test
    void getForLogContainsViaWhenSet() {
        RequestForm f = newMinimalForm();
        f.setVia("Claude");
        assertTrue(f.getForLog().contains("via:Claude"), f.getForLog());
    }

    @Test
    void getForLogOmitsViaWhenNotSet() {
        RequestForm f = newMinimalForm();
        assertFalse(f.getForLog().contains("via:"), f.getForLog());
    }
}
