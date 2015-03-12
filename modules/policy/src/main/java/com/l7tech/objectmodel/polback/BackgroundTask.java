package com.l7tech.objectmodel.polback;

/**
 * A policy-backed service interface that is used specify a policy that will be ran asynchronously.
 */
@PolicyBacked
public interface BackgroundTask {

    /**
     * Run the background task.
     */
    void run();
}
