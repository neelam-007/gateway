package com.l7tech.server.audit;

import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.cluster.ClusterContextFactory;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.cluster.ClusterContext;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gateway.common.logging.GenericLogAdmin;
import com.l7tech.gateway.common.logging.SSGLogRecord;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.log.LogRecordRingBuffer;

import javax.security.auth.Subject;
import java.net.ConnectException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Manager that handles retrieval of log records.
 *
 * <p>The manager is responsible for routing requests to the correct node in a cluster.</p>
 */
public class LogRecordManager {

    //- PUBLIC

    /**
     * Create a LogRecordManager with the given manager and buffer.
     *
     * <p>If the manger is null then all requests are concidered to be for
     * the locally held logs.</p>
     *
     * @param manager the ClusterInfoManager to use (may be null)
     * @param buffer the local log buffer (required)
     */
    public LogRecordManager(final ClusterInfoManager manager,
                            final LogRecordRingBuffer buffer,
                            final ClusterContextFactory factory ) {
        clusterInfoManager = manager;
        logRecordRingBuffer = buffer;
        clusterContextFactory = factory;
    }

    /**
     *
     */
    public SSGLogRecord[] find(final String nodeId, final long startOid, final int size) throws FindException {
        if(nodeId==null) throw new FindException("Null node id.");
        if(size<0 || size>Short.MAX_VALUE) throw new FindException("Search with out of bounds result set size '"+size+"'.");

        SSGLogRecord[] ssgLrs;

        if(isThisNodeMe(nodeId)) {
            // Get from our ring buffer.
            ssgLrs = getLocalLogRecords(nodeId, startOid, size);
        }
        else {
            // Find the info for the requested node
            final ClusterNodeInfo clusterNodeInfo = getClusterNodeInfo(nodeId);
            if(clusterNodeInfo==null) {
                logger.warning("Could not find info for node with id '"+nodeId+"'.");
                ssgLrs = new SSGLogRecord[0];
            }
            else {
                long startTime = logger.isLoggable(Level.FINEST) ? System.currentTimeMillis() : 0;
                ssgLrs = getRemoteLogRecords(clusterNodeInfo, startOid, size);
                if(logger.isLoggable(Level.FINEST)) {
                    logger.finest("Getting logs from NODE took "
                            + (System.currentTimeMillis()-startTime) + "ms.");
                }
            }
        }

        return ssgLrs;
    }

    //- PRIVATE

    // logger for the class
    private static final Logger logger = Logger.getLogger(LogRecordManager.class.getName());

    // members
    private final ClusterInfoManager clusterInfoManager;
    private final LogRecordRingBuffer logRecordRingBuffer;
    private final ClusterContextFactory clusterContextFactory;

    /**
     *
     */
    private ClusterNodeInfo getClusterNodeInfo(final String nodeId) throws FindException {
        ClusterNodeInfo clusterNodeInfo = null;

        if ( clusterInfoManager != null ) {
            Collection<ClusterNodeInfo> ClusterNodeInfos = clusterInfoManager.retrieveClusterStatus();
            for (ClusterNodeInfo currentNodeInfo : ClusterNodeInfos) {
                if (nodeId.equals(currentNodeInfo.getNodeIdentifier())) {
                    clusterNodeInfo = currentNodeInfo;
                    break;
                }
            }
        }
        return clusterNodeInfo;
    }

    /**
     * If the clusterInfoManager is available then check the node id, else
     * treat as a request for local logs.
     */
    private boolean isThisNodeMe(final String nodeId) {
        return clusterInfoManager == null || nodeId.equals(clusterInfoManager.thisNodeId());
    }

    /**
     *
     */
    private SSGLogRecord[] getLocalLogRecords(String nodeId, long startOid, int size) {
        SSGLogRecord[] ssgLrs;

        LogRecord[] records = logRecordRingBuffer.getLogRecords(startOid);
        ssgLrs = new SSGLogRecord[Math.min(size, records.length)];
        int startOffset = records.length-ssgLrs.length;
        for (int i = startOffset; i < records.length; i++) {
            LogRecord record = records[i];
            ssgLrs[i-startOffset] = new SSGLogRecord(record, nodeId);
        }

        return ssgLrs;
    }

    /**
     *
     */
    private SSGLogRecord[] getRemoteLogRecords(final ClusterNodeInfo clusterNodeInfo,
                                               final long startOid,
                                               final int size) throws FindException {
        SSGLogRecord[] ssgLrs = null;
        try {
            ssgLrs = Subject.doAs(null, new PrivilegedExceptionAction<SSGLogRecord[]>(){
                // It saves around 10ms if we don't serialize the subject (which we don't use).
                public SSGLogRecord[] run() throws Exception {
                    ClusterContext context = clusterContextFactory.buildClusterContext(clusterNodeInfo.getAddress(), clusterNodeInfo.getClusterPort() );
                    GenericLogAdmin gla = context.getLogAdmin();
                    return gla.getSystemLog(clusterNodeInfo.getNodeIdentifier(), -1, startOid, null, null, size);
                }
            });
        }
        catch(PrivilegedActionException e) {
            Throwable cause = e.getCause();
            if(cause instanceof FindException) {
                throw (FindException) cause;
            }
            if(ExceptionUtils.causedBy(cause, ConnectException.class)) {
                logger.log(Level.INFO, "Unable to connect to remote node '"+clusterNodeInfo.getNodeIdentifier()+"', for retrieval of logs.");            
            } else {
                logger.log(Level.WARNING, "Error during retrieval of logs from remote node '"+clusterNodeInfo.getNodeIdentifier()+"'", cause);
            }
        }
        catch(Exception e) {
            logger.log(Level.WARNING, "Unexpected error during retrieval of logs from remote node '"+clusterNodeInfo.getNodeIdentifier()+"'", e);
        }

        if(ssgLrs==null) ssgLrs = new SSGLogRecord[0];

        return ssgLrs;
    }
}
