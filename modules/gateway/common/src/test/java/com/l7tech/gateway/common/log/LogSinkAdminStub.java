package com.l7tech.gateway.common.log;

import com.l7tech.objectmodel.*;

import java.util.Collection;
import java.util.Collections;

/**
 * This is a stub that can be used in place of a LogSinkAdminImpl object.
 */
public class LogSinkAdminStub implements LogSinkAdmin {
    @Override
    public Collection<SinkConfiguration> findAllSinkConfigurations() throws FindException {
        return Collections.emptyList();
    }

    @Override
    public SinkConfiguration getSinkConfigurationByPrimaryKey(Goid oid) throws FindException {
        return null;
    }

    @Override
    public Goid saveSinkConfiguration(SinkConfiguration sinkConfiguration) throws SaveException, UpdateException {
        return SinkConfiguration.DEFAULT_GOID;
    }

    @Override
    public void deleteSinkConfiguration(Goid oid) throws DeleteException, FindException {
    }

    @Override
    public boolean sendTestSyslogMessage(SinkConfiguration sinkConfiguration, String message) {
        return true;
    }

    @Override
    public long getMaximumFileSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public long getReservedFileSize() {
        return 0L;
    }

    @Override
    public Collection<LogFileInfo> findAllFilesForSinkByNode(String nodeId, Goid sinkId) {
        return Collections.emptyList();
    }

    @Override
    public LogSinkData getSinkLogs(String nodeId, Goid sinkId, String file, LogSinkQuery query) {
        return null;
    }
}
