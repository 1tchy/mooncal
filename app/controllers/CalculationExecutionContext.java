package controllers;

import org.apache.pekko.actor.ActorSystem;
import play.libs.concurrent.CustomExecutionContext;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Runs the calendar calculations on a fixed thread pool with one thread per CPU, but at least two (see "calculation-dispatcher" in application.conf).
 * A large calendar takes a lot of memory while it is calculated, so only a few may run at once and the rest wait in
 * the queue. Without it, a burst of requests on a small server ran all at once and the heap ran out.
 */
@Singleton
public class CalculationExecutionContext extends CustomExecutionContext {

    @Inject
    public CalculationExecutionContext(ActorSystem actorSystem) {
        super(actorSystem, "calculation-dispatcher");
    }
}
