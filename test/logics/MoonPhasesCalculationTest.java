package logics;

import models.Event;
import models.RequestForm;
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
        requestForm.getPhases().put("full", true);
        requestForm.getPhases().put("new", true);
        requestForm.setFrom(ZonedDateTime.of(2015, 10, 20, 12, 0, 0, 0, ZoneOffset.UTC));
        requestForm.setTo(ZonedDateTime.of(2015, 11, 30, 12, 0, 0, 0, ZoneOffset.UTC));
    }

    private Collection<Event> calculate(RequestForm requestForm) {
        final Collection<Event> eventCollection = new TreeSet<>();
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
        final Collection<Event> actual = calculate(requestForm);
        assertThat(actual, contains(eventAt(LocalDate.of(2015, 11, 25))));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetMoonWithMoreResult() {
        final Collection<Event> actual = calculate(requestForm);
        assertThat(actual, containsInAnyOrder(eventAt(LocalDate.of(2015, 10, 27)), eventAt(LocalDate.of(2015, 11, 11)), eventAt(LocalDate.of(2015, 11, 25))));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetMoonWithResultsInTemporalOrder() {
        final Collection<Event> actual = calculate(requestForm);
        assertThat(actual, contains(eventAt(LocalDate.of(2015, 10, 27)), eventAt(LocalDate.of(2015, 11, 11)), eventAt(LocalDate.of(2015, 11, 25))));
    }

    @Test
    public void testGetHalfMoonResults() {
        requestForm.getPhases().clear();
        requestForm.getPhases().put("quarter", true);
        requestForm.setTo(ZonedDateTime.of(2015, 10, 31, 12, 0, 0, 0, ZoneOffset.UTC));

        final Collection<Event> actual = calculate(requestForm);

        assertThat(actual, contains(eventAt(LocalDate.of(2015, 10, 20))));
    }

    @Test
    public void testDailyEventsProvidedForAllDays() {
        requestForm.getPhases().clear();
        requestForm.getPhases().put("daily", true);

        final Collection<Event> actual = calculate(requestForm);

        assertThat(actual, hasSize(42));
    }

    private Matcher<Event> eventAt(final LocalDate expectedDate) {
        return new FeatureMatcher<Event, LocalDate>(equalTo(expectedDate), "LocalDate", "Date") {
            @Override
            protected LocalDate featureValueOf(final Event actual) {
                return actual.getDateTime().toLocalDate();
            }
        };
    }

}
