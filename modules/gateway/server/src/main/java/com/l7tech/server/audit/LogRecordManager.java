package com.l7tech.server.audit;

import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.gateway.common.cluster.ClusterLogin;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gateway.common.logging.GenericLogAdmin;
import com.l7tech.gateway.common.logging.SSGLogRecord;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.log.LogRecordRingBuffer;
import com.l7tech.gateway.common.spring.remoting.rmi.NamingURL;
import com.l7tech.gateway.common.spring.remoting.rmi.ResettableRmiProxyFactoryBean;
import com.l7tech.gateway.common.spring.remoting.rmi.ssl.SslRMIClientSocketFactory;
import com.l7tech.gateway.common.spring.remoting.rmi.ssl.SslRMIServerSocketFactory;

import javax.security.auth.Subject;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Manager that handles retrieval of log records.
 *
 * <p>The manager is responsible for routing requests to the correct node in a cluster.</p>
 *
 * @author $Author$
 * @version $Revision$
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
                            final LogRecordRingBuffer buffer) {
        clusterInfoManager = manager;
        logRecordRingBuffer = buffer;
        nodeLogAdmins = new HashMap<String, GenericLogAdmin>();
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
                // This is a not too safe check of whether the client is another node or the
                // SSM. If the client is another node we refuse to forward the request (since
                // that node should not have asked us for another nodes logs ...)
                if(SslRMIServerSocketFactory.getContext() != null &&
                   SslRMIServerSocketFactory.getContext().isRemoteClientCertAuthenticated()) {
                    throw new FindException("Cannot get logs for node '"+nodeId+"'.");
                }
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
    private final Map<String, GenericLogAdmin> nodeLogAdmins; // Map of nodeId (String) -> GenericLogAdmin

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
    private NamingURL getNamingURLForNode(ClusterNodeInfo clusterNodeInfo) throws MalformedURLException {
        String host = clusterNodeInfo.getAddress();
        int port = clusterNodeInfo.getClusterPort();
        String portStr = "";
        if (port > 0) {
            portStr = ":" + port;            
        }

        return NamingURL.parse(NamingURL.DEFAULT_SCHEME + "://" + host + portStr + "/ClusterLogin");
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
                    GenericLogAdmin gla = nodeLogAdmins.get(clusterNodeInfo.getNodeIdentifier());
                    if(gla==null) {
                        NamingURL adminServiceNamingURL = getNamingURLForNode(clusterNodeInfo);
                        ResettableRmiProxyFactoryBean pfb = new ResettableRmiProxyFactoryBean();
                        pfb.setServiceInterface(ClusterLogin.class);
                        pfb.setRefreshStubOnConnectFailure(false);
                        SslRMIClientSocketFactory socketFactory = new SslRMIClientSocketFactory();
                        socketFactory.setHost(clusterNodeInfo.getAddress());
                        pfb.setRegistryClientSocketFactory(socketFactory);
                        pfb.setServiceUrl(adminServiceNamingURL.toString());
                        pfb.afterPropertiesSet();
                        ClusterLogin cl = (ClusterLogin) pfb.getObject();
                        gla = cl.login().getLogAdmin();
                        synchronized(nodeLogAdmins) {
                            nodeLogAdmins.put(clusterNodeInfo.getNodeIdentifier(), gla);
                        }
                    }
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
            synchronized(nodeLogAdmins) {
                nodeLogAdmins.remove(clusterNodeInfo.getNodeIdentifier()); //remove reference so it is refreshed
            }
        }
        catch(Exception e) {
            logger.log(Level.WARNING, "Unexpected error during retrieval of logs from remote node '"+clusterNodeInfo.getNodeIdentifier()+"'", e);
            synchronized(nodeLogAdmins) {
                nodeLogAdmins.remove(clusterNodeInfo.getNodeIdentifier()); //remove reference so it is refreshed
            }
        }

        if(ssgLrs==null) ssgLrs = new SSGLogRecord[0];

        return ssgLrs;
    }
}
