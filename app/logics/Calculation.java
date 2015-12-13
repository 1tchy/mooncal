package logics;

import models.Event;
import models.RequestForm;

import java.time.format.DateTimeFormatter;
import java.util.Collection;

public interface Calculation {

    DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

    /**
     * @param eventCollection The collection which will be updated with the newly calculated events
     */
    void calculate(RequestForm requestForm, Collection<Event> eventCollection);
}
