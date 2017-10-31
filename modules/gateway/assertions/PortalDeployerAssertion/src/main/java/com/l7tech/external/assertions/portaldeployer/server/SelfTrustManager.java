package com.l7tech.external.assertions.portaldeployer.server;

import com.l7tech.common.io.CertUtils;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.security.cert.CertVerifier;
import com.l7tech.security.cert.KeyUsageException;
import com.l7tech.server.DefaultKey;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;
import javax.net.ssl.X509TrustManager;

/**
 * A TrustManager that trusts the provided SSL DefaultKey
 *
 * @author raqri01, 2017-10-30
 */
public class SelfTrustManager implements X509TrustManager {
  private static final Logger log = Logger.getLogger(SelfTrustManager.class.getName());
  private X509Certificate certificate;

  public SelfTrustManager(DefaultKey defaultKey) throws IOException {
    certificate = defaultKey.getSslInfo().getCertificate();
  }

  @Override
  public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
    throw new UnsupportedOperationException("This trust manager can only be used for outbound SSL connections");
  }

  @Override
  public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
    try {
      CertVerifier.verifyCertificateChain(x509Certificates, certificate);
    } catch (KeyUsageException e) {
      log.warning(e.getMessage()); // JSSE won't log it for us
      throw new KeyUsageException(e);
    } catch (CertUtils.CertificateUntrustedException e) {
      log.warning(e.getMessage()); // JSSE won't log it for us
      throw new ServerCertificateUntrustedException(e); 
    } catch (CertificateException e) {
      log.warning(e.getMessage()); // JSSE won't log it for us
      throw e;
    }
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return new X509Certificate[]{certificate};
  }
}
