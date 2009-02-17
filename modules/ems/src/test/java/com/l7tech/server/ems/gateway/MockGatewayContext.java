package com.l7tech.server.ems.gateway;

import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.TestDefaultKey;

import java.security.GeneralSecurityException;

/**
 *
 */
public class MockGatewayContext extends GatewayContext {
    public MockGatewayContext(String esmId, String gatewaySslHostname, String userId) throws GeneralSecurityException {
        super(new TestDefaultKey(new TestCertificateGenerator().generateWithKey()), gatewaySslHostname, 8443, esmId, userId);
    }

    @SuppressWarnings({"unchecked"})
    protected <T> T initApi(Class<T> apiClass, DefaultKey defaultKey, String url) {
        return null;
    }
}
