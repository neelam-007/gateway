package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheExternalReferenceFactory;
import com.l7tech.server.policy.export.PolicyExporterImporterManager;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class ModuleLoadListenerTest {

    private ApplicationContext applicationContext;
    private PolicyExporterImporterManager policyExporterImporterManager;

    @Before
    public void setup() {
        applicationContext = mock(ApplicationContext.class);
        policyExporterImporterManager = mock(PolicyExporterImporterManager.class);
        when(applicationContext.getBean("policyExporterImporterManager", PolicyExporterImporterManager.class)).thenReturn(policyExporterImporterManager);
    }

    @Test
    public void testOnModuleLoadedAndUnloaded() {
        ModuleLoadListener.onModuleLoaded(applicationContext);
        ModuleLoadListener.onModuleUnloaded();

        verify(policyExporterImporterManager, times(3)).register(any(RemoteCacheExternalReferenceFactory.class));
        verify(policyExporterImporterManager, times(3)).unregister(any(RemoteCacheExternalReferenceFactory.class));
    }
}
