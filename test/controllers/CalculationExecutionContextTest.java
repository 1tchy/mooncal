package controllers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.test.WithApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("JUnitMixedFramework")
class CalculationExecutionContextTest extends WithApplication {

    @BeforeEach
    void setUpPlay() {
        startPlay();
    }

    @AfterEach
    void tearDownPlay() {
        stopPlay();
    }

    @Test
    void runsAtMostOneCalculationPerCpuButAtLeastTwoAtOnce() throws Exception {
        final CalculationExecutionContext executor = app.injector().instanceOf(CalculationExecutionContext.class);
        final AtomicInteger running = new AtomicInteger();
        final AtomicInteger maxRunning = new AtomicInteger();
        final CountDownLatch release = new CountDownLatch(1);
        final int poolSize = Math.max(2, Runtime.getRuntime().availableProcessors());
        final List<CompletableFuture<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < poolSize + 4; i++) {
            tasks.add(CompletableFuture.runAsync(() -> {
                maxRunning.accumulateAndGet(running.incrementAndGet(), Math::max);
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                running.decrementAndGet();
            }, executor));
        }
        Thread.sleep(500); // give queued tasks a chance to start if the pool were larger
        release.countDown();
        CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).get(10, TimeUnit.SECONDS);
        assertEquals(poolSize, maxRunning.get());
    }
}
