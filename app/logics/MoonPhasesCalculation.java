package logics;

import com.bradsbrain.simpleastronomy.MoonPhaseFinder;
import models.MoonPhaseType;
import models.RequestForm;
import models.ZonedEvent;
import org.jetbrains.annotations.NotNull;
import play.i18n.Messages;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.function.Function;

public class MoonPhasesCalculation extends Calculation {

    @Override
    public void calculate(RequestForm requestForm, Collection<ZonedEvent> eventCollection) {
        final ZonedDateTime fromMorning = requestForm.getFrom().withHour(0).withMinute(0).withSecond(0);
        final ZonedDateTime toNight = requestForm.getTo().withHour(23).withMinute(59).withSecond(59);
        if (requestForm.includePhase(MoonPhaseType.FULLMOON)) {
            calculate(fromMorning, toNight, MoonPhaseFinder::findFullMoonFollowing, MoonPhase.FULLMOON.getName(), eventCollection);
        }
        if (requestForm.includePhase(MoonPhaseType.NEWMOON)) {
            calculate(fromMorning, toNight, MoonPhaseFinder::findNewMoonFollowing, MoonPhase.NEWMOON.getName(), eventCollection);
        }
        if (requestForm.includePhase(MoonPhaseType.QUARTER)) {
            calculate(fromMorning, toNight, MoonPhaseFinder::findFirsQuarterFollowing, MoonPhase.FIRST_QUARTER.getName(), eventCollection);
            calculate(fromMorning, toNight, MoonPhaseFinder::findLastQuarterFollowing, MoonPhase.LAST_QUARTER.getName(), eventCollection);
        }
        if (requestForm.includePhase(MoonPhaseType.DAILY)) {
            calculateDailyEvents(requestForm.getFrom().toLocalDate(), requestForm.getTo().toLocalDate(), requestForm.getFrom().getOffset(), eventCollection);
        }
    }

    private void calculate(ZonedDateTime from, ZonedDateTime to, Function<ZonedDateTime, ZonedDateTime> moonCalculation, String phaseName, Collection<ZonedEvent> eventCollection) {
        while (true) {
            final ZonedDateTime moonHappening = moonCalculation.apply(from);
            if (moonHappening.isAfter(to)) {
                break;
            }
            eventCollection.add(new ZonedEvent(moonHappening, phaseName, eventAt(moonHappening, phaseName, from.getZone()), from.getZone()));
            from = moonHappening.plusDays((int) Math.floor(MoonPhase.MOON_CYCLE_DAYS) - 1);
        }
    }

    private void calculateDailyEvents(LocalDate from, LocalDate to, ZoneId at, Collection<ZonedEvent> eventCollection) {
        while (!from.isAfter(to)) {
            eventCollection.add(calculateDailyEvent(from, at));
            from = from.plusDays(1);
        }
    }

    @NotNull
    private ZonedEvent calculateDailyEvent(LocalDate day, ZoneId at) {
        final String title = Messages.get("phases.daily.visibility", getMoonVisiblePercent(day.atTime(12, 0).atZone(at), new DecimalFormat("0")));
        final DecimalFormat precise = new DecimalFormat("0.0");
        final String moonVisiblePercentMorning = getMoonVisiblePercent(day.atTime(6, 0).atZone(at), precise);
        final String moonVisiblePercentAtNoon = getMoonVisiblePercent(day.atTime(12, 0).atZone(at), precise);
        final String moonVisiblePercentEvening = getMoonVisiblePercent(day.atTime(18, 0).atZone(at), precise);
        String description = Messages.get("phases.daily.visibility.morning6", moonVisiblePercentMorning) + "\n" +
                Messages.get("phases.daily.visibility.noon12", moonVisiblePercentAtNoon) + "\n" +
                Messages.get("phases.daily.visibility.evening6", moonVisiblePercentEvening);
        return new ZonedEvent(day.atTime(12, 0).atZone(at), title, description, at);
    }

    private String getMoonVisiblePercent(ZonedDateTime dateTime, DecimalFormat format) {
        return format.format(MoonPhaseFinder.getMoonVisiblePercent(dateTime) * 100);
    }

}
