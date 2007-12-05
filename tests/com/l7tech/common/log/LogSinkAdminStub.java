package com.l7tech.common.log;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;

import java.util.Collection;
import java.util.Vector;

/**
 * This is a stub that can be used in place of a LogSinkAdminImpl object.
 */
public class LogSinkAdminStub implements LogSinkAdmin {
    public Collection<SinkConfiguration> findAllSinkConfigurations() throws FindException {
        return new Vector<SinkConfiguration>();
    }

    public SinkConfiguration getSinkConfigurationByPrimaryKey(long oid) throws FindException {
        return null;
    }

    public long saveSinkConfiguration(SinkConfiguration sinkConfiguration) throws SaveException, UpdateException {
        return -1L;
    }

    public void deleteSinkConfiguration(long oid) throws DeleteException, FindException {
    }

    public boolean sendTestSyslogMessage(SinkConfiguration sinkConfiguration, String message) {
        return true;
    }
}
