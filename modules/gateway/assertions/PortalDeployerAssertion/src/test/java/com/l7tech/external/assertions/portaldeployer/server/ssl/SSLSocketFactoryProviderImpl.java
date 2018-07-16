package com.l7tech.external.assertions.portaldeployer.server.ssl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows having the KeyStore as streams to provide SSL support. This will allow us to dynamically add and remove host and/or keys w/o restarting the app server.
 *
 * @author rraquepo
 */
public class SSLSocketFactoryProviderImpl implements SSLSocketFactoryProvider {

  private static final Logger log = LoggerFactory.getLogger(SSLSocketFactoryProviderImpl.class);

  private SSLSocketFactory sslSocketFactory;  //our SSLSocketFactory object based on streams
  private KeyStore jks; //our Java KeyStore for CA Certs
  private CustomTrustManager tm; //TrustManager - CA Certs Manager
  private Map<String, KeyManager> keyManagerByAlias = new HashMap<String, KeyManager>(); //Map of our KeyManagers
  private Map<String, KeyManager> keyManagerByHost = new HashMap<String, KeyManager>(); //Map of our KeyManagers
  private SSLContext context = null;

  /**
   * Builds an empty SSL Socket Factory
   */
  public SSLSocketFactoryProviderImpl() throws SSLFactoryException {
    init(null, null);
  }

  public SSLSocketFactoryProviderImpl(InputStream trustStore, String trustStorePassword) throws SSLFactoryException {
    init(trustStore, trustStorePassword);
  }

  /**
   * Add a private key to support mutual auth
   */
  public boolean addPrivateKey(InputStream privateKeyStream, String privateKeyPassword) throws SSLFactoryException {
    return addPrivateKey("default", privateKeyStream, privateKeyPassword);
  }

  /**
   * Add a private key to specific host to support mutual auth for multiple hosts
   */
  public boolean addPrivateKey(String host, InputStream privateKeyStream, String privateKeyPassword) throws SSLFactoryException {
    if (privateKeyStream == null) {
      log.warn("privateKeyStream can't be null");
      return false;
    }
    try {
      KeyStore pks = KeyStore.getInstance("pkcs12");
      pks.load(privateKeyStream, privateKeyPassword == null ? null : privateKeyPassword.toCharArray());
      Enumeration aliasEnum = pks.aliases();
      String aliasEntry = null;
      while (aliasEnum.hasMoreElements()) {
        aliasEntry = (String) aliasEnum.nextElement();
        java.security.cert.Certificate cert = pks.getCertificate(aliasEntry);
        //Utils.dumpCert((X509Certificate) cert);
        break;
      }
      //check if the host entered is a URL, if so, we just need the hostname
      String hostname;
      if (host.indexOf('/') > 0) {
        URL url = new URL(host);
        hostname = url.getHost();
      } else {
        hostname = host;
      }
      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(pks, privateKeyPassword == null ? null : privateKeyPassword.toCharArray());
      keyManagerByAlias.put(aliasEntry, kmf.getKeyManagers()[0]);
      keyManagerByHost.put(hostname.toLowerCase(), kmf.getKeyManagers()[0]);
    } catch (IOException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    } catch (CertificateException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    } catch (UnrecoverableKeyException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    } catch (KeyStoreException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    }
    //rebuild the socketFactory since the KeyManager was updated using a new/updated privateKey
    buildSSLSocketFactory();
    return true;
  }

  /**
   * Adds an HTTPS host to our KeyStore
   * Returns true if https host was added, false if it's already trusted or can't add it
   */
  @Override
  public boolean addHost(String purl, String keystorePass) throws SSLFactoryException {
    purl = purl.toLowerCase();
    if (!purl.startsWith("https://")) {
      throw new SSLFactoryException("Can't process non HTTPS url");
    }
    purl = purl.replace("https://", "");
    String[] c = purl.split(":");
    String host = c[0];
    int port = (c.length == 1) ? 443 : Integer.parseInt(c[1]);
    log.debug("Opening connection to " + host + ":" + port + "...");
    SSLSocket socket = null;
    try {
      socket = (SSLSocket) sslSocketFactory.createSocket(host, port);
      socket.setSoTimeout(10000);
      log.debug("Starting SSL handshake...");
      socket.startHandshake();
      socket.close();
      log.debug("No errors, certificate is already trusted");
      return false;
    } catch (SSLException e) {
      //log.debug(e.getMessage());
      //e.printStackTrace();
    } catch (UnknownHostException e) {
      log.debug("SSL connection failed due to unknown host: " + e.getMessage());
      throw new SSLFactoryException("SSL connection failed due to unknown host: " + e.getMessage());
    } catch (SocketException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException("SSL connection refused. Check your port configuration.");
    } catch (IOException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    }

    X509Certificate[] chain = tm.chain;
    if (chain == null) {
      log.debug("Could not obtain server certificate chain");
      return false;
    }
    log.debug("Server sent " + chain.length + " certificate(s):");

    X509Certificate cert = chain[0];
    String alias = host + "-1";
    try {
      jks.setCertificateEntry(alias, cert);
      log.info("Added a new certificate entry...");
      //Utils.dumpCert(cert);
    } catch (KeyStoreException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    }

    //refresh the KeyStore stream
    try {
      OutputStream out = getKeystoreAsStream(keystorePass);
      InputStream in = new ByteArrayInputStream(((ByteArrayOutputStream) out).toByteArray());
      jks.load(in, keystorePass == null ? null : keystorePass.toCharArray());
    } catch (IOException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    } catch (CertificateException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    }
    buildSSLSocketFactory();
    return true;
  }

  /**
   * Returns true if a host is already trusted, false if not
   */
  @Override
  public boolean isHostTrusted(String purl) throws SSLFactoryException {
    purl = purl.toLowerCase();
    if (!purl.startsWith("https://")) {
      throw new SSLFactoryException("Can't process non HTTPS url");
    }
    purl = purl.replace("https://", "");
    String[] c = purl.split(":");
    String host = c[0];
    int port = (c.length == 1) ? 443 : Integer.parseInt(c[1]);
    log.debug("Opening connection to " + host + ":" + port + "...");
    SSLSocket socket = null;
    try {
      socket = (SSLSocket) sslSocketFactory.createSocket(host, port);
      socket.setSoTimeout(10000);
      log.debug("Starting SSL handshake...");
      socket.startHandshake();
      socket.close();
      log.debug("No errors, certificate is already trusted");
      return true;
    } catch (SSLException e) {
      return false;
    } catch (UnknownHostException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    } catch (SocketException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    } catch (IOException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    }
  }

  /**
   * Returns the SSLSocketFactory we can use for connecting to SSL endpoints
   */
  public SSLSocketFactory getSSLSocketFactory() {
    return sslSocketFactory;
  }

  /**
   * Adds an HTTPS host to our KeyStore
   * Returns true if https host was added, false if it's already trusted or can't add it
   */
  @Override
  public X509Certificate getCertificate(String purl) throws SSLFactoryException {
    purl = purl.toLowerCase();
    if (!purl.startsWith("https://")) {
      throw new SSLFactoryException("Can't process non HTTPS url");
    }
    purl = purl.replace("https://", "");
    String[] c = purl.split(":");
    String host = c[0];
    int port = (c.length == 1) ? 443 : Integer.parseInt(c[1]);
    log.debug("Opening connection to " + host + ":" + port + "...");
    SSLSocket socket = null;
    try {
      socket = (SSLSocket) sslSocketFactory.createSocket(host, port);
      socket.setSoTimeout(10000);
      log.debug("Starting SSL handshake...");
      socket.startHandshake();
      socket.close();
      log.debug("No errors, certificate is already trusted");
    } catch (SSLException e) {
      //log.debug(e.getMessage());
      //e.printStackTrace();
    } catch (UnknownHostException e) {
      log.debug("SSL connection failed due to unknown host: " + e.getMessage());
      throw new SSLFactoryException("SSL connection failed due to unknown host: " + e.getMessage());
    } catch (SocketException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException("SSL connection refused. Check your port configuration.");
    } catch (IOException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    }

    X509Certificate[] chain = tm.chain;
    if (chain == null) {
      String msg = "Could not obtain server certificate chain";
      log.debug(msg);
      throw new SSLFactoryException(msg);
    }
    log.debug("Server sent " + chain.length + " certificate(s):");

    return chain[0];
  }

  /**
   * Returns the keystore as an stream object for use like saving to a file or to a jcr node
   */
  @Override
  public OutputStream getKeystoreAsStream(String keystorePassword) throws SSLFactoryException {
    OutputStream out = new ByteArrayOutputStream();
    try {
      jks.store(out, keystorePassword == null ? null : keystorePassword.toCharArray());
    } catch (KeyStoreException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    } catch (IOException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    } catch (CertificateException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    }
    return out;
  }

  public HostnameVerifier getHostnameVerifier() {
    return new HostnameVerifier() {
      public boolean verify(String s, SSLSession sslSession) {
        return true;
      }
    };
  }

  public SSLContext getSslContext() throws SSLFactoryException {
    return context;
  }

  /**
   * builds & prepares the SSLSocketFactory base on the latest KeyStore object
   */
  protected void buildSSLSocketFactory() throws SSLFactoryException {
    try {
      //clean up old references
      tm = null;//TODO: do we need to synchronize?
      context = SSLContext.getInstance("TLS");
      TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(jks);
      X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
      tm = new CustomTrustManager(defaultTrustManager); //update our TrustManager object, which is used in adding https host

      context.init(new KeyManager[]{new CustomKeyManager(keyManagerByHost, keyManagerByAlias)}, new TrustManager[]{tm}, null);

      sslSocketFactory = context.getSocketFactory();
    } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
      throw new SSLFactoryException(e);
    }
  }

  protected void init(InputStream trustStore, String trustStorePassword) throws SSLFactoryException {
    try {
      if (jks == null) {
        log.debug("KeyStore not yet initialized... loading now");
        if (trustStore == null) {
          log.debug("Keystore stream is null, creating a fresh KeyStore object");
          jks = KeyStore.getInstance(KeyStore.getDefaultType());
          jks.load(null, trustStorePassword == null ? null : trustStorePassword.toCharArray());
        } else {
          log.debug("Using KeyStore stream");
          jks = KeyStore.getInstance(KeyStore.getDefaultType());
          jks.load(trustStore, trustStorePassword == null ? null : trustStorePassword.toCharArray());
        }
      }
    } catch (CertificateException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    } catch (KeyStoreException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    } catch (IOException e) {
      log.debug(e.getMessage(), e);
      throw new SSLFactoryException(e.getMessage());
    }
    buildSSLSocketFactory();
  }

  /**
   * This will allow us to trust and therefore be able to retrieve and save the SSL cert we want
   */
  private static class CustomTrustManager implements X509TrustManager {

    private final X509TrustManager tm;
    private X509Certificate[] chain;

    CustomTrustManager(X509TrustManager tm) {
      this.tm = tm;
    }

    public X509Certificate[] getAcceptedIssuers() {
      return tm.getAcceptedIssuers();
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      this.chain = chain;
      tm.checkClientTrusted(chain, authType);
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      this.chain = chain;
      tm.checkServerTrusted(chain, authType);
    }
  }

}
