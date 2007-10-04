/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.logging;

import com.l7tech.objectmodel.FindException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Provides a remote interface for retrieving server system log.
 *
 * @author mike
 * @version $Id$
 */
public interface GenericLogAdmin {

    public static final int TYPE_AUDIT = 1;
    public static final int TYPE_LOG = 2;

    /**
     * Retrieve the system logs of a node in between the startMsgNumber and endMsgNumber specified
     * up to the specified size. The retrieved block of logs will not contain the log whose
     * message number is equal to startMsgNumber or endMsgNumber.
     * <p>
     * The design principle of this log retrieval API is to facilitate the client to retrieve the latest
     * logs up to the number of logs that the client's buffer can hold in an efficient way. Hence, the
     * retrieval is designed to be done in the reverse order of time. This means that the startMsgNumber
     * is always greater than the endMsgNumber except for the cases where their value is set to -1.
     *<p>
     * <i>Log Retrieval Sequence I (Initial Retrieval):</i>
     * <br>This is a typical sequence of log retrievals when the client just started and retrieves the logs the first time.
     * In this case, both the starting and ending points of the logs in the retrieval are unknown.</br>
     * <br>1. Initially, the client specifies startMsgNumber=-1 and endMsgNumber=-1 since it has no knowledge of
     *    the starting and ending points.</br>
     * <br>2. After the retrieval succeeded, the client is now able to find the starting point for the next
     *    block of log retrieval from the logs just retrieved. The smallest message number found in the retrieved
     *    logs should be used as the startMsgNumber in the next call. The endMsgNumber should still be set to -1.</br>
     * <br>3. The client repeats the Step 2 above until:
     *   i) its allocated buffer is full; or
     *   ii) the number of logs retrieved is less than the specified size.</br>
     * <p>
     * <i>Log Retrieval Sequence II (Refresh):</i>
     * <br>This is a typical sequence of log retrievals for obtaining the latest logs after the initial retrieval
     * of the logs is done (as described in the Log Retrieval Sequence I above) and there is at least
     * one log message stored in the client's buffer.</br>
     * In this case, the client knows the ending message number of the logs to be retrieved. This number is the
     * greatest message number of the log messages stored in the client's buffer.
     * <br>1. The client specifies startMsgNumber=-1 and
     *    endMsgNumber=[the greatest message number of the log messages stored in the client's buffer].</br>
     * <br>2. After the retrieval succeeded, the client is able to find the starting point for the next
     *    block of log retrieval from the logs just retrieved. The smallest message number found in the retrieved
     *    logs should be used as the startMsgNumber in the next call. The same endMsgNumber
     *    in the Step 1 above should be used.</br>
     * <br>3. The client repeats the Step 2 above until:
     *   i) its allocated buffer is full; or
     *   ii) the number of logs retrieved is less than the specified size.</br>
     * <p>
     * To get the information about what nodes exist in the cluster and what their nodeid is,
     * call the method {@link com.l7tech.cluster.ClusterStatusAdmin#getClusterStatus}.
     *
     * @param nodeid the id of the node for which the logs should be retrieved.
     * @param startMsgNumber the message number to locate the start point.
     *                       Start from beginning of the SecureSpan Gateway message buffer if it equals to -1.
     * @param endMsgNumber   the message number to locate the end point.
     *                       Retrieve messages until the end of the SecureSpan Gateway message buffer is hit if it equals to -1.
     * @param size  the max. number of logsto be retrieved
     * @return SSGLogRecord[] the array of logs retrieved
     * @see com.l7tech.cluster.ClusterStatusAdmin#getClusterStatus
     * @see com.l7tech.logging.SSGLogRecord
     *
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    SSGLogRecord[] getSystemLog(String nodeid, long startMsgNumber, long endMsgNumber, Date startMsgDate, Date endMsgDate, int size) throws FindException;

    /**
     * Get the configured refresh period for the log type.
     *
     * @param typeId the type of the logs to retrieve.
     * @return the configured refresh period (seconds).
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    int getSystemLogRefresh(int typeId);
}
