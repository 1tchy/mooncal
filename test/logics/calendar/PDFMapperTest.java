package logics.calendar;

import de.redsix.pdfcompare.PdfComparator;
import logics.calculation.TotalCalculation;
import models.*;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import play.test.WithApplication;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertTrue;

public class PDFMapperTest extends WithApplication {
    private static final Lang LANG_DE = Lang.forCode("de");
    private static final Lang LANG_RO = Lang.forCode("ro");
    private static final Lang LANG_HI = Lang.forCode("hi");
    private TotalCalculation calculation;
    private PDFMapper cut;

    @Before
    public void setUp() {
        calculation = app.injector().instanceOf(TotalCalculation.class);
        cut = app.injector().instanceOf(PDFMapper.class);
    }

    // http://localhost:9000/mooncal.pdf?lang=de&phases[full]=true&phases[new]=true&phases[quarter]=true&phases[daily]=false&style=withDescription&events[lunareclipse]=true&events[solareclipse]=true&events[moonlanding]=true&from=2025-01-01T00:00:00Europe/Zurich&to=2025-12-31T23:59:59Europe/Zurich
    @Test
    public void de() throws IOException {
        var events = calculateEvents(LANG_DE, Map.of(MoonPhaseType.FULLMOON, true, MoonPhaseType.NEWMOON, true, MoonPhaseType.QUARTER, true));
        byte[] actual = cut.map(events, LANG_DE);
        assertEquals("mooncal_de.pdf", actual);
    }

    // http://localhost:9000/mooncal.pdf?lang=ro&phases[full]=true&phases[new]=false&phases[quarter]=false&phases[daily]=true&style=withDescription&events[lunareclipse]=true&events[solareclipse]=true&events[moonlanding]=true&from=2025-01-01T00:00:00Europe/Zurich&to=2025-12-31T23:59:59Europe/Zurich
    @Test
    public void roDaily() throws IOException {
        var events = calculateEvents(LANG_RO, Map.of(MoonPhaseType.FULLMOON, true, MoonPhaseType.DAILY, true));
        byte[] actual = cut.map(events, LANG_RO);
        assertEquals("mooncal_ro_daily.pdf", actual);
    }

    // http://localhost:9000/mooncal.pdf?lang=hi&phases[full]=true&phases[new]=false&phases[quarter]=false&phases[daily]=false&style=withDescription&events[lunareclipse]=true&events[solareclipse]=true&events[moonlanding]=true&from=2025-01-01T00:00:00Europe/Zurich&to=2025-12-31T23:59:59Europe/Zurich
    @Test
    public void hi() throws IOException {
        var events = calculateEvents(LANG_HI, Map.of(MoonPhaseType.FULLMOON, true));
        byte[] actual = cut.map(events, LANG_HI);
        assertEquals("mooncal_hi.pdf", actual);
    }

    private Collection<EventInstance> calculateEvents(Lang lang, Map<MoonPhaseType, Boolean> phases) {
        RequestForm requestForm = new RequestForm();
        requestForm.setFrom(ZonedDateTime.of(LocalDateTime.of(2025, 1, 1, 0, 0), ZoneId.of("Europe/Zurich")));
        requestForm.setTo(ZonedDateTime.of(LocalDateTime.of(2025, 12, 31, 23, 59), ZoneId.of("Europe/Zurich")));
        requestForm.setPhases(phases);
        requestForm.setStyle(EventStyle.WITH_DESCRIPTION.getStyle());
        requestForm.setEvents(Map.of(EventType.LUNARECLIPSE, true, EventType.SOLARECLIPSE, true));
        requestForm.setLang(lang);
        return calculation.calculate(requestForm);
    }

    private static void assertEquals(String expectedPdfFilename, byte[] actualPdf) throws IOException {
        assertTrue(new PdfComparator<>(
                requireNonNull(PDFMapperTest.class.getResourceAsStream("/" + expectedPdfFilename)),
                new ByteArrayInputStream(actualPdf)
        ).compare().writeTo("target/diff" + expectedPdfFilename.replace(".pdf", "")));
    }
}