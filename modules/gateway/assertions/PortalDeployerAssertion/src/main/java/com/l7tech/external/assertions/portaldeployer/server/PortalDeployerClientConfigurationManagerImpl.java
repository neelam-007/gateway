package com.l7tech.external.assertions.portaldeployer.server;

import com.l7tech.objectmodel.FindException;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.cluster.ClusterPropertyManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.context.ApplicationContext;

/**
 * Configuration manager for the Portal Deployer.
 */
public class PortalDeployerClientConfigurationManagerImpl implements PortalDeployerClientConfigurationManager {
  private static final Logger logger = Logger.getLogger(PortalDeployerClientConfigurationManagerImpl.class.getName());

  static final int PD_BROKER_KEEP_ALIVE_DEFAULT = 30;
  static final int PD_BROKER_CONNECTION_TIMEOUT_DEFAULT = 60;
  static final boolean PD_BROKER_CLEAN_SESSION_DEFAULT = false;
  static final String PD_BROKER_PROTOCOL_DEFAULT = "wss";

  //properties coming from enrollment bundle
  static final String PD_TENANT_ID_CP = "portal.config.name";
  static final String PD_TSSG_UUID_CP = "portal.config.node.id";
  static final String PD_BROKER_HOST_CP = "portal.config.broker.host";
  static final String PD_BROKER_PORT_CP = "portal.config.broker.port";
  static final String PD_APIM_HOST_CP = "portal.config.apim.host";
  static final String PD_APIM_PORT_CP = "portal.config.apim.port";
  //properties configured by this modass
  static final String PD_ENABLED_CP = "portal.deployer.enabled";
  static final String PD_BROKER_PROTOCOL_CP = "portal.config.broker.protocol";
  static final String PD_BROKER_KEEP_ALIVE_CP = "portal.deployer.broker.keepalive";
  static final String PD_BROKER_CLEAN_SESSION_CP = "portal.deployer.broker.cleansession";
  static final String PD_BROKER_CONNECTION_TIMEOUT_CP = "portal.deployer.broker.connectiontimeout";
  static final String PD_TOPIC_CP = "portal.deployer.topic";
  //extendable property for MessageProcessor
  public static final String PD_DEPLOY_TARGET_LOCATION = "portal.deploy.target.location";//supports comma delimited host
  public static final String PD_DEPLOY_CALLBACK_LOCATION = "portal.deploy.callback.location";//supports comma delimited host
  public static final String PD_USE_GATEWAY_TRUST_MANAGER_FOR_TARGET = "portal.deploy.use.trust.manager.for.target";

  private ClusterPropertyManager clusterPropertyManager;
  private ClusterInfoManager clusterInfoManager;

  public PortalDeployerClientConfigurationManagerImpl(ApplicationContext context) {
    this.clusterPropertyManager = context.getBean("clusterPropertyManager", ClusterPropertyManager.class);
    this.clusterInfoManager = context.getBean("clusterInfoManager", ClusterInfoManager.class);
  }

  /**
   * The id to uniquely identify this instance of Portal Deployer if tenant ID and tenant Gateway UUID are not enough.
   */
  public String getUniqueClientId() {
    return clusterInfoManager.thisNodeId();
  }

  /**
   * Defaults to wss if unspecified.
   */
  public String getBrokerProtocol() {
    String protocol = getClusterProperty(PD_BROKER_PROTOCOL_CP);
    if(protocol == null) {
      protocol = PD_BROKER_PROTOCOL_DEFAULT;
    }
    return protocol;
  }

  /**
   * Defaults to 30 if unspecified.
   */
  public int getBrokerKeepAlive() {
    int keepAlive = PD_BROKER_KEEP_ALIVE_DEFAULT;
    try{
      keepAlive = Integer.parseInt(getClusterProperty(PD_BROKER_KEEP_ALIVE_CP));
    } catch(NumberFormatException e) {
      logger.log(Level.INFO, String.format("Invalid keep alive value for ClusterProperty [%s]",
              PD_BROKER_KEEP_ALIVE_CP));
    }
    return keepAlive;
  }

  /**
   * Defaults to 30 if unspecified.
   */
  public int getBrokerConnectionTimeout() {
    int keepAlive = PD_BROKER_CONNECTION_TIMEOUT_DEFAULT;
    try{
      keepAlive = Integer.parseInt(getClusterProperty(PD_BROKER_CONNECTION_TIMEOUT_CP));
    } catch(NumberFormatException e) {
      logger.log(Level.INFO, String.format("Invalid keep alive value for ClusterProperty [%s]",
              PD_BROKER_CONNECTION_TIMEOUT_CP));
    }
    return keepAlive;
  }

  /**
   * Defaults to false if unspecified.
   */
  public boolean getBrokerCleanSession() {
    String cleanSession = getClusterProperty(PD_BROKER_CLEAN_SESSION_CP);
    if(cleanSession == null) {
      return PD_BROKER_CLEAN_SESSION_DEFAULT;
    }
    return Boolean.parseBoolean(cleanSession);
  }

  public String getBrokerPort() {
    return getClusterProperty(PD_BROKER_PORT_CP);
  }

  public String getBrokerHost() {
    return getClusterProperty(PD_BROKER_HOST_CP);
  }

  public String getIngressHost() {
    return getClusterProperty(PD_APIM_HOST_CP);
  }

  public String getIngressPort() {
    return getClusterProperty(PD_APIM_PORT_CP);
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
}
