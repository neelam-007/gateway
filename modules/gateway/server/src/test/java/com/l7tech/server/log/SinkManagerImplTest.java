package com.l7tech.server.log;

import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.AuditContextStub;
import com.l7tech.server.log.syslog.SyslogManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.SoapFaultManager;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;

/**
 * Test case(s) for SinkManagerImpl class.  Currently on configured to test Syslog sinks which requires
 * a configures syslog server to talk to.
 *
 * User: vchan
 */
@Ignore("This test requires a Syslog server configured")
public class SinkManagerImplTest extends TestCase {

    private static final String TEST_MSG_PREFIX = "JUnit test message - SinkManagerImplTest";

    private ApplicationContext _appCtx;
    private ServerConfig _config;
    private SinkManager _sinkMgr;

    @Before
    protected void setUp() throws Exception {

        if (_appCtx == null)
            _appCtx = ApplicationContexts.getTestApplicationContext();

        if (_config == null)
            _config = ServerConfig.getInstance();

        if (_sinkMgr == null)
            _sinkMgr = new SinkManagerImpl(_config,
                                           new SyslogManager(),
                                           new TrafficLogger(_config,
                                           new SoapFaultManager(_config, new AuditContextStub(), null)),
                                           new ApplicationEventProxy());
    }

    public void testInitialized() {

        try {
            assertNotNull(_sinkMgr);

        } catch (Exception ex) {
            fail("Unexpected exception encountered: " + ex);
        }
    }

    @Test
    public void testSendTestMessage() {
        try {

            boolean result = _sinkMgr.test( createSinkConfig(testTCPHosts), TEST_MSG_PREFIX+".testSendTestMessage(): (1)" );
            try {
                Thread.sleep(15000L);
            } catch (InterruptedException iex) {}

            System.out.println("testSendTestMessage() result: " + result);

        } catch (Exception ex) {
            fail("Unexpected exception encountered: " + ex);
        }
    }


    private static final ArrayList<String> testEmptyHosts = new ArrayList<String>();
    private static final ArrayList<String> testTCPHosts = new ArrayList<String>();
    static {
        testTCPHosts.add("vctools.l7tech.com:514");
        testTCPHosts.add("vctools.l7tech.com:515");
        testTCPHosts.add("vctools.l7tech.com:514");
    }
    
    private SinkConfiguration createSinkConfig(ArrayList<String> hostList) {
        SinkConfiguration cfg = new SinkConfiguration();

        cfg.setType(SinkConfiguration.SinkType.SYSLOG);
        cfg.setName("JUnit-SinkManagerImplTest-Syslog");
        cfg.setSeverity(SinkConfiguration.SeverityThreshold.INFO);
        cfg.setCategories("LOG,AUDIT");
        cfg.syslogHostList().addAll(hostList);

        cfg.setProperty("syslog.ssl.key.alias", null);
        cfg.setProperty("syslog.protocol", "TCP"); // UDP or TCP or SSL
        cfg.setProperty("syslog.logHostname", "true");
        cfg.setProperty("syslog.timezone", null);
        cfg.setProperty("syslog.facility", "1");
        cfg.setProperty("syslog.ssl.keystore.id", null);
        cfg.setProperty("syslog.ssl.clientAuth", "false");
        cfg.setProperty("syslog.charSet", "LATIN-1"); // UTF-8 or ASCII or LATIN-1

        return cfg;
    }
}
