package logics.calculation;

import models.EventInstance;
import models.RequestForm;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

public abstract class Calculation {

    protected final MessagesApi messagesApi;

    public Calculation(MessagesApi messagesApi) {
        this.messagesApi = messagesApi;
    }

    private static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm (z)");

    /**
     * @param eventCollection The collection which will be updated with the newly calculated events
     */
    public abstract void calculate(RequestForm requestForm, Collection<EventInstance> eventCollection, Lang lang);

    protected String eventAt(ZonedDateTime date, String eventName, ZoneId usersTimezone, Lang lang) {
        return messagesApi.get(lang, "events.at", eventName, date.withZoneSameInstant(usersTimezone).format(TIME_FORMATTER));
    }

}
