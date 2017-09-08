package com.l7tech.external.assertions.circuitbreaker.server;

/**
 * @author Ekta Khandelwal - khaek01@ca.com
 */
public interface FailureCondition {
    int getSamplingWindow();
    int getMaxFailureCount();
    String getType();
}
