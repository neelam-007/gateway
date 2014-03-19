package com.l7tech.server.transport.ftp;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.SsgConnectorManager;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.ftplet.Ftplet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
@RunWith(MockitoJUnitRunner.class)
public class SsgFtpServerFactoryTest {
    private static final String ADDRESS = "123.45.67.89";
    private static final String PORT_RANGE_START = "40001";
    private static final String PORT_RANGE_COUNT = "10";

    @Mock
    private SsgConnector connector;

    @Mock
    private SsgConnectorManager connectorManager;

    @Mock
    private FtpServerManager ftpServerManager;

    @Mock
    private ClusterPropertyManager clusterPropertyManager;

    @Mock
    private Ftplet ftplet;

    public SsgFtpServerFactoryTest() {
    }

    @Before
    public void init() {
        when(connector.getProperty(SsgConnector.PROP_BIND_ADDRESS)).thenReturn(ADDRESS);
        when(connector.getProperty(SsgConnector.PROP_PORT_RANGE_START)).thenReturn(PORT_RANGE_START);
        when(connector.getProperty(SsgConnector.PROP_PORT_RANGE_COUNT)).thenReturn(PORT_RANGE_COUNT);
    }

    @Test
    public void testCreate() throws Exception {
        FtpServer server = getFtpServer();

//        server.start();
//
//        assertTrue(!server.isStopped());
//
//        server.stop();
//
//        assertTrue(server.isStopped());

        assertNull(server);
    }

    private FtpServer getFtpServer() throws ListenerException {
        // TODO jwilliams: work out integration of Mockito and Spring with MockFtpServer

        return null;
    }
}
