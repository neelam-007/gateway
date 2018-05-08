package com.l7tech.external.assertions.logmessagetosyslog.server;

import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.log.SinkManagerImpl;
import com.l7tech.server.log.TrafficLogger;
import com.l7tech.server.log.syslog.SyslogManager;
import com.l7tech.server.util.ApplicationEventProxy;

import java.util.ArrayList;
import java.util.Collection;

public class MockSinkManagerImpl extends SinkManagerImpl {
    private Collection<SinkConfiguration> sinkConfigurationCollection;

    public MockSinkManagerImpl(final ServerConfig serverConfig, final SyslogManager syslogManager, final TrafficLogger trafficLogger, final ApplicationEventProxy eventProxy) {
        super(serverConfig, syslogManager, trafficLogger, eventProxy, null, null);

        sinkConfigurationCollection = new ArrayList<SinkConfiguration>();
    }

    public void addSinkConfiguration(SinkConfiguration sinkConfiguration)
    {
        if (sinkConfiguration != null)
        {
            sinkConfigurationCollection.add(sinkConfiguration);
        }
    }

    @Override
    public Collection<SinkConfiguration> findAll() throws FindException {
        return this.sinkConfigurationCollection;
    }

    @Override
    public SinkConfiguration findByPrimaryKey(Goid goid) throws FindException {
        for (SinkConfiguration sinkConfiguration: sinkConfigurationCollection)
        {
            if (sinkConfiguration.getGoid().equals(goid))
            {
                return sinkConfiguration;
            }
        }
        throw new FindException();
    }
}
