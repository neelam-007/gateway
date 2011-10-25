package com.l7tech.gateway.common.log;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;

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
    public SinkConfiguration getSinkConfigurationByPrimaryKey(long oid) throws FindException {
        return null;
    }

    @Override
    public long saveSinkConfiguration(SinkConfiguration sinkConfiguration) throws SaveException, UpdateException {
        return -1L;
    }

    @Override
    public void deleteSinkConfiguration(long oid) throws DeleteException, FindException {
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
    public Collection<LogFileInfo> findAllFilesForSinkByNode(String nodeId, long sinkId) {
        return Collections.emptyList();
    }

    @Override
    public LogSinkData getSinkLogs(String nodeId, long sinkId, String file, LogSinkQuery query) {
        return null;
    }
}
