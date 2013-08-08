package com.l7tech.server.uddi;

import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIPublishStatus;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.wsdl.Wsdl;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UDDIRegistryAdminImplTest {
    private static final Goid GOID = new Goid(0,1234L);
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
    @Mock
    private PublishedService service;
    private Map<String, Object> properties;
    private UDDIServiceControl control;
    private UDDIRegistry registry;
    @Mock
    private Wsdl wsdl;

    @Before
    public void setup() {
        admin = new UDDIRegistryAdminImpl(uddiRegistryManager, uddiHelper, uddiServiceControlManager, uddiCoordinator,
                serviceCache, uddiProxiedServiceInfoManager, uddiPublishStatusManager, uddiServiceControlRuntimeManager,
                businessServiceStatusManager, serviceManager);
        info = new UDDIProxiedServiceInfo();
        info.setGoid(GOID);
        zone = new SecurityZone();
        properties = new HashMap<>();
        control = new UDDIServiceControl();
        registry = new UDDIRegistry();
    }

    @Test
    public void updateProxiedServiceOnlySecurityZoneChangedFromNull() throws Exception {
        final UDDIProxiedServiceInfo existing = new UDDIProxiedServiceInfo();
        existing.setGoid(GOID);
        existing.setSecurityZone(null);
        info.setSecurityZone(zone);
        when(uddiProxiedServiceInfoManager.findByPrimaryKey(GOID)).thenReturn(existing);

        admin.updateProxiedServiceOnly(info);

        assertEquals(zone, existing.getSecurityZone());
        verify(uddiProxiedServiceInfoManager).update(existing);
    }

    @Test
    public void updateProxiedServiceOnlySecurityZoneChangedToNull() throws Exception {
        final UDDIProxiedServiceInfo existing = new UDDIProxiedServiceInfo();
        existing.setGoid(GOID);
        existing.setSecurityZone(zone);
        info.setSecurityZone(null);
        when(uddiProxiedServiceInfoManager.findByPrimaryKey(GOID)).thenReturn(existing);

        admin.updateProxiedServiceOnly(info);

        assertNull(existing.getSecurityZone());
        verify(uddiProxiedServiceInfoManager).update(existing);
    }

    @Test
    public void updateProxiedServiceOnlySecurityZoneChanged() throws Exception {
        final SecurityZone oldZone = new SecurityZone();
        oldZone.setName("oldZone");
        final UDDIProxiedServiceInfo existing = new UDDIProxiedServiceInfo();
        existing.setGoid(GOID);
        existing.setSecurityZone(oldZone);
        info.setSecurityZone(zone);
        when(uddiProxiedServiceInfoManager.findByPrimaryKey(GOID)).thenReturn(existing);

        admin.updateProxiedServiceOnly(info);

        assertEquals(zone, existing.getSecurityZone());
        verify(uddiProxiedServiceInfoManager).update(existing);
    }

    @Test
    public void updateProxiedServiceOnlySecurityZoneNotChangedFromNull() throws Exception {
        final UDDIProxiedServiceInfo existing = new UDDIProxiedServiceInfo();
        existing.setGoid(GOID);
        existing.setSecurityZone(null);
        info.setSecurityZone(null);
        when(uddiProxiedServiceInfoManager.findByPrimaryKey(GOID)).thenReturn(existing);

        admin.updateProxiedServiceOnly(info);

        verify(uddiProxiedServiceInfoManager, never()).update(existing);
    }

    @Test
    public void updateProxiedServiceOnlySecurityZoneNotChanged() throws Exception {
        final UDDIProxiedServiceInfo existing = new UDDIProxiedServiceInfo();
        existing.setGoid(GOID);
        existing.setSecurityZone(zone);
        info.setSecurityZone(zone);
        when(uddiProxiedServiceInfoManager.findByPrimaryKey(GOID)).thenReturn(existing);

        admin.updateProxiedServiceOnly(info);

        verify(uddiProxiedServiceInfoManager, never()).update(existing);
    }

    @Test
    public void publishGatewayEndpointSetsSecurityZone() throws Exception {
        when(serviceCache.getCachedService(any(Goid.class))).thenReturn(service);
        when(uddiServiceControlManager.findByPublishedServiceGoid(any(Goid.class))).thenReturn(control);
        when(uddiRegistryManager.findByPrimaryKey(any(Goid.class))).thenReturn(registry);
        when(service.parsedWsdl()).thenReturn(wsdl);
        when(wsdl.getHash()).thenReturn("abc123");

        admin.publishGatewayEndpoint(service, false, properties, zone);

        verify(uddiProxiedServiceInfoManager).save(argThat(infoWithSecurityZone(zone)));
        verify(uddiPublishStatusManager).save(any(UDDIPublishStatus.class));
    }

    @Test
    public void publishGatewayEndpointGifSetsSecurityZone() throws Exception {
        properties.put(UDDIProxiedServiceInfo.GIF_SCHEME, UDDIRegistryAdmin.EndpointScheme.HTTP);
        when(serviceCache.getCachedService(any(Goid.class))).thenReturn(service);
        when(uddiServiceControlManager.findByPublishedServiceGoid(any(Goid.class))).thenReturn(control);
        when(uddiRegistryManager.findByPrimaryKey(any(Goid.class))).thenReturn(registry);
        when(service.parsedWsdl()).thenReturn(wsdl);
        when(wsdl.getHash()).thenReturn("abc123");

        admin.publishGatewayEndpointGif(service, properties, zone);

        verify(uddiProxiedServiceInfoManager).save(argThat(infoWithSecurityZone(zone)));
        verify(uddiPublishStatusManager).save(any(UDDIPublishStatus.class));
    }

    @Test
    public void overwriteBusinessServiceInUDDISetsSecurityZone() throws Exception {
        when(serviceCache.getCachedService(any(Goid.class))).thenReturn(service);
        when(uddiServiceControlManager.findByPublishedServiceGoid(any(Goid.class))).thenReturn(control);
        when(service.parsedWsdl()).thenReturn(wsdl);
        when(wsdl.getHash()).thenReturn("abc123");

        admin.overwriteBusinessServiceInUDDI(service, false, zone);

        verify(uddiProxiedServiceInfoManager).save(argThat(infoWithSecurityZone(zone)));
        verify(uddiPublishStatusManager).save(any(UDDIPublishStatus.class));
    }

    @Test
    public void publishGatewayWsdlSetsSecurityZone() throws Exception {
        when(uddiRegistryManager.findByPrimaryKey(any(Goid.class))).thenReturn(registry);
        when(serviceCache.getCachedService(any(Goid.class))).thenReturn(service);
        when(service.parsedWsdl()).thenReturn(wsdl);
        when(wsdl.getHash()).thenReturn("abc123");

        admin.publishGatewayWsdl(service, new Goid(0,1234L), "bKey", "bName", false, zone);

        verify(uddiProxiedServiceInfoManager).save(argThat(infoWithSecurityZone(zone)));
        verify(uddiPublishStatusManager).save(any(UDDIPublishStatus.class));
    }

    private ProxiedServiceInfoWithSecurityZone infoWithSecurityZone(final SecurityZone zone) {
        return new ProxiedServiceInfoWithSecurityZone(zone);
    }

    private class ProxiedServiceInfoWithSecurityZone extends ArgumentMatcher<UDDIProxiedServiceInfo> {
        private SecurityZone zone;

        private ProxiedServiceInfoWithSecurityZone(@Nullable final SecurityZone zone) {
            this.zone = zone;
        }

        @Override
        public boolean matches(final Object o) {
            if (o != null) {
                final UDDIProxiedServiceInfo serviceInfo = (UDDIProxiedServiceInfo) o;
                if ((zone == null && serviceInfo.getSecurityZone() == null) ||
                        (zone != null && zone.equals(serviceInfo.getSecurityZone()))) {
                    return true;
                }
            }
            return false;
        }
    }
}
