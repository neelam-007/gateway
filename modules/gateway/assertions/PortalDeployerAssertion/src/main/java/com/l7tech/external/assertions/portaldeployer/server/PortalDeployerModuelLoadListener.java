package com.l7tech.external.assertions.portaldeployer.server;

import static com.l7tech.external.assertions.portaldeployer.server.PortalDeployerClientConfigurationManagerImpl.*;
import com.l7tech.external.assertions.portaldeployer.server.client.PortalDeployerClient;
import com.l7tech.external.assertions.portaldeployer.server.client.PortalDeployerClientException;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.GatewayState;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.admin.AdminEvent;
import com.l7tech.server.event.admin.Created;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.ExceptionUtils;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * Class that runs when the Portal Deployer modass is loaded. onModuleLoaded is executed via its binding specified
 * in {@link com.l7tech.external.assertions.portaldeployer.PortalDeployerAssertion} metadata.
 */
public class PortalDeployerModuelLoadListener implements ApplicationListener {
  private static final Logger logger = Logger.getLogger(PortalDeployerModuelLoadListener.class.getName());

  /**
   * Initializes portal deployer dependencies and starts a client if portal.deployer.enabled cluster property is
   * set to true.
   * @param context
   */
  public static synchronized void onModuleLoaded(@NotNull ApplicationContext context) {
    logger.log(Level.FINE, "PortalDeployer onModuleLoaded executing");
    if (instance == null) {
      instance = new PortalDeployerModuelLoadListener(context);
    }

    //only try and start portal deployer if gateway is fully started
    GatewayState gatewayState = context.getBean("gatewayState", GatewayState.class);
    if (gatewayState.isReadyForMessages()) {
      checkIfPortalDeployerEnabled();
    } else {
      applicationEventProxy.addApplicationListener(new ApplicationListener() {
        @Override
        public void onApplicationEvent(ApplicationEvent event) {
          if (event instanceof ReadyForMessages) {
            checkIfPortalDeployerEnabled();
          }
        }
      });
    }

  }

  private static PortalDeployerModuelLoadListener instance = null;
  private static ClusterPropertyManager clusterPropertyManager;
  private static ApplicationEventProxy applicationEventProxy;

  private PortalDeployerClient portalDeployerClient;
  private PortalDeployerClientConfigurationManager configurationManager;
  private PortalDeployerSslConfigurationManager sslConfigurationManager;

  PortalDeployerModuelLoadListener(final ApplicationContext context) {
    clusterPropertyManager = context.getBean("clusterPropertyManager", ClusterPropertyManager.class);
    applicationEventProxy = context.getBean("applicationEventProxy", ApplicationEventProxy.class);
    applicationEventProxy.addApplicationListener(this);
    configurationManager = new PortalDeployerClientConfigurationManagerImpl(context);
    sslConfigurationManager = new PortalDeployerSslConfigurationManagerImpl(context);
  }

  /**
   * Listens for cluster property create or update events that relate to the Portal Deployer. Setting the cluster
   * property 'portal.deployer.enabled' to true or false will enable or disable the Portal Deployer client.
   * @param event
   */
  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    if (event instanceof AdminEvent) {
      if (event instanceof Created && ((Created) event).getEntity() instanceof ClusterProperty) {
        ClusterProperty clusterProperty = (ClusterProperty) ((Created) event).getEntity();
        logger.log(Level.FINE, String.format("PortalDeployer onApplicationEvent created ClusterProperty: %s",
                clusterProperty.toString()));
        //TODO: any other cluster property updates we care about?
        if (clusterProperty.getName().equalsIgnoreCase(PD_ENABLED_CP)) {
          handleUpdateOfPortalDeployerEnabledClusterProperty(clusterProperty.getValue());
        }
      } else if (event instanceof Updated && ((Updated) event).getEntity() instanceof ClusterProperty) {
        ClusterProperty clusterProperty = (ClusterProperty) ((Updated) event).getEntity();
        logger.log(Level.INFO, String.format("PortalDeployer onApplicationEvent updated ClusterProperty: %s",
                clusterProperty.toString()));
        if (clusterProperty.getName().equalsIgnoreCase(PD_ENABLED_CP)) {
          handleUpdateOfPortalDeployerEnabledClusterProperty(clusterProperty.getValue());
        }
      }
    }
  }

  private static void checkIfPortalDeployerEnabled() {
    try {
      if (Boolean.parseBoolean(clusterPropertyManager.getProperty(PD_ENABLED_CP))) {
        instance.handleUpdateOfPortalDeployerEnabledClusterProperty(Boolean.TRUE.toString());
      }
    } catch (FindException e) {
      logger.log(Level.INFO, String.format("Cluster property [%s] not found, PortalDeployer not enabled during " +
              "startup.", PD_ENABLED_CP));
    }
  }

  private void handleUpdateOfPortalDeployerEnabledClusterProperty(String newValue) {
    if (Boolean.FALSE.toString().equalsIgnoreCase(newValue)) {
      logger.log(Level.INFO, "Stopping Portal Deployer MQTT Client");
    } else if (Boolean.TRUE.toString().equalsIgnoreCase(newValue)) {
      logger.log(Level.INFO, "Starting Portal Deployer MQTT Client");
      enablePortalDeployerClient();
    }
  }

  private void enablePortalDeployerClient() {
    try {
      //TODO: implement builder and fix client ids/topics to pull from somewhere
      portalDeployerClient = new PortalDeployerClient(
              String.format("wss://%s/", configurationManager.getBrokerHost()),
              String.format("%s_%s_%s", configurationManager.getTenantId(), configurationManager.getTenantGatewayUuid(), "1"),
              String.format("%s/api/cmd/deploy/tenantGatewayUuid/%s", configurationManager.getTenantId(), configurationManager.getTenantGatewayUuid()),
              60,
              30,
              sslConfigurationManager.getSniEnabledSocketFactory(configurationManager.getBrokerHost()));
      portalDeployerClient.startClient();
    } catch (PortalDeployerClientException | PortalDeployerConfigurationException e) {
      //TODO: config exceptions should set a status indicating cluster property
      logger.log(Level.WARNING, ExceptionUtils.getMessage(e), e);
    }
  }
}
