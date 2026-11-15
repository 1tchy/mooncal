package logics.calculation;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.pekko.actor.ActorSystem;
import org.jetbrains.annotations.NotNull;
import play.Logger;
import play.libs.concurrent.CustomExecutionContext;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs the calendar calculations on a fixed thread pool with one thread per CPU, but at least two (see "calculation-dispatcher" in application.conf).
 * A large calendar takes a lot of memory while it is calculated, so only a few may run at once and the rest wait in
 * the queue. Without it, a burst of requests on a small server ran all at once and the heap ran out.
 * <p>
 * The queue is unbounded and Pekko does not show its length, so the calculations are counted here and the numbers
 * logged every few minutes while there are any: a growing number waiting is the early sign of an overloaded server.
 */
@Singleton
public class CalculationExecutionContext extends CustomExecutionContext {

    private static final Duration LOG_INTERVAL = Duration.ofMinutes(5);

    private final Logger.ALogger logger = Logger.of(getClass());
    private final AtomicInteger waiting = new AtomicInteger();
    private final AtomicInteger running = new AtomicInteger();

    @Inject
    public CalculationExecutionContext(ActorSystem actorSystem) {
        super(actorSystem, "calculation-dispatcher");
        actorSystem.scheduler().scheduleAtFixedRate(LOG_INTERVAL, LOG_INTERVAL, this::logLoad, actorSystem.dispatcher());
    }

    @Override
    public void execute(@NotNull Runnable calculation) {
        waiting.incrementAndGet();
        try {
            super.execute(() -> {
                waiting.decrementAndGet();
                running.incrementAndGet();
                try {
                    calculation.run();
                } finally {
                    running.decrementAndGet();
                }
            });
        } catch (RuntimeException rejected) {
            waiting.decrementAndGet();
            throw rejected;
        }
    }

    /** @return the calculations running at the moment */
    public int running() {
        return running.get();
    }

    /** @return the calculations waiting for a free thread at the moment */
    public int waiting() {
        return waiting.get();
    }

    private void logLoad() {
        final int running = running();
        final int waiting = waiting();
        if (running > 0 || waiting > 0) {
            logger.info("Calculations: " + running + " running, " + waiting + " waiting");
        }
    }
}
