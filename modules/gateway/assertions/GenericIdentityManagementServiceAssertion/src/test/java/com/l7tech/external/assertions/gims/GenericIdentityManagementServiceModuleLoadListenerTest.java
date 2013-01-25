package com.l7tech.external.assertions.gims;

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

    GenericIdentityManagementServiceModuleLoadListener fixture;

    @Before
    public void setUp() throws Exception {
        when(mockContext.getBean("serviceTemplateManager", ServiceTemplateManager.class)).thenReturn(mockServiceTemplateManager);
        when(mockContext.getBean("applicationEventProxy", ApplicationEventProxy.class)).thenReturn(mockEventProxy);

        fixture = new GenericIdentityManagementServiceModuleLoadListener(mockContext);
    }

    @After
    public void tearDown() throws Exception {

    }


    @Test
    public void testOnModuleUnloaded() throws Exception {

        GenericIdentityManagementServiceModuleLoadListener.onModuleLoaded(mockContext);
        GenericIdentityManagementServiceModuleLoadListener.onModuleUnloaded();

        verify(mockServiceTemplateManager, times(1)).unregister(argThat(new ServiceTemplateArgMatcher(GenericIdentityManagementServiceModuleLoadListener.SERVICE_TEMPLATE_NAME, GenericIdentityManagementServiceModuleLoadListener.DEFAULT_URI_PREFIX)));
    }

    @Test
    public void testOnApplicationEvent() throws Exception {
        ApplicationEvent event = new LicenseEvent("", Level.INFO, "", "");
        fixture.onApplicationEvent(event);
        verify(mockServiceTemplateManager).register(argThat(new ServiceTemplateArgMatcher(GenericIdentityManagementServiceModuleLoadListener.SERVICE_TEMPLATE_NAME, GenericIdentityManagementServiceModuleLoadListener.DEFAULT_URI_PREFIX)));

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
