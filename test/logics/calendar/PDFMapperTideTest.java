package logics.calendar;

import com.fasterxml.jackson.databind.ObjectMapper;
import logics.calculation.TideCalculation;
import models.EventInstance;
import models.Hemisphere;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PDFMapperTideTest {

    private final MessagesApi messages = mock(MessagesApi.class, inv -> {
        Object[] args = inv.getArguments();
        if (args.length > 1 && args[1] instanceof String key) {
            return switch (key) {
                case "navigation.thank" -> "en/donate";
                default -> key;
            };
        }
        return "thank";
    });

    private final ZonedDateTime dt = ZonedDateTime.of(2026, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void attributionIsPrintedOnce() throws IOException {
        String text = extractText(map(tide(dt, "ATTRIBUTION_UNIQUE"), tide(dt.plusDays(1), "ATTRIBUTION_UNIQUE")));
        assertEquals(1, text.split("ATTRIBUTION_UNIQUE", -1).length - 1, text);
    }

    @Test
    void allDistinctAttributionsArePrinted() throws IOException {
        String text = extractText(map(tide(dt, "ATTRIBUTION_ONE"), tide(dt.plusDays(1), "ATTRIBUTION_TWO")));
        assertTrue(text.contains("ATTRIBUTION_ONE"), text);
        assertTrue(text.contains("ATTRIBUTION_TWO"), text);
    }

    @Test
    void noAttributionWithoutTideEvents() throws IOException {
        String text = extractText(map(new EventInstance(dt, "Full Moon", "Full Moon", null, ZoneOffset.UTC, "fullmoon")));
        assertFalse(text.contains("ATTRIBUTION"), text);
    }

    @Test
    void tideSymbolDoesNotCrash() throws IOException {
        EventInstance tide = new EventInstance(dt, "Flut ↑6:12 · Ebbe ↓0:05", "≈↑6:12 ↓0:05", null, ZoneOffset.UTC, TideCalculation.TIDE_EVENT_TYPE_ID, "ATTRIBUTION");
        for (String lang : List.of("de", "hi")) {
            assertTrue(extractText(new PDFMapper(messages).map(List.of(tide), Lang.forCode(lang), Hemisphere.NORTHERN)).contains("6:12"), lang);
        }
    }

    @Test
    void allExportedAttributionsCanBeRendered() throws IOException {
        List<String> attributions = new ArrayList<>();
        try (InputStream in = new GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/logics/calculation/tides/tide-stations.json.gz")))) {
            new ObjectMapper().readTree(in).get("attributions").forEach(attribution -> attributions.add(attribution.asText()));
        }
        List<EventInstance> events = new ArrayList<>();
        for (int i = 0; i < attributions.size(); i++) {
            events.add(tide(dt.plusDays(i), attributions.get(i)));
        }
        for (String lang : List.of("de", "hi")) {
            String text = extractText(new PDFMapper(messages).map(events, Lang.forCode(lang), Hemisphere.NORTHERN)).replaceAll("\\s+", " ");
            for (String attribution : attributions) {
                assertTrue(text.contains(attribution.substring(attribution.length() - 40)), lang + ": " + attribution);
            }
        }
    }

    private static EventInstance tide(ZonedDateTime at, String attribution) {
        return new EventInstance(at, "Flut ↑6:12", "≈↑6:12", "Hochwasser um 6:12", ZoneOffset.UTC, TideCalculation.TIDE_EVENT_TYPE_ID, attribution);
    }

    private byte[] map(EventInstance... events) {
        return new PDFMapper(messages).map(List.of(events), Lang.forCode("en"), Hemisphere.NORTHERN);
    }

    private static String extractText(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }
}
