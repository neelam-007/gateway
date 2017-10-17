package com.l7tech.external.assertions.portaldeployer.server;

import com.l7tech.common.io.SSLSocketFactoryWrapper;
import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.external.assertions.portaldeployer.server.client.PortalDeployerClient;
import com.l7tech.external.assertions.portaldeployer.server.client.PortalDeployerClientException;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.admin.AdminEvent;
import com.l7tech.server.event.admin.ClusterPropertyEvent;
import com.l7tech.server.event.admin.Created;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.transport.http.SslClientTrustManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.ExceptionUtils;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * Created by BAGJO04 on 2017-09-29.
 */
public class PortalDeployerModuelLoadListener implements ApplicationListener {
  private static final Logger logger = Logger.getLogger(PortalDeployerModuelLoadListener.class.getName());
  public static final String PD_ENABLED_CP = "portal.deployer.enabled";
  public static final String PD_BROKER_HOST_CP = "portal.deployer.broker.host";
  public static final String PD_TOPIC_CP = "portal.deployer.topic";
  public static final String PD_TENANT_ID_CP = "portal.config.name";
  public static final String PD_TSSG_UUID_CP = "portal.config.node.id";

  public static synchronized void onModuleLoaded(@NotNull ApplicationContext context) {
    logger.info("************** PortalDeployer loaded");
    if (instance == null) {
      instance = new PortalDeployerModuelLoadListener(context);
    }
  }

  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    if (event instanceof AdminEvent) {
      if (event instanceof Created && ((Created) event).getEntity() instanceof ClusterProperty) {
        ClusterProperty clusterProperty = (ClusterProperty) ((Created) event).getEntity();
        logger.log(Level.INFO, String.format("************** PortalDeployer onApplicationEvent created " +
                "ClusterProperty: %s", clusterProperty.toString()));
      } else if (event instanceof Updated && ((Updated) event).getEntity() instanceof ClusterProperty) {
        ClusterProperty clusterProperty = (ClusterProperty) ((Updated) event).getEntity();
        logger.log(Level.INFO, String.format("************** PortalDeployer onApplicationEvent updated " +
                "ClusterProperty: %s", clusterProperty.toString()));
        if (clusterProperty.getName().equalsIgnoreCase(PD_ENABLED_CP)) {
          handleUpdateOfPortalDeployerEnabledClusterProperty(clusterProperty.getValue());
        }
      }
    }
  }

  private void handleUpdateOfPortalDeployerEnabledClusterProperty(String newValue) {
    if (Boolean.FALSE.toString().equalsIgnoreCase(newValue)) {
      logger.log(Level.INFO, "Stopping Portal Deployer MQTT Client");
    } else if (Boolean.TRUE.toString().equalsIgnoreCase(newValue)) {
      logger.log(Level.INFO, "Starting Portal Deployer MQTT Client");
      instance.initClusterProperties();
      instance.initSslFactory();
      instance.initPortalDeployerClient();
    }
  }

  private String getClusterProperty(String clusterProperty) {
    try {
      return clusterPropertyManager.getProperty(clusterProperty);
    } catch (FindException e) {
      logger.log(Level.WARNING, String.format("Unable to find ClusterProperty [%s]", clusterProperty));
    }
    return null;
  }

  private static PortalDeployerModuelLoadListener instance = null;
  private static ClusterPropertyManager clusterPropertyManager;
  private final ApplicationEventProxy applicationEventProxy;
  private SsgKeyStoreManager ssgKeyStoreManager;
  private SslClientTrustManager trustManager;
  private SSLSocketFactory sslSocketFactory;
  private String mqttProtocol;
  private String brokerHost;
  private String clientId;
  private String topic;
  private String tenantId;
  private String tenantGatewayUuid;
  private int keepAlive = 30;
  private boolean cleanSession = false;
  private boolean portalDeployerEnabled = false;

  private PortalDeployerClient portalDeployerClient;

  PortalDeployerModuelLoadListener(final ApplicationContext context) {
    applicationEventProxy = context.getBean("applicationEventProxy", ApplicationEventProxy.class);
    applicationEventProxy.addApplicationListener(this);
    clusterPropertyManager = context.getBean("clusterPropertyManager", ClusterPropertyManager.class);
    ssgKeyStoreManager = context.getBean("ssgKeyStoreManager", SsgKeyStoreManager.class);
    trustManager = context.getBean("trustManager", SslClientTrustManager.class);

    initClusterProperties();
    initSslFactory();
    initPortalDeployerClient();
  }

  void initPortalDeployerClient() {
    try {
      portalDeployerClient = new PortalDeployerClient(
              String.format("wss://%s/", this.brokerHost),
              String.format("%s-%s", this.tenantId, this.tenantGatewayUuid),
              String.format("%s/%s/deploy", this.tenantId, this.tenantGatewayUuid),
              sslSocketFactory);

      portalDeployerClient.startClient();
    } catch (PortalDeployerClientException e) {
      logger.log(Level.WARNING, ExceptionUtils.getMessage(e), e);
    }
  }

  boolean initSslFactory() {
    try {
      SsgKeyEntry portalmanKey = ssgKeyStoreManager.lookupKeyByKeyAlias("portalman", PersistentEntity.DEFAULT_GOID);
      KeyManager keyManager = new SingleCertX509KeyManager(portalmanKey.getCertificateChain(), portalmanKey
              .getPrivate(), portalmanKey.getAlias());
      SSLContext sc = SSLContext.getInstance("TLSv1.2");
      sc.init(new KeyManager[]{keyManager}, new TrustManager[]{trustManager}, JceProvider.getInstance()
              .getSecureRandom());
      sslSocketFactory = new SSLSocketFactoryWrapper(sc.getSocketFactory()) {
        @Override
        protected Socket notifySocket(final Socket socket) {
          if (socket instanceof SSLSocket && brokerHost != null) {
            final SSLSocket sslSocket = (SSLSocket) socket;
            SSLParameters params = sslSocket.getSSLParameters();
            params.setServerNames(Collections.singletonList(new SNIHostName(brokerHost)));
            sslSocket.setSSLParameters(params);
          }
          return socket;
        }
      };
    } catch (FindException e) {
      logger.log(Level.WARNING, "Unable to find portalman key: " + ExceptionUtils.getMessage(e), e);
      return false;
    } catch (KeyStoreException e) {
      logger.log(Level.WARNING, "Unable to look up portalman key: " + ExceptionUtils.getMessage(e), e);
      return false;
    } catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException e) {
      logger.log(Level.WARNING, ExceptionUtils.getMessage(e), e);
      return false;
    }
    return true;
  }

  void initClusterProperties() {
    brokerHost = getClusterProperty(PD_BROKER_HOST_CP);
    tenantId = getClusterProperty(PD_TENANT_ID_CP);
    tenantGatewayUuid = getClusterProperty(PD_TSSG_UUID_CP);
    portalDeployerEnabled = Boolean.parseBoolean(getClusterProperty(PD_ENABLED_CP));

    logger.log(Level.INFO, String.format("brokerHost [%s], " + "tenantId [%s], " + "tenantGatewayUuid [%s], " +
            "portalDeployerEnabled [%s]", brokerHost, tenantId, tenantGatewayUuid, portalDeployerEnabled));
  }

  public String getBrokerHost() {
    return brokerHost;
  }

  public void setBrokerHost(String brokerHost) {
    this.brokerHost = brokerHost;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public String getMqttProtocol() {
    return mqttProtocol;
  }

  public void setMqttProtocol(String mqttProtocol) {
    this.mqttProtocol = mqttProtocol;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getTenantGatewayUuid() {
    return tenantGatewayUuid;
  }

  public void setTenantGatewayUuid(String tenantGatewayUuid) {
    this.tenantGatewayUuid = tenantGatewayUuid;
  }

  public int getKeepAlive() {
    return keepAlive;
  }

  public void setKeepAlive(int keepAlive) {
    this.keepAlive = keepAlive;
  }

  public boolean isCleanSession() {
    return cleanSession;
  }

  public void setCleanSession(boolean cleanSession) {
    this.cleanSession = cleanSession;
  }

  public boolean isPortalDeployerEnabled() {
    return portalDeployerEnabled;
  }

  public void setPortalDeployerEnabled(boolean portalDeployerEnabled) {
    this.portalDeployerEnabled = portalDeployerEnabled;
  }

  public SSLSocketFactory getSslSocketFactory() {
    return sslSocketFactory;
  }

  public void setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
    this.sslSocketFactory = sslSocketFactory;
  }
}
