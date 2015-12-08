package logics;

import com.bradsbrain.simpleastronomy.MoonPhaseFinder;
import models.Event;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

public class PhasesCalculation3 implements Calculation {

	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	@Override
	public Collection<Event> calculate(LocalDate from, LocalDate to, ZoneId at) {
		final ZonedDateTime fromMorning = from.atStartOfDay(at);
		final ZonedDateTime toNight = to.plusDays(1).atStartOfDay(at);
		final Collection<Event> res = calculate(fromMorning, toNight, MoonPhaseFinder::findFullMoonFollowing, MoonPhase.FULLMOON.getName());
		res.addAll(calculate(fromMorning, toNight, MoonPhaseFinder::findNewMoonFollowing, MoonPhase.NEWMOON.getName()));
		return res;
	}

	@NotNull
	private Collection<Event> calculate(ZonedDateTime from, ZonedDateTime to, Function<ZonedDateTime, ZonedDateTime> moonCalculation, String phaseName) {
		Collection<Event> ret = new ArrayList<>();
		while (true) {
			final ZonedDateTime moonHappening = moonCalculation.apply(from);
			if (moonHappening.isAfter(to)) {
				break;
			}
			ret.add(new Event(moonHappening, phaseName, phaseName + " ist um " + from.format(TIME_FORMATTER)));
			from = moonHappening.plusDays((int) Math.floor(MoonPhase.MOON_CYCLE_DAYS));
		}
		return ret;
	}

}
