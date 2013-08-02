package com.l7tech.external.assertions.apiportalintegration.server.upgrade;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.apiportalintegration.server.ApiKeyData;
import com.l7tech.external.assertions.apiportalintegration.server.ModuleConstants;
import com.l7tech.external.assertions.apiportalintegration.server.PortalGenericEntityManager;
import com.l7tech.external.assertions.apiportalintegration.server.PortalManagedService;
import com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager.PortalManagedServiceManager;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.service.ServiceManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UpgradePortalAdminImplTest {
    private UpgradePortalAdmin admin;
    @Mock
    private ServiceManager serviceManager;
    @Mock
    private PortalGenericEntityManager<ApiKeyData> keyManager;
    @Mock
    private PortalManagedServiceManager portalManagedServiceManager;
    @Mock
    private ClusterPropertyManager clusterPropertyManager;
    private List<ServiceHeader> headers;
    private List<PortalManagedService> portalManagedServices;
    private List<ApiKeyData> keys;

    @Before
    public void setup() {
        admin = new UpgradePortalAdminImpl(serviceManager, keyManager, portalManagedServiceManager, clusterPropertyManager);
        headers = new ArrayList<ServiceHeader>();
        portalManagedServices = new ArrayList<PortalManagedService>();
        keys = new ArrayList<ApiKeyData>();
    }

    @Test
    public void upgradeServices() throws Exception {
        // services do not have an apiId
        final PublishedService service1 = createService(new Goid(0,1L), null, "Test Service 1", null, "${apiKeyRecord.service}", "${service.oid}");
        final PublishedService service2 = createService(new Goid(0,2L), null, "Test Service 2", "${service.oid}", "${apiKeyRecord.service}", "${service.oid}");
        headers.add(new ServiceHeader(service1));
        headers.add(new ServiceHeader(service2));
        when(serviceManager.findAllHeaders()).thenReturn(headers);
        when(serviceManager.findByPrimaryKey(new Goid(0,1L))).thenReturn(service1);
        when(serviceManager.findByPrimaryKey(new Goid(0,2L))).thenReturn(service2);
        when(portalManagedServiceManager.fromService(service1)).thenReturn(createPortalManagedService("n1", "1"));
        when(portalManagedServiceManager.fromService(service2)).thenReturn(createPortalManagedService("n2", "2"));

        final List<UpgradedEntity> upgradedEntities = admin.upgradeServicesTo2_1();

        assertEquals(2, upgradedEntities.size());
        assertEquals("API", upgradedEntities.get(0).getType());
        assertEquals("n1", upgradedEntities.get(0).getId());
        assertEquals("Test Service 1", upgradedEntities.get(0).getDescription());
        assertEquals("API", upgradedEntities.get(1).getType());
        assertEquals("n2", upgradedEntities.get(1).getId());
        assertEquals("Test Service 2", upgradedEntities.get(1).getDescription());

        verify(serviceManager, times(2)).update(argThat(new ServiceWithApiIdsAndComparisonAssertion()));
    }

    @Test
    public void upgradeServiceWithMultipleAssertions() throws Exception {
        // service has multiple assertions w/out an apiId
        final PublishedService service1 = createServiceWithMultipleAssertions(new Goid(0,1L), 2, "Test Service 1", 1, null, 1, "${apiKeyRecord.service}", "${service.oid}");
        headers.add(new ServiceHeader(service1));
        when(serviceManager.findAllHeaders()).thenReturn(headers);
        when(serviceManager.findByPrimaryKey(new Goid(0,1L))).thenReturn(service1);
        when(portalManagedServiceManager.fromService(service1)).thenReturn(createPortalManagedService("n1", "1"));

        final List<UpgradedEntity> upgradedEntities = admin.upgradeServicesTo2_1();

        assertEquals(1, upgradedEntities.size());
        assertEquals("API", upgradedEntities.get(0).getType());
        assertEquals("n1", upgradedEntities.get(0).getId());
        assertEquals("Test Service 1", upgradedEntities.get(0).getDescription());

        verify(serviceManager).update(argThat(new ServiceWithApiIdsAndComparisonAssertion(2, 1, "${portal.managed.service.apiId}", 1, "${apiKeyRecord.service}", "${portal.managed.service.apiId}")));
    }

    @Test
    public void upgradeServiceNotPortalManaged() throws Exception {
        // service an assertion w/out an apiId
        final PublishedService service1 = createService(new Goid(0,1L), null, "Test Service 1", null, "${apiKeyRecord.service}", "${service.oid}");
        headers.add(new ServiceHeader(service1));
        when(serviceManager.findAllHeaders()).thenReturn(headers);
        when(serviceManager.findByPrimaryKey(new Goid(0,1L))).thenReturn(service1);
        // NOT PORTAL MANAGED!!!! - could happen if all assertions are disabled
        when(portalManagedServiceManager.fromService(service1)).thenReturn(null);

        final List<UpgradedEntity> upgradedEntities = admin.upgradeServicesTo2_1();

        assertEquals(1, upgradedEntities.size());
        assertEquals("SERVICE", upgradedEntities.get(0).getType());
        assertEquals(new Goid(0,1L).toHexString(), upgradedEntities.get(0).getId());
        assertEquals("Test Service 1", upgradedEntities.get(0).getDescription());

        verify(serviceManager).update(argThat(new ServiceWithApiIdsAndComparisonAssertion()));
    }

    @Test
    public void upgradeServiceNoPortalManagedServiceFlag() throws Exception {
        final PublishedService service1 = createNonPortalManagedService(new Goid(0,1L));
        headers.add(new ServiceHeader(service1));
        when(serviceManager.findAllHeaders()).thenReturn(headers);
        when(serviceManager.findByPrimaryKey(new Goid(0,1L))).thenReturn(service1);

        final List<UpgradedEntity> upgradedEntities = admin.upgradeServicesTo2_1();

        assertTrue(upgradedEntities.isEmpty());
        verify(serviceManager, never()).update(Matchers.<PublishedService>any());
    }

    @Test
    public void upgradeServiceContainsFlagInPolicyButNotAsAssertion() throws Exception {
        final PublishedService service1 = createNonPortalManagedServiceWithComment(new Goid(0,1L), ModuleConstants.PORTAL_MANAGED_SERVICE_INDICATOR);
        headers.add(new ServiceHeader(service1));
        when(serviceManager.findAllHeaders()).thenReturn(headers);
        when(serviceManager.findByPrimaryKey(new Goid(0,1L))).thenReturn(service1);

        final List<UpgradedEntity> upgradedEntities = admin.upgradeServicesTo2_1();

        assertTrue(upgradedEntities.isEmpty());
        verify(serviceManager, never()).update(Matchers.<PublishedService>any());
    }

    @Test
    public void upgradeServiceDoesNotRequireUpgrade() throws Exception {
        final PublishedService service1 = createService(new Goid(0,1L), "a1", "Test Service", "${portal.managed.service.apiId}", "${apiKeyRecord.service}", "${portal.managed.service.apiId}");
        headers.add(new ServiceHeader(service1));
        when(serviceManager.findAllHeaders()).thenReturn(headers);
        when(serviceManager.findByPrimaryKey(new Goid(0,1L))).thenReturn(service1);

        final List<UpgradedEntity> upgradedEntities = admin.upgradeServicesTo2_1();

        assertTrue(upgradedEntities.isEmpty());
        verify(serviceManager, never()).update(Matchers.<PublishedService>any());
    }

    @Test
    public void upgradeServiceMultipleComparisonAssertion() throws Exception {
        // service has multiple comparison assertions
        final PublishedService service1 = createServiceWithMultipleAssertions(new Goid(0,1L), 1, "Test Service 1", 1, null, 2, "${apiKeyRecord.service}", "${service.oid}");
        headers.add(new ServiceHeader(service1));
        when(serviceManager.findAllHeaders()).thenReturn(headers);
        when(serviceManager.findByPrimaryKey(new Goid(0,1L))).thenReturn(service1);
        when(portalManagedServiceManager.fromService(service1)).thenReturn(createPortalManagedService("n1", "1"));

        final List<UpgradedEntity> upgradedEntities = admin.upgradeServicesTo2_1();

        assertEquals(1, upgradedEntities.size());
        assertEquals("API", upgradedEntities.get(0).getType());
        assertEquals("n1", upgradedEntities.get(0).getId());
        assertEquals("Test Service 1", upgradedEntities.get(0).getDescription());

        verify(serviceManager).update(argThat(new ServiceWithApiIdsAndComparisonAssertion(1, 1, "${portal.managed.service.apiId}", 2, "${apiKeyRecord.service}", "${portal.managed.service.apiId}")));
    }

    @Test
    public void upgradeServiceSkipsIrrelevantComparisonAssertions() throws Exception {
        final PublishedService service1 = createService(new Goid(0,1L), "a1", "Test Service", "${portal.managed.service.apiId}", "${irrelevant}", "${service.oid}");
        headers.add(new ServiceHeader(service1));
        when(serviceManager.findAllHeaders()).thenReturn(headers);
        when(serviceManager.findByPrimaryKey(new Goid(0,1L))).thenReturn(service1);

        final List<UpgradedEntity> upgradedEntities = admin.upgradeServicesTo2_1();

        assertTrue(upgradedEntities.isEmpty());
        verify(serviceManager, never()).update(Matchers.<PublishedService>any());
    }

    @Test
    public void upgradeServiceSkipsEmptyLookupServiceId() throws Exception {
        final PublishedService service1 = createService(new Goid(0,1L), "a1", "Test Service", "", "${apiKeyRecord.service}", "${portal.managed.service.apiId}");
        headers.add(new ServiceHeader(service1));
        when(serviceManager.findAllHeaders()).thenReturn(headers);
        when(serviceManager.findByPrimaryKey(new Goid(0,1L))).thenReturn(service1);

        final List<UpgradedEntity> upgradedEntities = admin.upgradeServicesTo2_1();

        assertTrue(upgradedEntities.isEmpty());
        verify(serviceManager, never()).update(Matchers.<PublishedService>any());
    }

    @Test
    public void upgradeServiceMultipleLookupAssertion() throws Exception {
        // service has multiple comparison assertions
        final PublishedService service1 = createServiceWithMultipleAssertions(new Goid(0,1L), 1, "Test Service 1", 2, null, 1, "${apiKeyRecord.service}", "${service.oid}");
        headers.add(new ServiceHeader(service1));
        when(serviceManager.findAllHeaders()).thenReturn(headers);
        when(serviceManager.findByPrimaryKey(new Goid(0,1L))).thenReturn(service1);
        when(portalManagedServiceManager.fromService(service1)).thenReturn(createPortalManagedService("n1", "1"));

        final List<UpgradedEntity> upgradedEntities = admin.upgradeServicesTo2_1();

        assertEquals(1, upgradedEntities.size());
        assertEquals("API", upgradedEntities.get(0).getType());
        assertEquals("n1", upgradedEntities.get(0).getId());
        assertEquals("Test Service 1", upgradedEntities.get(0).getDescription());

        verify(serviceManager).update(argThat(new ServiceWithApiIdsAndComparisonAssertion(1, 2, "${portal.managed.service.apiId}", 1, "${apiKeyRecord.service}", "${portal.managed.service.apiId}")));
    }

    @Test(expected = UpgradeServiceException.class)
    public void upgradeServicesException() throws Exception {
        when(serviceManager.findAllHeaders()).thenThrow(new FindException("mocking exception"));

        admin.upgradeServicesTo2_1();
    }

    @Test
    public void upgradeKeys() throws Exception {
        portalManagedServices.add(createPortalManagedService("n1", "1"));
        portalManagedServices.add(createPortalManagedService("n2", "2"));
        final ApiKeyData key1 = createKey("k1", Collections.singletonMap("1", "plan1"), "label1", "callback1");
        final ApiKeyData key2 = createKey("k2", Collections.singletonMap("2", "plan2"), "label2", "callback2");
        final Map<String, String> both = new HashMap<String, String>();
        both.put("1", "plan1");
        both.put("2", "plan2");
        // key3 is linked to both services
        final ApiKeyData key3 = createKey("k3", both, "label3", "callback3");
        keys.add(key1);
        keys.add(key2);
        keys.add(key3);
        when(serviceManager.findAllHeaders()).thenReturn(headers);
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);
        when(keyManager.findAll()).thenReturn(keys);

        final List<UpgradedEntity> upgradedEntities = admin.upgradeKeysTo2_1();

        assertEquals(3, upgradedEntities.size());
        assertEquals("API KEY", upgradedEntities.get(0).getType());
        assertEquals("k1", upgradedEntities.get(0).getId());
        assertEquals("label1", upgradedEntities.get(0).getDescription());
        assertEquals("API KEY", upgradedEntities.get(1).getType());
        assertEquals("k2", upgradedEntities.get(1).getId());
        assertEquals("label2", upgradedEntities.get(1).getDescription());
        assertEquals("API KEY", upgradedEntities.get(2).getType());
        assertEquals("k3", upgradedEntities.get(2).getId());
        assertEquals("label3", upgradedEntities.get(2).getDescription());

        assertEquals("label1", key1.getLabel());
        assertEquals("callback1", key1.getOauthCallbackUrl());
        assertEquals(1, key1.getServiceIds().size());
        assertEquals("plan1", key1.getServiceIds().get("n1"));
        assertTrue(key1.getXmlRepresentation().contains("&lt;l7:S id=&quot;n1&quot; plan=&quot;plan1&quot;/&gt;"));

        assertEquals("label2", key2.getLabel());
        assertEquals("callback2", key2.getOauthCallbackUrl());
        assertEquals(1, key2.getServiceIds().size());
        assertEquals("plan2", key2.getServiceIds().get("n2"));
        assertTrue(key2.getXmlRepresentation().contains("&lt;l7:S id=&quot;n2&quot; plan=&quot;plan2&quot;/&gt;"));

        assertEquals("label3", key3.getLabel());
        assertEquals("callback3", key3.getOauthCallbackUrl());
        assertEquals(2, key3.getServiceIds().size());
        assertEquals("plan1", key3.getServiceIds().get("n1"));
        assertEquals("plan2", key3.getServiceIds().get("n2"));
        assertTrue(key3.getXmlRepresentation().contains("&lt;l7:S id=&quot;n1&quot; plan=&quot;plan1&quot;/&gt;"));
        assertTrue(key3.getXmlRepresentation().contains("&lt;l7:S id=&quot;n2&quot; plan=&quot;plan2&quot;/&gt;"));

        verify(keyManager).update(key1);
        verify(keyManager).update(key2);
        verify(keyManager).update(key3);
    }

    @Test
    public void upgradeKeysMultipleOAuthElements() throws Exception {
        portalManagedServices.add(createPortalManagedService("n1", "1"));
        final ApiKeyData key1 = createKeyWithMultipleOAuthElements("k1", Collections.singletonMap("1", "plan1"), "label1", "callback1");
        keys.add(key1);
        when(serviceManager.findAllHeaders()).thenReturn(headers);
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);
        when(keyManager.findAll()).thenReturn(keys);

        final List<UpgradedEntity> upgradedEntities = admin.upgradeKeysTo2_1();

        assertEquals(1, upgradedEntities.size());
        assertEquals("API KEY", upgradedEntities.get(0).getType());
        assertEquals("k1", upgradedEntities.get(0).getId());
        assertEquals("label1A", upgradedEntities.get(0).getDescription());

        assertEquals("label1A", key1.getLabel());
        assertEquals("callback1A", key1.getOauthCallbackUrl());
        assertEquals(1, key1.getServiceIds().size());
        assertEquals("plan1", key1.getServiceIds().get("n1"));
        assertTrue(key1.getXmlRepresentation().contains("&lt;l7:S id=&quot;n1&quot; plan=&quot;plan1&quot;/&gt;"));

        verify(keyManager).update(key1);
    }

    @Test
    public void upgradeKeyFieldsAlreadySet() throws Exception {
        final ApiKeyData key1 = createKey("k1", Collections.singletonMap("1", "plan1"), "label1", "callback1");
        key1.setLabel("label1");
        key1.setOauthCallbackUrl("callback1");
        keys.add(key1);
        when(keyManager.findAll()).thenReturn(keys);

        final List<UpgradedEntity> upgradedEntities = admin.upgradeKeysTo2_1();

        assertTrue(upgradedEntities.isEmpty());
        verify(keyManager, never()).update(Matchers.<ApiKeyData>any());
    }

    /**
     * Values in XML should take priority.
     */
    @Test
    public void upgradeKeyFieldsAlreadySetButDoNotMatch() throws Exception {
        final ApiKeyData key1 = createKey("k1", Collections.singletonMap("1", "plan1"), "label1", "callback1");
        key1.setLabel("labelX");
        key1.setOauthCallbackUrl("callbackX");
        keys.add(key1);
        when(keyManager.findAll()).thenReturn(keys);

        final List<UpgradedEntity> upgradedEntities = admin.upgradeKeysTo2_1();

        assertEquals(1, upgradedEntities.size());
        assertEquals("label1", key1.getLabel());
        assertEquals("callback1", key1.getOauthCallbackUrl());
        verify(keyManager).update(key1);
    }

    @Test
    public void upgradeKeyNoOAuth() throws Exception {
        final ApiKeyData key1 = createKey("k1", Collections.singletonMap("1", "plan1"), null, null);
        keys.add(key1);
        when(keyManager.findAll()).thenReturn(keys);

        final List<UpgradedEntity> upgradedEntities = admin.upgradeKeysTo2_1();

        assertTrue(upgradedEntities.isEmpty());
        verify(keyManager, never()).update(Matchers.<ApiKeyData>any());
    }

    @Test
    public void upgradeKeyNotLinkedToPortalManagedService() throws Exception {
        portalManagedServices.add(createPortalManagedService("n1", "portalmanagedoid"));
        final ApiKeyData key1 = createKey("k1", Collections.singletonMap("notportalmanagedoid", "plan1"), "label1", "callback1");
        key1.setLabel("label1");
        key1.setOauthCallbackUrl("callback1");
        keys.add(key1);
        when(portalManagedServiceManager.findAll()).thenReturn(portalManagedServices);
        when(keyManager.findAll()).thenReturn(keys);

        final List<UpgradedEntity> upgradedEntities = admin.upgradeKeysTo2_1();

        assertTrue(upgradedEntities.isEmpty());
        verify(keyManager, never()).update(Matchers.<ApiKeyData>any());
    }

    @Test(expected = UpgradeKeyException.class)
    public void upgradeKeysException() throws Exception {
        when(portalManagedServiceManager.findAll()).thenThrow(new FindException("mocking exception"));

        admin.upgradeKeysTo2_1();
    }

    @Test
    public void deleteClusterProperties() throws Exception {
        Goid goid1 = new Goid(0,1);
        Goid goid2 = new Goid(0,2);
        final ClusterProperty planProperty = new ClusterProperty();
        planProperty.setGoid(goid1);
        final ClusterProperty pmsProperty = new ClusterProperty();
        pmsProperty.setGoid(goid2);
        when(clusterPropertyManager.findByUniqueName(ModuleConstants.PORTAL_API_PLANS_UI_PROPERTY)).thenReturn(planProperty);
        when(clusterPropertyManager.findByUniqueName(ModuleConstants.PORTAL_MANAGED_SERVICES_UI_PROPERTY)).thenReturn(pmsProperty);

        admin.deleteUnusedClusterProperties();

        verify(clusterPropertyManager).delete(goid1);
        verify(clusterPropertyManager).delete(goid2);
    }

    @Test
    public void deleteClusterPropertiesDoNotExist() throws Exception {
        when(clusterPropertyManager.findByUniqueName(ModuleConstants.PORTAL_API_PLANS_UI_PROPERTY)).thenReturn(null);
        when(clusterPropertyManager.findByUniqueName(ModuleConstants.PORTAL_MANAGED_SERVICES_UI_PROPERTY)).thenReturn(null);

        admin.deleteUnusedClusterProperties();

        verify(clusterPropertyManager, never()).delete(any(Goid.class));
    }

    @Test(expected = UpgradeClusterPropertyException.class)
    public void deleteClusterPropertiesException() throws Exception {
        when(clusterPropertyManager.findByUniqueName(ModuleConstants.PORTAL_API_PLANS_UI_PROPERTY)).thenThrow(new FindException("mocking exception"));

        admin.deleteUnusedClusterProperties();
    }

    private PublishedService createService(final Goid serviceGoid, final String apiId, final String name, final String lookupServiceId, final String comparisonLeft, final String comparisonRight) {
        final StringBuilder stringBuilder = new StringBuilder();
        // ApiPortalIntegrationAssertion
        stringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:ApiPortalIntegration>\n" +
                "            <L7p:ApiGroup stringValue=\"\"/>\n" +
                "            <L7p:PortalManagedApiFlag stringValue=\"L7p:ApiPortalManagedServiceAssertion\"/>");
        if (apiId != null) {
            stringBuilder.append("<L7p:ApiId stringValue=\"" + apiId + "\"/>");
        }
        stringBuilder.append("</L7p:ApiPortalIntegration>\n");

        // Lookup key assertion
        stringBuilder.append("<L7p:LookupApiKey>\n" +
                "<L7p:ApiKey stringValue=\"${lookupApiKey}\"/>\n");
        if (lookupServiceId != null) {
            stringBuilder.append("<L7p:ServiceId stringValue=\"" + lookupServiceId + "\"/>");
        }
        stringBuilder.append("</L7p:LookupApiKey>");

        // comparison assertion
        stringBuilder.append("<L7p:ComparisonAssertion>\n" +
                "            <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
                "            <L7p:Expression1 stringValue=\"" + comparisonLeft + "\"/>\n" +
                "            <L7p:Operator operatorNull=\"null\"/>\n" +
                "            <L7p:Predicates predicates=\"included\">\n" +
                "                <L7p:item dataType=\"included\">\n" +
                "                    <L7p:Type variableDataType=\"string\"/>\n" +
                "                </L7p:item>\n" +
                "                <L7p:item binary=\"included\">\n" +
                "                    <L7p:RightValue stringValue=\"" + comparisonRight + "\"/>\n" +
                "                </L7p:item>\n" +
                "            </L7p:Predicates>\n" +
                "        </L7p:ComparisonAssertion>");
        stringBuilder.append("</wsp:All>\n" +
                "</wsp:Policy>");
        final PublishedService service = new PublishedService();
        service.setGoid(serviceGoid);
        service.setPolicy(new Policy(PolicyType.PRIVATE_SERVICE, "service policy", stringBuilder.toString(), false));
        service.setName(name);
        return service;
    }

    private PublishedService createServiceWithMultipleAssertions(final Goid serviceGoid, final int numAssertions, final String name, final int numLookupAssertions, final String lookupServiceId, final int numComparisonAssertions, final String comparisonLeft, final String comparisonRight) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "<wsp:All wsp:Usage=\"Required\">\n");
        for (int i = 0; i < numAssertions; i++) {
            stringBuilder.append("<L7p:ApiPortalIntegration>\n" +
                    "<L7p:ApiGroup stringValue=\"\"/>\n" +
                    "<L7p:PortalManagedApiFlag stringValue=\"L7p:ApiPortalManagedServiceAssertion\"/>" +
                    "</L7p:ApiPortalIntegration>\n");
        }
        for (int i = 0; i < numLookupAssertions; i++) {
            stringBuilder.append("<L7p:LookupApiKey>\n" +
                    "<L7p:ApiKey stringValue=\"${lookupApiKey}\"/>\n");
            if (lookupServiceId != null) {
                stringBuilder.append("<L7p:ServiceId stringValue=\"" + lookupServiceId + "\"/>");
            }
            stringBuilder.append("</L7p:LookupApiKey>");
        }
        for (int i = 0; i < numComparisonAssertions; i++) {
            stringBuilder.append("<L7p:ComparisonAssertion>\n" +
                    "            <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
                    "            <L7p:Expression1 stringValue=\"" + comparisonLeft + "\"/>\n" +
                    "            <L7p:Operator operatorNull=\"null\"/>\n" +
                    "            <L7p:Predicates predicates=\"included\">\n" +
                    "                <L7p:item dataType=\"included\">\n" +
                    "                    <L7p:Type variableDataType=\"string\"/>\n" +
                    "                </L7p:item>\n" +
                    "                <L7p:item binary=\"included\">\n" +
                    "                    <L7p:RightValue stringValue=\"" + comparisonRight + "\"/>\n" +
                    "                </L7p:item>\n" +
                    "            </L7p:Predicates>\n" +
                    "        </L7p:ComparisonAssertion>");
        }
        stringBuilder.append("</wsp:All>\n" +
                "</wsp:Policy>");
        final PublishedService service = new PublishedService();
        service.setGoid(serviceGoid);
        service.setPolicy(new Policy(PolicyType.PRIVATE_SERVICE, "service policy", stringBuilder.toString(), false));
        service.setName(name);
        return service;
    }

    private PublishedService createNonPortalManagedService(final Goid serviceGoid) {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        final PublishedService service = new PublishedService();
        service.setGoid(serviceGoid);
        service.setPolicy(new Policy(PolicyType.PRIVATE_SERVICE, "service policy", xml, false));
        return service;
    }

    private PublishedService createNonPortalManagedServiceWithComment(final Goid serviceGoid, final String comment) {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "    <L7p:CommentAssertion>\n" +
                "        <L7p:Comment stringValue=\"" + comment + "\"/>\n" +
                "    </L7p:CommentAssertion>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        final PublishedService service = new PublishedService();
        service.setGoid(serviceGoid);
        service.setPolicy(new Policy(PolicyType.PRIVATE_SERVICE, "service policy", xml, false));
        return service;
    }

    private PortalManagedService createPortalManagedService(final String apiId, final String serviceOid) {
        final PortalManagedService service = new PortalManagedService();
        service.setName(apiId);
        service.setDescription(serviceOid);
        return service;
    }

    private ApiKeyData createKey(final String name, final Map<String, String> serviceIds, final String label, final String callback) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
                "&lt;l7:ApiKey status=&quot;inactive&quot; xmlns:l7=&quot;http://ns.l7tech.com/2011/08/portal-api-keys&quot;&gt;\n" +
                "    &lt;l7:Value&gt;" + name + "&lt;/l7:Value&gt;\n" +
                "    &lt;l7:Secret&gt;shhhh&lt;/l7:Secret&gt;\n" +
                "    &lt;l7:Services&gt;");
        for (final Map.Entry<String, String> entry : serviceIds.entrySet()) {
            stringBuilder.append("&lt;l7:S id=&quot;" + entry.getKey() + "&quot; plan=&quot;" + entry.getValue() + "&quot;/&gt;");
        }
        stringBuilder.append("&lt;/l7:Services&gt;\n");
        if (label != null && callback != null) {
            stringBuilder.append("&lt;l7:OAuth callbackUrl=&quot;" + callback + "&quot; label=&quot;" + label + "&quot;/&gt;\n");
        }
        stringBuilder.append("&lt;/l7:ApiKey&gt;");
        final ApiKeyData key = new ApiKeyData();
        key.setName(name);
        key.setXmlRepresentation(stringBuilder.toString());
        key.setServiceIds(new HashMap<String, String>(serviceIds));
        return key;
    }

    private ApiKeyData createKeyWithMultipleOAuthElements(final String name, final Map<String, String> serviceIds, final String label, final String callback) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
                "&lt;l7:ApiKey status=&quot;inactive&quot; xmlns:l7=&quot;http://ns.l7tech.com/2011/08/portal-api-keys&quot;&gt;\n" +
                "    &lt;l7:Value&gt;" + name + "&lt;/l7:Value&gt;\n" +
                "    &lt;l7:Secret&gt;shhhh&lt;/l7:Secret&gt;\n" +
                "    &lt;l7:Services&gt;");
        for (final Map.Entry<String, String> entry : serviceIds.entrySet()) {
            stringBuilder.append("&lt;l7:S id=&quot;" + entry.getKey() + "&quot; plan=&quot;" + entry.getValue() + "&quot;/&gt;");
        }
        stringBuilder.append("&lt;/l7:Services&gt;\n");
        if (label != null && callback != null) {
            stringBuilder.append("&lt;l7:OAuth callbackUrl=&quot;" + (callback + "A") + "&quot; label=&quot;" + (label + "A") + "&quot;/&gt;\n");
            stringBuilder.append("&lt;l7:OAuth callbackUrl=&quot;" + (callback + "B") + "&quot; label=&quot;" + (label + "B") + "&quot;/&gt;\n");
        }
        stringBuilder.append("&lt;/l7:ApiKey&gt;");
        final ApiKeyData key = new ApiKeyData();
        key.setName(name);
        key.setXmlRepresentation(stringBuilder.toString());
        key.setServiceIds(new HashMap<String, String>(serviceIds));
        return key;
    }

    private class ServiceWithApiIdsAndComparisonAssertion extends ArgumentMatcher<PublishedService> {
        final int numExpectedApiIds;
        final int numExpectedComparisonAssertions;
        final int numExpectedLookupAssertions;
        final String comparisonLeft;
        final String comparisonRight;
        final String lookupServiceId;

        ServiceWithApiIdsAndComparisonAssertion() {
            this.numExpectedApiIds = 1;
            this.numExpectedComparisonAssertions = 1;
            this.numExpectedLookupAssertions = 1;
            this.comparisonLeft = "${apiKeyRecord.service}";
            this.comparisonRight = "${portal.managed.service.apiId}";
            this.lookupServiceId = "${portal.managed.service.apiId}";
        }

        ServiceWithApiIdsAndComparisonAssertion(final int numExpectedApiIds, final int numExpectedLookupAssertions, final String lookupServiceId, final int numExpectedComparisonAssertions, final String comparisonLeft, final String comparisonRight) {
            this.numExpectedApiIds = numExpectedApiIds;
            this.numExpectedComparisonAssertions = numExpectedComparisonAssertions;
            this.numExpectedLookupAssertions = numExpectedLookupAssertions;
            this.comparisonLeft = comparisonLeft;
            this.comparisonRight = comparisonRight;
            this.lookupServiceId = lookupServiceId;
        }

        @Override
        public boolean matches(final Object o) {
            final PublishedService service = (PublishedService) o;
            try {
                final Document document = XmlUtil.parse(service.getPolicy().getXml());

                final NodeList apiIdNodes = document.getElementsByTagName("L7p:ApiId");
                assertEquals(numExpectedApiIds, apiIdNodes.getLength());
                for (int i = 0; i < apiIdNodes.getLength(); i++) {
                    final Node apiIdNode = apiIdNodes.item(i);
                    assertFalse(apiIdNode.getAttributes().getNamedItem("stringValue").getTextContent().isEmpty());
                }

                final NodeList lookupNodes = document.getElementsByTagName("L7p:ServiceId");
                assertEquals(numExpectedLookupAssertions, lookupNodes.getLength());
                for (int i = 0; i < lookupNodes.getLength(); i++) {
                    final Node lookupNode = lookupNodes.item(i);
                    assertEquals(lookupServiceId, lookupNode.getAttributes().getNamedItem("stringValue").getTextContent());
                }

                final NodeList expression1Nodes = document.getElementsByTagName("L7p:Expression1");
                assertEquals(numExpectedComparisonAssertions, expression1Nodes.getLength());
                for (int i = 0; i < expression1Nodes.getLength(); i++) {
                    final Node expression1Node = expression1Nodes.item(i);
                    assertEquals(comparisonLeft, expression1Node.getAttributes().getNamedItem("stringValue").getTextContent());
                }
                final NodeList rightValueNodes = document.getElementsByTagName("L7p:RightValue");
                assertEquals(numExpectedComparisonAssertions, rightValueNodes.getLength());
                for (int i = 0; i < rightValueNodes.getLength(); i++) {
                    final Node rightValueNode = rightValueNodes.item(i);
                    assertEquals(comparisonRight, rightValueNode.getAttributes().getNamedItem("stringValue").getTextContent());
                }

                return true;
            } catch (final SAXException e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}
