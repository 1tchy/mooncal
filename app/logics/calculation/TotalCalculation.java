package logics.calculation;

import models.EventInstance;
import models.RequestForm;
import play.i18n.MessagesApi;

import javax.inject.Inject;
import java.util.Collection;
import java.util.TreeSet;

public class TotalCalculation extends Calculation {

    private final MoonPhasesCalculation moonPhasesCalculation;
    private final MoonEventCalculation moonEventCalculation;

    @Inject
    public TotalCalculation(MoonPhasesCalculation moonPhasesCalculation, MoonEventCalculation moonEventCalculation, MessagesApi messagesApi) {
        super(messagesApi);
        this.moonPhasesCalculation = moonPhasesCalculation;
        this.moonEventCalculation = moonEventCalculation;
    }

    public Collection<EventInstance> calculate(RequestForm requestForm) {
        final Collection<EventInstance> eventCollection = new TreeSet<>();
        calculate(requestForm, eventCollection);
        return eventCollection;
    }

    @Override
    public void calculate(RequestForm requestForm, Collection<EventInstance> eventCollection) {
        moonPhasesCalculation.calculate(requestForm, eventCollection);
        moonEventCalculation.calculate(requestForm, eventCollection);
    }

}
