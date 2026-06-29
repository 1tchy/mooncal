package logics.calendar;

import logics.calculation.TotalCalculation;
import models.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.i18n.Lang;
import play.test.WithApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PDFMapperGardenIconsTest extends WithApplication {

    private TotalCalculation calculation;
    private PDFMapper cut;

    @BeforeEach
    void setUp() {
        startPlay();
        calculation = app.injector().instanceOf(TotalCalculation.class);
        cut = app.injector().instanceOf(PDFMapper.class);
    }

    @AfterEach
    void tearDown() {
        stopPlay();
    }

    private Collection<EventInstance> events(boolean withGarden) {
        RequestForm f = new RequestForm();
        f.setFrom(ZonedDateTime.of(LocalDateTime.of(2026, 1, 1, 0, 0), ZoneId.of("Europe/Zurich")));
        f.setTo(ZonedDateTime.of(LocalDateTime.of(2026, 12, 31, 23, 59), ZoneId.of("Europe/Zurich")));
        f.setPhases(Map.of(MoonPhaseType.FULLMOON, true, MoonPhaseType.NEWMOON, true, MoonPhaseType.QUARTER, true));
        f.setStyle(EventStyle.WITH_DESCRIPTION.getStyle());
        f.setEvents(Map.of(
                EventType.LUNARECLIPSE, true, EventType.SOLARECLIPSE, true, EventType.MOONLANDING, false,
                EventType.GARDEN_SYNODIC, withGarden, EventType.GARDEN_BIODYNAMIC, withGarden));
        f.setHemisphere("northern");
        f.setLang(Lang.forCode("en"));
        return calculation.calculate(f);
    }

    private static int imageCount(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDResources resources = doc.getPage(0).getResources();
            int count = 0;
            for (COSName name : resources.getXObjectNames()) {
                PDXObject xObject = resources.getXObject(name);
                if (xObject instanceof PDImageXObject) {
                    count++;
                }
            }
            return count;
        }
    }

    @Test
    void gardenPdfEmbedsIconImagesBeyondTheNonGardenBaseline() throws IOException {
        byte[] gardenPdf = cut.map(events(true), Lang.forCode("en"), Hemisphere.NORTHERN);
        byte[] plainPdf = cut.map(events(false), Lang.forCode("en"), Hemisphere.NORTHERN);
        // Write the garden PDF out so it can be inspected visually.
        Files.write(Path.of("target/garden-icons-test.pdf"), gardenPdf);

        // The seven distinct garden icons (root/leaf/flower/fruit/bad-day/waxing/waning) appear over a full year,
        // so the garden PDF embeds clearly more image XObjects than the same calendar without garden events.
        int garden = imageCount(gardenPdf);
        int plain = imageCount(plainPdf);
        assertTrue(garden >= plain + 5,
                "expected garden PDF to embed the garden icons; images garden=" + garden + " plain=" + plain);
    }

    @Test
    void gardenPdfShowsTheSynodicAndBadDayLegend() throws IOException {
        byte[] pdf = cut.map(events(true), Lang.forCode("en"), Hemisphere.NORTHERN);
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("Waxing moon"), "synodic waxing legend missing: " + text);
            assertTrue(text.contains("Waning moon"), "synodic waning legend missing");
            assertTrue(text.contains("Unfavourable garden day"), "bad-day legend missing");
            assertTrue(text.contains("Root days"), "biodynamic legend missing");
        }
    }
}
