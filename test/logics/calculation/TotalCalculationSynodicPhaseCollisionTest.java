package logics.calculation;

import models.*;
import org.junit.jupiter.api.Test;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Regression test for the TreeSet dedup collision between MoonPhasesCalculation and
 * GardenSynodicCalculation: both emit events at the exact same instant (from moon-phases.csv),
 * so the second event was silently dropped when EventInstance.compareTo only compared dateTime.
 */
class TotalCalculationSynodicPhaseCollisionTest {

    @Test
    void fullmoonAndGardenSynodicWaningBothPresentWhenEnabled() {
        MessagesApi messages = mock(MessagesApi.class, inv ->
                inv.getArguments().length > 1 ? inv.getArguments()[1].toString() : "");
        // Use real MoonPhasesCalculation and GardenSynodicCalculation (both read moon-phases.csv)
        MoonPhasesCalculation phases = new MoonPhasesCalculation(messages);
        GardenSynodicCalculation synodic = new GardenSynodicCalculation(new MoonPhaseData(), messages);
        MoonEventCalculation moonEvents = mock(MoonEventCalculation.class);
        GardenBiodynamicCalculation biodynamic = mock(GardenBiodynamicCalculation.class);

        TotalCalculation total = new TotalCalculation(phases, moonEvents, synodic, biodynamic, messages);

        RequestForm f = new RequestForm();
        f.setLang(Lang.forCode("en"));
        f.setStyle("withDescription");
        f.setHemisphere("northern");
        f.setFrom(ZonedDateTime.parse("2025-01-01T00:00:00Z"));
        f.setTo(ZonedDateTime.parse("2025-04-01T00:00:00Z"));
        // Enable BOTH fullmoon phase and synodic garden calendar
        f.setPhases(Map.of(MoonPhaseType.FULLMOON, true));
        f.setEvents(Map.of(EventType.GARDEN_SYNODIC, true));

        Collection<EventInstance> out = total.calculate(f);

        boolean hasFullmoon = out.stream().anyMatch(e -> e.getEventTypeId().equals("fullmoon"));
        boolean hasGardenSynodicWaning = out.stream().anyMatch(e -> e.getEventTypeId().equals("garden-synodic-waning"));

        assertTrue(hasFullmoon, "Expected a fullmoon event but none found");
        assertTrue(hasGardenSynodicWaning, "Expected a garden-synodic-waning event but none found — collision bug: both events share the same instant and the synodic event was dropped by TreeSet dedup");
    }
}
