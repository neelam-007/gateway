package com.l7tech.external.assertions.portaldeployer.server;

import static com.l7tech.external.assertions.portaldeployer.server.PortalDeployerClientConfigurationManagerImpl.*;
import com.l7tech.external.assertions.portaldeployer.server.client.PortalDeployerClientException;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.GatewayState;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.admin.AdminEvent;
import com.l7tech.server.event.admin.Created;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.ExceptionUtils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Class that runs when the Portal Deployer modass is loaded. onModuleLoaded is executed via its binding specified
 * in {@link com.l7tech.external.assertions.portaldeployer.PortalDeployerAssertion} metadata. onModuleUnloaded is called
 * when the Portal Deployer modass is unloaded during events such as gateway shutdown, modass unloading (deletion of
 * assertion, loading of new assertion).
 */
public class PortalDeployerModuleLoadListener implements ApplicationListener {
  private static final Logger logger = Logger.getLogger(PortalDeployerModuleLoadListener.class.getName());
  static final String PD_STATUS_CP = "portal.deployer.status";

  /**
   * Initializes portal deployer dependencies and starts a client if portal.deployer.enabled cluster property is
   * set to true.
   * @param context
   */
  public static synchronized void onModuleLoaded(@NotNull ApplicationContext context) {
    logger.log(Level.FINE, "PortalDeployer onModuleLoaded executing");
    if (instance == null) {
      instance = new PortalDeployerModuleLoadListener(context,
              PortalDeployerClientManagerImpl.getInstance(context),
              Executors.newSingleThreadExecutor());
    }
    instance.checkIfPortalDeployerEnabledOnceGatewayReady();
  }

  /**
   * Unloads portal deployer by calling destroy. 
   */
  public static synchronized void onModuleUnloaded() {
    if (instance != null) {
      logger.log(Level.INFO, "PortalDeployer Module load listener is shutting down");
      try {
        instance.destroy();
      } catch (final Exception e) {
        logger.log(Level.WARNING, String.format("PortalDeployer Module load listener threw exception on shutdown: %s" +
                ExceptionUtils.getMessage(e), e));
      } finally {
        instance = null;
      }
    }
  }

  private static PortalDeployerModuleLoadListener instance = null;
  private ClusterPropertyManager clusterPropertyManager;
  private ApplicationEventProxy applicationEventProxy;
  private PortalDeployerClientManager portalDeployerClientManager;
  private PlatformTransactionManager transactionManager;
  private GatewayState gatewayState;
  private ExecutorService executorService;

  protected enum PortalDeployerStatus {STARTED, STOPPED, START_FAILED, STOP_FAILED}

  PortalDeployerModuleLoadListener(final ApplicationContext context,
                                   final PortalDeployerClientManager portalDeployerClientManager,
                                   final ExecutorService executorService) {
    clusterPropertyManager = context.getBean("clusterPropertyManager", ClusterPropertyManager.class);
    applicationEventProxy = context.getBean("applicationEventProxy", ApplicationEventProxy.class);
    transactionManager = context.getBean("transactionManager", PlatformTransactionManager.class);
    gatewayState = context.getBean("gatewayState", GatewayState.class);
    applicationEventProxy.addApplicationListener(this);
    this.portalDeployerClientManager = portalDeployerClientManager;
    this.executorService = executorService;
  }

  void checkIfPortalDeployerEnabledOnceGatewayReady() {
    //only try and start portal deployer if gateway is fully started
    if (gatewayState.isReadyForMessages()) {
      checkIfPortalDeployerEnabled();
    } else {
      applicationEventProxy.addApplicationListener(event -> {
        if (event instanceof ReadyForMessages) {
          checkIfPortalDeployerEnabled();
        }
      });
    }
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

  /**
   * Stop the Portal Deployer client and unhook it from the Gateway dependencies that may prevent garbage cleanup.
   * @throws PortalDeployerClientException
   * @throws PortalDeployerConfigurationException
   */
  void destroy() throws PortalDeployerClientException, PortalDeployerConfigurationException {
    applicationEventProxy.removeApplicationListener(this);
    portalDeployerClientManager.stop();
    updatePortalDeployerStatus(PortalDeployerStatus.STOPPED);
    executorService.shutdown();
  }

  private void checkIfPortalDeployerEnabled() {
    try {
      if (Boolean.parseBoolean(clusterPropertyManager.getProperty(PD_ENABLED_CP))) {
        handleUpdateOfPortalDeployerEnabledClusterProperty(Boolean.TRUE.toString());
      }
    } catch (FindException e) {
      logger.log(Level.INFO, String.format("Cluster property [%s] not found, PortalDeployer not enabled during " +
              "startup.", PD_ENABLED_CP));
    }
  }

  private void handleUpdateOfPortalDeployerEnabledClusterProperty(String newValue) {
    if (Boolean.FALSE.toString().equalsIgnoreCase(newValue)) {
      stopManager();
    } else if (Boolean.TRUE.toString().equalsIgnoreCase(newValue)) {
      startManager();
    }
  }

  /**
   * Stops the manager and sets portal.deployer.status as STARTED if successful or START_FAILED if not.
   */
  private void startManager() {
    logger.log(Level.INFO, "Starting Portal Deployer MQTT Client");
    try {
      portalDeployerClientManager.start();
      updatePortalDeployerStatus(PortalDeployerStatus.STARTED);
    } catch (PortalDeployerClientException | PortalDeployerConfigurationException e) {
      logger.log(Level.WARNING, "exception caught when starting portal deployer manager", e);
      updatePortalDeployerStatus(PortalDeployerStatus.START_FAILED);
    }
  }

  /**
   * Stops the manager and sets portal.deployer.status as STOPPED if successful or STOP_FAILED if not.
   */
  private void stopManager() {
    logger.log(Level.INFO, "Stopping Portal Deployer MQTT Client");
    try {
      portalDeployerClientManager.stop();
      updatePortalDeployerStatus(PortalDeployerStatus.STOPPED);
    } catch (PortalDeployerClientException | PortalDeployerConfigurationException e) {
      logger.log(Level.WARNING, "exception caught when stopping portal deployer manager", e);
      updatePortalDeployerStatus(PortalDeployerStatus.STOP_FAILED);
    }
  }

  private void updatePortalDeployerStatus(PortalDeployerStatus status) {
    //execute update of cluster property in separate thread as the update doesn't succeed if the calling thread
    //is already based on modificaiton of a cluster property i.e onApplicationEvent
    executorService.execute(() -> new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
      @Override
      protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
        try {
          clusterPropertyManager.putProperty(PD_STATUS_CP, status.name());
        } catch (FindException | SaveException | UpdateException e) {
          logger.log(Level.WARNING, String.format("unable to save Portal Deployer status cluster property " +
                  "[name=%s," + "value=%s]", PD_STATUS_CP, status));
        }
      }
    }));
  }
}
