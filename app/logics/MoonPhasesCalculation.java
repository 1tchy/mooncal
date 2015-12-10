package logics;

import com.bradsbrain.simpleastronomy.MoonPhaseFinder;
import models.Event;
import models.RequestForm;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.function.Function;

public class MoonPhasesCalculation implements Calculation {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void calculate(RequestForm requestForm, Collection<Event> eventCollection) {
        final ZonedDateTime fromMorning = requestForm.getFrom().withHour(0).withMinute(0).withSecond(0);
        final ZonedDateTime toNight = requestForm.getTo().withHour(23).withMinute(59).withSecond(59);
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
        if (requestForm.includePhase("tägliche Phasen")) {
            calculateDailyEvents(requestForm.getFrom().toLocalDate(), requestForm.getTo().toLocalDate(), requestForm.getFrom().getOffset(), eventCollection);
        }
    }

    private void calculate(ZonedDateTime from, ZonedDateTime to, Function<ZonedDateTime, ZonedDateTime> moonCalculation, String phaseName, Collection<Event> eventCollection) {
        while (true) {
            final ZonedDateTime moonHappening = moonCalculation.apply(from);
            if (moonHappening.isAfter(to)) {
                break;
            }
            eventCollection.add(new Event(moonHappening, phaseName, phaseName + " ist um " + moonHappening.format(TIME_FORMATTER)));
            from = moonHappening.plusDays((int) Math.floor(MoonPhase.MOON_CYCLE_DAYS) - 1);
        }
    }

    private void calculateDailyEvents(LocalDate from, LocalDate to, ZoneId at, Collection<Event> eventCollection) {
        while (!from.isAfter(to)) {
            eventCollection.add(calculateDailyEvent(from, at));
            from = from.plusDays(1);
        }
    }

    @NotNull
    private Event calculateDailyEvent(LocalDate day, ZoneId at) {
        final String title = "Mond " + getMoonVisiblePercent(day.atTime(12, 0).atZone(at), new DecimalFormat("0")) + "% sichtbar";
        final DecimalFormat precise = new DecimalFormat("0.0");
        final String moonVisiblePercentMorning = getMoonVisiblePercent(day.atTime(6, 0).atZone(at), precise);
        final String moonVisiblePercentAtNoon = getMoonVisiblePercent(day.atTime(12, 0).atZone(at), precise);
        final String moonVisiblePercentEvening = getMoonVisiblePercent(day.atTime(18, 0).atZone(at), precise);
        String description = "Morgens um 6:00 ist der Mond " + moonVisiblePercentMorning + "% sichtbar\n" +
                "Mittags um 12:00 ist der Mond " + moonVisiblePercentAtNoon + "% sichtbar\n" +
                "Abends um 18:00 ist der Mond " + moonVisiblePercentEvening + "% sichtbar";
        return new Event(day.atTime(12, 0).atZone(at), title, description);
    }

    private String getMoonVisiblePercent(ZonedDateTime dateTime, DecimalFormat format) {
        return format.format(MoonPhaseFinder.getMoonVisiblePercent(dateTime) * 100);
    }

}
