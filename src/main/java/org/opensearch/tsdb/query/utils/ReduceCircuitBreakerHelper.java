/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.tsdb.query.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.lucene.util.RamUsageEstimator;
import org.opensearch.common.util.BigArrays;
import org.opensearch.core.common.breaker.CircuitBreaker;
import org.opensearch.core.common.breaker.CircuitBreakingException;
import org.opensearch.core.indices.breaker.CircuitBreakerService;
import org.opensearch.search.aggregations.InternalAggregation.ReduceContext;
import org.opensearch.tsdb.metrics.TSDBMetrics;

import java.util.function.LongConsumer;

/**
 * Helper class for circuit breaker tracking during the reduce phase of aggregations.
 *
 * <p>This class provides a bridge between the reduce phase (which has access to {@link ReduceContext})
 * and the circuit breaker. It tracks memory allocations during reduce operations on coordinator
 * nodes, including data cluster coordinators in Cross-Cluster Search (CCS) setups.</p>
 *
 * <h2>Usage Pattern:</h2>
 * <pre>{@code
 * // In reduce() method with ReduceContext
 * LongConsumer cbConsumer = ReduceCircuitBreakerHelper.createConsumer(reduceContext);
 *
 * // Track allocations
 * cbConsumer.accept(estimatedBytes);
 *
 * // Or use the static helper
 * ReduceCircuitBreakerHelper.trackBytes(reduceContext, estimatedBytes);
 * }</pre>
 *
 * <h2>Thread Safety:</h2>
 * <p>The underlying BigArrays circuit breaker is thread-safe. However, the total bytes tracked
 * by a single consumer instance is not synchronized across threads.</p>
 */
public final class ReduceCircuitBreakerHelper {

    private static final Logger logger = LogManager.getLogger(ReduceCircuitBreakerHelper.class);

    /**
     * Label for circuit breaker tracking in reduce phase.
     */
    private static final String REDUCE_LABEL = "<reduce_time_series>";

    private ReduceCircuitBreakerHelper() {
        // Utility class
    }

    /**
     * Creates a {@link LongConsumer} that tracks memory allocations against the circuit breaker.
     *
     * <p>The returned consumer can be passed to reduce methods that need to track memory.
     * It handles both allocations (positive bytes) and releases (negative bytes).</p>
     *
     * @param reduceContext the reduce context containing BigArrays with circuit breaker access
     * @return a LongConsumer that tracks bytes against the circuit breaker, or a no-op consumer if context is null
     */
    public static LongConsumer createConsumer(ReduceContext reduceContext) {
        if (reduceContext == null || reduceContext.bigArrays() == null) {
            return bytes -> {}; // No-op consumer
        }

        BigArrays bigArrays = reduceContext.bigArrays();
        CircuitBreakerService breakerService = bigArrays.breakerService();
        if (breakerService == null) {
            return bytes -> {}; // No-op consumer
        }

        CircuitBreaker breaker = breakerService.getBreaker(CircuitBreaker.REQUEST);
        if (breaker == null) {
            return bytes -> {}; // No-op consumer
        }

        return new ReduceCircuitBreakerConsumer(breaker);
    }

    /**
     * Directly tracks bytes against the circuit breaker from a ReduceContext.
     *
     * <p>Use this for one-off tracking. For multiple tracking calls, prefer
     * {@link #createConsumer(ReduceContext)} to avoid repeated null checks.</p>
     *
     * @param reduceContext the reduce context containing BigArrays
     * @param bytes the number of bytes to track (positive for allocation, negative for release)
     */
    public static void trackBytes(ReduceContext reduceContext, long bytes) {
        if (reduceContext == null || reduceContext.bigArrays() == null || bytes == 0) {
            return;
        }
        CircuitBreakerService breakerService = reduceContext.bigArrays().breakerService();
        if (breakerService == null) {
            return;
        }
        CircuitBreaker breaker = breakerService.getBreaker(CircuitBreaker.REQUEST);
        if (breaker == null) {
            return;
        }
        adjustBreaker(breaker, bytes);
    }

    /**
     * Internal consumer implementation that tracks bytes.
     */
    private static class ReduceCircuitBreakerConsumer implements LongConsumer {
        private final CircuitBreaker breaker;
        private long totalTracked = 0;

        ReduceCircuitBreakerConsumer(CircuitBreaker breaker) {
            this.breaker = breaker;
        }

        @Override
        public void accept(long bytes) {
            if (bytes == 0) {
                return;
            }
            adjustBreaker(breaker, bytes);
            totalTracked += bytes;
        }
    }

    /**
     * Adjusts the circuit breaker by the specified number of bytes.
     *
     * @param breaker the CircuitBreaker to adjust
     * @param bytes the number of bytes to adjust (positive for allocation, negative for release)
     */
    private static void adjustBreaker(CircuitBreaker breaker, long bytes) {
        try {
            if (bytes > 0) {
                // Allocation - may throw CircuitBreakingException
                breaker.addEstimateBytesAndMaybeBreak(bytes, REDUCE_LABEL);
            } else {
                // Release - never throws
                breaker.addWithoutBreaking(bytes);
            }

            if (logger.isTraceEnabled()) {
                logger.trace(
                    () -> new ParameterizedMessage(
                        "Reduce phase circuit breaker: {} bytes, label={}",
                        bytes > 0 ? "+" + bytes : bytes,
                        REDUCE_LABEL
                    )
                );
            }
        } catch (CircuitBreakingException e) {
            // Log and increment metrics before rethrowing
            logger.warn(
                () -> new ParameterizedMessage(
                    "[request] Reduce phase circuit breaker tripped: attempted {} bytes, label={}",
                    RamUsageEstimator.humanReadableUnits(bytes),
                    REDUCE_LABEL
                )
            );

            // Increment circuit breaker trips counter
            TSDBMetrics.incrementCounter(TSDBMetrics.AGGREGATION.circuitBreakerTrips, 1);

            throw e;
        }
    }
}
