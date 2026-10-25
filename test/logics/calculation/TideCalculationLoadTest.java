package logics.calculation;

import models.EventInstance;
import models.MoonPhaseType;
import models.RequestForm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.i18n.Lang;
import play.test.WithApplication;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The largest calendar that can be requested with tides: daily phases, sun and tides over 100 years.
 */
@SuppressWarnings("JUnitMixedFramework")
class TideCalculationLoadTest extends WithApplication {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final LocalDate FROM = LocalDate.of(2000, 1, 1);
    private static final LocalDate TO = LocalDate.of(2099, 12, 31);
    private static final int DAYS = 36525;
    private TotalCalculation calculation;

    @BeforeEach
    void setUp() {
        startPlay();
        calculation = app.injector().instanceOf(TotalCalculation.class);
    }

    @AfterEach
    void tearDown() {
        stopPlay();
    }

    @Test
    void dailySunAndTideOver100Years() {
        long start = System.nanoTime();
        Collection<EventInstance> events = calculation.calculate(requestForm(Map.of(MoonPhaseType.DAILY, true, MoonPhaseType.SUN, true, MoonPhaseType.TIDE, true)));
        Duration all = Duration.ofNanos(System.nanoTime() - start);

        start = System.nanoTime();
        calculation.calculate(requestForm(Map.of(MoonPhaseType.SUN, true)));
        Duration sunOnly = Duration.ofNanos(System.nanoTime() - start);
        start = System.nanoTime();
        calculation.calculate(requestForm(Map.of(MoonPhaseType.TIDE, true)));
        Duration tideOnly = Duration.ofNanos(System.nanoTime() - start);
        System.out.println("daily, sun and tide over 100 years: " + all.toMillis() + " ms, sun alone: " + sunOnly.toMillis() + " ms, tide alone: " + tideOnly.toMillis() + " ms");

        assertThat(all, lessThan(Duration.ofSeconds(60)));
        assertEquals(3 * DAYS, events.size());
        List<String> attributions = events.stream().filter(event -> event.getEventTypeId().equals(TideCalculation.TIDE_EVENT_TYPE_ID)).map(EventInstance::getAttribution).toList();
        assertEquals(DAYS, attributions.size());
        // the same instance, not a copy per event
        assertEquals(1, attributions.stream().map(System::identityHashCode).distinct().count());
    }

    private static RequestForm requestForm(Map<MoonPhaseType, Boolean> phases) {
        RequestForm requestForm = new RequestForm();
        requestForm.setPhases(phases);
        requestForm.setFrom(FROM.atStartOfDay(BERLIN));
        requestForm.setTo(TO.atTime(23, 59, 59).atZone(BERLIN));
        requestForm.setLang(Lang.forCode("de"));
        requestForm.setLat(53.9);
        requestForm.setLon(8.7);
        return requestForm;
    }
}
