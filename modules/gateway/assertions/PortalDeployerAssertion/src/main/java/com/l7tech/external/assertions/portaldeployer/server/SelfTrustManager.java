package com.l7tech.external.assertions.portaldeployer.server;

import com.l7tech.common.io.CertUtils;
import com.l7tech.security.cert.CertVerifier;
import com.l7tech.server.DefaultKey;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

/**
 * @author raqri01, 2017-10-30
 */
public class SelfTrustManager implements X509TrustManager {

  private X509Certificate certificates;

  public SelfTrustManager(DefaultKey defaultKey) throws IOException {
    certificates = defaultKey.getSslInfo().getCertificate();
  }

  @Override
  public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
    try {
      CertVerifier.verifyCertificateChain(x509Certificates, certificates);
    } catch (CertUtils.CertificateUntrustedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
    try {
      CertVerifier.verifyCertificateChain(x509Certificates, certificates);
    } catch (CertUtils.CertificateUntrustedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return new X509Certificate[]{certificates};
  }
}
