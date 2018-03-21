package com.ca.apim.gateway.extension.sharedstate;

import com.ca.apim.gateway.extension.Extension;


/**
 * A SharedExecutorServiceProvider is a type of extension that provides specific implementation of
 * SharedScheduleExecutorService that allows for scheduling tasks to be run on any node or all nodes of a gateway
 * cluster
 */
public interface SharedExecutorServiceProvider extends Extension {
    /**
     * Get the schedule service executor identified by the name or create and return the ScheduledExecutorService with
     * the provided configuration.
     * @param name the name of the scheduled executor service
     * @param config the configuration for the scheduled service
     * @return SharedScheduledExecutorService
     */
    SharedScheduledExecutorService getScheduledExecutorService(String name, Configuration config);
}
