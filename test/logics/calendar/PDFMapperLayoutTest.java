package logics.calendar;

import logics.calculation.TotalCalculation;
import models.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.i18n.Lang;
import play.test.WithApplication;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The texts below and around the calendar grid (footer, garden legend, tide attribution) must not overlap.
 */
@SuppressWarnings("JUnitMixedFramework")
class PDFMapperLayoutTest extends WithApplication {

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

    @Test
    void noTextOverlapsWithGardenAndTides() throws IOException {
        for (String lang : List.of("de", "en", "hi")) {
            Collection<EventInstance> events = calculateEvents(Lang.forCode(lang));
            List<TextLine> lines = textLines(cut.map(events, Lang.forCode(lang), Hemisphere.NORTHERN));
            assertTrue(lines.stream().anyMatch(line -> line.text().contains("TICON-4")), lang + ": no attribution");
            for (int i = 0; i < lines.size(); i++) {
                for (int j = i + 1; j < lines.size(); j++) {
                    TextLine a = lines.get(i);
                    TextLine b = lines.get(j);
                    assertTrue(!a.bounds().intersects(b.bounds()), lang + ": \"" + a.text() + "\" " + a.bounds() + " overlaps \"" + b.text() + "\" " + b.bounds());
                }
            }
        }
    }

    private Collection<EventInstance> calculateEvents(Lang lang) {
        ZoneId zone = ZoneId.of("Europe/Berlin");
        RequestForm requestForm = new RequestForm();
        requestForm.setLat(53.9);
        requestForm.setLon(8.7);
        requestForm.setFrom(LocalDate.of(2026, 1, 1).atStartOfDay(zone));
        requestForm.setTo(LocalDate.of(2026, 12, 31).atTime(23, 59).atZone(zone));
        requestForm.setPhases(Map.of(MoonPhaseType.FULLMOON, true, MoonPhaseType.TIDE, true));
        requestForm.setStyle(EventStyle.WITH_DESCRIPTION.getStyle());
        requestForm.setEvents(Map.of(EventType.GARDEN_BIODYNAMIC, true));
        requestForm.setLang(lang);
        return calculation.calculate(requestForm);
    }

    private record TextLine(String text, Rectangle2D bounds) {
    }

    /** @return each piece of text with its box (y from the top), shrunk a little so that touching lines do not count */
    private static List<TextLine> textLines(byte[] pdf) throws IOException {
        List<TextLine> lines = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) {
                    if (text.isBlank()) {
                        return;
                    }
                    TextPosition first = positions.getFirst();
                    TextPosition last = positions.getLast();
                    float height = (float) positions.stream().mapToDouble(TextPosition::getHeightDir).max().orElse(0);
                    float x = first.getXDirAdj();
                    float width = last.getXDirAdj() + last.getWidthDirAdj() - x;
                    lines.add(new TextLine(text, new Rectangle2D.Float(x + 0.5f, first.getYDirAdj() - height + 0.5f, width - 1, height - 1)));
                }
            };
            stripper.getText(doc);
        }
        return lines;
    }
}
