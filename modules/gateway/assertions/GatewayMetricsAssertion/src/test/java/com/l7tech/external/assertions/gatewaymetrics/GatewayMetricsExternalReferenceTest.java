package com.l7tech.external.assertions.gatewaymetrics;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.InvalidDocumentFormatException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Registry.class})
public class GatewayMetricsExternalReferenceTest {

    private GatewayMetricsAssertion mockAssertion;
    private ExternalReferenceFinder mockFinder;
    private ServiceAdmin mockServiceAdmin;
    private PublishedService mockPublishedService;
    private ClusterStatusAdmin mockClusterStatusAdmin;


    @Before
    public void setUp() throws FindException {
        mockAssertion = mock(GatewayMetricsAssertion.class);
        mockFinder = mock(ExternalReferenceFinder.class);
        mockServiceAdmin = mock(ServiceAdmin.class);
        mockPublishedService = mock(PublishedService.class);
        mockClusterStatusAdmin = mock(ClusterStatusAdmin.class);
        when(mockAssertion.getPublishedServiceGoid()).thenReturn(Goid.DEFAULT_GOID);
    }


    @Test
    public void testSerializationWithAllClusterNodesAndAllPublishedServices() throws IOException, FindException {
        String EXPECTED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:References xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\"/>";

        Registry r = PowerMockito.mock(Registry.class);
        PowerMockito.mockStatic(Registry.class);
        when(Registry.getDefault()).thenReturn(r);
        when(r.getServiceManager()).thenReturn(mockServiceAdmin);
        when(mockServiceAdmin.findServiceByID(String.valueOf(mockAssertion.getPublishedServiceGoid()))).thenReturn(mockPublishedService);

        GatewayMetricsExternalReference reference = new GatewayMetricsExternalReference(mockFinder, mockAssertion);
        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        reference.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);

        assertEquals(EXPECTED_XML, asXml.trim());
    }

    @Test
    public void testSerializationWithAllClusterNodesAndASpecificPublishedService() throws IOException, FindException {
        String serviceName = "Specific Service";
        Goid specificServiceGoid = new Goid(1, 0);
        when(mockAssertion.getPublishedServiceGoid()).thenReturn(specificServiceGoid);
        when(mockPublishedService.getName()).thenReturn(serviceName);

        String EXPECTED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:References xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\">\n" +
                "    <GatewayMetricsReference RefType=\"com.l7tech.external.assertions.gatewaymetrics.GatewayMetricsExternalReference\">\n" +
                "        <GOID>" + specificServiceGoid.toString() + "</GOID>\n" +
                "        <Name>" + serviceName + "</Name>\n" +
                "        <Type>PublishedService</Type>\n" +
                "    </GatewayMetricsReference>\n" +
                "</exp:References>";

        Registry r = PowerMockito.mock(Registry.class);
        PowerMockito.mockStatic(Registry.class);
        when(Registry.getDefault()).thenReturn(r);
        when(r.getServiceManager()).thenReturn(mockServiceAdmin);
        when(mockServiceAdmin.findServiceByID(String.valueOf(mockAssertion.getPublishedServiceGoid()))).thenReturn(mockPublishedService);

        GatewayMetricsExternalReference reference = new GatewayMetricsExternalReference(mockFinder, mockAssertion);
        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        reference.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);

        assertEquals(EXPECTED_XML, asXml.trim());
    }

    @Test
    public void testSerializationWithASpecificClusterNodeAndAllPublishedServices() throws IOException, FindException {
        String node0Id = "Node 0";
        String node1Id = "Node 1";
        when(mockAssertion.getClusterNodeId()).thenReturn(node1Id);

        String EXPECTED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:References xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\">\n" +
                "    <GatewayMetricsReference RefType=\"com.l7tech.external.assertions.gatewaymetrics.GatewayMetricsExternalReference\">\n" +
                "        <ID>" + node1Id + "</ID>\n" +
                "        <Type>ClusterNode</Type>\n" +
                "    </GatewayMetricsReference>\n" +
                "</exp:References>";


        Registry r = PowerMockito.mock(Registry.class);
        PowerMockito.mockStatic(Registry.class);
        when(Registry.getDefault()).thenReturn(r);
        when(r.getClusterStatusAdmin()).thenReturn(mockClusterStatusAdmin);
        ClusterNodeInfo[] clusterNodeInfo = new ClusterNodeInfo[2];
        ClusterNodeInfo mockNode0 = mock(ClusterNodeInfo.class);
        ClusterNodeInfo mockNode1 = mock(ClusterNodeInfo.class);
        when(mockNode0.getId()).thenReturn(node0Id);
        when(mockNode1.getId()).thenReturn(node1Id);
        clusterNodeInfo[0] = mockNode0;
        clusterNodeInfo[1] = mockNode1;
        when(mockClusterStatusAdmin.getClusterStatus()).thenReturn(clusterNodeInfo);


        GatewayMetricsExternalReference reference = new GatewayMetricsExternalReference(mockFinder, mockAssertion);
        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        reference.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);

        assertEquals(EXPECTED_XML, asXml.trim());
    }

    @Test
    public void testSerializationWithASpecificClusterNodeAndASpecificPublishedService() throws IOException, FindException {
        String node0Id = "Node 0";
        String node1Id = "Node 1";
        when(mockAssertion.getClusterNodeId()).thenReturn(node1Id);
        String serviceName = "Specific Service";
        Goid specificServiceGoid = new Goid(1, 0);
        when(mockAssertion.getPublishedServiceGoid()).thenReturn(specificServiceGoid);
        when(mockPublishedService.getName()).thenReturn(serviceName);

        String EXPECTED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:References xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\">\n" +
                "    <GatewayMetricsReference RefType=\"com.l7tech.external.assertions.gatewaymetrics.GatewayMetricsExternalReference\">\n" +
                "        <ID>" + node1Id + "</ID>\n" +
                "        <Type>ClusterNode</Type>\n" +
                "    </GatewayMetricsReference>\n" +
                "    <GatewayMetricsReference RefType=\"com.l7tech.external.assertions.gatewaymetrics.GatewayMetricsExternalReference\">\n" +
                "        <GOID>" + specificServiceGoid.toString() + "</GOID>\n" +
                "        <Name>" + serviceName + "</Name>\n" +
                "        <Type>PublishedService</Type>\n" +
                "    </GatewayMetricsReference>\n" +
                "</exp:References>";


        Registry r = PowerMockito.mock(Registry.class);
        PowerMockito.mockStatic(Registry.class);
        when(Registry.getDefault()).thenReturn(r);

        when(r.getClusterStatusAdmin()).thenReturn(mockClusterStatusAdmin);
        ClusterNodeInfo[] clusterNodeInfo = new ClusterNodeInfo[2];
        ClusterNodeInfo mockNode0 = mock(ClusterNodeInfo.class);
        ClusterNodeInfo mockNode1 = mock(ClusterNodeInfo.class);
        when(mockNode0.getId()).thenReturn(node0Id);
        when(mockNode1.getId()).thenReturn(node1Id);
        clusterNodeInfo[0] = mockNode0;
        clusterNodeInfo[1] = mockNode1;
        when(mockClusterStatusAdmin.getClusterStatus()).thenReturn(clusterNodeInfo);

        when(r.getServiceManager()).thenReturn(mockServiceAdmin);
        when(mockServiceAdmin.findServiceByID(String.valueOf(mockAssertion.getPublishedServiceGoid()))).thenReturn(mockPublishedService);


        GatewayMetricsExternalReference reference = new GatewayMetricsExternalReference(mockFinder, mockAssertion);
        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        reference.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);

        assertEquals(EXPECTED_XML, asXml.trim());
    }

    @Test
    public void shouldVerifyWhenNoReferenceFound() throws InvalidPolicyStreamException {
        GatewayMetricsExternalReference reference = new GatewayMetricsExternalReference(mockFinder, mockAssertion);
        reference.setType(GatewayMetricsExternalReference.ExternalReferenceType.CLUSTER_NODE);
        assertTrue(reference.verifyReference());
    }

    @Test
    public void shouldVerifyWhenClusterIdFound() throws InvalidPolicyStreamException, FindException {
        String node0Id = "Node 0";
        String node1Id = "Node 1";
        String node1Name = "Cluster Node 1";
        when(mockAssertion.getClusterNodeId()).thenReturn(node1Id);
        Registry r = PowerMockito.mock(Registry.class);
        PowerMockito.mockStatic(Registry.class);
        when(Registry.getDefault()).thenReturn(r);
        when(r.getClusterStatusAdmin()).thenReturn(mockClusterStatusAdmin);
        ClusterNodeInfo[] clusterNodeInfo = new ClusterNodeInfo[2];
        ClusterNodeInfo mockNode0 = mock(ClusterNodeInfo.class);
        ClusterNodeInfo mockNode1 = mock(ClusterNodeInfo.class);
        when(mockNode0.getId()).thenReturn(node0Id);
        when(mockNode1.getId()).thenReturn(node1Id);
        when(mockNode1.getName()).thenReturn(node1Name);
        clusterNodeInfo[0] = mockNode0;
        clusterNodeInfo[1] = mockNode1;
        when(mockClusterStatusAdmin.getClusterStatus()).thenReturn(clusterNodeInfo);

        GatewayMetricsExternalReference reference = new GatewayMetricsExternalReference(mockFinder, mockAssertion);
        reference.setType(GatewayMetricsExternalReference.ExternalReferenceType.CLUSTER_NODE);

        assertTrue(reference.verifyReference());
    }

    @Test
    public void shouldVerifyWhenDefaultGoidFound() throws InvalidPolicyStreamException, FindException {
        GatewayMetricsExternalReference reference = new GatewayMetricsExternalReference(mockFinder, mockAssertion);
        reference.setType(GatewayMetricsExternalReference.ExternalReferenceType.PUBLISHED_SERVICE);
        assertTrue(reference.verifyReference());
    }

    @Test
    public void shouldVerifyWhenServiceIdFound() throws InvalidPolicyStreamException, FindException {
        String serviceName = "Specific Service";
        Goid specificServiceGoid = new Goid(1, 0);
        when(mockAssertion.getPublishedServiceGoid()).thenReturn(specificServiceGoid);
        when(mockPublishedService.getName()).thenReturn(serviceName);

        Registry r = PowerMockito.mock(Registry.class);
        PowerMockito.mockStatic(Registry.class);
        when(Registry.getDefault()).thenReturn(r);
        when(r.getServiceManager()).thenReturn(mockServiceAdmin);
        when(mockServiceAdmin.findServiceByID(String.valueOf(mockAssertion.getPublishedServiceGoid()))).thenReturn(mockPublishedService);

        GatewayMetricsExternalReference reference = new GatewayMetricsExternalReference(mockFinder, mockAssertion);
        reference.setType(GatewayMetricsExternalReference.ExternalReferenceType.PUBLISHED_SERVICE);
        assertTrue(reference.verifyReference());
    }

    @Test
    public void parseReferenceFromElementOfClusterNodeType() throws SAXException, InvalidDocumentFormatException {
        String node1Id = "Node 1";
        GatewayMetricsExternalReference reference = new GatewayMetricsExternalReference(mockFinder, mockAssertion);

        String GM_REF = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<GatewayMetricsReference RefType=\"com.l7tech.external.assertions.gatewaymetrics.GatewayMetricsExternalReference\">\n" +
                "    <ID>" + node1Id + "</ID>\n" +
                "    <Type>ClusterNode</Type>\n" +
                "</GatewayMetricsReference>";

        Element element = XmlUtil.parse(GM_REF).getDocumentElement();
        GatewayMetricsExternalReference newReference = (GatewayMetricsExternalReference) reference.parseFromElement(mockFinder, element);

        assertEquals(GatewayMetricsExternalReference.ExternalReferenceType.CLUSTER_NODE, newReference.getType());
        assertEquals(node1Id, newReference.getClusterNodeId());
        // The published service and service names should be null because we are getting all services in a specific
        // Cluster Node
        assertEquals(null, newReference.getPublishedServiceGoid());
        assertEquals(null, newReference.getPublishedServiceName());
    }

    @Test
    public void parseReferenceFromElementOfPublishedServiceType() throws SAXException, InvalidDocumentFormatException {

        GatewayMetricsExternalReference reference = new GatewayMetricsExternalReference(mockFinder, mockAssertion);

        String serviceName = "Specific Service";
        Goid specificServiceGoid = new Goid(1, 0);

        String GM_REF = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<GatewayMetricsReference RefType=\"com.l7tech.external.assertions.gatewaymetrics.GatewayMetricsExternalReference\">\n" +
                "    <GOID>" + specificServiceGoid.toString() + "</GOID>\n" +
                "    <Name>" + serviceName + "</Name>\n" +
                "    <Type>PublishedService</Type>\n" +
                "</GatewayMetricsReference>";

        Element element = XmlUtil.parse(GM_REF).getDocumentElement();
        GatewayMetricsExternalReference newReference = (GatewayMetricsExternalReference) reference.parseFromElement(mockFinder, element);
        assertEquals(GatewayMetricsExternalReference.ExternalReferenceType.PUBLISHED_SERVICE, newReference.getType());
        assertEquals(specificServiceGoid, newReference.getPublishedServiceGoid());
        assertEquals(serviceName, newReference.getPublishedServiceName());
    }

    @Test
    public void shouldLocalizeReplaceAssertionForClusterNode() throws Exception {
        String replacedNodeId = "Node 0";
        String oldNodeId = "Node 1";
        String node1Name = "Cluster Node 1";
        GatewayMetricsAssertion assertion = new GatewayMetricsAssertion();
        when(mockAssertion.getClusterNodeId()).thenReturn(oldNodeId);
        Registry r = PowerMockito.mock(Registry.class);
        PowerMockito.mockStatic(Registry.class);
        when(Registry.getDefault()).thenReturn(r);
        when(r.getClusterStatusAdmin()).thenReturn(mockClusterStatusAdmin);
        ClusterNodeInfo[] clusterNodeInfo = new ClusterNodeInfo[2];
        ClusterNodeInfo mockNode0 = mock(ClusterNodeInfo.class);
        ClusterNodeInfo mockNode1 = mock(ClusterNodeInfo.class);
        when(mockNode0.getId()).thenReturn(replacedNodeId);
        when(mockNode1.getId()).thenReturn(oldNodeId);
        when(mockNode1.getName()).thenReturn(node1Name);
        clusterNodeInfo[0] = mockNode0;
        clusterNodeInfo[1] = mockNode1;
        when(mockClusterStatusAdmin.getClusterStatus()).thenReturn(clusterNodeInfo);

        GatewayMetricsExternalReference reference = new GatewayMetricsExternalReference(mockFinder, mockAssertion);
        reference.setType(GatewayMetricsExternalReference.ExternalReferenceType.CLUSTER_NODE);
        reference.setLocalizeReplace(replacedNodeId);

        assertion.setPublishedServiceGoid(new Goid(2, 1));
        assertion.setClusterNodeId(oldNodeId);

        Boolean result = reference.localizeAssertion(assertion);

        assertEquals(replacedNodeId, assertion.getClusterNodeId());
        assertTrue(result);
    }

    @Test
    public void shouldLocalizeReplaceAssertionForPublishedService() throws Exception {
        Goid newGoid = new Goid(3, 1);
        Goid goid = new Goid(2, 1);
        String oldServiceName = "Old Service Name";
        GatewayMetricsAssertion assertion = new GatewayMetricsAssertion();
        assertion.setPublishedServiceGoid(goid);

        Registry r = PowerMockito.mock(Registry.class);
        PowerMockito.mockStatic(Registry.class);
        when(Registry.getDefault()).thenReturn(r);
        when(r.getServiceManager()).thenReturn(mockServiceAdmin);
        when(mockAssertion.getPublishedServiceGoid()).thenReturn(goid);
        when(mockServiceAdmin.findServiceByID(String.valueOf(mockAssertion.getPublishedServiceGoid()))).thenReturn(mockPublishedService);
        when(mockPublishedService.getName()).thenReturn(oldServiceName);

        GatewayMetricsExternalReference reference = new GatewayMetricsExternalReference(mockFinder, mockAssertion);
        reference.setType(GatewayMetricsExternalReference.ExternalReferenceType.PUBLISHED_SERVICE);
        reference.setLocalizeReplace(newGoid);

        Boolean result = reference.localizeAssertion(assertion);

        assertEquals(newGoid, assertion.getPublishedServiceGoid());
        assertTrue(result);
    }

    @Test
    public void shouldLocalizeIgnoreAssertion() throws Exception {
        GatewayMetricsAssertion assertion = new GatewayMetricsAssertion();

        GatewayMetricsExternalReference reference = new GatewayMetricsExternalReference(mockFinder, mockAssertion);
        reference.setType(GatewayMetricsExternalReference.ExternalReferenceType.CLUSTER_NODE);

        assertion.setPublishedServiceGoid(new Goid(2, 1));

        reference.setLocalizeIgnore();

        Boolean result = reference.localizeAssertion(assertion);
        assertTrue(result);
    }

    @Test
    public void shouldLocalizeDeleteAssertion() throws Exception {
        String replacedNodeId = "Node 0";
        String oldNodeId = "Node 1";
        String node1Name = "Cluster Node 1";
        GatewayMetricsAssertion assertion = new GatewayMetricsAssertion();
        when(mockAssertion.getClusterNodeId()).thenReturn(oldNodeId);
        Registry r = PowerMockito.mock(Registry.class);
        PowerMockito.mockStatic(Registry.class);
        when(Registry.getDefault()).thenReturn(r);
        when(r.getClusterStatusAdmin()).thenReturn(mockClusterStatusAdmin);
        ClusterNodeInfo[] clusterNodeInfo = new ClusterNodeInfo[2];
        ClusterNodeInfo mockNode0 = mock(ClusterNodeInfo.class);
        ClusterNodeInfo mockNode1 = mock(ClusterNodeInfo.class);
        when(mockNode0.getId()).thenReturn(replacedNodeId);
        when(mockNode1.getId()).thenReturn(oldNodeId);
        when(mockNode1.getName()).thenReturn(node1Name);
        clusterNodeInfo[0] = mockNode0;
        clusterNodeInfo[1] = mockNode1;
        when(mockClusterStatusAdmin.getClusterStatus()).thenReturn(clusterNodeInfo);

        GatewayMetricsExternalReference reference = new GatewayMetricsExternalReference(mockFinder, mockAssertion);
        reference.setType(GatewayMetricsExternalReference.ExternalReferenceType.CLUSTER_NODE);

        assertion.setPublishedServiceGoid(new Goid(2, 1));
        assertion.setClusterNodeId(oldNodeId);

        reference.setLocalizeDelete();

        Boolean result = reference.localizeAssertion(assertion);
        // on successful Delete it will return false
        assertFalse(result);
    }
}
