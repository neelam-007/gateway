package com.l7tech.server.uddi;

import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UDDIRegistryAdminImplTest {
    private static final long OID = 1234L;
    private UDDIRegistryAdmin admin;
    @Mock
    private UDDIRegistryManager uddiRegistryManager;
    @Mock
    private UDDIHelper uddiHelper;
    @Mock
    private UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager;
    @Mock
    private UDDIPublishStatusManager uddiPublishStatusManager;
    @Mock
    private UDDIServiceControlManager uddiServiceControlManager;
    @Mock
    private UDDIServiceControlRuntimeManager uddiServiceControlRuntimeManager;
    @Mock
    private UDDICoordinator uddiCoordinator;
    @Mock
    private ServiceCache serviceCache;
    @Mock
    private UDDIBusinessServiceStatusManager businessServiceStatusManager;
    @Mock
    private ServiceManager serviceManager;
    private UDDIProxiedServiceInfo info;
    private SecurityZone zone;

    @Before
    public void setup() {
        admin = new UDDIRegistryAdminImpl(uddiRegistryManager, uddiHelper, uddiServiceControlManager, uddiCoordinator,
                serviceCache, uddiProxiedServiceInfoManager, uddiPublishStatusManager, uddiServiceControlRuntimeManager,
                businessServiceStatusManager, serviceManager);
        info = new UDDIProxiedServiceInfo();
        info.setOid(OID);
        zone = new SecurityZone();
    }

    @Test
    public void updateProxiedServiceOnlySecurityZoneChangedFromNull() throws Exception {
        final UDDIProxiedServiceInfo existing = new UDDIProxiedServiceInfo();
        existing.setOid(OID);
        existing.setSecurityZone(null);
        info.setSecurityZone(zone);
        when(uddiProxiedServiceInfoManager.findByPrimaryKey(OID)).thenReturn(existing);

        admin.updateProxiedServiceOnly(info);

        assertEquals(zone, existing.getSecurityZone());
        verify(uddiProxiedServiceInfoManager).update(existing);
    }

    @Test
    public void updateProxiedServiceOnlySecurityZoneChangedToNull() throws Exception {
        final UDDIProxiedServiceInfo existing = new UDDIProxiedServiceInfo();
        existing.setOid(OID);
        existing.setSecurityZone(zone);
        info.setSecurityZone(null);
        when(uddiProxiedServiceInfoManager.findByPrimaryKey(OID)).thenReturn(existing);

        admin.updateProxiedServiceOnly(info);

        assertNull(existing.getSecurityZone());
        verify(uddiProxiedServiceInfoManager).update(existing);
    }

    @Test
    public void updateProxiedServiceOnlySecurityZoneChanged() throws Exception {
        final SecurityZone oldZone = new SecurityZone();
        oldZone.setName("oldZone");
        final UDDIProxiedServiceInfo existing = new UDDIProxiedServiceInfo();
        existing.setOid(OID);
        existing.setSecurityZone(oldZone);
        info.setSecurityZone(zone);
        when(uddiProxiedServiceInfoManager.findByPrimaryKey(OID)).thenReturn(existing);

        admin.updateProxiedServiceOnly(info);

        assertEquals(zone, existing.getSecurityZone());
        verify(uddiProxiedServiceInfoManager).update(existing);
    }

    @Test
    public void updateProxiedServiceOnlySecurityZoneNotChangedFromNull() throws Exception {
        final UDDIProxiedServiceInfo existing = new UDDIProxiedServiceInfo();
        existing.setOid(OID);
        existing.setSecurityZone(null);
        info.setSecurityZone(null);
        when(uddiProxiedServiceInfoManager.findByPrimaryKey(OID)).thenReturn(existing);

        admin.updateProxiedServiceOnly(info);

        verify(uddiProxiedServiceInfoManager, never()).update(existing);
    }

    @Test
    public void updateProxiedServiceOnlySecurityZoneNotChanged() throws Exception {
        final UDDIProxiedServiceInfo existing = new UDDIProxiedServiceInfo();
        existing.setOid(OID);
        existing.setSecurityZone(zone);
        info.setSecurityZone(zone);
        when(uddiProxiedServiceInfoManager.findByPrimaryKey(OID)).thenReturn(existing);

        admin.updateProxiedServiceOnly(info);

        verify(uddiProxiedServiceInfoManager, never()).update(existing);
    }
}
