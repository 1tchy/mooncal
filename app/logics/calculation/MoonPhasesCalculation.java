package logics.calculation;

import com.bradsbrain.simpleastronomy.MoonPhaseFinder;
import models.EventInstance;
import models.MoonPhaseType;
import models.RequestForm;
import org.jetbrains.annotations.NotNull;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import javax.inject.Inject;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.function.Function;

public class MoonPhasesCalculation extends Calculation {

    @Inject
    public MoonPhasesCalculation(MessagesApi messagesApi) {
        super(messagesApi);
    }

    @Override
    public void calculate(RequestForm requestForm, Collection<EventInstance> eventCollection, Lang lang) {
        final ZonedDateTime fromMorning = requestForm.getFrom().withHour(0).withMinute(0).withSecond(0);
        final ZonedDateTime toNight = requestForm.getTo().withHour(23).withMinute(59).withSecond(59);
        if (requestForm.includePhase(MoonPhaseType.FULLMOON)) {
            calculate(fromMorning, toNight, MoonPhaseFinder::findFullMoonFollowing, MoonPhase.FULLMOON, eventCollection, lang, "fullmoon");
        }
        if (requestForm.includePhase(MoonPhaseType.NEWMOON)) {
            calculate(fromMorning, toNight, MoonPhaseFinder::findNewMoonFollowing, MoonPhase.NEWMOON, eventCollection, lang, "newmoon");
        }
        if (requestForm.includePhase(MoonPhaseType.QUARTER)) {
            calculate(fromMorning, toNight, MoonPhaseFinder::findFirsQuarterFollowing, MoonPhase.FIRST_QUARTER, eventCollection, lang, "quarter");
            calculate(fromMorning, toNight, MoonPhaseFinder::findLastQuarterFollowing, MoonPhase.LAST_QUARTER, eventCollection, lang, "quarter");
        }
        if (requestForm.includePhase(MoonPhaseType.DAILY)) {
            calculateDailyEvents(lang, requestForm.getFrom().toLocalDate(), requestForm.getTo().toLocalDate(), requestForm.getFrom().getOffset(), eventCollection);
        }
    }

    private void calculate(ZonedDateTime from, ZonedDateTime to, Function<ZonedDateTime, ZonedDateTime> moonCalculation, MoonPhase phase, Collection<EventInstance> eventCollection, Lang lang, String eventTypeId) {
        while (true) {
            final ZonedDateTime moonHappening = moonCalculation.apply(from);
            if (moonHappening.isAfter(to)) {
                break;
            }
            eventCollection.add(new EventInstance(
                    moonHappening,
                    phase.getEmoticon() + " " + phase.getTitle(messagesApi, lang, moonHappening),
                    eventAt(moonHappening, phase.getSimpleName(messagesApi, lang), from.getZone(), lang),
                    from.getZone(),
                    lang,
                    eventTypeId));
            from = moonHappening.plusDays((int) Math.floor(MoonPhase.MOON_CYCLE_DAYS) - 1);
        }
    }

    private void calculateDailyEvents(Lang lang, LocalDate from, LocalDate to, ZoneId at, Collection<EventInstance> eventCollection) {
        while (!from.isAfter(to)) {
            eventCollection.add(calculateDailyEvent(lang, from, at));
            from = from.plusDays(1);
        }
    }

    @NotNull
    private EventInstance calculateDailyEvent(Lang lang, LocalDate day, ZoneId at) {
        final String title = messagesApi.get(lang, "phases.daily.visibility", getMoonVisiblePercent(day.atTime(12, 0).atZone(at), new DecimalFormat("0")));
        final DecimalFormat precise = new DecimalFormat("0.0");
        final String moonVisiblePercentMorning = getMoonVisiblePercent(day.atTime(6, 0).atZone(at), precise);
        final String moonVisiblePercentAtNoon = getMoonVisiblePercent(day.atTime(12, 0).atZone(at), precise);
        final String moonVisiblePercentEvening = getMoonVisiblePercent(day.atTime(18, 0).atZone(at), precise);
        String description = messagesApi.get(lang, "phases.daily.visibility.morning6", moonVisiblePercentMorning) + "\n" +
                messagesApi.get(lang, "phases.daily.visibility.noon12", moonVisiblePercentAtNoon) + "\n" +
                messagesApi.get(lang, "phases.daily.visibility.evening6", moonVisiblePercentEvening);
        return new EventInstance(day.atTime(12, 0).atZone(at), title, description, at, lang, "daily");
    }

    private String getMoonVisiblePercent(ZonedDateTime dateTime, DecimalFormat format) {
        return format.format(MoonPhaseFinder.getMoonVisiblePercent(dateTime) * 100);
    }

}
