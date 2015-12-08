package logics;

import models.Event;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class MoonPhasesCalculationTest {

	private MoonPhasesCalculation cut = new MoonPhasesCalculation();

	@Test
	public void testGetFullmoonWithNoResult() {
		assertThat(cut.calculate(LocalDate.of(2015, 11, 12), LocalDate.of(2015, 11, 20), ZoneOffset.UTC), is(empty()));
	}

	@Test
	public void testGetFullmoonWithOneResult() {
		final Collection<Event> actual = cut.calculate(LocalDate.of(2015, 11, 20), LocalDate.of(2015, 11, 30), ZoneOffset.UTC);
		assertThat(actual, contains(eventAt(LocalDate.of(2015, 11, 25))));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetMoonWithMoreResult() {
		final Collection<Event> actual = cut.calculate(LocalDate.of(2015, 10, 20), LocalDate.of(2015, 11, 30), ZoneOffset.UTC);
		assertThat(actual, containsInAnyOrder(eventAt(LocalDate.of(2015, 10, 27)), eventAt(LocalDate.of(2015, 11, 11)), eventAt(LocalDate.of(2015, 11, 25))));
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
