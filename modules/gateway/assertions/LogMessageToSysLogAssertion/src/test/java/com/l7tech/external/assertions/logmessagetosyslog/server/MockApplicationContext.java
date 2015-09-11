package com.l7tech.external.assertions.logmessagetosyslog.server;

import com.l7tech.external.assertions.logmessagetosyslog.LogMessageToSysLogAssertion;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.audit.AuditContextStub;
import com.l7tech.server.log.TrafficLogger;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.SoapFaultManager;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: spreibisch
 * Date: 2/29/12
 * Time: 4:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class MockApplicationContext extends ClassPathXmlApplicationContext {

    public final static String SYSLOG_MANAGER_BEAN_NAME = "syslogManager";
    public final static String SINK_MANAGER_BEAN_NAME = "sinkManager";
    public final static String SERVER_CONFIG_BEAN_NAME = "serverConfig";

    private ServerConfigStub serverConfig;
    private SinkConfiguration sinkConfiguration;
    private MockSyslogManager syslogManager;
    private MockSinkManagerImpl sinkManager;
    private Map<String, Object> testBeans;

    public MockApplicationContext() {

        serverConfig = new ServerConfigStub();

        sinkConfiguration = createSinkConfig(new Goid(1L, 1001L));
        sinkConfiguration.setEnabled(false);

        syslogManager = new MockSyslogManager();

        sinkManager = new MockSinkManagerImpl(serverConfig,
                syslogManager,
                new TrafficLogger(serverConfig,
                        new SoapFaultManager(serverConfig, null)),
                new ApplicationEventProxy());

        sinkManager.addSinkConfiguration(sinkConfiguration);
        
        testBeans = new HashMap<String, Object>();
        testBeans.put(SYSLOG_MANAGER_BEAN_NAME, syslogManager);
        testBeans.put(SINK_MANAGER_BEAN_NAME, sinkManager);
        testBeans.put(SERVER_CONFIG_BEAN_NAME, serverConfig);
    }

    public Object getBean(String beanName) {
        return testBeans.get(beanName);
    }

    public SinkConfiguration getSinkConfiguration() {
        return sinkConfiguration;
    }

    public MockSyslogManager getSyslogManager() {
        return syslogManager;
    }

    private static SinkConfiguration createSinkConfig(Goid goid) {
        SinkConfiguration cfg = new SinkConfiguration();

        cfg.setGoid(goid);
        cfg.setType(SinkConfiguration.SinkType.SYSLOG);
        cfg.setName(LogMessageToSysLogAssertion.SYSLOG_LOG_SINK_PREFIX + "_JUnit-SinkManagerImplTest-Syslog");
        cfg.setSeverity(SinkConfiguration.SeverityThreshold.INFO);
        cfg.setCategories("LOG,AUDIT");
        cfg.syslogHostList().add("localhost:514");

        cfg.setProperty("syslog.ssl.key.alias", null);
        cfg.setProperty("syslog.protocol", "UDP"); // UDP or TCP or SSL
        cfg.setProperty("syslog.logHostname", "true");
        cfg.setProperty("syslog.timezone", null);
        cfg.setProperty("syslog.facility", "17");
        cfg.setProperty("syslog.ssl.keystore.id", null);
        cfg.setProperty("syslog.ssl.clientAuth", "false");
        cfg.setProperty("syslog.charSet", "LATIN-1"); // UTF-8 or ASCII or LATIN-1

        return cfg;
    }
}
