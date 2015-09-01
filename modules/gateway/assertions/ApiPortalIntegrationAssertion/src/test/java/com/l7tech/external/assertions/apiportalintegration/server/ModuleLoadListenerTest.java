package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.ApiPortalIntegrationAssertion;

import com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager.PortalManagedEncassManager;
import com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager.PortalManagedServiceManager;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.LicenseChangeEvent;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
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

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
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
    PortalManagedEncassManager portalManagedEncassManager;
    @Mock
    PlatformTransactionManager transactionManager;
    @Mock
    PolicyManager policyManager;
    @Mock
    EncapsulatedAssertionConfigManager encapsulatedAssertionConfigManager;
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
    private List<PortalManagedEncass> portalManagedEncasses;

    @Before
    public void setup() {
        when(applicationContext.getBean("serviceTemplateManager", ServiceTemplateManager.class)).thenReturn(serviceTemplateManager);
        when(applicationContext.getBean("licenseManager", LicenseManager.class)).thenReturn(licenseManager);
        when(applicationContext.getBean("serverConfig", ServerConfig.class)).thenReturn(serverConfig);
        when(applicationContext.getBean("applicationEventProxy", ApplicationEventProxy.class)).thenReturn(applicationEventProxy);
        when(applicationContext.getBean("transactionManager", PlatformTransactionManager.class)).thenReturn(transactionManager);
        when(applicationContext.getBean("policyManager", PolicyManager.class)).thenReturn(policyManager);
when(applicationContext.getBean("encapsulatedAssertionConfigManager", EncapsulatedAssertionConfigManager.class)).thenReturn(encapsulatedAssertionConfigManager);
        when(applicationContext.getBean("policyVersionManager", PolicyVersionManager.class)).thenReturn(policyVersionManager);
        when(applicationContext.getBean("serviceManager", ServiceManager.class)).thenReturn(serviceManager);
        when(applicationContext.getBean("folderManager", FolderManager.class)).thenReturn(folderManager);
        when(applicationContext.getBean("clusterPropertyManager", ClusterPropertyManager.class)).thenReturn(clusterPropertyManager);
        listener = new ModuleLoadListener(applicationContext, apiKeyManager, portalManagedServiceManager, portalManagedEncassManager,
                ModuleLoadListener.API_KEY_MANAGEMENT_SERVICE_POLICY_XML, ModuleLoadListener.API_PORTAL_INTEGRATION_POLICY_XML);
        portalManagedServices = new ArrayList<PortalManagedService>();
        portalManagedEncasses = new ArrayList<>();
    }

    @Test
    public void onModuleLoadedAndUnloaded() {
        when(applicationContext.getBean("genericEntityManager", GenericEntityManager.class)).thenReturn(genericEntityManager);

        ModuleLoadListener.onModuleLoaded(applicationContext);
        ModuleLoadListener.onModuleUnloaded();

        verify(applicationEventProxy).removeApplicationListener(any(ApplicationListener.class));
        verify(serviceTemplateManager).unregister(argThat(new ServiceTemplateWithName(ModuleLoadListener.API_KEY_MANAGEMENT_INTERNAL_SERVICE_NAME)));
        verify(serviceTemplateManager).unregister(argThat(new ServiceTemplateWithName(ModuleLoadListener.API_PORTAL_INTEGRATION_INTERNAL_SERVICE_NAME)));
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

        final PortalManagedEncass encass1 = new PortalManagedEncass();
        encass1.setName("1111");
        final PortalManagedEncass encass2 = new PortalManagedEncass();
        encass2.setName("2222");
        portalManagedEncasses.add(encass1);
        portalManagedEncasses.add(encass2);
        when(portalManagedEncassManager.findAllFromEncass()).thenReturn(portalManagedEncasses);
        when(portalManagedEncassManager.findAll()).thenReturn(Collections.<PortalManagedEncass>emptyList());
        
        listener.onApplicationEvent(event);

        verify(portalManagedServiceManager).findAllFromPolicy();
        verify(portalManagedServiceManager).findAll();
        verify(portalManagedServiceManager).addOrUpdate(service1);
        verify(portalManagedServiceManager).addOrUpdate(service2);

        verify(portalManagedEncassManager).findAllFromEncass();
        verify(portalManagedEncassManager).findAll();
        verify(portalManagedEncassManager).addOrUpdate(encass1);
        verify(portalManagedEncassManager).addOrUpdate(encass2);
    }

    @Test
    public void onApplicationEventReadyForMessagesSkipsErrors() throws Exception {
        event = new ReadyForMessages("", Component.GATEWAY, "");
        final PortalManagedService service1 = new PortalManagedService();
        service1.setName("1111");
        final PortalManagedService service2 = new PortalManagedService();
        service2.setName("2222");
        final PortalManagedService service3 = new PortalManagedService();
        service3.setName("3333");
        portalManagedServices.add(service1);
        portalManagedServices.add(service2);
        portalManagedServices.add(service3);
        when(portalManagedServiceManager.findAllFromPolicy()).thenReturn(portalManagedServices);
        when(portalManagedServiceManager.findAll()).thenReturn(Collections.<PortalManagedService>emptyList());
        // something went wrong when processing service 2
        doThrow(new FindException("mocking exception")).when(portalManagedServiceManager).addOrUpdate(service2);

        final PortalManagedEncass encass1 = new PortalManagedEncass();
        encass1.setName("1111");
        final PortalManagedEncass encass2 = new PortalManagedEncass();
        encass2.setName("2222");
        final PortalManagedEncass encass3 = new PortalManagedEncass();
        encass3.setName("3333");
        portalManagedEncasses.add(encass1);
        portalManagedEncasses.add(encass2);
        portalManagedEncasses.add(encass3);
        when(portalManagedEncassManager.findAllFromEncass()).thenReturn(portalManagedEncasses);
        when(portalManagedEncassManager.findAll()).thenReturn(Collections.<PortalManagedEncass>emptyList());
        // something went wrong when processing encass 2
        doThrow(new FindException("mocking exception")).when(portalManagedEncassManager).addOrUpdate(encass2);

        listener.onApplicationEvent(event);

        verify(portalManagedServiceManager).findAllFromPolicy();
        verify(portalManagedServiceManager).findAll();
        verify(portalManagedServiceManager).addOrUpdate(service1);
        verify(portalManagedServiceManager).addOrUpdate(service2);
        // service 3 should still be processed even though service 2 failed
        verify(portalManagedServiceManager).addOrUpdate(service3);

        verify(portalManagedEncassManager).findAllFromEncass();
        verify(portalManagedEncassManager).findAll();
        verify(portalManagedEncassManager).addOrUpdate(encass1);
        verify(portalManagedEncassManager).addOrUpdate(encass2);
        // encass 3 should still be processed even though encass 2 failed
        verify(portalManagedEncassManager).addOrUpdate(encass3);
    }

    @Test
    public void onApplicationEventReadyForMessagesExceptionReadingPortalManagedServices() throws Exception {
        event = new ReadyForMessages("", Component.GATEWAY, "");
        when(portalManagedServiceManager.findAllFromPolicy()).thenThrow(new FindException("cannot retrieve portal managed services"));
when(portalManagedEncassManager.findAllFromEncass()).thenThrow(new FindException("cannot retrieve portal managed encasses"));

        listener.onApplicationEvent(event);

        verify(portalManagedServiceManager).findAllFromPolicy();
        verify(portalManagedServiceManager, never()).addOrUpdate(any(PortalManagedService.class));

        verify(portalManagedEncassManager).findAllFromEncass();
        verify(portalManagedEncassManager, never()).addOrUpdate(any(PortalManagedEncass.class));
    }

    @Test
    public void onApplicationEventReadyForMessagesExceptionWritingPortalManagedServices() throws Exception {
        event = new ReadyForMessages("", Component.GATEWAY, "");
        portalManagedServices.add(new PortalManagedService());
        when(portalManagedServiceManager.findAllFromPolicy()).thenReturn(portalManagedServices);
        doThrow(new SaveException("mocking exception")).when(portalManagedServiceManager).addOrUpdate(any(PortalManagedService.class));

        portalManagedEncasses.add(new PortalManagedEncass());
        when(portalManagedEncassManager.findAllFromEncass()).thenReturn(portalManagedEncasses);
        doThrow(new SaveException("mocking exception")).when(portalManagedEncassManager).addOrUpdate(any(PortalManagedEncass.class));

        listener.onApplicationEvent(event);

        verify(portalManagedServiceManager).findAllFromPolicy();
        verify(portalManagedServiceManager).addOrUpdate(portalManagedServices.get(0));

        verify(portalManagedEncassManager).findAllFromEncass();
        verify(portalManagedEncassManager).addOrUpdate(portalManagedEncasses.get(0));
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
final List<PortalManagedService> inDatabaseServices = new ArrayList<PortalManagedService>(portalManagedServices);
        // service 3 only exists in database but not in policy
        inDatabaseServices.add(service3);
        when(portalManagedServiceManager.findAll()).thenReturn(inDatabaseServices);

        final PortalManagedEncass encass1 = new PortalManagedEncass();
        encass1.setName("1111");
        final PortalManagedEncass encass2 = new PortalManagedEncass();
        encass2.setName("2222");
        final PortalManagedEncass encass3 = new PortalManagedEncass();
        encass3.setName("3333");
        portalManagedEncasses.add(encass1);
        portalManagedEncasses.add(encass2);
        when(portalManagedEncassManager.findAllFromEncass()).thenReturn(portalManagedEncasses);
        final List<PortalManagedEncass> inDatabaseEncasses = new ArrayList<PortalManagedEncass>(portalManagedEncasses);
        // service 3 only exists in database but not in policy
        inDatabaseEncasses.add(encass3);
        when(portalManagedEncassManager.findAll()).thenReturn(inDatabaseEncasses);

        listener.onApplicationEvent(event);

        verify(portalManagedServiceManager).findAllFromPolicy();
        verify(portalManagedServiceManager).findAll();
        verify(portalManagedServiceManager).addOrUpdate(service1);
        verify(portalManagedServiceManager).addOrUpdate(service2);
        verify(portalManagedServiceManager).delete("3333");

        verify(portalManagedEncassManager).findAllFromEncass();
        verify(portalManagedEncassManager).findAll();
        verify(portalManagedEncassManager).addOrUpdate(encass1);
        verify(portalManagedEncassManager).addOrUpdate(encass2);
        verify(portalManagedEncassManager).delete("3333");
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
        when(portalManagedServiceManager.findAllFromPolicy()).thenReturn(portalManagedServices);
        final List<PortalManagedService> inDatabaseServices = new ArrayList<PortalManagedService>(portalManagedServices);
        inDatabaseServices.add(service1);
        inDatabaseServices.add(service2);
        when(portalManagedServiceManager.findAll()).thenReturn(inDatabaseServices);

        final PortalManagedEncass encass1 = new PortalManagedEncass();
        encass1.setName("1111");
        final PortalManagedEncass encass2 = new PortalManagedEncass();
        encass2.setName("2222");
        final PortalManagedEncass encass3 = new PortalManagedEncass();
        encass3.setName("3333");
        portalManagedEncasses.add(encass1);
        portalManagedEncasses.add(encass2);
        // service 3 only exists in policy but not in database
        portalManagedEncasses.add(encass3);
        when(portalManagedEncassManager.findAllFromEncass()).thenReturn(portalManagedEncasses);
        final List<PortalManagedEncass> inDatabaseEncasses = new ArrayList<PortalManagedEncass>(portalManagedEncasses);
        inDatabaseEncasses.add(encass1);
        inDatabaseEncasses.add(encass2);
        when(portalManagedEncassManager.findAll()).thenReturn(inDatabaseEncasses);

        listener.onApplicationEvent(event);

        verify(portalManagedServiceManager).findAllFromPolicy();
        verify(portalManagedServiceManager).findAll();
        verify(portalManagedServiceManager).addOrUpdate(service1);
        verify(portalManagedServiceManager).addOrUpdate(service2);
        verify(portalManagedServiceManager).addOrUpdate(service3);

        verify(portalManagedEncassManager).findAllFromEncass();
        verify(portalManagedEncassManager).findAll();
        verify(portalManagedEncassManager).addOrUpdate(encass1);
        verify(portalManagedEncassManager).addOrUpdate(encass2);
        verify(portalManagedEncassManager).addOrUpdate(encass3);
    }

    @Test
    public void onApplicationEventLicenseEvent() {
        event = new LicenseChangeEvent("", Level.INFO, "", "");
        final String featureSetName = new ApiPortalIntegrationAssertion().getFeatureSetName();
        when(licenseManager.isFeatureEnabled(featureSetName)).thenReturn(true);

        listener.onApplicationEvent(event);

        verify(licenseManager).isFeatureEnabled(featureSetName);
        verify(serviceTemplateManager).register(argThat(new ServiceTemplateWithName(ModuleLoadListener.API_KEY_MANAGEMENT_INTERNAL_SERVICE_NAME)));
        verify(serviceTemplateManager).register(argThat(new ServiceTemplateWithName(ModuleLoadListener.API_PORTAL_INTEGRATION_INTERNAL_SERVICE_NAME)));
    }

    @Test
    public void onApplicationEventLicenseEventFeatureNotEnabled() {
        event = new LicenseChangeEvent("", Level.INFO, "", "");
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
        listener = new ModuleLoadListener(applicationContext, apiKeyManager, portalManagedServiceManager, portalManagedEncassManager,
                "doesnotexist", ModuleLoadListener.API_PORTAL_INTEGRATION_POLICY_XML);
        event = new LicenseChangeEvent("", Level.INFO, "", "");
        final String featureSetName = new ApiPortalIntegrationAssertion().getFeatureSetName();
        when(licenseManager.isFeatureEnabled(featureSetName)).thenReturn(true);

        listener.onApplicationEvent(event);

        verify(licenseManager).isFeatureEnabled(featureSetName);
        verify(serviceTemplateManager, never()).register(argThat(new ServiceTemplateWithName(ModuleLoadListener.API_KEY_MANAGEMENT_INTERNAL_SERVICE_NAME)));
        verify(serviceTemplateManager).register(argThat(new ServiceTemplateWithName(ModuleLoadListener.API_PORTAL_INTEGRATION_INTERNAL_SERVICE_NAME)));
    }

    @Test
    public void onApplicationEventLicenseEventExceptionReadingPlansPolicyFile() {
        listener = new ModuleLoadListener(applicationContext, apiKeyManager, portalManagedServiceManager, portalManagedEncassManager,
                ModuleLoadListener.API_KEY_MANAGEMENT_SERVICE_POLICY_XML, "doesnotexist");
        event = new LicenseChangeEvent("", Level.INFO, "", "");
        final String featureSetName = new ApiPortalIntegrationAssertion().getFeatureSetName();
        when(licenseManager.isFeatureEnabled(featureSetName)).thenReturn(true);

        listener.onApplicationEvent(event);

        verify(licenseManager).isFeatureEnabled(featureSetName);
        verify(serviceTemplateManager).register(argThat(new ServiceTemplateWithName(ModuleLoadListener.API_KEY_MANAGEMENT_INTERNAL_SERVICE_NAME)));
        verify(serviceTemplateManager, never()).register(argThat(new ServiceTemplateWithName(ModuleLoadListener.API_PORTAL_INTEGRATION_INTERNAL_SERVICE_NAME)));
    }

    @Test
    public void onApplicationEventLicenseEventExceptionReadingApiIntegrationPolicyFile() {
        listener = new ModuleLoadListener(applicationContext, apiKeyManager, portalManagedServiceManager, portalManagedEncassManager,
                "doesnotexist", ModuleLoadListener.API_PORTAL_INTEGRATION_POLICY_XML);
        event = new LicenseChangeEvent("", Level.INFO, "", "");
        final String featureSetName = new ApiPortalIntegrationAssertion().getFeatureSetName();
        when(licenseManager.isFeatureEnabled(featureSetName)).thenReturn(true);

        listener.onApplicationEvent(event);

        verify(licenseManager).isFeatureEnabled(featureSetName);
        verify(serviceTemplateManager).register(argThat(new ServiceTemplateWithName(ModuleLoadListener.API_PORTAL_INTEGRATION_INTERNAL_SERVICE_NAME)));
        verify(serviceTemplateManager, never()).register(argThat(new ServiceTemplateWithName(ModuleLoadListener.API_KEY_MANAGEMENT_INTERNAL_SERVICE_NAME)));
    }

    @Test
    public void onApplicationEventPortalManagedServiceUpdated() throws Exception {
        final PublishedService service = new PublishedService();
        event = new EntityInvalidationEvent(service, PublishedService.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.UPDATE});
        when(serviceManager.findByPrimaryKey(new Goid(0,1234L))).thenReturn(service);
        final PortalManagedService updated = new PortalManagedService();
        updated.setName("a1");
        updated.setDescription(new Goid(0,1234L).toHexString());
        updated.setApiGroup("newGroup");
        final PortalManagedService old = new PortalManagedService();
        old.setName("a1");
        old.setDescription(new Goid(0,1234L).toHexString());

        old.setApiGroup("oldGroup");
        portalManagedServices.add(old);
        when(portalManagedServiceManager.fromService(service)).thenReturn(updated);
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);

        listener.onApplicationEvent(event);

        verify(serviceManager).findByPrimaryKey(new Goid(0,1234L));
        verify(portalManagedServiceManager).fromService(service);
        verify(portalManagedServiceManager).findAll();
        verify(portalManagedServiceManager).addOrUpdate(updated);
    }

    @Test
    public void onApplicationEventPublishedServiceUpdatedNotPortalManaged() throws Exception {
        final PublishedService service = new PublishedService();
        event = new EntityInvalidationEvent(service, PublishedService.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.UPDATE});
        when(serviceManager.findByPrimaryKey(new Goid(0,1234L))).thenReturn(service);
        // not portal managed
        when(portalManagedServiceManager.fromService(service)).thenReturn(null);
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);

        listener.onApplicationEvent(event);

        verify(serviceManager).findByPrimaryKey(new Goid(0,1234L));
        verify(portalManagedServiceManager).fromService(service);
        verify(portalManagedServiceManager).findAll();
        verify(portalManagedServiceManager, never()).delete(anyString());
    }

    @Test
    public void onApplicationEventPublishedServiceUpdatedNoLongerPortalManaged() throws Exception {
        final PublishedService service = new PublishedService();
        event = new EntityInvalidationEvent(service, PublishedService.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.UPDATE});
        final PortalManagedService found = new PortalManagedService();
        found.setName("a1");
        found.setDescription(new Goid(0,1234L).toHexString());

        portalManagedServices.add(found);
        when(serviceManager.findByPrimaryKey(new Goid(0,1234L))).thenReturn(service);
        // was previously portal managed
        when(portalManagedServiceManager.fromService(service)).thenReturn(null);
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);

        listener.onApplicationEvent(event);

        verify(serviceManager).findByPrimaryKey(new Goid(0,1234L));
        verify(portalManagedServiceManager).fromService(service);
        verify(portalManagedServiceManager).findAll();
        verify(portalManagedServiceManager).delete("a1");
    }

    @Test
    public void onApplicationEventPortalManagedServiceUpdatedNameChanged() throws Exception {
        final PublishedService service = new PublishedService();
        event = new EntityInvalidationEvent(service, PublishedService.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.UPDATE});
        when(serviceManager.findByPrimaryKey(new Goid(0,1234L))).thenReturn(service);
        final PortalManagedService updated = new PortalManagedService();
        updated.setName("a2");
        updated.setDescription(new Goid(0,1234L).toHexString());
        final PortalManagedService found = new PortalManagedService();
        found.setName("a1");
        found.setDescription(new Goid(0,1234L).toHexString());

        portalManagedServices.add(found);
        when(portalManagedServiceManager.fromService(service)).thenReturn(updated);
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);

        listener.onApplicationEvent(event);

        verify(serviceManager).findByPrimaryKey(new Goid(0,1234L));
        verify(portalManagedServiceManager).fromService(service);
        verify(portalManagedServiceManager).findAll();
        // old one should be deleted
        verify(portalManagedServiceManager).delete("a1");
        verify(portalManagedServiceManager).addOrUpdate(updated);
    }

    @Test
    public void onApplicationEventPortalManagedServiceUpdatedApiIdConflict() throws Exception {
        final PublishedService service = new PublishedService();
        event = new EntityInvalidationEvent(service, PublishedService.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.UPDATE});
        when(serviceManager.findByPrimaryKey(new Goid(0,1234L))).thenReturn(service);
        final PortalManagedService update = new PortalManagedService();
        update.setName("a2");
        update.setDescription(new Goid(0,1234L).toHexString());

        final PortalManagedService old = new PortalManagedService();
        old.setName("a1");
        old.setDescription(new Goid(0,1234L).toHexString());

        final PortalManagedService conflict = new PortalManagedService();
        // api id conflict
        conflict.setName("a2");
        conflict.setDescription(new Goid(0,5678L).toHexString());
        portalManagedServices.add(old);
        portalManagedServices.add(conflict);
        when(portalManagedServiceManager.fromService(service)).thenReturn(update);
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);
        when(portalManagedServiceManager.find("a2")).thenReturn(conflict);

        listener.onApplicationEvent(event);

        verify(serviceManager).findByPrimaryKey(new Goid(0,1234L));
        verify(portalManagedServiceManager).fromService(service);
        verify(portalManagedServiceManager).findAll();
        verify(portalManagedServiceManager).delete("a1");
        verify(portalManagedServiceManager).find("a2");
        verify(portalManagedServiceManager, never()).addOrUpdate(update);
    }

    @Test
    public void onApplicationEventPublishedServiceCreatedNotPortalManaged() throws Exception {
        final PublishedService service = new PublishedService();
        event = new EntityInvalidationEvent(service, PublishedService.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.CREATE});
        when(serviceManager.findByPrimaryKey(new Goid(0,1234L))).thenReturn(service);
        when(portalManagedServiceManager.fromService(service)).thenReturn(null);

        listener.onApplicationEvent(event);

        verify(serviceManager).findByPrimaryKey(new Goid(0,1234L));
        verify(portalManagedServiceManager).fromService(service);
        verify(portalManagedServiceManager, never()).findAll();
        verify(portalManagedServiceManager, never()).delete(anyString());
    }

    @Test
    public void onApplicationEventPublishedServiceDeletedNotPortalManaged() throws Exception {
        final PublishedService service = new PublishedService();
        event = new EntityInvalidationEvent(service, PublishedService.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.DELETE});
        final PortalManagedService found = new PortalManagedService();
        found.setName("a1");
        // description doesn't match service oid
        found.setDescription("4567");
        portalManagedServices.add(found);
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);

        listener.onApplicationEvent(event);

        verify(portalManagedServiceManager).findAll();
        verify(serviceManager, never()).findByPrimaryKey(any(Goid.class));
        verify(portalManagedServiceManager, never()).fromService(any(PublishedService.class));
        verify(portalManagedServiceManager, never()).delete(anyString());
    }

    @Test
    public void onApplicationEventPortalManagedServiceCreated() throws Exception {
        final PublishedService service = new PublishedService();
        event = new EntityInvalidationEvent(service, PublishedService.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.CREATE});
        when(serviceManager.findByPrimaryKey(new Goid(0,1234L))).thenReturn(service);
        final PortalManagedService portalManagedService = new PortalManagedService();
        when(portalManagedServiceManager.fromService(service)).thenReturn(portalManagedService);

        listener.onApplicationEvent(event);

        verify(serviceManager).findByPrimaryKey(new Goid(0,1234L));
        verify(portalManagedServiceManager).fromService(service);
        verify(portalManagedServiceManager).addOrUpdate(portalManagedService);
    }

    @Test
    public void onApplicationEventPortalManagedServiceDeleted() throws Exception {
        final PublishedService service = new PublishedService();
        event = new EntityInvalidationEvent(service, PublishedService.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.DELETE});
        final PortalManagedService found = new PortalManagedService();
        found.setName("a1");
        found.setDescription(new Goid(0,1234L).toHexString());
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
        event = new EntityInvalidationEvent(service, PublishedService.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.DELETE});
        final PortalManagedService found1 = new PortalManagedService();
        found1.setName("a1");
        found1.setDescription(new Goid(0,1234L).toHexString());

        final PortalManagedService found2 = new PortalManagedService();
        found2.setName("a2");
        found2.setDescription(new Goid(0,1234L).toHexString());

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
        service1.setGoid(new Goid(0,1111L));
        final PublishedService service2 = new PublishedService();
        service2.setGoid(new Goid(0,2222L));
        event = new EntityInvalidationEvent("", PublishedService.class, new Goid[]{new Goid(0,1111L), new Goid(0,2222L), new Goid(0,3333L)}, new char[]{EntityInvalidationEvent.CREATE, EntityInvalidationEvent.UPDATE, EntityInvalidationEvent.DELETE});
        when(serviceManager.findByPrimaryKey(new Goid(0,1111L))).thenReturn(service1);
        when(serviceManager.findByPrimaryKey(new Goid(0,2222L))).thenReturn(service2);
        final PortalManagedService portalManagedService1 = new PortalManagedService();
        portalManagedService1.setName("a1");
        portalManagedService1.setDescription(new Goid(0,1111L).toHexString());
        final PortalManagedService portalManagedService2 = new PortalManagedService();
        portalManagedService2.setName("a2");
        portalManagedService2.setDescription(new Goid(0,2222L).toHexString());
        final PortalManagedService portalManagedService3 = new PortalManagedService();
        portalManagedService3.setName("a3");
        portalManagedService3.setDescription(new Goid(0,3333L).toHexString());
        portalManagedServices.add(portalManagedService1);
        portalManagedServices.add(portalManagedService2);
        portalManagedServices.add(portalManagedService3);
        when(portalManagedServiceManager.fromService(service1)).thenReturn(portalManagedService1);
        when(portalManagedServiceManager.fromService(service2)).thenReturn(portalManagedService2);
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);

        listener.onApplicationEvent(event);

        verify(serviceManager).findByPrimaryKey(new Goid(0,1111L));
        verify(serviceManager).findByPrimaryKey(new Goid(0,2222L));
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
        service1.setGoid(new Goid(0,1111L));
        final PublishedService service2 = new PublishedService();
        service2.setGoid(new Goid(0,2222L));
        event = new EntityInvalidationEvent("", PublishedService.class, new Goid[]{new Goid(0,1111L), new Goid(0,2222L), new Goid(0,3333L)}, new char[]{EntityInvalidationEvent.CREATE, EntityInvalidationEvent.UPDATE, EntityInvalidationEvent.DELETE});
        when(serviceManager.findByPrimaryKey(new Goid(0,1111L))).thenReturn(service1);
        when(serviceManager.findByPrimaryKey(new Goid(0,2222L))).thenReturn(service2);
        final PortalManagedService portalManagedService1 = new PortalManagedService();
        portalManagedService1.setName("a1");
        portalManagedService1.setDescription(new Goid(0,1111L).toHexString());
        final PortalManagedService portalManagedService2 = new PortalManagedService();
        portalManagedService2.setName("a2");
        portalManagedService2.setDescription(new Goid(0,2222L).toHexString());
        final PortalManagedService portalManagedService3 = new PortalManagedService();
        portalManagedService3.setName("a3");
        portalManagedService3.setDescription(new Goid(0,3333L).toHexString());
        portalManagedServices.add(portalManagedService1);
        portalManagedServices.add(portalManagedService2);
        portalManagedServices.add(portalManagedService3);
        when(portalManagedServiceManager.fromService(service1)).thenReturn(portalManagedService1);
        // something went wrong handling service 2
        when(portalManagedServiceManager.fromService(service2)).thenThrow(new FindException("mocking exception"));
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);

        listener.onApplicationEvent(event);

        verify(serviceManager).findByPrimaryKey(new Goid(0,1111L));
        verify(serviceManager).findByPrimaryKey(new Goid(0,2222L));
        verify(portalManagedServiceManager).fromService(service1);
        verify(portalManagedServiceManager).fromService(service2);
        verify(portalManagedServiceManager).addOrUpdate(portalManagedService1);
        // portal managed service 2 should have been skipped
        verify(portalManagedServiceManager, never()).addOrUpdate(portalManagedService2);
        verify(portalManagedServiceManager).findAll();
        verify(portalManagedServiceManager).delete("a3");
    }

    @Test
   public void onApplicationEventEncassCreated() throws FindException, SaveException, UpdateException {
        final EncapsulatedAssertionConfig encapsulatedAssertionConfig = new EncapsulatedAssertionConfig();
        event = new EntityInvalidationEvent(encapsulatedAssertionConfig, EncapsulatedAssertionConfig.class, new Goid[] { new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.CREATE});
        when(encapsulatedAssertionConfigManager.findByPrimaryKey(new Goid(0,1234L))).thenReturn(encapsulatedAssertionConfig);
        final PortalManagedEncass portalManagedEncass = new PortalManagedEncass();
        when(portalManagedEncassManager.fromEncass(encapsulatedAssertionConfig)).thenReturn(portalManagedEncass);

        listener.onApplicationEvent(event);

        verify(encapsulatedAssertionConfigManager).findByPrimaryKey(new Goid(0,1234L));
        verify(portalManagedEncassManager).fromEncass(encapsulatedAssertionConfig);
        verify(portalManagedEncassManager).addOrUpdate(portalManagedEncass);
    }

    @Test
    public void onApplicationEventEncassCreatedNotPortalManaged() throws FindException, DeleteException {
        final EncapsulatedAssertionConfig encapsulatedAssertionConfig = new EncapsulatedAssertionConfig();
        event = new EntityInvalidationEvent(encapsulatedAssertionConfig, EncapsulatedAssertionConfig.class, new Goid[] { new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.CREATE});
        when(encapsulatedAssertionConfigManager.findByPrimaryKey(new Goid(0,1234L))).thenReturn(encapsulatedAssertionConfig);
        when(portalManagedEncassManager.fromEncass(encapsulatedAssertionConfig)).thenReturn(null);

        listener.onApplicationEvent(event);

        verify(encapsulatedAssertionConfigManager).findByPrimaryKey(new Goid(0,1234L));
        verify(portalManagedEncassManager).fromEncass(encapsulatedAssertionConfig);
        verify(portalManagedEncassManager, never()).findAll();
        verify(portalManagedEncassManager, never()).delete(anyString());
    }

    @Test
    public void onApplicationEventPolicyCreated() throws FindException, SaveException, UpdateException {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "", "", false);
        event = new EntityInvalidationEvent(policy, Policy.class, new Goid[] { new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.CREATE});

        final EncapsulatedAssertionConfig encapsulatedAssertionConfig = new EncapsulatedAssertionConfig();
        when(encapsulatedAssertionConfigManager.findByPolicyGoid(new Goid(0,1234L))).thenReturn(Arrays.asList(encapsulatedAssertionConfig));
        final PortalManagedEncass portalManagedEncass = new PortalManagedEncass();
        when(portalManagedEncassManager.fromEncass(encapsulatedAssertionConfig)).thenReturn(portalManagedEncass);

        listener.onApplicationEvent(event);

        verify(encapsulatedAssertionConfigManager).findByPolicyGoid(new Goid(0, 1234L));
        verify(portalManagedEncassManager).fromEncass(encapsulatedAssertionConfig);
        verify(portalManagedEncassManager).addOrUpdate(portalManagedEncass);
    }

    @Test
    public void onApplicationEventPolicyCreatedNoEncass() throws FindException, DeleteException, SaveException, UpdateException {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "", "", false);
        event = new EntityInvalidationEvent(policy, Policy.class, new Goid[] { new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.CREATE});

        when(encapsulatedAssertionConfigManager.findByPolicyGoid(new Goid(0, 1234L))).thenReturn(Collections.<EncapsulatedAssertionConfig>emptySet());

        listener.onApplicationEvent(event);

        verify(encapsulatedAssertionConfigManager).findByPolicyGoid(new Goid(0, 1234L));
        verify(portalManagedEncassManager, never()).fromEncass(any(EncapsulatedAssertionConfig.class));
        verify(portalManagedEncassManager, never()).addOrUpdate(any(PortalManagedEncass.class));
    }

    @Test
    public void onApplicationEventPolicyCreatedNotPortalManaged() throws FindException, DeleteException, SaveException, UpdateException {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "", "", false);
        event = new EntityInvalidationEvent(policy, Policy.class, new Goid[] { new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.CREATE});

        final EncapsulatedAssertionConfig encapsulatedAssertionConfig = new EncapsulatedAssertionConfig();
        when(encapsulatedAssertionConfigManager.findByPolicyGoid(new Goid(0, 1234L))).thenReturn(Arrays.asList(encapsulatedAssertionConfig));
        when(portalManagedEncassManager.fromEncass(encapsulatedAssertionConfig)).thenReturn(null);

        listener.onApplicationEvent(event);

        verify(encapsulatedAssertionConfigManager).findByPolicyGoid(new Goid(0, 1234L));
        verify(portalManagedEncassManager).fromEncass(encapsulatedAssertionConfig);
        verify(portalManagedEncassManager, never()).addOrUpdate(any(PortalManagedEncass.class));
    }

    @Test
    public void onApplicationEventEncassUpdated() throws FindException, SaveException, UpdateException {
        final EncapsulatedAssertionConfig encapsulatedAssertionConfig = new EncapsulatedAssertionConfig();
        event = new EntityInvalidationEvent(encapsulatedAssertionConfig, EncapsulatedAssertionConfig.class, new Goid[] { new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.UPDATE});
        when(encapsulatedAssertionConfigManager.findByPrimaryKey(new Goid(0,1234L))).thenReturn(encapsulatedAssertionConfig);
        final PortalManagedEncass portalManagedEncass = new PortalManagedEncass();
        when(portalManagedEncassManager.fromEncass(encapsulatedAssertionConfig)).thenReturn(portalManagedEncass);

        listener.onApplicationEvent(event);

        verify(encapsulatedAssertionConfigManager).findByPrimaryKey(new Goid(0,1234L));
        verify(portalManagedEncassManager).fromEncass(encapsulatedAssertionConfig);
        verify(portalManagedEncassManager).addOrUpdate(portalManagedEncass);
    }

    @Test
    public void onApplicationEventEncassUpdatedNotPortalManaged() throws FindException, SaveException, UpdateException {
        final EncapsulatedAssertionConfig encapsulatedAssertionConfig = new EncapsulatedAssertionConfig();
        event = new EntityInvalidationEvent(encapsulatedAssertionConfig, EncapsulatedAssertionConfig.class, new Goid[] { new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.UPDATE});
        when(encapsulatedAssertionConfigManager.findByPrimaryKey(new Goid(0,1234L))).thenReturn(encapsulatedAssertionConfig);
        when(portalManagedEncassManager.fromEncass(encapsulatedAssertionConfig)).thenReturn(null);

        listener.onApplicationEvent(event);

        verify(encapsulatedAssertionConfigManager).findByPrimaryKey(new Goid(0,1234L));
        verify(portalManagedEncassManager).fromEncass(encapsulatedAssertionConfig);
        verify(portalManagedEncassManager, never()).addOrUpdate(any(PortalManagedEncass.class));
    }

    @Test
    public void onApplicationEventPolicyUpdated() throws FindException, SaveException, UpdateException {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "", "", false);
        event = new EntityInvalidationEvent(policy, Policy.class, new Goid[] { new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.UPDATE});

        final EncapsulatedAssertionConfig encapsulatedAssertionConfig = new EncapsulatedAssertionConfig();
        encapsulatedAssertionConfig.setGoid(new Goid(0,1234L));
        when(encapsulatedAssertionConfigManager.findByPolicyGoid(new Goid(0, 1234L))).thenReturn(Arrays.asList(encapsulatedAssertionConfig));
        when(encapsulatedAssertionConfigManager.findByPrimaryKey(new Goid(0,1234L))).thenReturn(encapsulatedAssertionConfig);

        final PortalManagedEncass portalManagedEncass = new PortalManagedEncass();
        when(portalManagedEncassManager.fromEncass(encapsulatedAssertionConfig)).thenReturn(portalManagedEncass);

        listener.onApplicationEvent(event);

        verify(encapsulatedAssertionConfigManager).findByPolicyGoid(new Goid(0, 1234L));
        verify(encapsulatedAssertionConfigManager).findByPrimaryKey(new Goid(0,1234L));
        verify(portalManagedEncassManager).fromEncass(encapsulatedAssertionConfig);
        verify(portalManagedEncassManager).addOrUpdate(portalManagedEncass);
    }

    @Test
    public void onApplicationEventPolicyUpdatedNoEncass() throws FindException, DeleteException, SaveException, UpdateException {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "", "", false);
        event = new EntityInvalidationEvent(policy, Policy.class, new Goid[] { new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.UPDATE});

        when(encapsulatedAssertionConfigManager.findByPolicyGoid(new Goid(0, 1234L))).thenReturn(Collections.<EncapsulatedAssertionConfig>emptySet());

        listener.onApplicationEvent(event);

        verify(encapsulatedAssertionConfigManager).findByPolicyGoid(new Goid(0, 1234L));
        verify(portalManagedEncassManager, never()).fromEncass(any(EncapsulatedAssertionConfig.class));
        verify(portalManagedEncassManager, never()).addOrUpdate(any(PortalManagedEncass.class));
    }

    @Test
    public void onApplicationEventPolicyUpdatedNotPortalManaged() throws FindException, DeleteException, SaveException, UpdateException {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "", "", false);
        event = new EntityInvalidationEvent(policy, Policy.class, new Goid[] { new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.UPDATE});

        final EncapsulatedAssertionConfig encapsulatedAssertionConfig = new EncapsulatedAssertionConfig();
        encapsulatedAssertionConfig.setGoid(new Goid(0,1234L));
        when(encapsulatedAssertionConfigManager.findByPolicyGoid(new Goid(0, 1234L))).thenReturn(Arrays.asList(encapsulatedAssertionConfig));
        when(encapsulatedAssertionConfigManager.findByPrimaryKey(new Goid(0,1234L))).thenReturn(encapsulatedAssertionConfig);
        when(portalManagedEncassManager.fromEncass(encapsulatedAssertionConfig)).thenReturn(null);

        listener.onApplicationEvent(event);

        verify(encapsulatedAssertionConfigManager).findByPolicyGoid(new Goid(0, 1234L));
        verify(encapsulatedAssertionConfigManager).findByPrimaryKey(new Goid(0,1234L));
        verify(portalManagedEncassManager).fromEncass(encapsulatedAssertionConfig);
        verify(portalManagedEncassManager, never()).addOrUpdate(any(PortalManagedEncass.class));
    }

    @Test
    public void onApplicationEventEncassDeleted() throws FindException, SaveException, UpdateException, DeleteException {
        final EncapsulatedAssertionConfig encapsulatedAssertionConfig = new EncapsulatedAssertionConfig();
        encapsulatedAssertionConfig.setGoid(new Goid(0,1234L));
        event = new EntityInvalidationEvent(encapsulatedAssertionConfig, EncapsulatedAssertionConfig.class, new Goid[] { new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.DELETE});

        final PortalManagedEncass portalManagedEncass = new PortalManagedEncass();
        portalManagedEncass.setEncassId(new Goid(0,1234L).toHexString());
        portalManagedEncass.setEncassGuid("a1");
        when(portalManagedEncassManager.findAll()).thenReturn(Arrays.asList(portalManagedEncass));

        listener.onApplicationEvent(event);

        verify(portalManagedEncassManager).findAll();
        verify(portalManagedEncassManager).delete("a1");
    }

    @Test
    public void onApplicationEventEncassDeletedNotPortalManaged() throws FindException, SaveException, UpdateException, DeleteException {
        final EncapsulatedAssertionConfig encapsulatedAssertionConfig = new EncapsulatedAssertionConfig();
        encapsulatedAssertionConfig.setGoid(new Goid(0,1234L));
        event = new EntityInvalidationEvent(encapsulatedAssertionConfig, EncapsulatedAssertionConfig.class, new Goid[] { new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.DELETE});

        final PortalManagedEncass portalManagedEncass = new PortalManagedEncass();
        portalManagedEncass.setEncassId(new Goid(0,12345L).toHexString());
        portalManagedEncass.setEncassGuid("a1");
        when(portalManagedEncassManager.findAll()).thenReturn(Arrays.asList(portalManagedEncass));

        listener.onApplicationEvent(event);

        verify(portalManagedEncassManager).findAll();
        verify(portalManagedEncassManager, never()).delete(anyString());
    }

    @Test
    public void onApplicationEventPolicyDeleted() throws FindException, SaveException, UpdateException, DeleteException {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "", "", false);
        event = new EntityInvalidationEvent(policy, Policy.class, new Goid[] { new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.DELETE});

        final EncapsulatedAssertionConfig encapsulatedAssertionConfig = new EncapsulatedAssertionConfig();
        encapsulatedAssertionConfig.setGuid("a1");
        when(encapsulatedAssertionConfigManager.findByPolicyGoid(new Goid(0, 1234L))).thenReturn(Arrays.asList(encapsulatedAssertionConfig));

        listener.onApplicationEvent(event);

        verify(encapsulatedAssertionConfigManager).findByPolicyGoid(new Goid(0, 1234L));
        verify(portalManagedEncassManager).delete("a1");
    }

    @Test
    public void onApplicationEventPolicyDeletedMultiple() throws FindException, SaveException, UpdateException, DeleteException {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "", "", false);
        event = new EntityInvalidationEvent(policy, Policy.class, new Goid[] { new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.DELETE});

        final EncapsulatedAssertionConfig encapsulatedAssertionConfig1 = new EncapsulatedAssertionConfig();
        encapsulatedAssertionConfig1.setGuid("a1");
        final EncapsulatedAssertionConfig encapsulatedAssertionConfig2 = new EncapsulatedAssertionConfig();
        encapsulatedAssertionConfig2.setGuid("a2");
        when(encapsulatedAssertionConfigManager.findByPolicyGoid(new Goid(0, 1234L))).thenReturn(Arrays.asList(encapsulatedAssertionConfig1, encapsulatedAssertionConfig2));

        listener.onApplicationEvent(event);

        verify(encapsulatedAssertionConfigManager).findByPolicyGoid(new Goid(0, 1234L));
        verify(portalManagedEncassManager).delete("a1");
        verify(portalManagedEncassManager).delete("a2");
    }

    @Test
    public void onApplicationEventPolicyDeletedNotPortalManaged() throws FindException, SaveException, UpdateException, DeleteException {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "", "", false);
        event = new EntityInvalidationEvent(policy, Policy.class, new Goid[] { new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.DELETE});

        when(encapsulatedAssertionConfigManager.findByPolicyGoid(new Goid(0, 1234L))).thenReturn(Collections.<EncapsulatedAssertionConfig>emptySet());

        listener.onApplicationEvent(event);

        verify(encapsulatedAssertionConfigManager).findByPolicyGoid(new Goid(0, 1234L));
        verify(portalManagedEncassManager, never()).delete(anyString());
    }

    @Test
    public void onApplicationEventApiPortalIntegrationClusterPropertyEntityRequestDoesNotExist() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(ModuleLoadListener.API_PORTAL_INTEGRATION_INTERNAL_SERVICE_NAME);
        event = new EntityInvalidationEvent(service, PublishedService.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.CREATE});

        when(serviceManager.findByPrimaryKey(new Goid(0,1234L))).thenReturn(service);
        when(folderManager.findByUniqueName(anyString())).thenReturn(null);

        listener.onApplicationEvent(event);

        verify(clusterPropertyManager, atLeast(3)).save(Matchers.<ClusterProperty>any());
        verify(clusterPropertyManager, atMost(5)).save(Matchers.<ClusterProperty>any());

        ClusterProperty notInstalled = new ClusterProperty(ModuleConstants.NOT_INSTALLED_VALUE, ModuleConstants.NOT_INSTALLED_VALUE);

        when(clusterPropertyManager.findByUniqueName(ModuleConstants.API_DELETED_FOLDER_ID)).thenReturn(notInstalled);
        when(clusterPropertyManager.findByUniqueName(ModuleConstants.OAUTH1X_FRAGMENT_GUID)).thenReturn(notInstalled);
        when(clusterPropertyManager.findByUniqueName(ModuleConstants.OAUTH20_FRAGMENT_GUID)).thenReturn(notInstalled);
        when(clusterPropertyManager.findByUniqueName(ModuleConstants.API_PLANS_FRAGMENT_GUID)).thenReturn(notInstalled);
        when(clusterPropertyManager.findByUniqueName(ModuleConstants.ACCOUNT_PLANS_FRAGMENT_GUID)).thenReturn(notInstalled);
        
        assertEquals(ModuleConstants.NOT_INSTALLED_VALUE, clusterPropertyManager.findByUniqueName(ModuleConstants.API_DELETED_FOLDER_ID).getValue());
        assertEquals(ModuleConstants.NOT_INSTALLED_VALUE, clusterPropertyManager.findByUniqueName(ModuleConstants.OAUTH1X_FRAGMENT_GUID).getValue());
        assertEquals(ModuleConstants.NOT_INSTALLED_VALUE, clusterPropertyManager.findByUniqueName(ModuleConstants.OAUTH20_FRAGMENT_GUID).getValue());
        assertEquals(ModuleConstants.NOT_INSTALLED_VALUE, clusterPropertyManager.findByUniqueName(ModuleConstants.API_PLANS_FRAGMENT_GUID).getValue());
        assertEquals(ModuleConstants.NOT_INSTALLED_VALUE, clusterPropertyManager.findByUniqueName(ModuleConstants.ACCOUNT_PLANS_FRAGMENT_GUID).getValue());

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
    public void onApplicationEventApiPlansFragmentAlreadyExists() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(ModuleLoadListener.API_PORTAL_INTEGRATION_INTERNAL_SERVICE_NAME);
        event = new EntityInvalidationEvent(service, PublishedService.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.CREATE});
        when(serviceManager.findByPrimaryKey(new Goid(0,1234L))).thenReturn(service);
        when(policyManager.findByUniqueName(ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME)).thenReturn(new Policy(PolicyType.INCLUDE_FRAGMENT, ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME, "", false));
        when(policyManager.findByUniqueName(ModuleLoadListener.ACCOUNT_PLANS_FRAGMENT_POLICY_NAME)).thenReturn(new Policy(PolicyType.INCLUDE_FRAGMENT, ModuleLoadListener.ACCOUNT_PLANS_FRAGMENT_POLICY_NAME, "", false));

        listener.onApplicationEvent(event);

        verify(policyManager, times(4)).findByUniqueName(anyString());
        verify(policyManager, never()).save(Matchers.<Policy>any());
        verify(policyVersionManager, never()).checkpointPolicy(any(Policy.class), any(Boolean.class), any(Boolean.class));
    }

    //Account Plans

    @Test
    public void onApplicationEventAccountPlansFragmentAlreadyExists() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(ModuleLoadListener.API_PORTAL_INTEGRATION_INTERNAL_SERVICE_NAME);
        event = new EntityInvalidationEvent(service, PublishedService.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.CREATE});
        when(serviceManager.findByPrimaryKey(new Goid(0,1234L))).thenReturn(service);
        when(policyManager.findByUniqueName(ModuleLoadListener.ACCOUNT_PLANS_FRAGMENT_POLICY_NAME)).thenReturn(new Policy(PolicyType.INCLUDE_FRAGMENT, ModuleLoadListener.ACCOUNT_PLANS_FRAGMENT_POLICY_NAME, "", false));
        when(policyManager.findByUniqueName(ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME)).thenReturn(new Policy(PolicyType.INCLUDE_FRAGMENT, ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME, "", false));

        listener.onApplicationEvent(event);

        verify(policyManager, times(4)).findByUniqueName(anyString());
        verify(policyManager, atMost(4)).findByUniqueName(ModuleLoadListener.ACCOUNT_PLANS_FRAGMENT_POLICY_NAME);
        verify(policyManager, never()).save(Matchers.<Policy>any());
        verify(policyVersionManager, never()).checkpointPolicy(any(Policy.class), any(Boolean.class), any(Boolean.class));
    }

    /**
     * Don't let the exception bubble up.
     */
    @Test
    public void onApplicationEventAccountPlansExceptionFindingPolicy() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(ModuleLoadListener.API_PORTAL_INTEGRATION_INTERNAL_SERVICE_NAME);
        event = new EntityInvalidationEvent(service, PublishedService.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.CREATE});
        when(serviceManager.findByPrimaryKey(new Goid(0,1234L))).thenReturn(service);
        when(policyManager.findByUniqueName(anyString())).thenThrow(new FindException("cannot find policy"));

        listener.onApplicationEvent(event);

        verify(policyManager, times(4)).findByUniqueName(anyString());
        verify(policyManager, atMost(4)).findByUniqueName(ModuleLoadListener.ACCOUNT_PLANS_FRAGMENT_POLICY_NAME);
        verify(policyManager, never()).save(Matchers.<Policy>any());
        verify(policyVersionManager, never()).checkpointPolicy(any(Policy.class), any(Boolean.class), any(Boolean.class));
    }

    /**
     * Don't let the exception bubble up.
     */
    @Test
    public void onApplicationEventAccountPlansExceptionSavingPolicy() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(ModuleLoadListener.API_PORTAL_INTEGRATION_INTERNAL_SERVICE_NAME);
        event = new EntityInvalidationEvent(service, PublishedService.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.CREATE});
        when(serviceManager.findByPrimaryKey(new Goid(0,1234L))).thenReturn(service);
        when(policyManager.findByUniqueName(anyString())).thenReturn(null);
        when(policyManager.save(any(Policy.class))).thenThrow(new SaveException("cannot save policy"));

        listener.onApplicationEvent(event);

        verify(policyManager, times(4)).findByUniqueName(anyString());
        verify(policyManager, atMost(4)).findByUniqueName(ModuleLoadListener.ACCOUNT_PLANS_FRAGMENT_POLICY_NAME);
        final PolicyWithName matchesPolicyFragment = new PolicyWithName(ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME);
        verify(policyManager).save(argThat(matchesPolicyFragment));
        //final PolicyWithName matchesPolicyFragment2 = new PolicyWithName(ModuleLoadListener.ACCOUNT_PLANS_FRAGMENT_POLICY_NAME);
        //verify(policyManager).save(argThat(matchesPolicyFragment2));
        verify(policyVersionManager, never()).checkpointPolicy(any(Policy.class), any(Boolean.class), any(Boolean.class));
    }

    /**
     * Don't let the exception bubble up.
     */
    @Test
    public void onApplicationEventAccountPlansExceptionActivatingPolicy() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(ModuleLoadListener.API_PORTAL_INTEGRATION_INTERNAL_SERVICE_NAME);
        event = new EntityInvalidationEvent(service, PublishedService.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.CREATE});
        when(serviceManager.findByPrimaryKey(new Goid(0,1234L))).thenReturn(service);
        when(policyManager.findByUniqueName(anyString())).thenReturn(null);
        when(policyVersionManager.checkpointPolicy(any(Policy.class), any(Boolean.class), any(Boolean.class))).thenThrow(new ObjectModelException("cannot activate policy"));

        listener.onApplicationEvent(event);

        verify(policyManager, times(4)).findByUniqueName(anyString());
        verify(policyManager, atMost(4)).findByUniqueName(ModuleLoadListener.ACCOUNT_PLANS_FRAGMENT_POLICY_NAME);
        final PolicyWithName matchesPolicyFragment = new PolicyWithName(ModuleLoadListener.API_PLANS_FRAGMENT_POLICY_NAME);
        verify(policyManager).save(argThat(matchesPolicyFragment));
        //final PolicyWithName matchesPolicyFragment2 = new PolicyWithName(ModuleLoadListener.ACCOUNT_PLANS_FRAGMENT_POLICY_NAME);
        //verify(policyManager).save(argThat(matchesPolicyFragment2));
        verify(policyVersionManager).checkpointPolicy(argThat(matchesPolicyFragment), Matchers.eq(true), Matchers.eq(true));
    }

    @Test
    public void onApplicationEventEntityInvalidationEventNotPublishedService() throws Exception {
        event = new EntityInvalidationEvent("", Policy.class, new Goid[]{}, new char[]{});

        listener.onApplicationEvent(event);

        verify(portalManagedServiceManager, never()).findAllFromPolicy();
        verify(portalManagedServiceManager, never()).addOrUpdate(any(PortalManagedService.class));
verify(portalManagedEncassManager, never()).findAllFromEncass();
        verify(portalManagedEncassManager, never()).addOrUpdate(any(PortalManagedEncass.class));
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
