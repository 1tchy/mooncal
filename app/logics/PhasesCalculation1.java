package logics;

import models.Event;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;

public class PhasesCalculation1 {

	private static final Duration MOON_CYCLE = Duration.ofDays(29).plusHours(12).plusMinutes(44).plusSeconds(2).plusMillis(803);
	private static final ZonedDateTime A_NEW_MOON = ZonedDateTime.of(2015, 11, 11, 17, 47, 45, 0, ZoneOffset.UTC);
	private boolean includeFullmoons;
	private boolean includeHalfmoons;
	private boolean includeQuartermoons;
	private boolean includeNewmoons;
	@NotNull
	private ZonedDateTime from;
	@NotNull
	private ZonedDateTime to;

	public PhasesCalculation1(boolean includeFullmoons, boolean includeHalfmoons, boolean includeQuartermoons, boolean includeNewmoons, @NotNull ZonedDateTime from, @NotNull ZonedDateTime to) {
		this.includeFullmoons = includeFullmoons;
		this.includeHalfmoons = includeHalfmoons;
		this.includeQuartermoons = includeQuartermoons;
		this.includeNewmoons = includeNewmoons;
		this.from = from;
		this.to = to;
	}

	public void addResultsTo(@NotNull Collection<Event> eventList) {
		ZonedDateTime nextAfter = getNewmoonBefore(from);
		nextAfter = addNextPhase(eventList, nextAfter);
	}

	@NotNull
	protected static ZonedDateTime getNewmoonBefore(@NotNull ZonedDateTime from) {
		ZonedDateTime res = A_NEW_MOON;
		while (res.isBefore(from)) {
			res = res.plus(MOON_CYCLE);
		}
		while (res.isAfter(from)) {
			res = res.minus(MOON_CYCLE);
		}
		return res;
	}

	protected ZonedDateTime addNextPhase(@NotNull Collection<Event> eventList, @NotNull ZonedDateTime nextAfter) {
		return null;
	}

	protected void addIf(@NotNull Collection<Event> eventListToAddTo, @NotNull Event eventToAdd) {
		if (eventToAdd.getDateTime().isAfter(from) && eventToAdd.getDateTime().isBefore(to)) {
			eventListToAddTo.add(eventToAdd);
		}
	}

}
