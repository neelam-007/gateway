/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import javax.xml.bind.annotation.XmlEnum;

/**
 * Represents a host's current NTP status.
 */
@XmlEnum(String.class)
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
