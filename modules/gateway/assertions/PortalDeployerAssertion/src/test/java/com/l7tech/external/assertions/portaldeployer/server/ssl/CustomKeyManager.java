package com.l7tech.external.assertions.portaldeployer.server.ssl;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomKeyManager implements X509KeyManager {

  private static final Logger log = LoggerFactory.getLogger(CustomKeyManager.class);

  private Map<String, KeyManager> keyManagersByHost;
  private Map<String, KeyManager> keyManagersByAlias;

  public CustomKeyManager(Map<String, KeyManager> keyManagersByHost, Map<String, KeyManager> keyManagersByAlias) {
    this.keyManagersByHost = Collections.unmodifiableMap(keyManagersByHost);
    this.keyManagersByAlias = Collections.unmodifiableMap(keyManagersByAlias);
  }

  /**
   * Chooses the client alias found that is mapped to a particular host
   */
  @Override
  public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
    String host = socket.getInetAddress().getHostName().toLowerCase();
    X509KeyManager keyManager = (X509KeyManager) keyManagersByHost.get(host);
    if (keyManager == null) { //if no mapping found by host, try if a private key was added as default
      log.debug("Unable to find PrivateKey by host - " + host + ". Trying default...");
      keyManager = (X509KeyManager) keyManagersByHost.get("default");
    }
    if (keyManager == null) {
      log.debug("Unable to find a default PrivateKey");
    }
    if ("PORTAL_ALLOW_PRIVATE_KEY_HOST_MISMATCH".equals(System.getenv("PORTAL_INSTANCE_ID"))) {
      if (keyManager == null) {//if still null, and there is only one key anyways, use that.
        int size = keyManagersByHost.size();
        log.debug("Checking if there are more than 1 PrivateKey..." + size);
        if (size == 1) {
          for (Map.Entry entry : keyManagersByHost.entrySet()) {
            log.debug("There is exactly one, so using PrivateKey added for " + entry.getKey());
            keyManager = (X509KeyManager) entry.getValue();
          }
        } else {
          log.debug("Unable to figure out a proper PrivateKey. Not attaching any PrivateKey to the request.");
        }
      }
    }
    if (keyManager != null) {
      String alias = keyManager.chooseClientAlias(keyType, issuers, socket);
      if (alias != null) {
        return alias;
      }
    }
    return null;
  }

  /**
   * Chooses the server alias found that is mapped to a particular host
   */
  @Override
  public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
    String host = socket.getInetAddress().getHostName().toLowerCase();
    X509KeyManager keyManager = (X509KeyManager) keyManagersByHost.get(host);
    if (keyManager == null) { //if no mapping found by host, try if a private key was added as default
      keyManager = (X509KeyManager) keyManagersByHost.get("default");
    }
    if (keyManager != null) {
      String alias = keyManager.chooseServerAlias(keyType, issuers, socket);
      if (alias != null) {
        return alias;
      }
    }
    return null;
  }

  /**
   * Get the Private Key that is mapped to a particular alias
   */
  @Override
  public PrivateKey getPrivateKey(String alias) {
    X509KeyManager keyManager = (X509KeyManager) keyManagersByAlias.get(alias);
    if (keyManager != null) {
      PrivateKey privateKey = keyManager.getPrivateKey(alias);
      if (privateKey != null) {
        return privateKey;
      }
    }
    return null;
  }

  /**
   * Get the Certificate Chain that is mapped to a particular alias
   */
  @Override
  public X509Certificate[] getCertificateChain(String alias) {
    X509KeyManager keyManager = (X509KeyManager) keyManagersByAlias.get(alias);
    if (keyManager != null) {
      X509Certificate[] chain = keyManager.getCertificateChain(alias);
      if (chain != null && chain.length > 0) {
        return chain;
      }
    }
    return null;
  }

  @Override
  public String[] getClientAliases(String keyType, Principal[] issuers) {
    List<String[]> aliases = new ArrayList<String[]>();
    for (KeyManager keyManager : keyManagersByAlias.values()) {
      aliases.add(((X509KeyManager) keyManager).getClientAliases(keyType, issuers));
    }
    if (aliases.size() == 0) {
      return null;
    }
    List<String> aliasflatten = new ArrayList<String>();
    for (String[] alias : aliases) {
      for (String str : alias) {
        aliasflatten.add(str);
      }
    }
    return aliasflatten.toArray(new String[aliasflatten.size()]);
  }

  @Override
  public String[] getServerAliases(String keyType, Principal[] issuers) {
    List<String[]> aliases = new ArrayList<String[]>();
    for (KeyManager keyManager : keyManagersByAlias.values()) {
      aliases.add(((X509KeyManager) keyManager).getServerAliases(keyType, issuers));
    }
    if (aliases.size() == 0) {
      return null;
    }
    List<String> aliasflatten = new ArrayList<String>();
    for (String[] alias : aliases) {
      for (String str : alias) {
        aliasflatten.add(str);
      }
    }
    return aliasflatten.toArray(new String[aliasflatten.size()]);
  }

}