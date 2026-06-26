package logics.calculation;

import models.*;
import org.junit.jupiter.api.Test;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TotalCalculationBiodynamicWiringTest {
    @Test
    void includesBiodynamicGardenEventsWhenToggled() {
        MessagesApi messages = mock(MessagesApi.class, inv ->
                inv.getArguments().length > 1 ? inv.getArguments()[1].toString() : "");
        MoonPhasesCalculation phases = mock(MoonPhasesCalculation.class);
        MoonEventCalculation events = mock(MoonEventCalculation.class);
        GardenSynodicCalculation synodic = mock(GardenSynodicCalculation.class);
        GardenBiodynamicCalculation biodynamic = new GardenBiodynamicCalculation(new logics.astronomy.MeeusEphemeris(), messages);
        TotalCalculation total = new TotalCalculation(phases, events, synodic, biodynamic, messages);

        RequestForm f = new RequestForm();
        f.setLang(Lang.forCode("en"));
        f.setStyle("withDescription");
        f.setHemisphere("northern");
        f.setFrom(ZonedDateTime.parse("2025-01-01T00:00:00Z"));
        f.setTo(ZonedDateTime.parse("2025-03-01T00:00:00Z"));
        f.setEvents(Map.of(EventType.GARDEN_BIODYNAMIC, true));

        Collection<EventInstance> out = total.calculate(f);
        assertTrue(out.stream().anyMatch(e -> e.getEventTypeId().startsWith("garden-biodynamic")));
    }
}
