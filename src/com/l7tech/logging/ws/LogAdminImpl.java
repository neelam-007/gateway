package com.l7tech.logging.ws;

import com.l7tech.logging.LogAdmin;
import com.l7tech.logging.SSGLogRecord;
import com.l7tech.logging.ServerLogManager;
import com.l7tech.objectmodel.PersistenceContext;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * AdminWS for consulting the server system log.
 * <p/>
 * <br/><br/>
 * Layer 7 technologies, inc.<br/>
 * User: flascell<br/>
 * Date: Jul 3, 2003<br/>
 */
public class LogAdminImpl implements LogAdmin {
    public static final String SERVICE_DEPENDENT_URL_PORTION = "/services/loggingAdmin";

    /**
     * Retrieve the system logs of a node in between the startMsgNumber and endMsgNumber specified
     * up to the specified size.
     *
     * The design principle of this log retrieval API is to facilitate the client to retrieve the latest
     * logs up to the number of logs that the client's buffer can hold in an efficient way. Hence, the
     * retrieval is designed to be done in the reverse order of time. This means that the startMsgNumber
     * is always greater than the endMsgNumber except for the cases where their value is set to -1.
     *
     * Log Retrieval Sequence I (Initial Retrieval):
     * This is a typical sequence of log retrievals when the client just started and retrieves the logs the first time.
     * In this case, both the starting and ending points of the logs in the retrieval are unknown.
     * 1. Initially, the client specifies startMsgNumber=-1 and endMsgNumber=-1 since it has no knowledge of
     *    the starting and ending points.
     * 2. After the retrieval succeeded, the client is now able to find the starting point for the next
     *    block of log retrieval from the logs just retrieved. The smallest message number found in the retrieved
     *    logs should be used as the startMsgNumber in the next call. The endMsgNumber should still be set to -1.
     * 3. The client repeats the Step 2 above until:
     *   i) its allocated buffer is full; or
     *   ii) the number of logs retrieved is zero.
     *
     * Log Retrieval Sequence II (Refresh):
     * This is a typical sequence of log retrievals for obtaining the latest logs after the initial retrieval
     * of the logs is done (as described in the Log Retrieval Sequence I above) and there is at least
     * one log message stored in the client's buffer.
     * In this case, the client knows the ending message number of the logs to be retrieved. This number is the
     * greatest message number of the log messages stored in the client's buffer.
     * 1. The client specifies startMsgNumber=-1 and
     *    endMsgNumber=<the greatest message number of the log messages stored in the client's buffer>.
     * 2. After the retrieval succeeded, the client is able to find the starting point for the next
     *    block of log retrieval from the logs just retrieved. The smallest message number found in the retrieved
     *    logs should be used as the startMsgNumber in the next call. The same endMsgNumber
     *    in the Step 1 above should be used.
     * 3. The client repeats the Step 2 above until:
     *   i) its allocated buffer is full; or
     *   ii) the number of logs retrieved is zero.
     *
     * To get the information about what nodes exist in the cluster and what their nodeid is,
     * call the method {@link com.l7tech.cluster.ClusterStatusAdmin#getClusterStatus}.
     *
     * @param nodeid the id of the node for which the logs should be retrieved.
     * @param startMsgNumber the message number to locate the start point.
     *                       Start from beginning of the SecureSpan Gateway message buffer if it equals to -1.
     * @param endMsgNumber   the message number to locate the end point.
     *                       Retrieve messages until the end of the SecureSpan Gateway message buffer is hit if it equals to -1.
     * @param size  the max. number of messages retrieved
     * @return String[] the array of messages retrieved
     * @see com.l7tech.cluster.ClusterStatusAdmin#getClusterStatus
     * 
     */
    public SSGLogRecord[] getSystemLog(String nodeid, long startMsgNumber, long endMsgNumber, int size) throws RemoteException {
        try {
            return (SSGLogRecord[])ServerLogManager.getInstance().getLogRecords(nodeid, startMsgNumber, endMsgNumber, size).toArray(new SSGLogRecord[]{});
        } finally {
            try {
                PersistenceContext.getCurrent().close();
            } catch (SQLException e) {
                logger.fine("error closing context");
            }
        }

    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private final Logger logger = Logger.getLogger(getClass().getName());
}
