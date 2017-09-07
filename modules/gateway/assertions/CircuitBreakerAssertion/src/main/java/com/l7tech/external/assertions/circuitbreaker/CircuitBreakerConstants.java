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
    String CB_EVENT_TRACKER_CLEANUP_INTERVAL_DESC = "The Event Tracker cleanup interval (milliseconds). Every interval, failure records older than the interval will be removed from Event Trackers (i.e. every 10 minutes, failures older than 10 minutes will be removed). Default is 600000 ms (10 minutes).";
    long CB_EVENT_TRACKER_CLEANUP_INTERVAL_DEFAULT = 10L * 60 * 1000; //10 minutes

    // Force circuit properties
    String CB_FORCE_EVENT_TRACKER_LIST_CIRCUIT_OPEN =  "circuitBreakerForceCircuitOpen";
    String CB_FORCE_EVENT_TRACKER_LIST_CIRCUIT_OPEN_UI_PROPERTY =  "circuitBreaker.forceCircuitOpenEventTrackerIdList";
    String CB_FORCE_EVENT_TRACKER_LIST_CIRCUIT_OPEN_DESC =  "Event Tracker IDs, listed one per line, for which to force Circuits open. Circuits which reference a listed Event Tracker ID will fail immediately and the child policy will not be executed. Remove an Event Tracker ID from the list to return it to standard behaviour.";

    //Circuit types.
    String CIRCUIT_TYPE_POLICY_FAILURE = "Policy Failure";
    String CIRCUIT_TYPE_LATENCY = "Latency";
}
