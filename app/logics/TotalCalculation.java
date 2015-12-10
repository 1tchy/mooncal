package logics;

import models.Event;
import models.RequestForm;

import javax.inject.Inject;
import java.util.Collection;
import java.util.TreeSet;

public class TotalCalculation implements Calculation {

    private final MoonPhasesCalculation moonPhasesCalculation;
    private final MoonEventCalculation moonEventCalculation;

    @Inject
    public TotalCalculation(MoonPhasesCalculation moonPhasesCalculation, MoonEventCalculation moonEventCalculation) {
        this.moonPhasesCalculation = moonPhasesCalculation;
        this.moonEventCalculation = moonEventCalculation;
    }

    public Collection<Event> calculate(RequestForm requestForm) {
        final Collection<Event> eventCollection = new TreeSet<>();
        calculate(requestForm, eventCollection);
        return eventCollection;
    }

    @Override
    public void calculate(RequestForm requestForm, Collection<Event> eventCollection) {
        moonPhasesCalculation.calculate(requestForm, eventCollection);
        moonEventCalculation.calculate(requestForm, eventCollection);
    }

}
