package logics;

import com.bradsbrain.simpleastronomy.MoonPhaseFinder;
import models.Event;
import models.RequestForm;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.TreeSet;
import java.util.function.Function;

public class MoonPhasesCalculation implements Calculation {

	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	@Override
	public Collection<Event> calculate(RequestForm requestForm) {
		final Collection<Event> eventCollection = new TreeSet<>();
		calculate(requestForm, eventCollection);
		return eventCollection;
	}

	@Override
	public void calculate(RequestForm requestForm, Collection<Event> eventCollection) {
		final ZonedDateTime fromMorning = requestForm.from.withHour(0).withMinute(0).withSecond(0);
		final ZonedDateTime toNight = requestForm.to.withHour(23).withMinute(59).withSecond(59);
		if (requestForm.includePhase("Vollmond")) {
			calculate(fromMorning, toNight, MoonPhaseFinder::findFullMoonFollowing, MoonPhase.FULLMOON.getName(), eventCollection);
		}
		if (requestForm.includePhase("Neumond")) {
			calculate(fromMorning, toNight, MoonPhaseFinder::findNewMoonFollowing, MoonPhase.NEWMOON.getName(), eventCollection);
		}
		if (requestForm.includePhase("Halbmond")) {
			calculate(fromMorning, toNight, MoonPhaseFinder::findFirsQuarterFollowing, MoonPhase.FIRST_QUARTER.getName(), eventCollection);
			calculate(fromMorning, toNight, MoonPhaseFinder::findLastQuarterFollowing, MoonPhase.LAST_QUARTER.getName(), eventCollection);
		}
	}

	private void calculate(ZonedDateTime from, ZonedDateTime to, Function<ZonedDateTime, ZonedDateTime> moonCalculation, String phaseName, Collection<Event> eventCollection) {
		while (true) {
			final ZonedDateTime moonHappening = moonCalculation.apply(from);
			if (moonHappening.isAfter(to)) {
				break;
			}
			eventCollection.add(new Event(moonHappening, phaseName, phaseName + " ist um " + moonHappening.format(TIME_FORMATTER)));
			from = moonHappening.plusDays((int) Math.floor(MoonPhase.MOON_CYCLE_DAYS)-1);
		}
	}

}
