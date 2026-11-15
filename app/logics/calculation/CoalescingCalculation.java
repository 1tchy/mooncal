package logics.calculation;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import models.EventInstance;
import models.RequestForm;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

/**
 * Calculates on the calculation pool, and only once for identical requests that arrive while one is still waiting or
 * running: a calendar fetched twice at once (several devices, a retry) is held in memory once and queued once.
 * Requests are identical when their ETags are, which ignores the request's creation time and origin.
 * The result is shared and read-only; nothing is kept once the calculation has completed.
 */
@Singleton
public class CoalescingCalculation {

    private final TotalCalculation calculation;
    private final Executor executor;
    private final ConcurrentMap<String, CompletableFuture<Collection<EventInstance>>> inFlight = new ConcurrentHashMap<>();

    @SuppressWarnings("unused") //used by Guice
    @Inject
    public CoalescingCalculation(TotalCalculation calculation, CalculationExecutionContext executor) {
        this(calculation, (Executor) executor);
    }

    CoalescingCalculation(TotalCalculation calculation, Executor executor) {
        this.calculation = calculation;
        this.executor = executor;
    }

    public CompletionStage<Collection<EventInstance>> calculate(RequestForm requestForm) {
        final String key = requestForm.calculateETag();
        final CompletableFuture<Collection<EventInstance>> fresh = new CompletableFuture<>();
        final CompletableFuture<Collection<EventInstance>> running = inFlight.putIfAbsent(key, fresh);
        if (running != null) {
            return running;
        }
        try {
            CompletableFuture.supplyAsync(() -> calculation.calculate(requestForm), executor)
                    .whenComplete((result, failure) -> {
                        inFlight.remove(key, fresh);
                        if (failure != null) {
                            fresh.completeExceptionally(failure);
                        } else {
                            fresh.complete(result);
                        }
                    });
        } catch (RuntimeException rejected) {
            // the pool is shutting down: the joined requests must not wait forever
            inFlight.remove(key, fresh);
            fresh.completeExceptionally(rejected);
        }
        return fresh;
    }
}
