package com.l7tech.server.log;

import com.l7tech.gateway.common.log.*;
import com.l7tech.objectmodel.*;

import java.util.Collection;

/**
 * Server-side implementation of the LogSinkAdmin API.
 */
public class LogSinkAdminImpl implements LogSinkAdmin {
    private final SinkManager sinkManager;

    public LogSinkAdminImpl(SinkManager sinkManager) {
        this.sinkManager = sinkManager;
    }

    @Override
    public Collection<SinkConfiguration> findAllSinkConfigurations() throws FindException {
        return sinkManager.findAll();
    }

    @Override
    public SinkConfiguration getSinkConfigurationByPrimaryKey(Goid oid) throws FindException {
        return sinkManager.findByPrimaryKey(oid);
    }

    @Override
    public Goid saveSinkConfiguration(SinkConfiguration sinkConfiguration) throws SaveException, UpdateException {
        if ( sinkConfiguration.isUnsaved() ) {
            return sinkManager.save(sinkConfiguration);
        } else {
            sinkManager.update(sinkConfiguration);
            return sinkConfiguration.getGoid();
        }
    }

    @Override
    public void deleteSinkConfiguration(Goid goid) throws DeleteException, FindException {
        sinkManager.delete(goid);
    }

    @Override
    public boolean sendTestSyslogMessage(SinkConfiguration sinkConfiguration, String message) {
        return sinkManager.test(sinkConfiguration, message);
    }

    @Override
    public long getMaximumFileSize() {
        return sinkManager.getMaximumFileStorageSpace();
    }

    @Override
    public long getReservedFileSize() {
        return sinkManager.getReservedFileStorageSpace();
    }

    @Override
    public Collection<LogFileInfo> findAllFilesForSinkByNode(final String nodeId, final Goid sinkId) throws FindException{
        if ( nodeId == null ) throw new FindException("Missing node identifier");
        return sinkManager.findAllFilesForSinkByNode(nodeId, sinkId);
    }

    @Override
    public LogSinkData getSinkLogs( final String nodeId,
                                    final Goid sinkId,
                                    final String file,
                                    final LogSinkQuery query) throws FindException {
        return sinkManager.getSinkLogs(nodeId, sinkId,file, query);
    }
}
