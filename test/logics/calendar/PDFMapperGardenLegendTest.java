package logics.calendar;

import models.EventInstance;
import models.Hemisphere;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PDFMapperGardenLegendTest {
    private static final String DISCLAIMER = "GARDEN_DISCLAIMER_UNIQUE_TEXT";

    private final MessagesApi messages = mock(MessagesApi.class, inv -> {
        Object[] args = inv.getArguments();
        if (args.length > 1 && args[1] instanceof String key) {
            return switch (key) {
                case "garden.disclaimer" -> DISCLAIMER;
                case "garden.pdf.legend.root" -> "LEGENDROOT";
                case "garden.pdf.legend.leaf" -> "LEGENDLEAF";
                case "garden.pdf.legend.flower" -> "LEGENDFLOWER";
                case "garden.pdf.legend.fruit" -> "LEGENDFRUIT";
                case "pdf.title" -> "Moon Calendar";
                case "navigation.thank" -> "en/donate";
                case "pdf.timezone" -> "Timezone";
                default -> key;
            };
        }
        return "thank";
    });

    private final ZonedDateTime dt = ZonedDateTime.of(2025, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void noDisclaimerInPdfEvenWithGardenEvent() throws IOException {
        PDFMapper mapper = new PDFMapper(messages);
        EventInstance biodynamic = new EventInstance(dt, "Leaf", "leaf", null, ZoneOffset.UTC, "garden-biodynamic-leaf");
        String text = extractText(mapper.map(List.of(biodynamic), Lang.forCode("en"), Hemisphere.NORTHERN));
        assertFalse(text.contains(DISCLAIMER), "Disclaimer must NOT appear in the PDF. Extracted: " + text);
    }

    @Test
    void containsAllPlantPartExplanationsWhenBiodynamicEventPresent() throws IOException {
        PDFMapper mapper = new PDFMapper(messages);
        EventInstance biodynamic = new EventInstance(dt, "Leaf", "leaf", null, ZoneOffset.UTC, "garden-biodynamic-leaf");
        String text = extractText(mapper.map(List.of(biodynamic), Lang.forCode("en"), Hemisphere.NORTHERN));
        for (String marker : List.of("LEGENDROOT", "LEGENDLEAF", "LEGENDFLOWER", "LEGENDFRUIT")) {
            assertTrue(text.contains(marker), "Expected plant-part explanation " + marker + " in PDF. Extracted: " + text);
        }
    }

    @Test
    void noLegendWhenNoBiodynamicEvent() throws IOException {
        PDFMapper mapper = new PDFMapper(messages);
        EventInstance synodic = new EventInstance(dt, "Waxing", "Waxing", null, ZoneOffset.UTC, "garden-synodic-waxing");
        EventInstance fullmoon = new EventInstance(dt, "Full Moon", "Full Moon", null, ZoneOffset.UTC, "fullmoon");
        String text = extractText(mapper.map(List.of(synodic, fullmoon), Lang.forCode("en"), Hemisphere.NORTHERN));
        assertFalse(text.contains("LEGENDROOT"), "Legend should only appear for biodynamic events. Extracted: " + text);
    }

    private String extractText(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }
}
