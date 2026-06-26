package logics.calculation;

import jakarta.inject.Inject;
import models.EventInstance;
import models.EventType;
import models.RequestForm;
import org.jetbrains.annotations.Nullable;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;

public class GardenSynodicCalculation extends Calculation {

    private final MoonPhaseData moonPhaseData;

    @Inject
    public GardenSynodicCalculation(MoonPhaseData moonPhaseData, MessagesApi messagesApi) {
        super(messagesApi);
        this.moonPhaseData = moonPhaseData;
    }

    @Override
    public void calculate(RequestForm requestForm, Collection<EventInstance> eventCollection) {
        if (!requestForm.includeEvent(EventType.GARDEN_SYNODIC)) {
            return;
        }
        ZonedDateTime from = requestForm.getFrom().withHour(0).withMinute(0).withSecond(0);
        ZonedDateTime to = requestForm.getTo().withHour(23).withMinute(59).withSecond(59);
        ZoneId zone = requestForm.getFrom().getZone();
        Lang lang = requestForm.getLang();

        ZonedDateTime utcFrom = from.withZoneSameInstant(ZoneOffset.UTC);
        ZonedDateTime utcTo = to.withZoneSameInstant(ZoneOffset.UTC);
        Instant fromInstant = utcFrom.toInstant();
        Instant toInstant = utcTo.toInstant();

        ZonedDateTime searchStart = utcFrom.minus(Duration.ofDays(16));

        for (Map.Entry<ZonedDateTime, MoonPhase> e :
                moonPhaseData.phases().subMap(searchStart, true, utcTo, true).entrySet()) {
            ZonedDateTime when = e.getKey();
            MoonPhase phase = e.getValue();
            Period period = period(phase);
            if (period == null) {
                continue; // simple mode skips quarter phases
            }
            ZonedDateTime windowEnd = computeWindowEnd(when, phase);
            // Skip windows that end at or before the requested start
            if (windowEnd == null || !windowEnd.toInstant().isAfter(fromInstant)) {
                continue;
            }
            // Clamp display start to from, display end to to
            ZonedDateTime displayStart = when.toInstant().isBefore(fromInstant)
                    ? from.withZoneSameInstant(zone) : when.withZoneSameInstant(zone);
            ZonedDateTime displayEnd = windowEnd.toInstant().isAfter(toInstant)
                    ? to.withZoneSameInstant(zone) : windowEnd.withZoneSameInstant(zone);
            eventCollection.add(buildEvent(displayStart, displayEnd, period, zone, lang));
        }
    }

    private EventInstance buildEvent(ZonedDateTime when, ZonedDateTime windowEnd, Period period, ZoneId zone, Lang lang) {
        String label = messagesApi.get(lang, period.titleKey);
        String title = period.emoji + " " + label;
        String guidance = messagesApi.get(lang, period.guidanceKey);
        String plantText = label.contains("—") ? label.substring(label.indexOf("—") + 1).trim() : label;
        return new EventInstance(when, windowEnd, title, plantText, guidance, zone, period.eventTypeId);
    }

    @Nullable
    private static Period period(MoonPhase phase) {
        return switch (phase) {
            case NEWMOON -> Period.WAXING;
            case FULLMOON -> Period.WANING;
            default -> null;
        };
    }

    @Nullable
    private ZonedDateTime computeWindowEnd(ZonedDateTime when, MoonPhase phase) {
        // Find the next occurrence of the closing phase
        MoonPhase targetPhase = (phase == MoonPhase.NEWMOON) ? MoonPhase.FULLMOON : MoonPhase.NEWMOON;
        NavigableMap<ZonedDateTime, MoonPhase> tail = moonPhaseData.phases().tailMap(when, false);
        for (Map.Entry<ZonedDateTime, MoonPhase> entry : tail.entrySet()) {
            if (entry.getValue() == targetPhase) {
                return entry.getKey();
            }
        }
        return null;
    }

    private enum Period {
        WAXING("🌱", "garden.synodic.waxing.title", "garden.synodic.waxing.guidance", "garden-synodic-waxing"),
        WANING("🍂", "garden.synodic.waning.title", "garden.synodic.waning.guidance", "garden-synodic-waning");

        final String emoji, titleKey, guidanceKey, eventTypeId;

        Period(String emoji, String titleKey, String guidanceKey, String eventTypeId) {
            this.emoji = emoji;
            this.titleKey = titleKey;
            this.guidanceKey = guidanceKey;
            this.eventTypeId = eventTypeId;
        }
    }
}
