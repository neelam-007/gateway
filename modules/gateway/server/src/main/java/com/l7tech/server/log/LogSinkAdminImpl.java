package com.l7tech.server.log;

import com.l7tech.gateway.common.log.LogFileInfo;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.gateway.common.log.LogSinkData;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;

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
    public SinkConfiguration getSinkConfigurationByPrimaryKey(long oid) throws FindException {
        return sinkManager.findByPrimaryKey(oid);
    }

    @Override
    public long saveSinkConfiguration(SinkConfiguration sinkConfiguration) throws SaveException, UpdateException {
        if (sinkConfiguration.getOid() == SinkConfiguration.DEFAULT_OID) {
            return sinkManager.save(sinkConfiguration);
        } else {
            sinkManager.update(sinkConfiguration);
            return sinkConfiguration.getOid();
        }
    }

    @Override
    public void deleteSinkConfiguration(long oid) throws DeleteException, FindException {
        sinkManager.delete(oid);
        sinkManager.deleteRoles(oid);
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
    public Collection<LogFileInfo> findAllFilesForSinkByNode(final String nodeId, final long sinkId) throws FindException{
        return sinkManager.findAllFilesForSinkByNode(nodeId, sinkId);
    }

    @Override
    public LogSinkData getSinkLogs( final String nodeId,
                                    final long sinkId,
                                    final String file,
                                    final long startPosition ) throws FindException {
        return sinkManager.getSinkLogs(nodeId, sinkId,file, startPosition);
    }
}
