package com.l7tech.external.assertions.portaldeployer.server.ssl;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * A custom SSLSocketFactory Provider for handling SSL certificates and Private Keys
 *
 * @author rraquepo
 */
public interface SSLSocketFactoryProvider {
  /**
   * Returns the SSLSocketFactory we can use for connecting to SSL endpoints
   */
  SSLSocketFactory getSSLSocketFactory();

  /**
   * Returns the keystore as an stream object for use like saving to a file or to a jcr node
   */
  OutputStream getKeystoreAsStream(String trustStorePassword) throws SSLFactoryException;

  /**
   * Adds an HTTPS host to our KeyStore
   * Returns true if https host was added, false if it's already trusted or can't add it
   */
  boolean addHost(String purl, String trustStorePassword) throws SSLFactoryException;

  /**
   * Add a private key to support mutual auth
   * Returns true if it was added/updated, false means it was not able to add it
   */
  public boolean addPrivateKey(InputStream privateKeyStream, String privateKeyPassword) throws SSLFactoryException;

  /**
   * Add a private key to specific host to support mutual auth for multiple hosts
   * Returns true if it was added/updated, false means it was not able to add it
   */
  public boolean addPrivateKey(String host, InputStream privateKeyStream, String privateKeyPassword) throws SSLFactoryException;

  /**
   * Returns true if a host is already trusted, false if not
   */
  boolean isHostTrusted(String purl) throws SSLFactoryException;

  /**
   * Returns a custom HostnameVerifier that's less strict
   */
  HostnameVerifier getHostnameVerifier();

  /**
   * Returns an SSL context for the current configuration.
   *
   * @return An SSL context for the current configuration.
   */
  SSLContext getSslContext() throws SSLFactoryException;

  /**
   * Returns the Certificate(SSL) for the given host
   *
   * @return An SSL context for the current configuration.
   */
  X509Certificate getCertificate(String purl) throws SSLFactoryException;
}
