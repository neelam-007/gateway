/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

/**
 * Represents a host's current NTP status.
 */
public enum NtpStatus {
    /** The ntpd is running and the clock is synchronized. */
    OK,

    /** 
     * The ntpd is running but the clock is not currently synchronized.
     * This status is also reported if the ntpd is in the process of starting up.
     */
    UNSYNCHRONIZED,

    /**
     * The ntpd cannot be contacted.  It may have failed or may not be running.
     */
    UNKNOWN,
}
