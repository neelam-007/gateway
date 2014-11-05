package com.l7tech.common.io;

import com.l7tech.util.ExceptionUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

        final List<String> sslProtocols = new ArrayList<String>();
        sslProtocols.addAll(Arrays.asList(sslContext.getSupportedSSLParameters().getProtocols()));

        // SSG-9624 - move SSLv3 to the end of the list so that it is only tried after protocols that are less likely to cause problems
        if ( sslProtocols.contains("SSLv3") ) {
            sslProtocols.remove("SSLv3");
            sslProtocols.add("SSLv3");
        }

        Exception lastException = null;
        for ( final String protocol : sslProtocols ) {
            if ("SSLv2Hello".equals(protocol))
                continue;
            try {
                return doRetrieveCertFromUrl( purl, ignoreHostname, sslContext, protocol );
            } catch ( SSLException e ) {
                // For SSLException, assume handshake failure and try again with one of the other TLS versions
                lastException = e;
            } catch ( Exception e ) {
                // For any other exception type (including SocketTimeoutException), record the failure and give up
                lastException = e;
                break;
            }
        }

        if ( lastException instanceof SSLException ) {
            throw (SSLException) lastException;
        } else if ( lastException != null ) {
            throw new SSLException( "Error: " + ExceptionUtils.getMessage(lastException) );
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
                logger.log( Level.INFO, "Unable to connect to: " + purl + " using " + protocol + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e) );

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
