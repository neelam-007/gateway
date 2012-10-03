package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.ApiPortalIntegrationAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager.PortalManagedServiceManager;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.LicenseEvent;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.service.ServiceTemplateManager;
import com.l7tech.server.util.ApplicationEventProxy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;
import java.util.logging.Level;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ModuleLoadListenerTest {
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private ServiceTemplateManager serviceTemplateManager;
    @Mock
    private LicenseManager licenseManager;
    @Mock
    private ServerConfig serverConfig;
    @Mock
    private ApplicationEventProxy applicationEventProxy;
    @Mock
    private PortalGenericEntityManager<ApiKeyData> apiKeyManager;
    @Mock
    PortalManagedServiceManager portalManagedServiceManager;
    @Mock
    PlatformTransactionManager transactionManager;
    @Mock
    PolicyManager policyManager;
    @Mock
    PolicyVersionManager policyVersionManager;
    @Mock
    GenericEntityManager genericEntityManager;
    @Mock
    FolderManager folderManager;
    @Mock
    ClusterPropertyManager clusterPropertyManager;
    @Mock
    ServiceManager serviceManager;
    private ModuleLoadListener listener;
    private ApplicationEvent event;
    private List<PortalManagedService> portalManagedServices;

    @Before
    public void setup() {
        when(applicationContext.getBean("serviceTemplateManager", ServiceTemplateManager.class)).thenReturn(serviceTemplateManager);
        when(applicationContext.getBean("licenseManager", LicenseManager.class)).thenReturn(licenseManager);
        when(applicationContext.getBean("serverConfig", ServerConfig.class)).thenReturn(serverConfig);
        when(applicationContext.getBean("applicationEventProxy", ApplicationEventProxy.class)).thenReturn(applicationEventProxy);
        when(applicationContext.getBean("transactionManager", PlatformTransactionManager.class)).thenReturn(transactionManager);
        when(applicationContext.getBean("policyManager", PolicyManager.class)).thenReturn(policyManager);
        when(applicationContext.getBean("policyVersionManager", PolicyVersionManager.class)).thenReturn(policyVersionManager);
        when(applicationContext.getBean("serviceManager", ServiceManager.class)).thenReturn(serviceManager);
        when(applicationContext.getBean("folderManager", FolderManager.class)).thenReturn(folderManager);
        when(applicationContext.getBean("clusterPropertyManager", ClusterPropertyManager.class)).thenReturn(clusterPropertyManager);
        listener = new ModuleLoadListener(applicationContext, apiKeyManager, portalManagedServiceManager,
                ModuleLoadListener.API_KEY_MANAGEMENT_SERVICE_POLICY_XML, ModuleLoadListener.RELOAD_API_PLANS_SERVICE_POLICY_XML, ModuleLoadListener.API_PORTAL_INTEGRATION_POLICY_XML);
        portalManagedServices = new ArrayList<PortalManagedService>();
    }

    @Test
    public void onModuleLoadedAndUnloaded() {
        when(applicationContext.getBean("genericEntityManager", GenericEntityManager.class)).thenReturn(genericEntityManager);

        ModuleLoadListener.onModuleLoaded(applicationContext);
        ModuleLoadListener.onModuleUnloaded();

        verify(applicationEventProxy).removeApplicationListener(any(ApplicationListener.class));
        verify(serviceTemplateManager).unregister(argThat(new ServiceTemplateWithName(ModuleLoadListener.API_KEY_MANAGEMENT_INTERNAL_SERVICE_NAME)));
        verify(serviceTemplateManager).unregister(argThat(new ServiceTemplateWithName(ModuleLoadListener.RELOAD_API_PLANS_INTERNAL_SERVICE_NAME)));
    }

    @Test
    public void onApplicationEventReadyForMessages() throws Exception {
        event = new ReadyForMessages("", Component.GATEWAY, "");
        final PortalManagedService service1 = new PortalManagedService();
        service1.setName("1111");
        final PortalManagedService service2 = new PortalManagedService();
        service2.setName("2222");
        portalManagedServices.add(service1);
        portalManagedServices.add(service2);
        when(portalManagedServiceManager.findAllFromPolicy()).thenReturn(portalManagedServices);
        when(portalManagedServiceManager.findAll()).thenReturn(Collections.<PortalManagedService>emptyList());

        listener.onApplicationEvent(event);

        verify(portalManagedServiceManager).findAllFromPolicy();
        verify(portalManagedServiceManager).findAll();
        verify(portalManagedServiceManager).addOrUpdate(service1);
        verify(portalManagedServiceManager).addOrUpdate(service2);
    }

    @Test
    public void onApplicationEventReadyForMessagesSkipsErrors() throws Exception {
        event = new ReadyForMessages("", Component.GATEWAY, "");
        final PortalManagedService service1 = new PortalManagedService();
        service1.setName("1111");
        final PortalManagedService service2 = new PortalManagedService();
        service2.setName("2222");
        final PortalManagedService service3 = new PortalManagedService();
        service2.setName("3333");
        portalManagedServices.add(service1);
        portalManagedServices.add(service2);
        portalManagedServices.add(service3);
        when(portalManagedServiceManager.findAllFromPolicy()).thenReturn(portalManagedServices);
        when(portalManagedServiceManager.findAll()).thenReturn(Collections.<PortalManagedService>emptyList());
        // something went wrong when processing service 2
        doThrow(new FindException("mocking exception")).when(portalManagedServiceManager).addOrUpdate(service2);

        listener.onApplicationEvent(event);

        verify(portalManagedServiceManager).findAllFromPolicy();
        verify(portalManagedServiceManager).findAll();
        verify(portalManagedServiceManager).addOrUpdate(service1);
        verify(portalManagedServiceManager).addOrUpdate(service2);
        // service 3 should still be processed even though service 2 failed
        verify(portalManagedServiceManager).addOrUpdate(service3);
    }

    @Test
    public void onApplicationEventReadyForMessagesExceptionReadingPortalManagedServices() throws Exception {
        event = new ReadyForMessages("", Component.GATEWAY, "");
        when(portalManagedServiceManager.findAllFromPolicy()).thenThrow(new FindException("cannot retrieve portal managed services"));

        listener.onApplicationEvent(event);

        verify(portalManagedServiceManager).findAllFromPolicy();
        verify(portalManagedServiceManager, never()).addOrUpdate(any(PortalManagedService.class));
    }

    @Test
    public void onApplicationEventReadyForMessagesExceptionWritingPortalManagedServices() throws Exception {
        event = new ReadyForMessages("", Component.GATEWAY, "");
        portalManagedServices.add(new PortalManagedService());
        when(portalManagedServiceManager.findAllFromPolicy()).thenReturn(portalManagedServices);
        doThrow(new SaveException("mocking exception")).when(portalManagedServiceManager).addOrUpdate(any(PortalManagedService.class));

        listener.onApplicationEvent(event);

        verify(portalManagedServiceManager).findAllFromPolicy();
        verify(portalManagedServiceManager).addOrUpdate(portalManagedServices.get(0));
    }

    @Test
    public void onApplicationEventReadyForMessagesSomePortalManagedServicesRemoved() throws Exception {
        event = new ReadyForMessages("", Component.GATEWAY, "");
        final PortalManagedService service1 = new PortalManagedService();
        service1.setName("1111");
        final PortalManagedService service2 = new PortalManagedService();
        service2.setName("2222");
        final PortalManagedService service3 = new PortalManagedService();
        service3.setName("3333");
        portalManagedServices.add(service1);
        portalManagedServices.add(service2);
        when(portalManagedServiceManager.findAllFromPolicy()).thenReturn(portalManagedServices);
        final List<PortalManagedService> inDatabase = new ArrayList<PortalManagedService>(portalManagedServices);
        // service 3 only exists in database but not in policy
        inDatabase.add(service3);
        when(portalManagedServiceManager.findAll()).thenReturn(inDatabase);

        listener.onApplicationEvent(event);

        verify(portalManagedServiceManager).findAllFromPolicy();
        verify(portalManagedServiceManager).findAll();
        verify(portalManagedServiceManager).addOrUpdate(service1);
        verify(portalManagedServiceManager).addOrUpdate(service2);
        verify(portalManagedServiceManager).delete("3333");
    }

    @Test
    public void onApplicationEventReadyForMessagesSomePortalManagedServicesAdded() throws Exception {
        event = new ReadyForMessages("", Component.GATEWAY, "");
        final PortalManagedService service1 = new PortalManagedService();
        service1.setName("1111");
        final PortalManagedService service2 = new PortalManagedService();
        service2.setName("2222");
        final PortalManagedService service3 = new PortalManagedService();
        service3.setName("3333");
        portalManagedServices.add(service1);
        portalManagedServices.add(service2);
        // service 3 only exists in policy but not in database
        portalManagedServices.add(service3);
        when(portalManagedServiceManager.findAllFromPolicy()).thenReturn(portalManagedServices);
        final List<PortalManagedService> inDatabase = new ArrayList<PortalManagedService>(portalManagedServices);
        inDatabase.add(service1);
        inDatabase.add(service2);
        when(portalManagedServiceManager.findAll()).thenReturn(inDatabase);

        listener.onApplicationEvent(event);

        verify(portalManagedServiceManager).findAllFromPolicy();
        verify(portalManagedServiceManager).findAll();
        verify(portalManagedServiceManager).addOrUpdate(service1);
        verify(portalManagedServiceManager).addOrUpdate(service2);
        verify(portalManagedServiceManager).addOrUpdate(service3);
    }

    @Test
    public void onApplicationEventLicenseEvent() {
        event = new LicenseEvent("", Level.INFO, "", "");
        final String featureSetName = new ApiPortalIntegrationAssertion().getFeatureSetName();
        when(licenseManager.isFeatureEnabled(featureSetName)).thenReturn(true);

        listener.onApplicationEvent(event);

        verify(licenseManager).isFeatureEnabled(featureSetName);
        verify(serviceTemplateManager).register(argThat(new ServiceTemplateWithName(ModuleLoadListener.API_KEY_MANAGEMENT_INTERNAL_SERVICE_NAME)));
        verify(serviceTemplateManager).register(argThat(new ServiceTemplateWithName(ModuleLoadListener.RELOAD_API_PLANS_INTERNAL_SERVICE_NAME)));
    }

    @Test
    public void onApplicationEventLicenseEventFeatureNotEnabled() {
        event = new LicenseEvent("", Level.INFO, "", "");
        final String featureSetName = new ApiPortalIntegrationAssertion().getFeatureSetName();
        when(licenseManager.isFeatureEnabled(featureSetName)).thenReturn(false);

        listener.onApplicationEvent(event);

        verify(licenseManager).isFeatureEnabled(featureSetName);
        verify(serviceTemplateManager, never()).register(any(ServiceTemplate.class));
    }

    /**
     * Should still create the reload api plans service template.
     */
    @Test
    public void onApplicationEventLicenseEventExceptionReadingKeyPolicyFile() {
        listener = new ModuleLoadListener(applicationContext, apiKeyManager, portalManagedServiceManager,
                "doesnotexist", ModuleLoadListener.RELOAD_API_PLANS_SERVICE_POLICY_XML, "doesnotexist");
        event = new LicenseEvent("", Level.INFO, "", "");
        final String featureSetName = new ApiPortalIntegrationAssertion().getFeatureSetName();
        when(licenseManager.isFeatureEnabled(featureSetName)).thenReturn(true);

        listener.onApplicationEvent(event);

        verify(licenseManager).isFeatureEnabled(featureSetName);
        verify(serviceTemplateManager, never()).register(argThat(new ServiceTemplateWithName(ModuleLoadListener.API_KEY_MANAGEMENT_INTERNAL_SERVICE_NAME)));
        verify(serviceTemplateManager).register(argThat(new ServiceTemplateWithName(ModuleLoadListener.RELOAD_API_PLANS_INTERNAL_SERVICE_NAME)));
    }

    @Test
    public void onApplicationEventLicenseEventExceptionReadingPlansPolicyFile() {
        listener = new ModuleLoadListener(applicationContext, apiKeyManager, portalManagedServiceManager,
                ModuleLoadListener.API_KEY_MANAGEMENT_SERVICE_POLICY_XML, "doesnotexist", "doesnotexist");
        event = new LicenseEvent("", Level.INFO, "", "");
        final String featureSetName = new ApiPortalIntegrationAssertion().getFeatureSetName();
        when(licenseManager.isFeatureEnabled(featureSetName)).thenReturn(true);

        listener.onApplicationEvent(event);

        verify(licenseManager).isFeatureEnabled(featureSetName);
        verify(serviceTemplateManager).register(argThat(new ServiceTemplateWithName(ModuleLoadListener.API_KEY_MANAGEMENT_INTERNAL_SERVICE_NAME)));
        verify(serviceTemplateManager, never()).register(argThat(new ServiceTemplateWithName(ModuleLoadListener.RELOAD_API_PLANS_INTERNAL_SERVICE_NAME)));
    }

    @Test
    public void onApplicationEventLicenseEventExceptionReadingApiIntegrationPolicyFile() {
        listener = new ModuleLoadListener(applicationContext, apiKeyManager, portalManagedServiceManager,
                "doesnotexist", "doesnotexist", ModuleLoadListener.API_PORTAL_INTEGRATION_POLICY_XML);
        event = new LicenseEvent("", Level.INFO, "", "");
        final String featureSetName = new ApiPortalIntegrationAssertion().getFeatureSetName();
        when(licenseManager.isFeatureEnabled(featureSetName)).thenReturn(true);

        listener.onApplicationEvent(event);

        verify(licenseManager).isFeatureEnabled(featureSetName);
        verify(serviceTemplateManager).register(argThat(new ServiceTemplateWithName(ModuleLoadListener.API_PORTAL_INTEGRATION_INTERNAL_SERVICE_NAME)));
        verify(serviceTemplateManager, never()).register(argThat(new ServiceTemplateWithName(ModuleLoadListener.RELOAD_API_PLANS_INTERNAL_SERVICE_NAME)));
    }

    @Test
    public void onApplicationEventPortalManagedServiceUpdated() throws Exception {
        final PublishedService service = new PublishedService();
        event = new EntityInvalidationEvent(service, PublishedService.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.UPDATE});
        when(serviceManager.findByPrimaryKey(1234L)).thenReturn(service);
        final PortalManagedService updated = new PortalManagedService();
        updated.setName("a1");
        updated.setDescription("1234");
        updated.setApiGroup("newGroup");
        final PortalManagedService old = new PortalManagedService();
        old.setName("a1");
        old.setDescription("1234");
        old.setApiGroup("oldGroup");
        portalManagedServices.add(old);
        when(portalManagedServiceManager.fromService(service)).thenReturn(updated);
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);

        listener.onApplicationEvent(event);

        verify(serviceManager).findByPrimaryKey(1234L);
        verify(portalManagedServiceManager).fromService(service);
        verify(portalManagedServiceManager).findAll();
        verify(portalManagedServiceManager).addOrUpdate(updated);
    }

    @Test
    public void onApplicationEventPublishedServiceUpdatedNotPortalManaged() throws Exception {
        final PublishedService service = new PublishedService();
        event = new EntityInvalidationEvent(service, PublishedService.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.UPDATE});
        when(serviceManager.findByPrimaryKey(1234L)).thenReturn(service);
        // not portal managed
        when(portalManagedServiceManager.fromService(service)).thenReturn(null);
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);

        listener.onApplicationEvent(event);

        verify(serviceManager).findByPrimaryKey(1234L);
        verify(portalManagedServiceManager).fromService(service);
        verify(portalManagedServiceManager).findAll();
        verify(portalManagedServiceManager, never()).delete(anyString());
    }

    @Test
    public void onApplicationEventPublishedServiceUpdatedNoLongerPortalManaged() throws Exception {
        final PublishedService service = new PublishedService();
        event = new EntityInvalidationEvent(service, PublishedService.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.UPDATE});
        final PortalManagedService found = new PortalManagedService();
        found.setName("a1");
        found.setDescription("1234");
        portalManagedServices.add(found);
        when(serviceManager.findByPrimaryKey(1234L)).thenReturn(service);
        // was previously portal managed
        when(portalManagedServiceManager.fromService(service)).thenReturn(null);
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);

        listener.onApplicationEvent(event);

        verify(serviceManager).findByPrimaryKey(1234L);
        verify(portalManagedServiceManager).fromService(service);
        verify(portalManagedServiceManager).findAll();
        verify(portalManagedServiceManager).delete("a1");
    }

    @Test
    public void onApplicationEventPortalManagedServiceUpdatedNameChanged() throws Exception {
        final PublishedService service = new PublishedService();
        event = new EntityInvalidationEvent(service, PublishedService.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.UPDATE});
        when(serviceManager.findByPrimaryKey(1234L)).thenReturn(service);
        final PortalManagedService updated = new PortalManagedService();
        updated.setName("a2");
        updated.setDescription("1234");
        final PortalManagedService found = new PortalManagedService();
        found.setName("a1");
        found.setDescription("1234");
        portalManagedServices.add(found);
        when(portalManagedServiceManager.fromService(service)).thenReturn(updated);
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);

        listener.onApplicationEvent(event);

        verify(serviceManager).findByPrimaryKey(1234L);
        verify(portalManagedServiceManager).fromService(service);
        verify(portalManagedServiceManager).findAll();
        // old one should be deleted
        verify(portalManagedServiceManager).delete("a1");
        verify(portalManagedServiceManager).addOrUpdate(updated);
    }

    @Test
    public void onApplicationEventPortalManagedServiceUpdatedApiIdConflict() throws Exception {
        final PublishedService service = new PublishedService();
        event = new EntityInvalidationEvent(service, PublishedService.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.UPDATE});
        when(serviceManager.findByPrimaryKey(1234L)).thenReturn(service);
        final PortalManagedService update = new PortalManagedService();
        update.setName("a2");
        update.setDescription("1234");
        final PortalManagedService old = new PortalManagedService();
        old.setName("a1");
        old.setDescription("1234");
        final PortalManagedService conflict = new PortalManagedService();
        // api id conflict
        conflict.setName("a2");
        conflict.setDescription("5678");
        portalManagedServices.add(old);
        portalManagedServices.add(conflict);
        when(portalManagedServiceManager.fromService(service)).thenReturn(update);
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);
        when(portalManagedServiceManager.find("a2")).thenReturn(conflict);

        listener.onApplicationEvent(event);

        verify(serviceManager).findByPrimaryKey(1234L);
        verify(portalManagedServiceManager).fromService(service);
        verify(portalManagedServiceManager).findAll();
        verify(portalManagedServiceManager).delete("a1");
        verify(portalManagedServiceManager).find("a2");
        verify(portalManagedServiceManager, never()).addOrUpdate(update);
    }

    @Test
    public void onApplicationEventPublishedServiceCreatedNotPortalManaged() throws Exception {
        final PublishedService service = new PublishedService();
        event = new EntityInvalidationEvent(service, PublishedService.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.CREATE});
        when(serviceManager.findByPrimaryKey(1234L)).thenReturn(service);
        when(portalManagedServiceManager.fromService(service)).thenReturn(null);

        listener.onApplicationEvent(event);

        verify(serviceManager).findByPrimaryKey(1234L);
        verify(portalManagedServiceManager).fromService(service);
        verify(portalManagedServiceManager, never()).findAll();
        verify(portalManagedServiceManager, never()).delete(anyString());
    }

    @Test
    public void onApplicationEventPublishedServiceDeletedNotPortalManaged() throws Exception {
        final PublishedService service = new PublishedService();
        event = new EntityInvalidationEvent(service, PublishedService.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.DELETE});
        final PortalManagedService found = new PortalManagedService();
        found.setName("a1");
        // description doesn't match service oid
        found.setDescription("4567");
        portalManagedServices.add(found);
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);

        listener.onApplicationEvent(event);

        verify(portalManagedServiceManager).findAll();
        verify(serviceManager, never()).findByPrimaryKey(anyLong());
        verify(portalManagedServiceManager, never()).fromService(any(PublishedService.class));
        verify(portalManagedServiceManager, never()).delete(anyString());
    }

    @Test
    public void onApplicationEventPortalManagedServiceCreated() throws Exception {
        final PublishedService service = new PublishedService();
        event = new EntityInvalidationEvent(service, PublishedService.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.CREATE});
        when(serviceManager.findByPrimaryKey(1234L)).thenReturn(service);
        final PortalManagedService portalManagedService = new PortalManagedService();
        when(portalManagedServiceManager.fromService(service)).thenReturn(portalManagedService);

        listener.onApplicationEvent(event);

        verify(serviceManager).findByPrimaryKey(1234L);
        verify(portalManagedServiceManager).fromService(service);
        verify(portalManagedServiceManager).addOrUpdate(portalManagedService);
    }

    @Test
    public void onApplicationEventPortalManagedServiceDeleted() throws Exception {
        final PublishedService service = new PublishedService();
        event = new EntityInvalidationEvent(service, PublishedService.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.DELETE});
        final PortalManagedService found = new PortalManagedService();
        found.setName("a1");
        found.setDescription("1234");
        portalManagedServices.add(found);
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);

        listener.onApplicationEvent(event);

        verify(portalManagedServiceManager).findAll();
        verify(portalManagedServiceManager).delete("a1");
    }

    /**
     * If a published service that is connected to multiple PortalManagedService entities is deleted, all PortalManagedService entities should be deleted.
     * <p/>
     * Shouldn't happen but technically could happen because there's no unique restriction in the database for the description column which holds the service oid.
     */
    @Test
    public void onApplicationEventPortalManagedServiceDeletedMultiple() throws Exception {
        final PublishedService service = new PublishedService();
        event = new EntityInvalidationEvent(service, PublishedService.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.DELETE});
        final PortalManagedService found1 = new PortalManagedService();
        found1.setName("a1");
        found1.setDescription("1234");
        final PortalManagedService found2 = new PortalManagedService();
        found2.setName("a2");
        found2.setDescription("1234");
        portalManagedServices.add(found1);
        portalManagedServices.add(found2);
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);

        listener.onApplicationEvent(event);

        verify(portalManagedServiceManager).findAll();
        verify(portalManagedServiceManager).delete("a1");
        verify(portalManagedServiceManager).delete("a2");
    }

    @Test
    public void onApplicationEventPortalManagedServiceMultiple() throws Exception {
        final PublishedService service1 = new PublishedService();
        service1.setOid(1111L);
        final PublishedService service2 = new PublishedService();
        service2.setOid(2222L);
        event = new EntityInvalidationEvent("", PublishedService.class, new long[]{1111L, 2222L, 3333L}, new char[]{EntityInvalidationEvent.CREATE, EntityInvalidationEvent.UPDATE, EntityInvalidationEvent.DELETE});
        when(serviceManager.findByPrimaryKey(1111L)).thenReturn(service1);
        when(serviceManager.findByPrimaryKey(2222L)).thenReturn(service2);
        final PortalManagedService portalManagedService1 = new PortalManagedService();
        portalManagedService1.setName("a1");
        portalManagedService1.setDescription("1111");
        final PortalManagedService portalManagedService2 = new PortalManagedService();
        portalManagedService2.setName("a2");
        portalManagedService2.setDescription("2222");
        final PortalManagedService portalManagedService3 = new PortalManagedService();
        portalManagedService3.setName("a3");
        portalManagedService3.setDescription("3333");
        portalManagedServices.add(portalManagedService1);
        portalManagedServices.add(portalManagedService2);
        portalManagedServices.add(portalManagedService3);
        when(portalManagedServiceManager.fromService(service1)).thenReturn(portalManagedService1);
        when(portalManagedServiceManager.fromService(service2)).thenReturn(portalManagedService2);
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);

        listener.onApplicationEvent(event);

        verify(serviceManager).findByPrimaryKey(1111L);
        verify(serviceManager).findByPrimaryKey(2222L);
        verify(portalManagedServiceManager).fromService(service1);
        verify(portalManagedServiceManager).fromService(service2);
        verify(portalManagedServiceManager).addOrUpdate(portalManagedService1);
        verify(portalManagedServiceManager).addOrUpdate(portalManagedService2);
        verify(portalManagedServiceManager, atMost(2)).findAll();
        verify(portalManagedServiceManager).delete("a3");
    }

    /**
     * Errors should not prevent other entities from being processed.
     */
    @Test
    public void onApplicationEventPortalManagedServiceMultipleSkipsErrors() throws Exception {
        final PublishedService service1 = new PublishedService();
        service1.setOid(1111L);
        final PublishedService service2 = new PublishedService();
        service2.setOid(2222L);
        event = new EntityInvalidationEvent("", PublishedService.class, new long[]{1111L, 2222L, 3333L}, new char[]{EntityInvalidationEvent.CREATE, EntityInvalidationEvent.UPDATE, EntityInvalidationEvent.DELETE});
        when(serviceManager.findByPrimaryKey(1111L)).thenReturn(service1);
        when(serviceManager.findByPrimaryKey(2222L)).thenReturn(service2);
        final PortalManagedService portalManagedService1 = new PortalManagedService();
        portalManagedService1.setName("a1");
        portalManagedService1.setDescription("1111");
        final PortalManagedService portalManagedService2 = new PortalManagedService();
        portalManagedService2.setName("a2");
        portalManagedService2.setDescription("2222");
        final PortalManagedService portalManagedService3 = new PortalManagedService();
        portalManagedService3.setName("a3");
        portalManagedService3.setDescription("3333");
        portalManagedServices.add(portalManagedService1);
        portalManagedServices.add(portalManagedService2);
        portalManagedServices.add(portalManagedService3);
        when(portalManagedServiceManager.fromService(service1)).thenReturn(portalManagedService1);
        // something went wrong handling service 2
        when(portalManagedServiceManager.fromService(service2)).thenThrow(new FindException("mocking exception"));
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);

        listener.onApplicationEvent(event);

        verify(serviceManager).findByPrimaryKey(1111L);
        verify(serviceManager).findByPrimaryKey(2222L);
        verify(portalManagedServiceManager).fromService(service1);
        verify(portalManagedServiceManager).fromService(service2);
        verify(portalManagedServiceManager).addOrUpdate(portalManagedService1);
        // portal managed service 2 should have been skipped
        verify(portalManagedServiceManager, never()).addOrUpdate(portalManagedService2);
        verify(portalManagedServiceManager).findAll();
        verify(portalManagedServiceManager).delete("a3");
    }

    @Test
    public void onApplicationEventReloadApiPlans() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(ModuleLoadListener.RELOAD_API_PLANS_INTERNAL_SERVICE_NAME);
        event = new EntityInvalidationEvent(service, PublishedService.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.CREATE});
        when(serviceManager.findByPrimaryKey(1234L)).thenReturn(service);
        when(policyManager.findByUniqueName(ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME)).thenReturn(null);

        listener.onApplicationEvent(event);

        verify(policyManager).findByUniqueName(ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME);
        final PolicyWithName matchesPolicyFragment = new PolicyWithName(ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME);
        verify(policyManager).save(argThat(matchesPolicyFragment));
        verify(policyVersionManager).checkpointPolicy(argThat(matchesPolicyFragment), Matchers.eq(true), Matchers.eq(true));
    }

    @Test
    public void onApplicationEventApiPortalIntegrationCreateClusterProperty() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(ModuleLoadListener.API_PORTAL_INTEGRATION_INTERNAL_SERVICE_NAME);
        event = new EntityInvalidationEvent(service, PublishedService.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.CREATE});
        final Policy policyOAuth1 = new Policy(PolicyType.INCLUDE_FRAGMENT, ModuleLoadListener.OAUTH1X_FRAGMENT_POLICY_NAME, "<policy></policy>", false);
        policyOAuth1.setGuid("policyOAuth1");
        final Policy policyOAuth2 = new Policy(PolicyType.INCLUDE_FRAGMENT, ModuleLoadListener.OAUTH20_FRAGMENT_POLICY_NAME, "<policy></policy>", false);
        policyOAuth2.setGuid("policyOAuth2");
        final Policy policyApiPlans = new Policy(PolicyType.INCLUDE_FRAGMENT, ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME, "<policy></policy>", false);
        policyApiPlans.setGuid("policyApiPlans");

        final Folder folder = new Folder(ModuleLoadListener.API_DELETED_FOLDER_NAME, null);
        folder.setOid(4444L);
        when(serviceManager.findByPrimaryKey(1234L)).thenReturn(service);
        when(policyManager.findByUniqueName(ModuleLoadListener.OAUTH1X_FRAGMENT_POLICY_NAME)).thenReturn(policyOAuth1);
        when(policyManager.findByUniqueName(ModuleLoadListener.OAUTH20_FRAGMENT_POLICY_NAME)).thenReturn(policyOAuth2);
        when(policyManager.findByUniqueName(ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME)).thenReturn(policyApiPlans);
        when(folderManager.findByUniqueName(ModuleLoadListener.API_DELETED_FOLDER_NAME)).thenReturn(folder);
        when(clusterPropertyManager.findByUniqueName(ModuleConstants.API_DELETED_FOLDER_ID)).thenReturn(null);
        when(clusterPropertyManager.findByUniqueName(ModuleConstants.OAUTH1X_FRAGMENT_GUID)).thenReturn(null);
        when(clusterPropertyManager.findByUniqueName(ModuleConstants.OAUTH20_FRAGMENT_GUID)).thenReturn(null);
        when(clusterPropertyManager.findByUniqueName(ModuleConstants.API_PLANS_FRAGMENT_GUID)).thenReturn(null);

        listener.onApplicationEvent(event);

        verify(clusterPropertyManager, atLeast(4)).save(Matchers.<ClusterProperty>any());
    }

    @Test
    public void onApplicationEventApiPortalIntegrationAlreadyExistClusterProperty() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(ModuleLoadListener.API_PORTAL_INTEGRATION_INTERNAL_SERVICE_NAME);
        event = new EntityInvalidationEvent(service, PublishedService.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.CREATE});
        final Policy policyOAuth1 = new Policy(PolicyType.INCLUDE_FRAGMENT, ModuleLoadListener.OAUTH1X_FRAGMENT_POLICY_NAME, "<policy></policy>", false);
        policyOAuth1.setGuid("policyOAuth1");
        final Policy policyOAuth2 = new Policy(PolicyType.INCLUDE_FRAGMENT, ModuleLoadListener.OAUTH20_FRAGMENT_POLICY_NAME, "<policy></policy>", false);
        policyOAuth2.setGuid("policyOAuth2");
        final Policy policyApiPlans = new Policy(PolicyType.INCLUDE_FRAGMENT, ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME, "<policy></policy>", false);
        policyApiPlans.setGuid("policyApiPlans");

        final Folder folder = new Folder(ModuleLoadListener.API_DELETED_FOLDER_NAME, null);
        folder.setOid(4444L);
        when(serviceManager.findByPrimaryKey(1234L)).thenReturn(service);
        when(policyManager.findByUniqueName(ModuleLoadListener.OAUTH1X_FRAGMENT_POLICY_NAME)).thenReturn(policyOAuth1);
        when(policyManager.findByUniqueName(ModuleLoadListener.OAUTH20_FRAGMENT_POLICY_NAME)).thenReturn(policyOAuth2);
        when(policyManager.findByUniqueName(ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME)).thenReturn(policyApiPlans);
        when(folderManager.findByUniqueName(ModuleLoadListener.API_DELETED_FOLDER_NAME)).thenReturn(folder);

        ClusterProperty folderProperty = new ClusterProperty(ModuleConstants.API_DELETED_FOLDER_ID, String.valueOf(folder.getId()));
        ClusterProperty oauth1xProperty = new ClusterProperty(ModuleConstants.OAUTH1X_FRAGMENT_GUID, policyOAuth1.getGuid());
        ClusterProperty oautn20Property = new ClusterProperty(ModuleConstants.OAUTH20_FRAGMENT_GUID, policyOAuth2.getGuid());
        ClusterProperty policyApiProperty = new ClusterProperty(ModuleConstants.API_PLANS_FRAGMENT_GUID, policyApiPlans.getGuid());

        when(clusterPropertyManager.findByUniqueName(ModuleConstants.API_DELETED_FOLDER_ID)).thenReturn(folderProperty);
        when(clusterPropertyManager.findByUniqueName(ModuleConstants.OAUTH1X_FRAGMENT_GUID)).thenReturn(oauth1xProperty);
        when(clusterPropertyManager.findByUniqueName(ModuleConstants.OAUTH20_FRAGMENT_GUID)).thenReturn(oautn20Property);
        when(clusterPropertyManager.findByUniqueName(ModuleConstants.API_PLANS_FRAGMENT_GUID)).thenReturn(policyApiProperty);

        listener.onApplicationEvent(event);

        verify(clusterPropertyManager, never()).save(Matchers.<ClusterProperty>any());

        assertEquals(String.valueOf(folder.getId()), clusterPropertyManager.findByUniqueName(ModuleConstants.API_DELETED_FOLDER_ID).getValue());
        assertEquals(policyOAuth1.getGuid(), clusterPropertyManager.findByUniqueName(ModuleConstants.OAUTH1X_FRAGMENT_GUID).getValue());
        assertEquals(policyOAuth2.getGuid(), clusterPropertyManager.findByUniqueName(ModuleConstants.OAUTH20_FRAGMENT_GUID).getValue());
        assertEquals(policyApiPlans.getGuid(), clusterPropertyManager.findByUniqueName(ModuleConstants.API_PLANS_FRAGMENT_GUID).getValue());
    }

    @Test
    public void onApplicationEventApiPortalIntegrationClusterPropertyEntityRequestDoesNotExist() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(ModuleLoadListener.API_PORTAL_INTEGRATION_INTERNAL_SERVICE_NAME);
        event = new EntityInvalidationEvent(service, PublishedService.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.CREATE});

        when(serviceManager.findByPrimaryKey(1234L)).thenReturn(service);
        when(folderManager.findByUniqueName(anyString())).thenReturn(null);

        listener.onApplicationEvent(event);

        verify(clusterPropertyManager, atLeast(4)).save(Matchers.<ClusterProperty>any());
        verify(clusterPropertyManager, atMost(4)).save(Matchers.<ClusterProperty>any());

        ClusterProperty notInstalled = new ClusterProperty(ModuleConstants.NOT_INSTALLED_VALUE, ModuleConstants.NOT_INSTALLED_VALUE);

        when(clusterPropertyManager.findByUniqueName(ModuleConstants.API_DELETED_FOLDER_ID)).thenReturn(notInstalled);
        when(clusterPropertyManager.findByUniqueName(ModuleConstants.OAUTH1X_FRAGMENT_GUID)).thenReturn(notInstalled);
        when(clusterPropertyManager.findByUniqueName(ModuleConstants.OAUTH20_FRAGMENT_GUID)).thenReturn(notInstalled);
        when(clusterPropertyManager.findByUniqueName(ModuleConstants.API_PLANS_FRAGMENT_GUID)).thenReturn(notInstalled);
        
        assertEquals(ModuleConstants.NOT_INSTALLED_VALUE, clusterPropertyManager.findByUniqueName(ModuleConstants.API_DELETED_FOLDER_ID).getValue());
        assertEquals(ModuleConstants.NOT_INSTALLED_VALUE, clusterPropertyManager.findByUniqueName(ModuleConstants.OAUTH1X_FRAGMENT_GUID).getValue());
        assertEquals(ModuleConstants.NOT_INSTALLED_VALUE, clusterPropertyManager.findByUniqueName(ModuleConstants.OAUTH20_FRAGMENT_GUID).getValue());
        assertEquals(ModuleConstants.NOT_INSTALLED_VALUE, clusterPropertyManager.findByUniqueName(ModuleConstants.API_PLANS_FRAGMENT_GUID).getValue());

        verify(clusterPropertyManager, never()).update(Matchers.<ClusterProperty>any());

        reset(clusterPropertyManager);

        final Policy policyApiPlans = new Policy(PolicyType.INCLUDE_FRAGMENT, ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME, "<policy></policy>", false);
        policyApiPlans.setGuid("policyApiPlans");
        when(policyManager.findByUniqueName(ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME)).thenReturn(policyApiPlans);
        ClusterProperty policyApiProperty = new ClusterProperty(ModuleConstants.API_PLANS_FRAGMENT_GUID, policyApiPlans.getGuid());
        when(clusterPropertyManager.findByUniqueName(ModuleConstants.API_PLANS_FRAGMENT_GUID)).thenReturn(notInstalled).thenReturn(policyApiProperty);

        listener.onApplicationEvent(event);

        verify(clusterPropertyManager, atLeastOnce()).update(Matchers.<ClusterProperty>any());
        assertNotSame(ModuleConstants.NOT_INSTALLED_VALUE, clusterPropertyManager.findByUniqueName(ModuleConstants.API_PLANS_FRAGMENT_GUID).getValue());
    }

    @Test
    public void onApplicationEventReloadApiPlansFragmentAlreadyExists() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(ModuleLoadListener.RELOAD_API_PLANS_INTERNAL_SERVICE_NAME);
        event = new EntityInvalidationEvent(service, PublishedService.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.CREATE});
        when(serviceManager.findByPrimaryKey(1234L)).thenReturn(service);
        when(policyManager.findByUniqueName(ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME)).thenReturn(new Policy(PolicyType.INCLUDE_FRAGMENT, ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME, "", false));

        listener.onApplicationEvent(event);

        verify(policyManager).findByUniqueName(ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME);
        verify(policyManager, never()).save(Matchers.<Policy>any());
        verify(policyVersionManager, never()).checkpointPolicy(any(Policy.class), any(Boolean.class), any(Boolean.class));
    }

    /**
     * Don't let the exception bubble up.
     */
    @Test
    public void onApplicationEventReloadApiPlansExceptionFindingPolicy() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(ModuleLoadListener.RELOAD_API_PLANS_INTERNAL_SERVICE_NAME);
        event = new EntityInvalidationEvent(service, PublishedService.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.CREATE});
        when(serviceManager.findByPrimaryKey(1234L)).thenReturn(service);
        when(policyManager.findByUniqueName(ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME)).thenThrow(new FindException("cannot find policy"));

        listener.onApplicationEvent(event);

        verify(policyManager).findByUniqueName(ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME);
        verify(policyManager, never()).save(Matchers.<Policy>any());
        verify(policyVersionManager, never()).checkpointPolicy(any(Policy.class), any(Boolean.class), any(Boolean.class));
    }

    /**
     * Don't let the exception bubble up.
     */
    @Test
    public void onApplicationEventReloadApiPlansExceptionSavingPolicy() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(ModuleLoadListener.RELOAD_API_PLANS_INTERNAL_SERVICE_NAME);
        event = new EntityInvalidationEvent(service, PublishedService.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.CREATE});
        when(serviceManager.findByPrimaryKey(1234L)).thenReturn(service);
        when(policyManager.findByUniqueName(ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME)).thenReturn(null);
        when(policyManager.save(any(Policy.class))).thenThrow(new SaveException("cannot save policy"));

        listener.onApplicationEvent(event);

        verify(policyManager).findByUniqueName(ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME);
        final PolicyWithName matchesPolicyFragment = new PolicyWithName(ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME);
        verify(policyManager).save(argThat(matchesPolicyFragment));
        verify(policyVersionManager, never()).checkpointPolicy(any(Policy.class), any(Boolean.class), any(Boolean.class));
    }

    /**
     * Don't let the exception bubble up.
     */
    @Test
    public void onApplicationEventReloadApiPlansExceptionActivatingPolicy() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(ModuleLoadListener.RELOAD_API_PLANS_INTERNAL_SERVICE_NAME);
        event = new EntityInvalidationEvent(service, PublishedService.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.CREATE});
        when(serviceManager.findByPrimaryKey(1234L)).thenReturn(service);
        when(policyManager.findByUniqueName(ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME)).thenReturn(null);
        when(policyVersionManager.checkpointPolicy(any(Policy.class), any(Boolean.class), any(Boolean.class))).thenThrow(new ObjectModelException("cannot activate policy"));

        listener.onApplicationEvent(event);

        verify(policyManager).findByUniqueName(ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME);
        final PolicyWithName matchesPolicyFragment = new PolicyWithName(ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME);
        verify(policyManager).save(argThat(matchesPolicyFragment));
        verify(policyVersionManager).checkpointPolicy(argThat(matchesPolicyFragment), Matchers.eq(true), Matchers.eq(true));
    }

    @Test
    public void onApplicationEventEntityInvalidationEventNotPublishedService() throws Exception {
        event = new EntityInvalidationEvent("", Policy.class, new long[]{}, new char[]{});

        listener.onApplicationEvent(event);

        verify(portalManagedServiceManager, never()).findAllFromPolicy();
        verify(portalManagedServiceManager, never()).addOrUpdate(any(PortalManagedService.class));
    }

    private class PolicyWithName extends ArgumentMatcher<Policy> {
        private final String name;

        PolicyWithName(final String name) {
            this.name = name;
        }

        @Override
        public boolean matches(Object o) {
            final Policy policy = (Policy) o;
            if (name.equals(policy.getName())) {
                return true;
            }
            return false;
        }
    }

    private class ServiceTemplateWithName extends ArgumentMatcher<ServiceTemplate> {
        private final String name;

        /**
         * @param name the expected service template name.
         */
        ServiceTemplateWithName(final String name) {
            this.name = name;
        }

        @Override
        public boolean matches(final Object o) {
            final ServiceTemplate template = (ServiceTemplate) o;
            if (name.equals(template.getName())) {
                return true;
            }
            return false;
        }
    }
}
