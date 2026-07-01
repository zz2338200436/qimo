package com._202510007517.platform.gateway.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CircuitBreakerStateTransitionPropertyTest {

    private static final int MINIMUM_CALLS = 10;
    private static final int HALF_OPEN_PROBES = 4;
    private static final int FAILURE_RATE_THRESHOLD = 50;

    @ParameterizedTest(name = "P27 failure-rate series #{index}: {0}")
    @MethodSource("failureRateSeriesArb")
    void circuitBreakerMovesFromClosedToOpenToHalfOpenThenClosedOrOpen(FailureRateSeries series)
            throws InterruptedException {
        CircuitBreaker circuitBreaker = CircuitBreaker.of("p27-" + series.hashCode(), circuitBreakerConfig());
        List<String> transitions = new ArrayList<>();
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> transitions.add(event.getStateTransition().name()));

        for (boolean failure : series.closedWindowFailures()) {
            assertThat(circuitBreaker.tryAcquirePermission()).isTrue();
            recordCall(circuitBreaker, failure);
        }

        assertThat(series.closedWindowFailureRate()).isGreaterThanOrEqualTo(FAILURE_RATE_THRESHOLD);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.tryAcquirePermission()).isFalse();

        Thread.sleep(120);

        assertThat(circuitBreaker.tryAcquirePermission()).isTrue();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        recordCall(circuitBreaker, series.halfOpenProbeFailures().get(0));

        for (int index = 1; index < series.halfOpenProbeFailures().size()
                && circuitBreaker.getState() != CircuitBreaker.State.CLOSED
                && circuitBreaker.getState() != CircuitBreaker.State.OPEN; index++) {
            assertThat(circuitBreaker.tryAcquirePermission()).isTrue();
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
            recordCall(circuitBreaker, series.halfOpenProbeFailures().get(index));
        }

        assertThat(transitions).contains("CLOSED_TO_OPEN", "OPEN_TO_HALF_OPEN");
        if (series.halfOpenFailureRate() >= FAILURE_RATE_THRESHOLD) {
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
            assertThat(transitions).contains("HALF_OPEN_TO_OPEN");
        } else {
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            assertThat(transitions).contains("HALF_OPEN_TO_CLOSED");
        }
    }

    static Stream<FailureRateSeries> failureRateSeriesArb() {
        List<FailureRateSeries> boundaryCases = List.of(
                new FailureRateSeries(
                        List.of(true, true, true, true, true, true, false, false, false, false),
                        List.of(false, false, false, false)),
                new FailureRateSeries(
                        List.of(true, true, true, true, true, true, true, true, true, true),
                        List.of(true, true, false, false)),
                new FailureRateSeries(
                        List.of(false, true, false, true, false, true, false, true, true, true),
                        List.of(false, true, false, false)));
        Random random = new Random(20260520L);
        Stream<FailureRateSeries> generated = IntStream.range(0, 24)
                .mapToObj(index -> randomFailureRateSeries(random));
        return Stream.concat(boundaryCases.stream(), generated);
    }

    private static FailureRateSeries randomFailureRateSeries(Random random) {
        List<Boolean> closedWindow = new ArrayList<>();
        int failures = 0;
        for (int index = 0; index < MINIMUM_CALLS; index++) {
            boolean failure = random.nextBoolean();
            closedWindow.add(failure);
            if (failure) {
                failures++;
            }
        }
        while (failures * 100 / MINIMUM_CALLS <= FAILURE_RATE_THRESHOLD) {
            int index = random.nextInt(MINIMUM_CALLS);
            if (!closedWindow.get(index)) {
                closedWindow.set(index, true);
                failures++;
            }
        }
        List<Boolean> halfOpen = random.nextBoolean()
                ? randomHalfOpenSeries(random, random.nextBoolean() ? 0 : 1)
                : randomHalfOpenSeries(random, 2 + random.nextInt(HALF_OPEN_PROBES - 1));
        return new FailureRateSeries(closedWindow, halfOpen);
    }

    private static List<Boolean> randomHalfOpenSeries(Random random, int failureCount) {
        List<Boolean> halfOpen = new ArrayList<>();
        for (int index = 0; index < HALF_OPEN_PROBES; index++) {
            halfOpen.add(index < failureCount);
        }
        for (int index = halfOpen.size() - 1; index > 0; index--) {
            int swapIndex = random.nextInt(index + 1);
            Boolean current = halfOpen.get(index);
            halfOpen.set(index, halfOpen.get(swapIndex));
            halfOpen.set(swapIndex, current);
        }
        return halfOpen;
    }

    private static CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(FAILURE_RATE_THRESHOLD)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(MINIMUM_CALLS)
                .minimumNumberOfCalls(MINIMUM_CALLS)
                .waitDurationInOpenState(Duration.ofMillis(100))
                .permittedNumberOfCallsInHalfOpenState(HALF_OPEN_PROBES)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .build();
    }

    private static void recordCall(CircuitBreaker circuitBreaker, boolean failure) {
        if (failure) {
            circuitBreaker.onError(1, TimeUnit.MILLISECONDS, new IllegalStateException("generated failure"));
        } else {
            circuitBreaker.onSuccess(1, TimeUnit.MILLISECONDS);
        }
    }

    record FailureRateSeries(List<Boolean> closedWindowFailures, List<Boolean> halfOpenProbeFailures) {

        int closedWindowFailureRate() {
            long failures = closedWindowFailures.stream()
                    .filter(Boolean.TRUE::equals)
                    .count();
            return (int) (failures * 100 / closedWindowFailures.size());
        }

        int halfOpenFailureRate() {
            long failures = halfOpenProbeFailures.stream()
                    .filter(Boolean.TRUE::equals)
                    .count();
            return (int) (failures * 100 / halfOpenProbeFailures.size());
        }

        @Override
        public String toString() {
            return "closedFailureRate=" + closedWindowFailureRate()
                    + ", halfOpenFailureRate=" + halfOpenFailureRate()
                    + ", halfOpenFailures=" + halfOpenProbeFailures;
        }
    }
}
