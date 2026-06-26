package logics.calculation;

import models.*;
import org.junit.jupiter.api.Test;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TotalCalculationSynodicWiringTest {
    @Test
    void includesSynodicGardenEventsWhenToggled() {
        MessagesApi messages = mock(MessagesApi.class, inv ->
                inv.getArguments().length > 1 ? inv.getArguments()[1].toString() : "");
        MoonPhasesCalculation phases = mock(MoonPhasesCalculation.class);
        MoonEventCalculation events = mock(MoonEventCalculation.class);
        GardenSynodicCalculation synodic = new GardenSynodicCalculation(new MoonPhaseData(), messages);
        GardenBiodynamicCalculation biodynamic = new GardenBiodynamicCalculation(new logics.astronomy.MeeusEphemeris(), messages);
        // NOTE: constructor signature below must match Task C4 implementation.
        TotalCalculation total = new TotalCalculation(phases, events, synodic, biodynamic, messages);

        RequestForm f = new RequestForm();
        f.setLang(Lang.forCode("en"));
        f.setStyle("withDescription");
        f.setHemisphere("northern");
        f.setFrom(ZonedDateTime.parse("2025-01-01T00:00:00Z"));
        f.setTo(ZonedDateTime.parse("2025-03-01T00:00:00Z"));
        f.setEvents(Map.of(EventType.GARDEN_SYNODIC, true));

        Collection<EventInstance> out = total.calculate(f);
        assertTrue(out.stream().anyMatch(e -> e.getEventTypeId().startsWith("garden-synodic")));
    }
}
