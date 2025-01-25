package logics.calculation;

import com.bradsbrain.simpleastronomy.MoonPhaseFinder;
import models.EventInstance;
import models.EventStyle;
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

    public static final String FULLMOON_EVENT_TYPE_ID = "fullmoon";
    public static final String NEWMOON_EVENT_TYPE_ID = "newmoon";
    public static final String FIRST_QUARTER_EVENT_TYPE_ID = "first-quarter";
    public static final String LAST_QUARTER_EVENT_TYPE_ID = "last-quarter";

    @Inject
    public MoonPhasesCalculation(MessagesApi messagesApi) {
        super(messagesApi);
    }

    @Override
    public void calculate(RequestForm requestForm, Collection<EventInstance> eventCollection) {
        final ZonedDateTime fromMorning = requestForm.getFrom().withHour(0).withMinute(0).withSecond(0);
        final ZonedDateTime toNight = requestForm.getTo().withHour(23).withMinute(59).withSecond(59);
        if (requestForm.includePhase(MoonPhaseType.FULLMOON)) {
            calculate(fromMorning, toNight, MoonPhaseFinder::findFullMoonFollowing, MoonPhase.FULLMOON, eventCollection, requestForm.getLang(), requestForm.getStyle(), FULLMOON_EVENT_TYPE_ID);
        }
        if (requestForm.includePhase(MoonPhaseType.NEWMOON)) {
            calculate(fromMorning, toNight, MoonPhaseFinder::findNewMoonFollowing, MoonPhase.NEWMOON, eventCollection, requestForm.getLang(), requestForm.getStyle(), NEWMOON_EVENT_TYPE_ID);
        }
        if (requestForm.includePhase(MoonPhaseType.QUARTER)) {
            calculate(fromMorning, toNight, MoonPhaseFinder::findFirsQuarterFollowing, MoonPhase.FIRST_QUARTER, eventCollection, requestForm.getLang(), requestForm.getStyle(), FIRST_QUARTER_EVENT_TYPE_ID);
            calculate(fromMorning, toNight, MoonPhaseFinder::findLastQuarterFollowing, MoonPhase.LAST_QUARTER, eventCollection, requestForm.getLang(), requestForm.getStyle(), LAST_QUARTER_EVENT_TYPE_ID);
        }
        if (requestForm.includePhase(MoonPhaseType.DAILY)) {
            calculateDailyEvents(requestForm.getLang(), requestForm.getFrom().toLocalDate(), requestForm.getTo().toLocalDate(), requestForm.getFrom().getOffset(), eventCollection);
        }
    }

    private void calculate(ZonedDateTime from, ZonedDateTime to, Function<ZonedDateTime, ZonedDateTime> moonCalculation, MoonPhase phase, Collection<EventInstance> eventCollection, Lang lang, EventStyle style, String eventTypeId) {
        while (true) {
            final ZonedDateTime moonHappening = moonCalculation.apply(from);
            if (moonHappening.isAfter(to)) {
                break;
            }
            eventCollection.add(new EventInstance(
                    moonHappening,
                    phase.getTitle(messagesApi, lang, moonHappening, style),
                    phase.getPdfTitle(messagesApi, lang, moonHappening, style),
                    eventAt(moonHappening, phase.getSimpleName(messagesApi, lang), from.getZone(), lang),
                    from.getZone(),
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
        final String moonVisiblePercentMidnight = getMoonVisiblePercent(day.plusDays(1).atTime(0, 0).atZone(at), precise);
        String description = messagesApi.get(lang, "phases.daily.visibility.morning6", moonVisiblePercentMorning) + "\n" +
                messagesApi.get(lang, "phases.daily.visibility.noon12", moonVisiblePercentAtNoon) + "\n" +
                messagesApi.get(lang, "phases.daily.visibility.evening6", moonVisiblePercentEvening) + "\n" +
                messagesApi.get(lang, "phases.daily.visibility.midnight", moonVisiblePercentMidnight);
        return new EventInstance(day.atTime(12, 0).atZone(at), title, title, description, at, "daily");
    }

    private String getMoonVisiblePercent(ZonedDateTime dateTime, DecimalFormat format) {
        return format.format(MoonPhaseFinder.getMoonVisiblePercent(dateTime) * 100);
    }

    public CurrentMoonPhase getCurrentMoonPhase() {
        ZonedDateTime now = ZonedDateTime.now();
        int moonAngle = (int) Math.round(MoonPhaseFinder.getMoonAngle(now));
        int moonVisiblePercent = (int) Math.round(MoonPhaseFinder.getMoonVisiblePercent(now) * 100);
        return new CurrentMoonPhase(moonVisiblePercent, moonAngle < 180);
    }

    public record CurrentMoonPhase(int visibility, boolean isWaxing) {
        /**
         * @return A value from 0% (new moon) to 100% (full moon) to 200% (new moon again)
         */
        public int getPhaseVisibilityPercentage() {
            return isWaxing ? visibility : 200 - visibility;
        }
    }
}
