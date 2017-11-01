package com.l7tech.external.assertions.portaldeployer.server;

import com.l7tech.objectmodel.FindException;
import com.l7tech.server.cluster.ClusterPropertyManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.context.ApplicationContext;

/**
 * Configuration manager for the Portal Deployer.
 */
public class PortalDeployerClientConfigurationManagerImpl implements PortalDeployerClientConfigurationManager {
  private static final Logger logger = Logger.getLogger(PortalDeployerClientConfigurationManagerImpl.class.getName());
  //properties coming from enrollment bundle
  public static final String PD_TENANT_ID_CP = "portal.config.name";
  public static final String PD_TSSG_UUID_CP = "portal.config.node.id";
  public static final String PD_BROKER_PROTOCOL_CP = "portal.config.broker.protocol";
  public static final String PD_BROKER_HOST_CP = "portal.config.broker.host";
  public static final String PD_APIM_HOST_CP = "portal.config.apim.host";
  //properties configured by this modass
  public static final String PD_ENABLED_CP = "portal.deployer.enabled";
  public static final String PD_BROKER_KEEP_ALIVE_CP = "portal.deployer.broker.keepalive";
  public static final String PD_BROKER_CLEAN_SESSION_CP = "portal.deployer.broker.cleansession";
  public static final String PD_TOPIC_CP = "portal.deployer.topic";
  //extendable property for MessageProcessor
  public static final String PD_DEPLOY_TARGET_LOCATION = "portal.deploy.target.location";//supports comma delimited host
  public static final String PD_DEPLOY_CALLBACK_LOCATION = "portal.deploy.callback.location";//supports comma delimited host
  public static final String PD_USE_GATEWAY_TRUST_MANAGER_FOR_TARGET = "portal.deploy.use.trust.manager.for.target";
  private String brokerHost;
  private String tenantId;
  private String tenantGatewayUuid;
  private boolean portalDeployerEnabled = false;
  //TODO: define as cluster properties that default to values
  private String mqttProtocol;
  private String clientId;
  private String topic;
  private int keepAlive = 30;
  private boolean cleanSession = false;
  private int connectTimeout = 60;
  private int keepAliveInterval = 30;

  private ClusterPropertyManager clusterPropertyManager;

  public PortalDeployerClientConfigurationManagerImpl(ApplicationContext context) {
    this.clusterPropertyManager = context.getBean("clusterPropertyManager", ClusterPropertyManager.class);
  }

  public String getBrokerHost() {
    return getClusterProperty(PD_BROKER_HOST_CP);
  }

  public String getIngressHost() {
    return getClusterProperty(PD_APIM_HOST_CP);
  }

  public String getTenantId() {
    return getClusterProperty(PD_TENANT_ID_CP);
  }

  public String getTenantGatewayUuid() {
    return getClusterProperty(PD_TSSG_UUID_CP);
  }

  public String getTargetLocation(String entity) {
    if (entity != null && entity.trim().length() > 0)
      return getClusterProperty(PD_DEPLOY_TARGET_LOCATION + "." + entity.trim().toLowerCase());
    else
      return getClusterProperty(PD_DEPLOY_TARGET_LOCATION);
  }

  public String getCallbackLocation(String entity) {
    if (entity != null && entity.trim().length() > 0)
      return getClusterProperty(PD_DEPLOY_CALLBACK_LOCATION + "." + entity.trim().toLowerCase());
    else
      return getClusterProperty(PD_DEPLOY_CALLBACK_LOCATION);
  }

  public boolean isPortalDeployerEnabled() {
    return Boolean.parseBoolean(getClusterProperty(PD_ENABLED_CP));
  }

  private String getClusterProperty(String clusterProperty) {
    try {
      return clusterPropertyManager.getProperty(clusterProperty);
    } catch (FindException e) {
      logger.log(Level.INFO, String.format("Unable to find ClusterProperty [%s]", clusterProperty));
    }
    return null;
  }

  public int getConnectTimeout() {
    return connectTimeout;
  }

  public int getKeepAliveInterval() {
    return keepAliveInterval;
  }
}
