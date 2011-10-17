package com.l7tech.server.log;

import com.l7tech.gateway.common.log.LogAccessAdmin;
import com.l7tech.gateway.common.log.LogFileInfo;
import com.l7tech.gateway.common.log.LogSinkData;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.cluster.ClusterInfoManager;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;

/**
 * Implements log access for inter-node communication
 */
public class LogAccessAdminImpl implements LogAccessAdmin {

    //- PUBLIC

    @Override
    public Collection<LogFileInfo> findAllFilesForSinkByNode( final String nodeId, final long sinkId ) throws FindException {
        if ( !nodeId.equals( clusterInfoManager.thisNodeId() ) ) return Collections.emptyList();
        return sinkManager.findAllFilesForSinkByNode( nodeId, sinkId );
    }

    @Override
    public LogSinkData getSinkLogs( final String nodeId, final long sinkId, final String file, final long startPosition, boolean fromEnd ) throws FindException {
        if ( !nodeId.equals( clusterInfoManager.thisNodeId() ) ) return null;
        return sinkManager.getSinkLogs( nodeId, sinkId, file, startPosition, fromEnd );
    }

    //- PRIVATE

    @Inject
    private SinkManager sinkManager;

    @Inject
    private ClusterInfoManager clusterInfoManager;
}
