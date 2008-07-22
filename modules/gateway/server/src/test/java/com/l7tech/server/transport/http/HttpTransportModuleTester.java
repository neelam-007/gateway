package com.l7tech.server.transport.http;

import com.l7tech.gateway.common.transport.SsgConnector;

/**
 * Utilities for configuring a test-mode HttpTransportModule
 */
public class HttpTransportModuleTester {
    public static void setGlobalTransportModule(HttpTransportModule global) {
        HttpTransportModule.testMode = true;
        HttpTransportModule.testModule = global;
    }

    public static void setGlobalConnector(SsgConnector connector) {
        HttpTransportModule.testMode = true;
        HttpTransportModule.testConnector = connector;
    }
}
