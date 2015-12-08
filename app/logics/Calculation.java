package logics;

import models.Event;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;

public interface Calculation {
	Collection<Event> calculate(LocalDate from, LocalDate to, ZoneId at);

	/**
	 * @param eventCollection The collection which will be updated with the newly calculated events
	 */
	void calculate(LocalDate from, LocalDate to, ZoneId at, Collection<Event> eventCollection);
}
