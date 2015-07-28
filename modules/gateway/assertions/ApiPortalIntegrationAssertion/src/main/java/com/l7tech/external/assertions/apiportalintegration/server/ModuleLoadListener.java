package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.ApiPortalIntegrationAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKeyManagerFactory;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKeyManagerImpl;
import com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager.PortalManagedServiceManager;
import com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager.PortalManagedServiceManagerImpl;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.gateway.common.service.ServiceType;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.LicenseChangeEvent;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.service.ServiceTemplateManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Listener that ensures the Api portal integration module gets initialized.
 */
public class ModuleLoadListener implements ApplicationListener {
  public ModuleLoadListener(final ApplicationContext context) {
    this(context, ApiKeyManagerFactory.getInstance() == null ? new ApiKeyManagerImpl(context) : ApiKeyManagerFactory.getInstance(), PortalManagedServiceManagerImpl.getInstance(context), API_KEY_MANAGEMENT_SERVICE_POLICY_XML, API_PORTAL_INTEGRATION_POLICY_XML);
    if (ApiKeyManagerFactory.getInstance() == null) {
      ApiKeyManagerFactory.setInstance(apiKeysManager);
    }
  }

  public static synchronized void onModuleLoaded(final ApplicationContext context) {
    if (instance != null) {
      logger.log(Level.WARNING, "API portal integration module is already initialized");
    } else {
      instance = new ModuleLoadListener(context);
    }
  }

  /*
   * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
   * that would otherwise keep our instances from getting collected.
   */
  public static synchronized void onModuleUnloaded() {
    if (instance != null) {
      logger.log(Level.INFO, "Module load listener is shutting down");
      try {
        instance.destroy();
      } catch (final Exception e) {
        logger.log(Level.WARNING, "Module load listener threw exception on shutdown: " + ExceptionUtils.getMessage(e), e);
      } finally {
        instance = null;
      }
    }
  }

  @Override
  public void onApplicationEvent(final ApplicationEvent applicationEvent) {
    if (applicationEvent instanceof ReadyForMessages) {
      // init portal managed services
      refreshAllPortalManagedServices();
    } else if (applicationEvent instanceof LicenseChangeEvent) {
      registerServiceTemplates();
    } else if (applicationEvent instanceof EntityInvalidationEvent) {
      final EntityInvalidationEvent event = (EntityInvalidationEvent) applicationEvent;
      if (PublishedService.class.equals(event.getEntityClass())) {
        handlePublishedServiceInvalidationEvent(event);
      }
    }
  }

  /**
   * Constructor for mocked unit tests.
   */
  ModuleLoadListener(final ApplicationContext context, final PortalGenericEntityManager<ApiKeyData> apiKeyManager, final PortalManagedServiceManager portalManagedServiceManager, final String apiKeyManagementPolicyXmlFile, final String apiPortalIntegrationPolicyXmlFile) {
    serviceTemplateManager = getBean(context, "serviceTemplateManager", ServiceTemplateManager.class);
    licenseManager = getBean(context, "licenseManager", LicenseManager.class);
    serverConfig = getBean(context, "serverConfig", ServerConfig.class);
    serviceManager = getBean(context, "serviceManager", ServiceManager.class);
    transactionManager = getBean(context, "transactionManager", PlatformTransactionManager.class);
    policyManager = getBean(context, "policyManager", PolicyManager.class);
    policyVersionManager = getBean(context, "policyVersionManager", PolicyVersionManager.class);
    folderManager = getBean(context, "folderManager", FolderManager.class);
    clusterPropertyManager = getBean(context, "clusterPropertyManager", ClusterPropertyManager.class);
    applicationEventProxy = getBean(context, "applicationEventProxy", ApplicationEventProxy.class);
    applicationEventProxy.addApplicationListener(this);
    apiKeyManagementServiceTemplate = createServiceTemplate(apiKeyManagementPolicyXmlFile, API_KEY_MANAGEMENT_INTERNAL_SERVICE_NAME, API_KEY_MANAGEMENT_INTERNAL_SERVICE_URI_PREFIX);
    apiPortalIntegrationServiceTemplate = createServiceTemplate(apiPortalIntegrationPolicyXmlFile, API_PORTAL_INTEGRATION_INTERNAL_SERVICE_NAME, API_PORTAL_INTEGRATION_INTERNAL_SERVICE_URI_PREFIX);
    this.apiKeysManager = apiKeyManager;
    this.portalManagedServiceManager = portalManagedServiceManager;
  }

  static final String API_KEY_MANAGEMENT_SERVICE_POLICY_XML = "APIKeyManagementServicePolicy.xml";
  static final String API_KEY_MANAGEMENT_INTERNAL_SERVICE_NAME = "API Key Management Service";
  static final String API_PORTAL_INTEGRATION_INTERNAL_SERVICE_NAME = "API Portal Integration Service";
  static final String API_PLANS_FRAGMENT_POLICY_NAME = "API Plans Fragment";
  static final String API_PORTAL_INTEGRATION_POLICY_XML = "APIPortalIntegration.xml";
  private static final Logger logger = Logger.getLogger(ModuleLoadListener.class.getName());
  private static final String API_KEY_MANAGEMENT_INTERNAL_SERVICE_URI_PREFIX = "/api/keys/*";
  private static final String API_PLANS_FRAGMENT_POLICY_XML = "APIPlansFragment.xml";
  private static final String API_PORTAL_INTEGRATION_INTERNAL_SERVICE_URI_PREFIX = "/portalman/*";
  static final String ACCOUNT_PLANS_FRAGMENT_POLICY_NAME = "Account Plans Fragment";
  private static final String ACCOUNT_PLANS_FRAGMENT_POLICY_XML = "AccountPlansFragment.xml";
  static final String ROOT_FOLDER_NAME = "Root Node";
  static final String API_DELETED_FOLDER_NAME = "APIs Deleted from Portal";
  static final String OAUTH1X_FRAGMENT_POLICY_NAME = "Require OAuth 1.0 Token";
  static final String OAUTH20_FRAGMENT_POLICY_NAME = "Require OAuth 2.0 Token";
  private final ApplicationEventProxy applicationEventProxy;
  private final ServiceTemplateManager serviceTemplateManager;
  private final PortalGenericEntityManager<ApiKeyData> apiKeysManager;
  private final ServerConfig serverConfig;
  private final ServiceManager serviceManager;
  private final PlatformTransactionManager transactionManager;
  private final PolicyManager policyManager;
  private final PolicyVersionManager policyVersionManager;
  private static ModuleLoadListener instance = null;
  private static PortalManagedServiceManager portalManagedServiceManager;
  private static LicenseManager licenseManager;
  private static FolderManager folderManager;
  private static ClusterPropertyManager clusterPropertyManager;
  /**
   * Can be null if the policy template xml is invalid.
   */
  @Nullable
  private final ServiceTemplate apiKeyManagementServiceTemplate;
  /**
   * Can be null if the policy template xml is invalid.
   */
  @Nullable
  private final ServiceTemplate apiPortalIntegrationServiceTemplate;

  private void handlePublishedServiceInvalidationEvent(final EntityInvalidationEvent event) {
    final Goid[] entityIds = event.getEntityIds();
    final char[] entityOperations = event.getEntityOperations();
    for (int i = 0; i < entityIds.length; i++) {
      final Goid entityId = entityIds[i];
      final char entityOperation = entityOperations[i];
      try {
        switch (entityOperation) {
          case EntityInvalidationEvent.CREATE: {
            handleCreate(entityId);
            break;
          }
          case EntityInvalidationEvent.UPDATE: {
            handleUpdate(entityId);
            break;
          }
          case EntityInvalidationEvent.DELETE: {
            deletePortalManagedServiceIfFound(entityId);
            break;
          }
          default: {
            logger.log(Level.WARNING, "Unsupported operation: " + entityOperation);
          }
        }
      } catch (final ObjectModelException e) {
        logger.log(Level.WARNING, "Error processing entity invalidation event for service with oid=" + entityId, ExceptionUtils.getDebugException(e));
      }
    }
  }

  private void handleUpdate(final Goid entityId) throws FindException, DeleteException, UpdateException, SaveException {
    final PublishedService service = serviceManager.findByPrimaryKey(entityId);
    if (service == null) {
      // service has been deleted
      deletePortalManagedServiceIfFound(entityId);
      return;
    }
    final PortalManagedService portalManagedService = portalManagedServiceManager.fromService(service);
    if (portalManagedService != null) {
      // service is portal managed

      // check if api id was changed for the updated service
      final List<PortalManagedService> matchesServiceOid = findByServiceGoid(entityId);
      for (final PortalManagedService found : matchesServiceOid) {
        if (!found.getName().equals(portalManagedService.getName())) {
          // api id has changed, delete the old one(s)
          portalManagedServiceManager.delete(found.getName());
        }
      }

      // check for api id conflict with other services
      final PortalManagedService nameConflict = portalManagedServiceManager.find(portalManagedService.getName());
      if (nameConflict != null && !portalManagedService.getDescription().equals(nameConflict.getDescription())) {
        logger.log(Level.WARNING, "Cannot add a Portal Managed Service with apiId=" + portalManagedService.getName() + " for service id=" + portalManagedService.getDescription() + " because one already exists for service id=" + nameConflict.getDescription());
      } else {
        portalManagedServiceManager.addOrUpdate(portalManagedService);
      }
    } else {
      // updated service is not portal managed
      deletePortalManagedServiceIfFound(entityId);
    }
  }

  private void handleCreate(final Goid entityId) throws FindException, SaveException, UpdateException {
    final PublishedService service = serviceManager.findByPrimaryKey(entityId);
    if (API_PORTAL_INTEGRATION_INTERNAL_SERVICE_NAME.equals(service.getName())) {
      createPolicyFragments();
      processClusterProperties();
    }
    final PortalManagedService portalManagedService = portalManagedServiceManager.fromService(service);
    if (portalManagedService != null) {
      // service cache triggers UPDATE invalidation event for services that already exist so we can't assume
      // that we need to only add here - must add or update
      portalManagedServiceManager.addOrUpdate(portalManagedService);
    }
  }

  private void deletePortalManagedServiceIfFound(final Goid serviceId) throws FindException, DeleteException {
    for (final PortalManagedService remove : findByServiceGoid(serviceId)) {
      portalManagedServiceManager.delete(remove.getName());
    }
  }

  private List<PortalManagedService> findByServiceGoid(final Goid serviceId) throws FindException {
    // unfortunately we don't have a more efficient way of retrieving portal managed services by service oid (description)
    final List<PortalManagedService> all = portalManagedServiceManager.findAll();
    final List<PortalManagedService> subset = new ArrayList<PortalManagedService>();
    for (final PortalManagedService portalManagedService : all) {
      if (String.valueOf(serviceId).equalsIgnoreCase(portalManagedService.getDescription())) {
        subset.add(portalManagedService);
      }
    }
    return subset;
  }

  /**
   * Only creates the fragments if it doesn't exist.
   */
  private void createPolicyFragments() {
    try {
      final Policy found = policyManager.findByUniqueName(API_PLANS_FRAGMENT_POLICY_NAME);
      if (found == null) {
        final String policyXml = readPolicyFile(API_PLANS_FRAGMENT_POLICY_XML);
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, API_PLANS_FRAGMENT_POLICY_NAME, policyXml, false);
        policy.setGuid(UUID.randomUUID().toString());
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
          @Override
          protected void doInTransactionWithoutResult(final TransactionStatus transactionStatus) {
            try {
              policyManager.save(policy);
              policyVersionManager.checkpointPolicy(policy, true, true);
            } catch (final ObjectModelException e) {
              transactionStatus.setRollbackOnly();
              logger.log(Level.WARNING, "Error persisting policy fragment. " + API_PLANS_FRAGMENT_POLICY_NAME + " will not be available.", ExceptionUtils.getDebugException(e));
            }
          }
        });
      }
    } catch (final Exception e) {
      logger.log(Level.WARNING, "Error creating policy fragment. " + API_PLANS_FRAGMENT_POLICY_NAME + " will not be available.", ExceptionUtils.getDebugException(e));
    }
    try {
      final Policy found = policyManager.findByUniqueName(ACCOUNT_PLANS_FRAGMENT_POLICY_NAME);
      if (found == null) {
        final String policyXml = readPolicyFile(ACCOUNT_PLANS_FRAGMENT_POLICY_XML);
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, ACCOUNT_PLANS_FRAGMENT_POLICY_NAME, policyXml, false);
        policy.setGuid(UUID.randomUUID().toString());
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
          @Override
          protected void doInTransactionWithoutResult(final TransactionStatus transactionStatus) {
            try {
              policyManager.save(policy);
              policyVersionManager.checkpointPolicy(policy, true, true);
            } catch (final ObjectModelException e) {
              transactionStatus.setRollbackOnly();
              logger.log(Level.WARNING, "Error persisting policy fragment. " + ACCOUNT_PLANS_FRAGMENT_POLICY_NAME + " will not be available.", ExceptionUtils.getDebugException(e));
            }
          }
        });
      }
    } catch (final Exception e) {
      logger.log(Level.WARNING, "Error creating policy fragment. " + ACCOUNT_PLANS_FRAGMENT_POLICY_NAME + " will not be available.", ExceptionUtils.getDebugException(e));
    }
  }

  /**
   * Process the entity we need to setup for cluster properties
   */
  private void processClusterProperties() {
    createDeletedFolder();
    try {
      final Policy found = policyManager.findByUniqueName(API_PLANS_FRAGMENT_POLICY_NAME);
      if (found != null) {
        createClusterPropertyIfNotExist(ModuleConstants.API_PLANS_FRAGMENT_GUID, found.getGuid());
      } else {
        createClusterPropertyIfNotExist(ModuleConstants.API_PLANS_FRAGMENT_GUID, ModuleConstants.NOT_INSTALLED_VALUE);
        logger.log(Level.WARNING, "Error retrieving policy fragment. " + API_PLANS_FRAGMENT_POLICY_NAME + ". defaulting to " + ModuleConstants.NOT_INSTALLED_VALUE);
      }
    } catch (final Exception e) {
      logger.log(Level.WARNING, "Error retrieving policy fragment. " + API_PLANS_FRAGMENT_POLICY_NAME + ". It's guid will not be available.", ExceptionUtils.getDebugException(e));
    }
    try {
      final Policy found = policyManager.findByUniqueName(ACCOUNT_PLANS_FRAGMENT_POLICY_NAME);
      if (found != null) {
        createClusterPropertyIfNotExist(ModuleConstants.ACCOUNT_PLANS_FRAGMENT_GUID, found.getGuid());
      } else {
        createClusterPropertyIfNotExist(ModuleConstants.ACCOUNT_PLANS_FRAGMENT_GUID, ModuleConstants.NOT_INSTALLED_VALUE);
        logger.log(Level.WARNING, "Error retrieving policy fragment. " + ACCOUNT_PLANS_FRAGMENT_POLICY_NAME + ". defaulting to " + ModuleConstants.NOT_INSTALLED_VALUE);
      }
    } catch (final Exception e) {
      logger.log(Level.WARNING, "Error retrieving policy fragment. " + ACCOUNT_PLANS_FRAGMENT_POLICY_NAME + ". It's guid will not be available.", ExceptionUtils.getDebugException(e));
    }
    try {
      final Policy found = policyManager.findByUniqueName(OAUTH1X_FRAGMENT_POLICY_NAME);
      if (found != null) {
        createClusterPropertyIfNotExist(ModuleConstants.OAUTH1X_FRAGMENT_GUID, found.getGuid());
      } else {
        createClusterPropertyIfNotExist(ModuleConstants.OAUTH1X_FRAGMENT_GUID, ModuleConstants.NOT_INSTALLED_VALUE);
        logger.log(Level.WARNING, "Error retrieving policy fragment. " + OAUTH1X_FRAGMENT_POLICY_NAME + ". defaulting to " + ModuleConstants.NOT_INSTALLED_VALUE);
      }
    } catch (final Exception e) {
      logger.log(Level.WARNING, "Error retrieving policy fragment. " + OAUTH1X_FRAGMENT_POLICY_NAME + ". It's guid will not be available.", ExceptionUtils.getDebugException(e));
    }
    try {
      final Policy found = policyManager.findByUniqueName(OAUTH20_FRAGMENT_POLICY_NAME);
      if (found != null) {
        createClusterPropertyIfNotExist(ModuleConstants.OAUTH20_FRAGMENT_GUID, found.getGuid());
      } else {
        createClusterPropertyIfNotExist(ModuleConstants.OAUTH20_FRAGMENT_GUID, ModuleConstants.NOT_INSTALLED_VALUE);
        logger.log(Level.WARNING, "Error retrieving policy fragment. " + OAUTH20_FRAGMENT_POLICY_NAME + ". defaulting to " + ModuleConstants.NOT_INSTALLED_VALUE);
      }
    } catch (final Exception e) {
      logger.log(Level.WARNING, "Error retrieving policy fragment. " + OAUTH20_FRAGMENT_POLICY_NAME + ". It's guid will not be available.", ExceptionUtils.getDebugException(e));
    }
  }

  /**
   * Only creates the folder if it doesn't exist.
   */
  private void createDeletedFolder() {
    try {
      final Folder folder = folderManager.findByUniqueName(API_DELETED_FOLDER_NAME);
      if (folder != null) {
        createClusterPropertyIfNotExist(ModuleConstants.API_DELETED_FOLDER_ID, folder.getId().toString());
      } else {
        logger.log(Level.WARNING, "Error retrieving folder. " + API_DELETED_FOLDER_NAME + ". defaulting to " + ModuleConstants.NOT_INSTALLED_VALUE);
        createClusterPropertyIfNotExist(ModuleConstants.API_DELETED_FOLDER_ID, ModuleConstants.NOT_INSTALLED_VALUE);
      }
    } catch (final Exception e) {
      logger.log(Level.WARNING, "Error retrieving API Deleted folder." + API_DELETED_FOLDER_NAME + " will not be available.", ExceptionUtils.getDebugException(e));
    }
  }

  /**
   * Only creates the cluster property if it doesn't exist or its value is "not installed" and we are updating to a valid one
   */
  private void createClusterPropertyIfNotExist(final String clusterPropertyName, final String value) {
    try {
      final ClusterProperty found = clusterPropertyManager.findByUniqueName(clusterPropertyName);
      if (found == null) {
        final ClusterProperty clusterProperty = new ClusterProperty(clusterPropertyName, value);
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
          @Override
          protected void doInTransactionWithoutResult(final TransactionStatus transactionStatus) {
            try {
              clusterPropertyManager.save(clusterProperty);
            } catch (final ObjectModelException e) {
              transactionStatus.setRollbackOnly();
              logger.log(Level.WARNING, "Error creating cluster property. " + clusterPropertyName + " will not be available.", ExceptionUtils.getDebugException(e));
            }
          }
        });
      } else if (!ModuleConstants.NOT_INSTALLED_VALUE.equals(value) && ModuleConstants.NOT_INSTALLED_VALUE.equals(found.getValue())) {
        found.setValue(value);
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
          @Override
          protected void doInTransactionWithoutResult(final TransactionStatus transactionStatus) {
            try {
              clusterPropertyManager.update(found);
            } catch (final ObjectModelException e) {
              transactionStatus.setRollbackOnly();
              logger.log(Level.WARNING, "Error creating cluster property. " + clusterPropertyName + " will not be available.", ExceptionUtils.getDebugException(e));
            }
          }
        });

      }
    } catch (final Exception e) {
      logger.log(Level.WARNING, "Error retrieving cluster property." + clusterPropertyName + " will not be available.", ExceptionUtils.getDebugException(e));
    }
  }

  private void registerServiceTemplates() {
    if (licenseManager.isFeatureEnabled(new ApiPortalIntegrationAssertion().getFeatureSetName())) {
      if (apiKeyManagementServiceTemplate != null) {
        serviceTemplateManager.register(apiKeyManagementServiceTemplate);
      }
      if (apiPortalIntegrationServiceTemplate != null) {
        serviceTemplateManager.register(apiPortalIntegrationServiceTemplate);
      }
    }
  }

  private void destroy() throws Exception {
    applicationEventProxy.removeApplicationListener(this);
    serviceTemplateManager.unregister(apiKeyManagementServiceTemplate);
    serviceTemplateManager.unregister(apiPortalIntegrationServiceTemplate);
  }

  private void refreshAllPortalManagedServices() {
    List<PortalManagedService> fromPolicy = new ArrayList<PortalManagedService>();
    List<PortalManagedService> fromDatabase = new ArrayList<PortalManagedService>();
    try {
      fromPolicy.addAll(portalManagedServiceManager.findAllFromPolicy());
      fromDatabase.addAll(portalManagedServiceManager.findAll());
    } catch (final FindException e) {
      logger.log(Level.WARNING, "Error retrieving Portal Managed Services: " + e.getMessage(), ExceptionUtils.getDebugException(e));
    }
    final Set<String> namesFromPolicy = new HashSet<String>();
    for (final PortalManagedService portalManagedService : fromPolicy) {
      try {
        namesFromPolicy.add(portalManagedService.getName());
        portalManagedServiceManager.addOrUpdate(portalManagedService);
      } catch (final ObjectModelException e) {
        logger.log(Level.WARNING, "Error adding/updating Portal Managed Service with name=" + portalManagedService.getName() + ": " + e.getMessage(), ExceptionUtils.getDebugException(e));
      }
    }

    for (final PortalManagedService serviceFromDatabase : fromDatabase) {
      final String name = serviceFromDatabase.getName();
      if (!namesFromPolicy.contains(name)) {
        logger.log(Level.FINE, "Detected Portal Managed Service in database that does not exist in policy with name=" + name);
        try {
          portalManagedServiceManager.delete(name);
        } catch (final ObjectModelException e) {
          logger.log(Level.WARNING, "Error deleting Portal Managed Service with name=" + name + ": " + e.getMessage(), ExceptionUtils.getDebugException(e));
        }
      }
    }
  }

  private ServiceTemplate createServiceTemplate(final String policyResourceFile, final String serviceName, final String uriPrefix) {
    ServiceTemplate template = null;
    try {
      final String policyContents = readPolicyFile(policyResourceFile);
      template = new ServiceTemplate(serviceName, uriPrefix, policyContents, ServiceType.OTHER_INTERNAL_SERVICE, null);
    } catch (final IOException e) {
      logger.log(Level.WARNING, "Error creating service template. " + serviceName + " will not be available.", ExceptionUtils.getDebugException(e));
    }
    return template;
  }

  private String readPolicyFile(final String policyResourceFile) throws IOException {
    final InputStream resourceAsStream = ModuleLoadListener.class.getClassLoader().getResourceAsStream(policyResourceFile);
    if (resourceAsStream == null) {
      throw new IOException("Policy resource file does not exist: " + policyResourceFile);
    }
    final byte[] fileBytes = IOUtils.slurpStream(resourceAsStream);
    resourceAsStream.close();
    return new String(fileBytes);
  }

  private static <T> T getBean(final BeanFactory beanFactory, final String beanName, final Class<T> beanClass) {
    final T got = beanFactory.getBean(beanName, beanClass);
    if (got != null && beanClass.isAssignableFrom(got.getClass())) {
      return got;
    }
    throw new IllegalStateException("Unable to get bean from application context: " + beanName);
  }

}
