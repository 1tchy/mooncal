package modules;

import com.google.inject.AbstractModule;
import logics.astronomy.Ephemeris;
import logics.astronomy.MeeusEphemeris;

public class AstronomyModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Ephemeris.class).to(MeeusEphemeris.class);
    }
}
