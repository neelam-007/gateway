package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;

import java.util.Properties;
import java.util.Timer;

/**
 * User: rseminoff
 * Date: 25/05/12
 */
public class MockSoapFaultManager extends SoapFaultManager {
    public MockSoapFaultManager(final Config config, Timer timer) {
        super(config, timer);
    }

    public MockSoapFaultManager() {
        super(new ConfigFactory.DefaultConfig(new Properties(), 0), new Timer());
    }
}
