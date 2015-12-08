package logics;

import models.Event;
import models.RequestForm;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class MoonEventCalculationTest {

	private MoonEventCalculation cut = new MoonEventCalculation();

	private RequestForm prepareRequestForm(String event, LocalDate from, LocalDate to) {
		RequestForm requestForm = new RequestForm();
		requestForm.events = new ArrayList<>();
		requestForm.phases = new ArrayList<>();
		requestForm.events.add(new RequestForm.EventRequest(event, true));
		requestForm.from = ZonedDateTime.of(from, LocalTime.NOON, ZoneOffset.UTC);
		requestForm.to = ZonedDateTime.of(to, LocalTime.NOON, ZoneOffset.UTC);
		return requestForm;
	}

	@Test
	public void testFindFirstKnownLunarEclipse() {
		RequestForm requestForm = prepareRequestForm("Mondfinsternis", LocalDate.of(-2000, 1, 1), LocalDate.of(-1998, 5, 18));
		final Collection<Event> actual = cut.calculate(requestForm);
		assertThat(actual, hasSize(1));
		assertThat(actual.iterator().next().getDateTime(), equalTo(ZonedDateTime.of(-1998, 5, 17, 5, 47, 36, 0, ZoneOffset.UTC)));
	}

	@Test
	public void testFindLastKnownLunarEclipse() {
		RequestForm requestForm = prepareRequestForm("Mondfinsternis", LocalDate.of(2999, 11, 1), LocalDate.of(5000, 1, 1));
		final Collection<Event> actual = cut.calculate(requestForm);
		assertThat(actual, hasSize(1));
		assertThat(actual.iterator().next().getDateTime(), equalTo(ZonedDateTime.of(2999, 11, 14, 16, 41, 25, 0, ZoneOffset.UTC)));
	}

	@Test
	public void testFindSeveralLunarEclipse() {
		RequestForm requestForm = prepareRequestForm("Mondfinsternis", LocalDate.of(2014, 1, 1), LocalDate.of(2015, 12, 31));
		final Collection<Event> actual = cut.calculate(requestForm);
		assertThat(actual, hasSize(4));
	}

}
