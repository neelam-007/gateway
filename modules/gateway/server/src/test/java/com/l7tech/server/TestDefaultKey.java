package com.l7tech.server;

import com.l7tech.common.TestDocuments;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.util.Pair;

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
    SsgKeyEntry auditViewerInfo;

    public TestDefaultKey() throws Exception {
        super(null, null, null, null);
        X509Certificate cert = TestDocuments.getDotNetServerCertificate();
        PrivateKey key = TestDocuments.getDotNetServerPrivateKey();
        sslInfo = makeFakeKeyEntry(cert, key);
        caInfo = sslInfo;
    }

    public TestDefaultKey(SsgKeyEntry sslInfo) {
        super(null, null, null, null);
        this.sslInfo = sslInfo;
        this.caInfo = sslInfo;
    }

    public TestDefaultKey(SsgKeyEntry sslInfo, SsgKeyEntry caInfo) {
        super(null, null, null, null);
        this.sslInfo = sslInfo;
        this.caInfo = caInfo;
    }

    public TestDefaultKey(X509Certificate sslCert, PrivateKey sslKey) {
        super(null, null, null, null);
        this.sslInfo = makeFakeKeyEntry(sslCert, sslKey);
        this.caInfo = sslInfo;
    }

    public TestDefaultKey(Pair<X509Certificate, PrivateKey> certAndKey) {
        this(certAndKey.left, certAndKey.right);
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

    @Override
    public SsgKeyEntry getAuditViewerInfo() {
        return auditViewerInfo;
    }

    @Override
    public Pair<Goid, String> getAuditViewerAlias() {
        return auditViewerInfo == null ? null : new Pair<Goid, String>(auditViewerInfo.getKeystoreId(), auditViewerInfo.getAlias());
    }

    public void setCaInfo(SsgKeyEntry caInfo) {
        this.caInfo = caInfo;
    }

    public void setAuditViewerInfo(SsgKeyEntry info) {
        this.auditViewerInfo = info;
    }

    private SsgKeyEntry makeFakeKeyEntry(X509Certificate cert, PrivateKey key) {
        return new SsgKeyEntry(GoidEntity.DEFAULT_GOID, "SSL", new X509Certificate[] {cert}, key);
    }
}
