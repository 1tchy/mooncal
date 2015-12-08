package logics;

import models.Event;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;

public interface Calculation {
	Collection<Event> calculate(LocalDate from, LocalDate to, ZoneId at);
}
