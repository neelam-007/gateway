/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.audit;

import java.io.Serializable;
import java.util.Date;
import java.util.logging.Level;

/**
 * @author mike
 */
public final class AuditSearchCriteria implements Serializable {
    /**
     * @param fromTime the time of the earliest record to retrieve (null = one day ago)
     * @param toTime the time of the latest record to retrieve (null = now)
     * @param fromLevel the level of the least severe record to retrieve (null = {@link java.util.logging.Level#INFO})
     * @param toLevel the level of the most severe record to retrieve (null = {@link java.util.logging.Level#SEVERE})
     * @param nodeId the ID (usually a MAC address) of the cluster node to search (null = any)
     * @param startMessageNumber the minimum OID to find
     * @param endMessageNumber the maximum OID to find
     * @param maxRecords the maximum number of records to retrieve (0 = 4096);
     */
    public AuditSearchCriteria(Date fromTime, Date toTime, Level fromLevel, Level toLevel, Class recordClass, String nodeId, long startMessageNumber, long endMessageNumber, int maxRecords) {
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.fromLevel = fromLevel;
        this.toLevel = toLevel;
        this.recordClass = recordClass;
        this.nodeId = nodeId;
        this.startMessageNumber = startMessageNumber;
        this.endMessageNumber = endMessageNumber;
        this.maxRecords = maxRecords;
    }

    /**
     * @param start the minimum OID to find
     * @param end the maximum OID to find
     * @param max the maximum number of records to retrieve (0 = 4096);
     */
    public AuditSearchCriteria(String nodeId, long start, long end, int max) {
        this(null, null, null, null, null, nodeId, start, end, max);
    }

    public final Date fromTime;
    public final Date toTime;
    public final Level fromLevel;
    public final Level toLevel;
    public final Class recordClass;
    public final String nodeId;

    public final long startMessageNumber;
    public final long endMessageNumber;
    public final int maxRecords;
}
