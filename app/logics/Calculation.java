package logics;

import models.Event;
import models.RequestForm;

import java.util.Collection;

public interface Calculation {
    /**
     * @param eventCollection The collection which will be updated with the newly calculated events
     */
    void calculate(RequestForm requestForm, Collection<Event> eventCollection);
}
