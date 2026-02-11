/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.tsdb.metrics;

import org.opensearch.telemetry.metrics.Counter;
import org.opensearch.telemetry.metrics.Histogram;
import org.opensearch.telemetry.metrics.MetricsRegistry;

/**
 * Index/Retention/Compaction related TSDB metrics.
 */
public class TSDBIndexMetrics {
    /** Counter for total indexes created */
    public Counter indexCreatedTotal;

    /** Counter for total indexes deleted by retention */
    public Counter retentionSuccessTotal;

    /** Counter for total failed retention operations */
    public Counter retentionFailureTotal;

    /** Counter for total successful compaction operations */
    public Counter compactionSuccessTotal;

    /** Counter for total failed compaction operations */
    public Counter compactionFailureTotal;

    /** Counter for total indexes deleted by compaction */
    public Counter compactionDeletedTotal;

    /** Histogram for collective size of indexes */
    public Histogram indexSize;

    /** Histogram for total samples per index */
    public Histogram indexSamples;

    /** Histogram for age of online indexes */
    public Histogram indexOnlineAge;

    /** Histogram for age of offline indexes */
    public Histogram indexOfflineAge;

    /** Histogram for retention operation latency */
    public Histogram retentionLatency;

    /** Histogram for configured retention period */
    public Histogram retentionAge;

    /** Histogram for compaction operation latency */
    public Histogram compactionLatency;

    public void initialize(MetricsRegistry registry) {
        indexCreatedTotal = registry.createCounter(
            TSDBMetricsConstants.INDEX_CREATED_TOTAL,
            TSDBMetricsConstants.INDEX_CREATED_TOTAL_DESC,
            TSDBMetricsConstants.UNIT_COUNT
        );
        retentionSuccessTotal = registry.createCounter(
            TSDBMetricsConstants.RETENTION_SUCCESS_TOTAL,
            TSDBMetricsConstants.RETENTION_SUCCESS_TOTAL_DESC,
            TSDBMetricsConstants.UNIT_COUNT
        );
        retentionFailureTotal = registry.createCounter(
            TSDBMetricsConstants.RETENTION_FAILURE_TOTAL,
            TSDBMetricsConstants.RETENTION_FAILURE_TOTAL_DESC,
            TSDBMetricsConstants.UNIT_COUNT
        );
        compactionSuccessTotal = registry.createCounter(
            TSDBMetricsConstants.COMPACTION_SUCCESS_TOTAL,
            TSDBMetricsConstants.COMPACTION_SUCCESS_TOTAL_DESC,
            TSDBMetricsConstants.UNIT_COUNT
        );
        compactionFailureTotal = registry.createCounter(
            TSDBMetricsConstants.COMPACTION_FAILURE_TOTAL,
            TSDBMetricsConstants.COMPACTION_FAILURE_TOTAL_DESC,
            TSDBMetricsConstants.UNIT_COUNT
        );
        compactionDeletedTotal = registry.createCounter(
            TSDBMetricsConstants.COMPACTION_DELETED_TOTAL,
            TSDBMetricsConstants.COMPACTION_DELETED_TOTAL_DESC,
            TSDBMetricsConstants.UNIT_COUNT
        );

        indexSize = registry.createHistogram(
            TSDBMetricsConstants.INDEX_SIZE,
            TSDBMetricsConstants.INDEX_SIZE_DESC,
            TSDBMetricsConstants.UNIT_BYTES
        );
        indexSamples = registry.createHistogram(
            TSDBMetricsConstants.INDEX_SAMPLES,
            TSDBMetricsConstants.INDEX_SAMPLES_DESC,
            TSDBMetricsConstants.UNIT_COUNT
        );
        indexOnlineAge = registry.createHistogram(
            TSDBMetricsConstants.INDEX_ONLINE_AGE,
            TSDBMetricsConstants.INDEX_ONLINE_AGE_DESC,
            TSDBMetricsConstants.UNIT_MILLISECONDS
        );
        indexOfflineAge = registry.createHistogram(
            TSDBMetricsConstants.INDEX_OFFLINE_AGE,
            TSDBMetricsConstants.INDEX_OFFLINE_AGE_DESC,
            TSDBMetricsConstants.UNIT_MILLISECONDS
        );
        retentionLatency = registry.createHistogram(
            TSDBMetricsConstants.RETENTION_LATENCY,
            TSDBMetricsConstants.RETENTION_LATENCY_DESC,
            TSDBMetricsConstants.UNIT_MILLISECONDS
        );
        retentionAge = registry.createHistogram(
            TSDBMetricsConstants.RETENTION_AGE,
            TSDBMetricsConstants.RETENTION_AGE_DESC,
            TSDBMetricsConstants.UNIT_MILLISECONDS
        );
        compactionLatency = registry.createHistogram(
            TSDBMetricsConstants.COMPACTION_LATENCY,
            TSDBMetricsConstants.COMPACTION_LATENCY_DESC,
            TSDBMetricsConstants.UNIT_MILLISECONDS
        );
    }

    public void cleanup() {
        indexCreatedTotal = null;
        retentionSuccessTotal = null;
        retentionFailureTotal = null;
        compactionSuccessTotal = null;
        compactionFailureTotal = null;
        compactionDeletedTotal = null;
        indexSize = null;
        indexSamples = null;
        indexOnlineAge = null;
        indexOfflineAge = null;
        retentionLatency = null;
        retentionAge = null;
        compactionLatency = null;
    }
}
