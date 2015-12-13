package logics;

import models.Event;
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
    public abstract void calculate(RequestForm requestForm, Collection<Event> eventCollection);

    protected static String eventAt(String eventName, ZonedDateTime date, ZoneId usersTimezone) {
        return Messages.get("events.at", eventName, date.withZoneSameInstant(usersTimezone).format(TIME_FORMATTER));
    }

}
