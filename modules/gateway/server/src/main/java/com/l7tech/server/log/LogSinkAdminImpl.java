package com.l7tech.server.log;

import com.l7tech.gateway.common.log.LogSinkAdmin;
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

    public Collection<SinkConfiguration> findAllSinkConfigurations() throws FindException {
        return sinkManager.findAll();
    }

    public SinkConfiguration getSinkConfigurationByPrimaryKey(long oid) throws FindException {
        return sinkManager.findByPrimaryKey(oid);
    }

    public long saveSinkConfiguration(SinkConfiguration sinkConfiguration) throws SaveException, UpdateException {
        if (sinkConfiguration.getOid() == SinkConfiguration.DEFAULT_OID) {
            return sinkManager.save(sinkConfiguration);
        } else {
            sinkManager.update(sinkConfiguration);
            return sinkConfiguration.getOid();
        }
    }

    public void deleteSinkConfiguration(long oid) throws DeleteException, FindException {
        sinkManager.delete(oid);
    }

    public boolean sendTestSyslogMessage(SinkConfiguration sinkConfiguration, String message) {
        return sinkManager.test(sinkConfiguration, message);
    }

    public long getMaximumFileSize() {
        return sinkManager.getMaximumFileStorageSpace();
    }

    public long getReservedFileSize() {
        return sinkManager.getReservedFileStorageSpace();
    }
}
