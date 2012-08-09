package com.l7tech.common.io;

import javax.net.ssl.*;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class that can sniff server certificates from an SSL connection.
 * <p/>
 * Code moved here to common from TrustedCertAdminImpl since the Bridge GUI now uses it as well.
 */
public class SslCertificateSniffer {
    protected static final Logger logger = Logger.getLogger(SslCertificateSniffer.class.getName());

    public static class HostnameMismatchException extends Exception {
        private final String certname;
        private final String hostname;

        public HostnameMismatchException(String certname, String hostname) {
            super("SSL Certificate with DN '" + certname + "' does not match the expected hostname '" + hostname + "'");
            this.certname = certname;
            this.hostname = hostname;
        }

        public String getCertname() {
            return certname;
        }

        public String getHostname() {
            return hostname;
        }
    }

    public static X509Certificate[] retrieveCertFromUrl(String purl, boolean ignoreHostname)
      throws IOException, HostnameMismatchException {
        if (!purl.startsWith("https://")) throw new IllegalArgumentException("Can't load certificate from non-https URLs");

        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null,
              new X509TrustManager[]{new X509TrustManager() {
                  @Override
                  public X509Certificate[] getAcceptedIssuers() {
                      return new X509Certificate[0];
                  }

                  @Override
                  public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
                  }

                  @Override
                  public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
                  }
              }},
              null);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.INFO, e.getMessage(), e);
            throw new IOException(e.getMessage());
        } catch (KeyManagementException e) {
            logger.log(Level.INFO, e.getMessage(), e);
            throw new IOException(e.getMessage());
        }

        final String[] sslProtocols = sslContext.getSupportedSSLParameters().getProtocols();

        SSLException lastSSLException = null;
        for ( final String protocol : sslProtocols ) {
            try {
                return doRetrieveCertFromUrl( purl, ignoreHostname, sslContext, protocol );
            } catch ( SSLException se ) {
                lastSSLException = se;
            }
        }

        if ( lastSSLException != null ) {
            throw lastSSLException;
        } else {
            throw new SSLException( "No supported protocols" );
        }
    }

    private static X509Certificate[] doRetrieveCertFromUrl( final String purl,
                                                            final boolean ignoreHostname,
                                                            final SSLContext sslContext,
                                                            final String protocol ) throws IOException, HostnameMismatchException {
        final URL url = new URL(purl);
        final URLConnection gconn = url.openConnection();
        if (gconn instanceof HttpsURLConnection ) {
            HttpsURLConnection conn = (HttpsURLConnection)gconn;
            conn.setConnectTimeout( 20000 );
            conn.setSSLSocketFactory(new SSLSocketFactoryWrapper(sslContext.getSocketFactory()){
                @Override
                protected Socket notifySocket( final Socket socket ) {
                    if ( socket instanceof SSLSocket ) {
                        final SSLSocket sslSocket = (SSLSocket) socket;
                        sslSocket.setEnabledProtocols( new String[]{ protocol } );
                    }

                    return socket;
                }
            });
            final String[] sawHost = new String[] { null };
            if (ignoreHostname) {
                conn.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String s, SSLSession sslSession) {
                        sawHost[0] = s;
                        return true;
                    }
                });
            }

            try {
                conn.connect();
            } catch (IOException e) {
                logger.log( Level.INFO, "Unable to connect to: " + purl + " using " + protocol);

                // rethrow it
                throw e;
            }

            try {
                return (X509Certificate[])conn.getServerCertificates();
            } catch (IOException e) {
                logger.log(Level.WARNING, "SSL server hostname didn't match cert", e);
                if (e.getMessage().startsWith("HTTPS hostname wrong")) {
                    throw new HostnameMismatchException(sawHost[0], url.getHost());
                }
                throw e;
            }
        } else
            throw new IOException("URL resulted in a non-HTTPS connection");
    }
}
