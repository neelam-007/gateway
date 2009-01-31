package com.l7tech.server.processcontroller.monitoring;

/**
 * Represents the overall health of RAID on the current host.
 */
public enum RaidStatus {
    /** No raid arrays are present. */
    NOT_RAID,

    /** All drives are up in all arrays. */
    OK,

    /** At least one drive in one array is not up, and no arrays are currently rebuilding. */
    BAD,

    /** At least one drive in one array is not up, and at least one array is currently rebuilding. */
    REBUILDING
}
