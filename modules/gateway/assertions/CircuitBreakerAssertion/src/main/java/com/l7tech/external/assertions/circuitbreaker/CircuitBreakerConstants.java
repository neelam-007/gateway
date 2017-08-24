package com.l7tech.external.assertions.circuitbreaker;

/**
 * @author Ekta Khandelwal - khaek01@ca.com
 */
public interface CircuitBreakerConstants {

    // Policy Failure Circuit - Default value for recovery period
    int CB_POLICY_FAILURE_CIRCUIT_RECOVERY_PERIOD_DEFAULT = 10000;

    // Policy Failure Circuit - Default value for max failures
    int CB_POLICY_FAILURE_CIRCUIT_MAX_FAILURES_DEFAULT = 5;

    // Policy Failure Circuit - Default value for sampling window
    int CB_POLICY_FAILURE_CIRCUIT_SAMPLING_WINDOW_DEFAULT = 5000;

    // Latency Circuit - Default value for recovery period
    int CB_LATENCY_CIRCUIT_RECOVERY_PERIOD_DEFAULT = 10000;

    // Latency Circuit - Default value for max failures
    int CB_LATENCY_CIRCUIT_MAX_FAILURES_DEFAULT = 5;

    // Latency Circuit - Default value for sampling window
    int CB_LATENCY_CIRCUIT_SAMPLING_WINDOW_DEFAULT = 5000;

    // Latency Circuit - Default value for latency limit
    int CB_LATENCY_CIRCUIT_MAX_LATENCY_DEFAULT = 500;

    // Event Tracker clean up interval cluster property
    String CB_EVENT_TRACKER_CLEANUP_INTERVAL_PROPERTY = "eventTrackerCleanupInterval";
    String CB_EVENT_TRACKER_CLEANUP_INTERVAL_UI_PROPERTY = "circuitBreaker.eventTrackerCleanupInterval";
    String CB_EVENT_TRACKER_CLEANUP_INTERVAL_DESC = "Event tracker cleanup interval (milliseconds)";
    long CB_EVENT_TRACKER_CLEANUP_INTERVAL_DEFAULT = 10L * 60 * 1000; //10 minutes
}
