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

    @Test
    void locationIsRoundedToATenthOfADegree() {
        RequestForm f = newMinimalForm();
        f.setLat(47.3769);
        f.setLon(-8.5417);
        assertEquals(47.4, f.getLat());
        assertEquals(-8.5, f.getLon());
    }

    @Test
    void getForLogDoesNotContainTheLocation() {
        RequestForm f = newMinimalForm();
        f.setLat(47.3769);
        f.setLon(8.5417);
        assertFalse(f.getForLog().contains("47"), f.getForLog());
        assertTrue(f.getForLog().contains("with location"), f.getForLog());
    }

    @Test
    void eTagDependsOnTheLocation() {
        RequestForm withoutLocation = newMinimalForm();
        RequestForm zurich = newMinimalForm();
        zurich.setLat(47.4);
        zurich.setLon(8.5);
        RequestForm geneva = newMinimalForm();
        geneva.setLat(46.2);
        geneva.setLon(6.1);
        assertNotEquals(withoutLocation.calculateETag(), zurich.calculateETag());
        assertNotEquals(zurich.calculateETag(), geneva.calculateETag());
    }

    @Test
    void validLocationIsAccepted() {
        RequestForm f = newMinimalForm();
        f.setLat(-90.0);
        f.setLon(180.0);
        assertNull(f.validate());
    }

    @Test
    void locationOutOfRangeIsRejected() {
        RequestForm f = newMinimalForm();
        f.setLat(91.0);
        f.setLon(8.5);
        assertEquals("error.location", f.validate().message());
    }

    @Test
    void dailyPhasesWithoutTimeframeDoNotCrashValidation() {
        RequestForm f = new RequestForm();
        f.setPhases(java.util.Map.of(MoonPhaseType.DAILY, true));
        assertNull(f.validate()); // missing from/to is reported by @Constraints.Required
    }

    @Test
    void sunWithTooLargeTimeframeIsRejected() {
        RequestForm f = newMinimalForm();
        f.setPhases(java.util.Map.of(MoonPhaseType.SUN, true));
        f.setFrom(java.time.ZonedDateTime.of(1800, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC));
        f.setTo(java.time.ZonedDateTime.of(2100, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC));
        assertEquals("error.fromTo.tolargefordaily", f.validate().message());
    }

    @Test
    void dailyWithUpTo102YearsIsAccepted() {
        RequestForm f = newMinimalForm();
        f.setPhases(java.util.Map.of(MoonPhaseType.DAILY, true));
        f.setFrom(java.time.ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC));
        f.setTo(java.time.ZonedDateTime.of(2102, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC));
        assertNull(f.validate());
    }

    @Test
    void dailyWithMoreThan102YearsIsRejectedAnnouncing100Years() {
        RequestForm f = newMinimalForm();
        f.setPhases(java.util.Map.of(MoonPhaseType.DAILY, true));
        f.setFrom(java.time.ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC));
        f.setTo(java.time.ZonedDateTime.of(2102, 1, 2, 0, 0, 0, 0, java.time.ZoneOffset.UTC));
        assertEquals("error.fromTo.tolargefordaily", f.validate().message());
        assertEquals("100", f.validate().key());
    }

    @Test
    void phasesWithoutDailyAreNotLimited() {
        RequestForm f = newMinimalForm();
        f.setPhases(java.util.Map.of(MoonPhaseType.FULLMOON, true));
        f.setFrom(java.time.ZonedDateTime.of(1000, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC));
        f.setTo(java.time.ZonedDateTime.of(3000, 12, 31, 0, 0, 0, 0, java.time.ZoneOffset.UTC));
        assertNull(f.validate());
    }

    @Test
    void locationWithOnlyLatitudeIsRejected() {
        RequestForm f = newMinimalForm();
        f.setLat(47.4);
        assertEquals("error.location", f.validate().message());
    }
}
