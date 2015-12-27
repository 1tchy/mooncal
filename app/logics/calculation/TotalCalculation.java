package logics.calculation;

import models.ZonedEvent;
import models.RequestForm;

import javax.inject.Inject;
import java.util.Collection;
import java.util.TreeSet;

public class TotalCalculation extends Calculation {

    private final MoonPhasesCalculation moonPhasesCalculation;
    private final MoonEventCalculation moonEventCalculation;

    @Inject
    public TotalCalculation(MoonPhasesCalculation moonPhasesCalculation, MoonEventCalculation moonEventCalculation) {
        this.moonPhasesCalculation = moonPhasesCalculation;
        this.moonEventCalculation = moonEventCalculation;
    }

    public Collection<ZonedEvent> calculate(RequestForm requestForm) {
        final Collection<ZonedEvent> eventCollection = new TreeSet<>();
        calculate(requestForm, eventCollection);
        return eventCollection;
    }

    @Override
    public void calculate(RequestForm requestForm, Collection<ZonedEvent> eventCollection) {
        moonPhasesCalculation.calculate(requestForm, eventCollection);
        moonEventCalculation.calculate(requestForm, eventCollection);
    }

}
