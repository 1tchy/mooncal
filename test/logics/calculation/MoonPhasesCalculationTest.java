package logics.calculation;

import models.MoonPhaseType;
import models.RequestForm;
import models.ZonedEvent;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import play.test.WithApplication;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.TreeSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class MoonPhasesCalculationTest extends WithApplication {

    private MoonPhasesCalculation cut = new MoonPhasesCalculation();
    private RequestForm requestForm;

    @Before
    public void setup() {
        requestForm = new RequestForm();
        requestForm.getPhases().put(MoonPhaseType.FULLMOON, true);
        requestForm.getPhases().put(MoonPhaseType.NEWMOON, true);
        requestForm.setFrom(ZonedDateTime.of(2015, 10, 20, 12, 0, 0, 0, ZoneOffset.UTC));
        requestForm.setTo(ZonedDateTime.of(2015, 11, 30, 12, 0, 0, 0, ZoneOffset.UTC));
    }

    private Collection<ZonedEvent> calculate(RequestForm requestForm) {
        final Collection<ZonedEvent> eventCollection = new TreeSet<>();
        cut.calculate(requestForm, eventCollection);
        return eventCollection;
    }

    @Test
    public void testGetFullmoonWithNoResult() {
        requestForm.setFrom(ZonedDateTime.of(2015, 11, 12, 12, 0, 0, 0, ZoneOffset.UTC));
        requestForm.setTo(ZonedDateTime.of(2015, 11, 20, 12, 0, 0, 0, ZoneOffset.UTC));
        assertThat(calculate(requestForm), is(empty()));
    }

    @Test
    public void testGetFullmoonWithOneResult() {
        requestForm.setFrom(ZonedDateTime.of(2015, 11, 20, 12, 0, 0, 0, ZoneOffset.UTC));
        final Collection<ZonedEvent> actual = calculate(requestForm);
        assertThat(actual, contains(eventAt(LocalDate.of(2015, 11, 25))));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetMoonWithMoreResult() {
        final Collection<ZonedEvent> actual = calculate(requestForm);
        assertThat(actual, containsInAnyOrder(eventAt(LocalDate.of(2015, 10, 27)), eventAt(LocalDate.of(2015, 11, 11)), eventAt(LocalDate.of(2015, 11, 25))));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetMoonWithResultsInTemporalOrder() {
        final Collection<ZonedEvent> actual = calculate(requestForm);
        assertThat(actual, contains(eventAt(LocalDate.of(2015, 10, 27)), eventAt(LocalDate.of(2015, 11, 11)), eventAt(LocalDate.of(2015, 11, 25))));
    }

    @Test
    public void testGetHalfMoonResults() {
        requestForm.getPhases().clear();
        requestForm.getPhases().put(MoonPhaseType.QUARTER, true);
        requestForm.setTo(ZonedDateTime.of(2015, 10, 31, 12, 0, 0, 0, ZoneOffset.UTC));

        final Collection<ZonedEvent> actual = calculate(requestForm);

        assertThat(actual, contains(eventAt(LocalDate.of(2015, 10, 20))));
    }

    @Test
    public void testDailyEventsProvidedForAllDays() {
        requestForm.getPhases().clear();
        requestForm.getPhases().put(MoonPhaseType.DAILY, true);

        final Collection<ZonedEvent> actual = calculate(requestForm);

        assertThat(actual, hasSize(42));
    }

    private Matcher<ZonedEvent> eventAt(final LocalDate expectedDate) {
        return new FeatureMatcher<ZonedEvent, LocalDate>(equalTo(expectedDate), "LocalDate", "Date") {
            @Override
            protected LocalDate featureValueOf(final ZonedEvent actual) {
                return actual.getDateTime().toLocalDate();
            }
        };
    }

}
