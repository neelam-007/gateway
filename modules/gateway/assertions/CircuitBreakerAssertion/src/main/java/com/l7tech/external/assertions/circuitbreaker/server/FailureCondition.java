package com.l7tech.external.assertions.circuitbreaker.server;

/**
 * @author Ekta Khandelwal - khaek01@ca.com
 */
public interface FailureCondition {
    public long getSamplingWindow();
    public long getMaxFailureCount();
    public String getType();
}
