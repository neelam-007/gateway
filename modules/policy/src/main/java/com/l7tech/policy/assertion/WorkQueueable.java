package com.l7tech.policy.assertion;

/**
 * This is an interface for assertions that use WorkQueue entities.
 */
public interface WorkQueueable {
    public String getWorkQueueName();

    public void setWorkQueueName(String workQueueName);
}
