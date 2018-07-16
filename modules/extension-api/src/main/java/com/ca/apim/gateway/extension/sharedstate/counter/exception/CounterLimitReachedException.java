package com.ca.apim.gateway.extension.sharedstate.counter.exception;

public class CounterLimitReachedException extends Exception {
    public CounterLimitReachedException(String message) {
        super(message);
    }
}
