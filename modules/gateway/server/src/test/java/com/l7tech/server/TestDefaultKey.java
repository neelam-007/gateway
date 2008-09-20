package com.l7tech.server;

import com.l7tech.common.TestDocuments;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.server.audit.AuditContextStub;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Stub implementation of DefaultKey for testing.
 * Always uses {@link com.l7tech.common.TestDocuments#getDotNetServerPrivateKey()}
 * as both the SSL and CA private keys.
 */
public class TestDefaultKey extends DefaultKeyImpl {
    SsgKeyEntry sslInfo;
    SsgKeyEntry caInfo;

    public TestDefaultKey() throws Exception {
        super(null, new AuditContextStub(), null, null);
        X509Certificate cert = TestDocuments.getDotNetServerCertificate();
        PrivateKey key = TestDocuments.getDotNetServerPrivateKey();
        sslInfo = makeFakeKeyEntry(cert, key);
        caInfo = sslInfo;
    }

    public TestDefaultKey(SsgKeyEntry sslInfo) {
        super(null, new AuditContextStub(), null, null);
        this.sslInfo = sslInfo;
        this.caInfo = sslInfo;
    }

    public TestDefaultKey(SsgKeyEntry sslInfo, SsgKeyEntry caInfo) {
        super(null, new AuditContextStub(), null, null);
        this.sslInfo = sslInfo;
        this.caInfo = caInfo;
    }

    public TestDefaultKey(X509Certificate sslCert, PrivateKey sslKey) {
        super(null, new AuditContextStub(), null, null);
        this.sslInfo = makeFakeKeyEntry(sslCert, sslKey);
        this.caInfo = sslInfo;
    }

    public SsgKeyEntry getSslInfo() {
        return sslInfo;
    }

    public void setSslInfo(SsgKeyEntry sslInfo) {
        this.sslInfo = sslInfo;
    }

    public SsgKeyEntry getCaInfo() {
        return caInfo;
    }

    public void setCaInfo(SsgKeyEntry caInfo) {
        this.caInfo = caInfo;
    }

    private SsgKeyEntry makeFakeKeyEntry(X509Certificate cert, PrivateKey key) {
        return new SsgKeyEntry(-1, "SSL", new X509Certificate[] {cert}, key);
    }
}
