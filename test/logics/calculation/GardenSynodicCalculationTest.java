package logics.calculation;

import models.EventInstance;
import models.EventType;
import models.RequestForm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.i18n.Lang;
import play.i18n.MessagesApi;
import play.test.WithApplication;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GardenSynodicCalculationTest extends WithApplication {

    private final MessagesApi messages = mock(MessagesApi.class, invocation -> {
        // echo the i18n key so assertions can match on it
        Object[] a = invocation.getArguments();
        return a.length > 1 ? a[1].toString() : "";
    });

    private MoonPhaseData moonPhaseData;

    @BeforeEach
    void setup() {
        startPlay();
        moonPhaseData = new MoonPhaseData();
    }

    @AfterEach
    void tearDown() {
        stopPlay();
    }

    private RequestForm form() {
        RequestForm f = new RequestForm();
        f.setLang(Lang.forCode("en"));
        f.setStyle("withDescription");
        f.setHemisphere("northern");
        f.setFrom(ZonedDateTime.parse("2025-01-01T00:00:00Z"));
        f.setTo(ZonedDateTime.parse("2025-03-01T00:00:00Z"));
        f.setEvents(Map.of(EventType.GARDEN_SYNODIC, true));
        return f;
    }

    @Test
    void simpleModeEmitsWaxingAndWaningTransitions() {
        GardenSynodicCalculation calc = new GardenSynodicCalculation(moonPhaseData, messages);
        Collection<EventInstance> out = new TreeSet<>();
        calc.calculate(form(), out);
        assertFalse(out.isEmpty());
        Set<String> types = new HashSet<>();
        out.forEach(e -> types.add(e.getEventTypeId()));
        assertEquals(Set.of("garden-synodic-waxing", "garden-synodic-waning"), types);
    }

    @Test
    void disabledEmitsNothing() {
        RequestForm f = form();
        f.setEvents(Map.of(EventType.GARDEN_SYNODIC, false));
        Collection<EventInstance> out = new TreeSet<>();
        new GardenSynodicCalculation(moonPhaseData, messages).calculate(f, out);
        assertTrue(out.isEmpty());
    }

    @Test
    void simpleModeEventsAreMultiDay() {
        GardenSynodicCalculation calc = new GardenSynodicCalculation(moonPhaseData, messages);
        Collection<EventInstance> out = new TreeSet<>();
        calc.calculate(form(), out);
        assertFalse(out.isEmpty());
        // In simple mode, waxing starts at new moon and ends at next full moon (~14 days)
        // All events should be multi-day (end instant always exists in the loaded phase data
        // for dates well within the data range)
        out.forEach(e -> assertTrue(e.isMultiDay(),
                "Expected multi-day synodic event but got single-day: " + e
                + " end=" + e.getEndLocalDate()));
    }

}
