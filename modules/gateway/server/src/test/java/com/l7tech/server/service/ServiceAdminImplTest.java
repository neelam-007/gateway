package com.l7tech.server.service;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.util.Config;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ServiceAdminImplTest {
    private static final long OID = 1234L;
    private ServiceAdminImpl serviceAdmin;
    @Mock
    private ServiceManager serviceManager;
    @Mock
    private ServiceAliasManager serviceAliasManager;
    @Mock
    private Config config;
    @Mock
    private PolicyVersionManager policyVersionManager;

    @Before
    public void setup() {
        serviceAdmin = new ServiceAdminImpl(null, null, serviceManager, serviceAliasManager, null, null, null, null, null, policyVersionManager, config, null, null, null, null, null, null, null);
    }

    @Test
    public void findByAlias() throws Exception {
        final PublishedService service = new PublishedService();
        service.setOid(1L);
        final PublishedServiceAlias alias = new PublishedServiceAlias(service, null);
        when(serviceAliasManager.findByPrimaryKey(OID)).thenReturn(alias);
        when(serviceManager.findByPrimaryKey(1L)).thenReturn(service);
        assertEquals(service, serviceAdmin.findByAlias(OID));
    }

    @Test
    public void findByAliasDoesNotExist() throws Exception {
        when(serviceAliasManager.findByPrimaryKey(anyLong())).thenReturn(null);
        assertNull(serviceAdmin.findByAlias(OID));
        verify(serviceManager, never()).findByPrimaryKey(anyLong());
    }
}
