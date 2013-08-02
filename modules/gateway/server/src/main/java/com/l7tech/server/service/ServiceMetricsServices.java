/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service;

import com.l7tech.identity.User;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.objectmodel.Goid;

import java.util.List;

/**
 * "Top half" of the Service Metrics system, intended for use by most SSG components.  Explicitly non-transactional.
 */
public interface ServiceMetricsServices {
    static final int MINUTE = 60 * 1000;
    static final int HOUR = 60 * MINUTE;
    static final long DAY = 24 * HOUR;
    static final long YEAR = 365 * DAY;

    /** Minimum allowed fine resolution bin interval (in milliseconds). */
    static final int MIN_FINE_BIN_INTERVAL = 1000; // 1 second

    /** Maximum allowed fine resolution bin interval (in milliseconds). */
    static final int MAX_FINE_BIN_INTERVAL = 5 * MINUTE; // 5 minutes

    /** Default fine resolution bin interval (in milliseconds). */
    static final int DEF_FINE_BIN_INTERVAL = 5 * 1000; // 5 seconds

    static final int MIN_FINE_AGE = MINUTE * 65; // more than 1 hour to ensure an hourly rollup bin can be created
    static final int MAX_FINE_AGE = MINUTE * 65;
    static final int DEF_FINE_AGE = MINUTE * 65;

    static final long MIN_HOURLY_AGE = DAY + (5 * MINUTE); // more than 1 day to ensure an hourly rollup bin can be created
    static final long MAX_HOURLY_AGE = 31 * DAY;     // a month
    static final long DEF_HOURLY_AGE = 7 * DAY;      // a week

    static final long MIN_DAILY_AGE = 31 * DAY;      // a month
    static final long MAX_DAILY_AGE = 10 * YEAR;     // 10 years
    static final long DEF_DAILY_AGE = YEAR;          // 1 year

    /** Name of cluster property that enables/disables service metrics collection. */
    static final String CLUSTER_PROP_ENABLED = "serviceMetricsEnabled";

    /**
     * @return whether collection of service metrics is currently enabled
     */
    boolean isEnabled();

    /**
     * Ensure service metrics are tracked for a given published service.
     *
     * @param serviceGoid    GOID of published service
     */
    void trackServiceMetrics(Goid serviceGoid);

    /**
     * Record service metrics for a given published service and mapping info.
     *
     * @param serviceGoid  GOID of published service
     * @param operation   the published service operation (may be null)
     * @param authorizedUser the user for the request (may be null)
     * @param mappings    Message context mapping information (may be null)
     * @param authorized True if the policy execution was successful (routing attempted).
     * @param completed  True if the routing was successful
     * @param frontTime  Complete time for request processing
     * @param backTime   Time taken by the protected service
     */
    void addRequest(Goid serviceGoid, String operation, User authorizedUser, List<MessageContextMapping> mappings, boolean authorized, boolean completed, int frontTime, int backTime);

    /**
     * Get the interval for fine metrics bins.
     *
     * @return the interval in millis
     */
    int getFineInterval();

}
