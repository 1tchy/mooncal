package logics;

import com.bradsbrain.simpleastronomy.MoonPhaseFinder;
import models.Event;
import models.RequestForm;
import org.jetbrains.annotations.NotNull;
import play.i18n.Messages;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.function.Function;

public class MoonPhasesCalculation implements Calculation {

    @Override
    public void calculate(RequestForm requestForm, Collection<Event> eventCollection) {
        final ZonedDateTime fromMorning = requestForm.getFrom().withHour(0).withMinute(0).withSecond(0);
        final ZonedDateTime toNight = requestForm.getTo().withHour(23).withMinute(59).withSecond(59);
        if (requestForm.includePhase("full")) {
            calculate(fromMorning, toNight, MoonPhaseFinder::findFullMoonFollowing, MoonPhase.FULLMOON.getName(), eventCollection);
        }
        if (requestForm.includePhase("new")) {
            calculate(fromMorning, toNight, MoonPhaseFinder::findNewMoonFollowing, MoonPhase.NEWMOON.getName(), eventCollection);
        }
        if (requestForm.includePhase("quarter")) {
            calculate(fromMorning, toNight, MoonPhaseFinder::findFirsQuarterFollowing, MoonPhase.FIRST_QUARTER.getName(), eventCollection);
            calculate(fromMorning, toNight, MoonPhaseFinder::findLastQuarterFollowing, MoonPhase.LAST_QUARTER.getName(), eventCollection);
        }
        if (requestForm.includePhase("daily")) {
            calculateDailyEvents(requestForm.getFrom().toLocalDate(), requestForm.getTo().toLocalDate(), requestForm.getFrom().getOffset(), eventCollection);
        }
    }

    private void calculate(ZonedDateTime from, ZonedDateTime to, Function<ZonedDateTime, ZonedDateTime> moonCalculation, String phaseName, Collection<Event> eventCollection) {
        while (true) {
            final ZonedDateTime moonHappening = moonCalculation.apply(from);
            if (moonHappening.isAfter(to)) {
                break;
            }
            eventCollection.add(new Event(moonHappening, phaseName, Messages.get("phases.at", phaseName, formatTime(moonHappening))));
            from = moonHappening.plusDays((int) Math.floor(MoonPhase.MOON_CYCLE_DAYS) - 1);
        }
    }

    @NotNull
    private String formatTime(ZonedDateTime moonHappening) {
        return moonHappening.format(TIME_FORMATTER);
    }

    private void calculateDailyEvents(LocalDate from, LocalDate to, ZoneId at, Collection<Event> eventCollection) {
        while (!from.isAfter(to)) {
            eventCollection.add(calculateDailyEvent(from, at));
            from = from.plusDays(1);
        }
    }

    @NotNull
    private Event calculateDailyEvent(LocalDate day, ZoneId at) {
        final String title = Messages.get("phases.daily.visibility", getMoonVisiblePercent(day.atTime(12, 0).atZone(at), new DecimalFormat("0")));
        final DecimalFormat precise = new DecimalFormat("0.0");
        final String moonVisiblePercentMorning = getMoonVisiblePercent(day.atTime(6, 0).atZone(at), precise);
        final String moonVisiblePercentAtNoon = getMoonVisiblePercent(day.atTime(12, 0).atZone(at), precise);
        final String moonVisiblePercentEvening = getMoonVisiblePercent(day.atTime(18, 0).atZone(at), precise);
        String description = Messages.get("phases.daily.visibility.morning6", moonVisiblePercentMorning) + "\n" +
                Messages.get("phases.daily.visibility.noon12", moonVisiblePercentAtNoon) + "\n" +
                Messages.get("phases.daily.visibility.evening6", moonVisiblePercentEvening);
        return new Event(day.atTime(12, 0).atZone(at), title, description);
    }

    private String getMoonVisiblePercent(ZonedDateTime dateTime, DecimalFormat format) {
        return format.format(MoonPhaseFinder.getMoonVisiblePercent(dateTime) * 100);
    }

}
