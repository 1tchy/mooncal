package logics.calculation;

import models.EventInstance;
import models.MoonPhaseType;
import models.RequestForm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import play.i18n.Lang;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CoalescingCalculationTest {

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final TotalCalculation calculation = mock(TotalCalculation.class);
    private final CoalescingCalculation cut = new CoalescingCalculation(calculation, executor);

    @AfterEach
    void shutDown() {
        executor.shutdownNow();
    }

    @Test
    void identicalRequestsShareOneCalculation() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger calculations = new AtomicInteger();
        when(calculation.calculate(any())).thenAnswer(invocation -> {
            calculations.incrementAndGet();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return List.of();
        });

        final CompletionStage<Collection<EventInstance>> first = cut.calculate(form("Europe/Zurich"));
        final CompletionStage<Collection<EventInstance>> second = cut.calculate(form("Europe/Zurich"));
        release.countDown();

        assertSame(first.toCompletableFuture().get(5, TimeUnit.SECONDS), second.toCompletableFuture().get(5, TimeUnit.SECONDS));
        assertEquals(1, calculations.get());
    }

    @Test
    void aRequestIdenticalToOneStillWaitingInTheQueueJoinsIt() throws Exception {
        final ExecutorService singleThread = Executors.newSingleThreadExecutor();
        try {
            final CoalescingCalculation cut = new CoalescingCalculation(calculation, singleThread);
            final CountDownLatch release = new CountDownLatch(1);
            when(calculation.calculate(any())).thenAnswer(invocation -> {
                assertTrue(release.await(5, TimeUnit.SECONDS));
                return List.of();
            });

            final CompletionStage<Collection<EventInstance>> blocking = cut.calculate(form("Europe/Zurich"));
            final CompletionStage<Collection<EventInstance>> queued = cut.calculate(form("Europe/London"));
            final CompletionStage<Collection<EventInstance>> joined = cut.calculate(form("Europe/London"));
            release.countDown();

            blocking.toCompletableFuture().get(5, TimeUnit.SECONDS);
            assertSame(queued.toCompletableFuture().get(5, TimeUnit.SECONDS), joined.toCompletableFuture().get(5, TimeUnit.SECONDS));
            verify(calculation, times(2)).calculate(any());
        } finally {
            singleThread.shutdownNow();
        }
    }

    @Test
    void differentRequestsAreCalculatedSeparately() throws Exception {
        when(calculation.calculate(any())).thenReturn(List.of());

        cut.calculate(form("Europe/Zurich")).toCompletableFuture().get(5, TimeUnit.SECONDS);
        cut.calculate(form("Europe/London")).toCompletableFuture().get(5, TimeUnit.SECONDS);

        verify(calculation, times(2)).calculate(any());
    }

    @Test
    void aRequestAfterCompletionIsCalculatedAgain() throws Exception {
        when(calculation.calculate(any())).thenReturn(List.of());

        cut.calculate(form("Europe/Zurich")).toCompletableFuture().get(5, TimeUnit.SECONDS);
        cut.calculate(form("Europe/Zurich")).toCompletableFuture().get(5, TimeUnit.SECONDS);

        verify(calculation, times(2)).calculate(any());
    }

    @Test
    void aFailureReachesEveryJoinedRequestAndIsNotKept() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        when(calculation.calculate(any())).thenAnswer(invocation -> {
            assertTrue(release.await(5, TimeUnit.SECONDS));
            throw new IllegalStateException("boom");
        }).thenReturn(List.of());

        final CompletionStage<Collection<EventInstance>> first = cut.calculate(form("Europe/Zurich"));
        final CompletionStage<Collection<EventInstance>> second = cut.calculate(form("Europe/Zurich"));
        release.countDown();

        for (CompletionStage<Collection<EventInstance>> joined : List.of(first, second)) {
            final ExecutionException failure = assertThrows(ExecutionException.class, () -> joined.toCompletableFuture().get(5, TimeUnit.SECONDS));
            assertInstanceOf(IllegalStateException.class, failure.getCause());
        }
        assertEquals(List.of(), cut.calculate(form("Europe/Zurich")).toCompletableFuture().get(5, TimeUnit.SECONDS));
    }

    private static RequestForm form(String zone) {
        final RequestForm form = new RequestForm();
        form.setPhases(Map.of(MoonPhaseType.FULLMOON, true));
        form.setStyle("withDescription");
        form.setFrom(LocalDate.of(2026, 1, 1).atStartOfDay(ZoneOffset.UTC));
        form.setTo(LocalDate.of(2026, 12, 31).atStartOfDay(ZoneOffset.UTC));
        form.setZone(java.time.ZoneId.of(zone));
        form.setLang(Lang.forCode("en"));
        form.setCreated(System.nanoTime()); // differs per request, as in the browser, but not part of the key
        return form;
    }
}
