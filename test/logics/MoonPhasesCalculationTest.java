package logics;

import models.Event;
import models.RequestForm;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class MoonPhasesCalculationTest {

	private MoonPhasesCalculation cut = new MoonPhasesCalculation();
	private RequestForm requestForm;

	@Before
	public void setup() {
		requestForm = new RequestForm();
		requestForm.events = new ArrayList<>();
		requestForm.phases = new ArrayList<>();
		requestForm.phases.add(new RequestForm.EventRequest("Vollmond", true));
		requestForm.phases.add(new RequestForm.EventRequest("Neumond", true));
		requestForm.from = ZonedDateTime.of(2015, 10, 20, 12, 0, 0, 0, ZoneOffset.UTC);
		requestForm.to = ZonedDateTime.of(2015, 11, 30, 12, 0, 0, 0, ZoneOffset.UTC);
	}

	@Test
	public void testGetFullmoonWithNoResult() {
		requestForm.from = ZonedDateTime.of(2015, 11, 12, 12, 0, 0, 0, ZoneOffset.UTC);
		requestForm.to = ZonedDateTime.of(2015, 11, 20, 12, 0, 0, 0, ZoneOffset.UTC);
		assertThat(cut.calculate(requestForm), is(empty()));
	}

	@Test
	public void testGetFullmoonWithOneResult() {
		requestForm.from = ZonedDateTime.of(2015, 11, 20, 12, 0, 0, 0, ZoneOffset.UTC);
		final Collection<Event> actual = cut.calculate(requestForm);
		assertThat(actual, contains(eventAt(LocalDate.of(2015, 11, 25))));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetMoonWithMoreResult() {
		final Collection<Event> actual = cut.calculate(requestForm);
		assertThat(actual, containsInAnyOrder(eventAt(LocalDate.of(2015, 10, 27)), eventAt(LocalDate.of(2015, 11, 11)), eventAt(LocalDate.of(2015, 11, 25))));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetMoonWithResultsInTemporalOrder() {
		final Collection<Event> actual = cut.calculate(requestForm);
		assertThat(actual, contains(eventAt(LocalDate.of(2015, 10, 27)), eventAt(LocalDate.of(2015, 11, 11)), eventAt(LocalDate.of(2015, 11, 25))));
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
