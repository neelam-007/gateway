package com.ca.apim.gateway.extension.sharedstate.counter.exception;

public class IllegalFieldOfInterestException extends IllegalArgumentException {
    public IllegalFieldOfInterestException(String fieldOfInterest) {
        super("Unknown ThroughputQuota field of interest: " + fieldOfInterest);
    }
}