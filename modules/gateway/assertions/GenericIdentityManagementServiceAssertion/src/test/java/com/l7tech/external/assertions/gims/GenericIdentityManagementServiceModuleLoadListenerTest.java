package com.l7tech.external.assertions.gims;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.server.event.system.LicenseEvent;
import com.l7tech.server.service.ServiceTemplateManager;
import com.l7tech.server.util.ApplicationEventProxy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

import java.util.logging.Level;

import static org.mockito.Mockito.*;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 1/24/13
 */
@RunWith(MockitoJUnitRunner.class)
public class GenericIdentityManagementServiceModuleLoadListenerTest {

    @Mock
    ApplicationContext mockContext;
    @Mock
    ServiceTemplateManager mockServiceTemplateManager;
    @Mock
    ApplicationEventProxy mockEventProxy;
    @Mock
    LicenseManager mockLicenseManager;

    GenericIdentityManagementServiceModuleLoadListener fixture;

    @Before
    public void setUp() throws Exception {
        when(mockContext.getBean("serviceTemplateManager", ServiceTemplateManager.class)).thenReturn(mockServiceTemplateManager);
        when(mockContext.getBean("applicationEventProxy", ApplicationEventProxy.class)).thenReturn(mockEventProxy);
        when(mockContext.getBean("licenseManager", LicenseManager.class)).thenReturn(mockLicenseManager);

        fixture = new GenericIdentityManagementServiceModuleLoadListener(mockContext);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testOnModuleLoadedUnloaded() throws Exception {
        when(mockLicenseManager.isFeatureEnabled(new GenericIdentityManagementServiceAssertion().getFeatureSetName())).thenReturn(true);
        GenericIdentityManagementServiceModuleLoadListener.onModuleLoaded(mockContext);
        verify(mockServiceTemplateManager, times(1)).register(argThat(new ServiceTemplateArgMatcher(GenericIdentityManagementServiceModuleLoadListener.SERVICE_TEMPLATE_NAME, GenericIdentityManagementServiceModuleLoadListener.DEFAULT_URI_PREFIX)));
        GenericIdentityManagementServiceModuleLoadListener.onModuleUnloaded();
        verify(mockServiceTemplateManager, times(1)).unregister(argThat(new ServiceTemplateArgMatcher(GenericIdentityManagementServiceModuleLoadListener.SERVICE_TEMPLATE_NAME, GenericIdentityManagementServiceModuleLoadListener.DEFAULT_URI_PREFIX)));
    }

    @Test
    public void testOnApplicationEvent() throws Exception {
        when(mockLicenseManager.isFeatureEnabled(new GenericIdentityManagementServiceAssertion().getFeatureSetName())).thenReturn(true);
        ApplicationEvent event = new LicenseEvent("", Level.INFO, "", "");
        fixture.onApplicationEvent(event);
        verify(mockServiceTemplateManager, times(1)).register(argThat(new ServiceTemplateArgMatcher(GenericIdentityManagementServiceModuleLoadListener.SERVICE_TEMPLATE_NAME, GenericIdentityManagementServiceModuleLoadListener.DEFAULT_URI_PREFIX)));

    }

    @Test
    public void shouldFailOnLicenseEventWhenFeatureNotEnabled() throws Exception {
        when(mockLicenseManager.isFeatureEnabled(new GenericIdentityManagementServiceAssertion().getFeatureSetName())).thenReturn(false);
        ApplicationEvent event = new LicenseEvent("", Level.INFO, "", "");
        fixture.onApplicationEvent(event);
        verify(mockServiceTemplateManager, never()).register(argThat(new ServiceTemplateArgMatcher(GenericIdentityManagementServiceModuleLoadListener.SERVICE_TEMPLATE_NAME, GenericIdentityManagementServiceModuleLoadListener.DEFAULT_URI_PREFIX)));

    }

    private class ServiceTemplateArgMatcher extends ArgumentMatcher<ServiceTemplate> {

        private final String templateName;
        private final String templateUrl;

        public ServiceTemplateArgMatcher(String name, String url) {
            templateName = name;
            templateUrl = url;
        }

        @Override
        public boolean matches(Object o) {
            if(o == null)
                return false;
            if(o instanceof ServiceTemplate) {
                ServiceTemplate t = (ServiceTemplate)o;
                return templateName.equals(t.getName()) && templateUrl.equals(t.getDefaultUriPrefix());
            }
            return false;
        }
    }
}
