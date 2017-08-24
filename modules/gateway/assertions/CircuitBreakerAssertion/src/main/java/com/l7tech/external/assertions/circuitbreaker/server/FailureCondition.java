package com.l7tech.external.assertions.circuitbreaker.server;

/**
 * @author Ekta Khandelwal - khaek01@ca.com
 */
public interface FailureCondition {
    public int getSamplingWindow();
    public int getMaxFailureCount();
    public String getType();
}
