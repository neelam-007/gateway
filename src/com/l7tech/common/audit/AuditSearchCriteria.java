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
 * A set of criteria that can be used to search for {@link AuditRecord}s using the {@link AuditAdmin} interface.
 *
 * @author mike
 */
public final class AuditSearchCriteria implements Serializable {
    /**
     * Constructs a new AuditSearchCriteria with all possible search parameters
     *
     * @param fromTime the time of the earliest record to retrieve, inclusive (null = one day ago)
     * @param toTime the time of the latest record to retrieve, inclusive (null = now)
     * @param fromLevel the level of the least severe record to retrieve, inclusive (null = {@link java.util.logging.Level#INFO})
     * @param toLevel the level of the most severe record to retrieve, inclusive (null = {@link java.util.logging.Level#SEVERE})
     * @param recordClass the type of {@link AuditRecord}s to retrieve; must be AuditRecord.class or one of its subclasses (null = AuditRecord.class)
     * @param nodeId the ID (usually a MAC address) of the cluster node to search (null = any, see {@link com.l7tech.cluster.ClusterStatusAdmin#getClusterStatus()})
     * @param startMessageNumber the minimum OID to find, inclusive (0 = don't care)
     * @param endMessageNumber the maximum OID to find, inclusive (0 = don't care)
     * @param maxRecords the maximum number of records to retrieve (0 = 4096);
     */
    public AuditSearchCriteria(Date fromTime, Date toTime, Level fromLevel, Level toLevel,
                               Class recordClass, String nodeId, long startMessageNumber, long endMessageNumber,
                               int maxRecords)
    {
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
     * Constructs a simple AuditSearchCriteria
     *
     * @param nodeId the ID (usually a MAC address) of the cluster node to search (null = any, see {@link com.l7tech.cluster.ClusterStatusAdmin#getClusterStatus()})
     * @param start the minimum OID to find, inclusive (0 = any)
     * @param end the maximum OID to find, inclusive (0 = any)
     * @param max the maximum number of records to retrieve (0 = 4096);
     */
    public AuditSearchCriteria(String nodeId, long start, long end, int max) {
        this(null, null, null, null, null, nodeId, start, end, max);
    }

    /** The time of the earliest record to retrieve (null = 24 hours ago) */
    public final Date fromTime;
    /** The time of the latest record to retrieve (null = now) */
    public final Date toTime;

    /** the level of the least severe record to retrieve (null = {@link java.util.logging.Level#INFO}) */
    public final Level fromLevel;
    /** the level of the most severe record to retrieve (null = {@link java.util.logging.Level#SEVERE}) */
    public final Level toLevel;

    /** the type of {@link AuditRecord}s to retrieve; must be AuditRecord.class or one of its subclasses */
    public final Class recordClass;

    /** the ID (usually a MAC address) of the cluster node to search (null = any, see  {@link com.l7tech.cluster.ClusterStatusAdmin#getClusterStatus()}) */
    public final String nodeId;

    /** the minimum OID to find */
    public final long startMessageNumber;

    /** the maximum OID to find */
    public final long endMessageNumber;

    /** the maximum number of records to retrieve (0 = 4096) */
    public final int maxRecords;
}
