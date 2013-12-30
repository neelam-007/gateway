package com.l7tech.server.service;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.policy.PolicyAssertionRbacChecker;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Config;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ServiceAdminImplTest {
    private static final Goid GOID = new Goid(0,1234L);
    private ServiceAdminImpl serviceAdmin;
    @Mock
    private ServiceManager serviceManager;
    @Mock
    private ServiceAliasManager serviceAliasManager;
    @Mock
    private Config config;
    @Mock
    private PolicyVersionManager policyVersionManager;
    @Mock
    private PolicyAssertionRbacChecker policyChecker;

    @Before
    public void setup() {
        serviceAdmin = new ServiceAdminImpl(null, null, serviceManager, serviceAliasManager, null, null, null, null, null, policyVersionManager, config, null, null, null, null, null, null, null);
        ApplicationContexts.inject(serviceAdmin, CollectionUtils.MapBuilder.<String, Object>builder()
                .put("policyChecker", policyChecker)
                .map(), false);
    }

    @Test
    public void findByAlias() throws Exception {
        final PublishedService service = new PublishedService();
        service.setGoid(new Goid(0,1L));
        final PublishedServiceAlias alias = new PublishedServiceAlias(service, null);
        when(serviceAliasManager.findByPrimaryKey(GOID)).thenReturn(alias);
        when(serviceManager.findByPrimaryKey(new Goid(0,1L))).thenReturn(service);
        assertEquals(service, serviceAdmin.findByAlias(GOID));
    }

    @Test
    public void findByAliasDoesNotExist() throws Exception {
        when(serviceAliasManager.findByPrimaryKey(any(Goid.class))).thenReturn(null);
        assertNull(serviceAdmin.findByAlias(GOID));
        verify(serviceManager, never()).findByPrimaryKey(any(Goid.class));
    }

    @Test
    public void saveCreatesRoles() throws Exception {
        final PublishedService service = new PublishedService();
        service.setPolicy(new Policy(PolicyType.PRIVATE_SERVICE, "test", "<xml/>", false));
        serviceAdmin.savePublishedService(service);
        verify(serviceManager).createRoles(service);
    }
}
