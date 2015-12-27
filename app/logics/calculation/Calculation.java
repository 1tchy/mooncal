package logics.calculation;

import models.ZonedEvent;
import models.RequestForm;
import play.i18n.Messages;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

public abstract class Calculation {

    private static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm (z)");

    /**
     * @param eventCollection The collection which will be updated with the newly calculated events
     */
    public abstract void calculate(RequestForm requestForm, Collection<ZonedEvent> eventCollection);

    protected static String eventAt(ZonedDateTime date, String eventName, ZoneId usersTimezone) {
        return Messages.get("events.at", eventName, date.withZoneSameInstant(usersTimezone).format(TIME_FORMATTER));
    }

}
