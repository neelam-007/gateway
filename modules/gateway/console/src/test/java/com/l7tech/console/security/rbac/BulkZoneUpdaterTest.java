package com.l7tech.console.security.rbac;

import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.Serializable;
import java.util.*;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BulkZoneUpdaterTest {
    private static final Goid ZONE_GOID = new Goid(0,1234L);
    private BulkZoneUpdater updater;
    @Mock
    private RbacAdmin rbacAdmin;
    @Mock
    private UDDIRegistryAdmin uddiAdmin;
    @Mock
    private JmsAdmin jmsAdmin;
    @Mock
    private PolicyAdmin policyAdmin;
    private List<EntityHeader> headers;

    @Before
    public void setup() {
        updater = new BulkZoneUpdater(rbacAdmin, uddiAdmin, jmsAdmin, policyAdmin);
        headers = new ArrayList<>();
    }

    @Test
    public void bulkUpdateEntityTypeWithoutDependencies() throws Exception {
        headers.add(new EntityHeader(1L, EntityType.POLICY, "test", "test"));
        updater.bulkUpdate(ZONE_GOID, EntityType.POLICY, headers);
        verify(rbacAdmin).setSecurityZoneForEntities(ZONE_GOID,
                Collections.<EntityType, Collection<Serializable>>singletonMap(EntityType.POLICY, Arrays.<Serializable>asList(1L)));
    }

    @Test
    public void bulkUpdateGoidEntityTypeWithoutDependencies() throws Exception {
        headers.add(new EntityHeader(new Goid(0,1), EntityType.JDBC_CONNECTION, "test", "test"));
        updater.bulkUpdate(ZONE_GOID, EntityType.JDBC_CONNECTION, headers);
        verify(rbacAdmin).setSecurityZoneForEntities(ZONE_GOID,
                Collections.<EntityType, Collection<Serializable>>singletonMap(EntityType.JDBC_CONNECTION, Arrays.<Serializable>asList(new Goid(0,1))));
    }

    @Test
    public void bulkUpdateNoEntities() throws Exception {
        updater.bulkUpdate(ZONE_GOID, EntityType.SERVICE, Collections.<EntityHeader>emptyList());
        verify(uddiAdmin, never()).findProxiedServiceInfoForPublishedService(anyLong());
        verify(uddiAdmin, never()).getUDDIServiceControl(anyLong());
        verify(rbacAdmin, never()).setSecurityZoneForEntities(any(Goid.class), anyMap());
    }

    @Test
    public void bulkUpdateServicesUpdatesDependencies() throws Exception {
        final PublishedService serviceWithUddi = new PublishedService();
        serviceWithUddi.setOid(1L);
        final PublishedService serviceWithPolicy = new PublishedService();
        serviceWithPolicy.setOid(2L);
        final Policy policy = new Policy(PolicyType.PRIVATE_SERVICE, "test", "test", false);
        policy.setOid(3L);
        serviceWithPolicy.setPolicy(policy);
        final UDDIProxiedServiceInfo info = new UDDIProxiedServiceInfo();
        info.setOid(4L);
        final UDDIServiceControl control = new UDDIServiceControl();
        control.setOid(5L);
        headers.add(new ServiceHeader(serviceWithUddi));
        headers.add(new ServiceHeader(serviceWithPolicy));
        when(uddiAdmin.findProxiedServiceInfoForPublishedService(1L)).thenReturn(info);
        when(uddiAdmin.getUDDIServiceControl(1L)).thenReturn(control);
        when(policyAdmin.findPolicyByPrimaryKey(3L)).thenReturn(policy);

        updater.bulkUpdate(ZONE_GOID, EntityType.SERVICE, headers);
        final Map<EntityType, Collection<Serializable>> expected = new HashMap<>();
        expected.put(EntityType.SERVICE, Arrays.<Serializable>asList(1L, 2L));
        expected.put(EntityType.UDDI_PROXIED_SERVICE_INFO, Arrays.<Serializable>asList(4L));
        expected.put(EntityType.UDDI_SERVICE_CONTROL, Arrays.<Serializable>asList(5L));
        expected.put(EntityType.POLICY, Arrays.<Serializable>asList(3L));
        verify(rbacAdmin).setSecurityZoneForEntities(ZONE_GOID, expected);
    }

    @Test
    public void bulkUpdateJmsConnectionUpdatesDependencies() throws Exception {
        final JmsEndpoint e1 = new JmsEndpoint();
        e1.setGoid(new Goid(0,2L));
        final JmsEndpoint e2 = new JmsEndpoint();
        e2.setGoid(new Goid(0,3L));
        headers.add(new EntityHeader(new Goid(0,1L), EntityType.JMS_CONNECTION, "testWithEndpoint", "testWithEndpoint"));
        headers.add(new EntityHeader(new Goid(0,4L), EntityType.JMS_CONNECTION, "testWithoutEndpoint", "testWithoutEndpoint"));
        when(jmsAdmin.getEndpointsForConnection(new Goid(0,1L))).thenReturn(new JmsEndpoint[]{e1, e2});

        updater.bulkUpdate(ZONE_GOID, EntityType.JMS_CONNECTION, headers);
        final Map<EntityType, Collection<Serializable>> expected = new HashMap<>();
        expected.put(EntityType.JMS_CONNECTION, Arrays.<Serializable>asList(new Goid(0,1L),new Goid(0, 4L)));
        expected.put(EntityType.JMS_ENDPOINT, Arrays.<Serializable>asList(new Goid(0,2L), new Goid(0,3L)));
        verify(rbacAdmin).setSecurityZoneForEntities(ZONE_GOID, expected);
    }

    @Test(expected = IllegalArgumentException.class)
    public void bulkUpdatePrivateKeys() throws Exception {
        headers.add(new EntityHeader(new Goid(0,1L), EntityType.SSG_KEY_ENTRY, "privateKeysAreNotPersisted", "privateKeysAreNotPersisted"));
        updater.bulkUpdate(ZONE_GOID, EntityType.SSG_KEY_ENTRY, headers);
    }

    @Test(expected = UpdateException.class)
    public void bulkUpdateError() throws Exception {
        headers.add(new EntityHeader(new Goid(0,1L), EntityType.POLICY, "test", "test"));
        doThrow(new UpdateException("mocking exception")).when(rbacAdmin).setSecurityZoneForEntities(any(Goid.class), anyMap());
        updater.bulkUpdate(ZONE_GOID, EntityType.POLICY, headers);
    }

    @Test(expected = FindException.class)
    public void bulkUpdateFindError() throws Exception {
        headers.add(new EntityHeader(new Goid(0,1L), EntityType.SERVICE, "testWithUddi", "testWithUddi"));
        when(uddiAdmin.findProxiedServiceInfoForPublishedService(anyLong())).thenThrow(new FindException("mocking exception"));
        updater.bulkUpdate(ZONE_GOID, EntityType.SERVICE, headers);
    }

}
