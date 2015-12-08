package logics;

import com.bradsbrain.simpleastronomy.MoonPhaseFinder;
import models.Event;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.TreeSet;
import java.util.function.Function;

public class MoonPhasesCalculation implements Calculation {

	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	@Override
	public Collection<Event> calculate(LocalDate from, LocalDate to, ZoneId at) {
		final Collection<Event> eventCollection = new TreeSet<>();
		calculate(from, to, at, eventCollection);
		return eventCollection;
	}

	@Override
	public void calculate(LocalDate from, LocalDate to, ZoneId at, Collection<Event> eventCollection) {
		final ZonedDateTime fromMorning = from.atStartOfDay(at);
		final ZonedDateTime toNight = to.plusDays(1).atStartOfDay(at);
		calculate(fromMorning, toNight, MoonPhaseFinder::findFullMoonFollowing, MoonPhase.FULLMOON.getName(), eventCollection);
		calculate(fromMorning, toNight, MoonPhaseFinder::findNewMoonFollowing, MoonPhase.NEWMOON.getName(), eventCollection);
	}

	private void calculate(ZonedDateTime from, ZonedDateTime to, Function<ZonedDateTime, ZonedDateTime> moonCalculation, String phaseName, Collection<Event> eventCollection) {
		while (true) {
			final ZonedDateTime moonHappening = moonCalculation.apply(from);
			if (moonHappening.isAfter(to)) {
				break;
			}
			eventCollection.add(new Event(moonHappening, phaseName, phaseName + " ist um " + from.format(TIME_FORMATTER)));
			from = moonHappening.plusDays((int) Math.floor(MoonPhase.MOON_CYCLE_DAYS));
		}
	}

}
