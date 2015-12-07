package com.l7tech.external.assertions.extensiblesocketconnectorassertion;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.HL7CodecConfiguration;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.MLLPCodecConfiguration;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.util.InvalidDocumentFormatException;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: ashah
 * Date: 04/05/12
 * Time: 10:12 AM
 * To change this template use File | Settings | File Templates.
 */

public class ServerExtensibleSocketExternalReferenceTest {
    private static final Logger log = Logger.getLogger(ServerExtensibleSocketExternalReferenceTest.class.getName());

    private ExternalReferenceFinder mockFinder;
    private ExtensibleSocketConnectorAssertion mockAssertion;
    private ExtensibleSocketConnectorEntity mockEntity;
    private ExtensibleSocketConnectorEntityAdmin mockAdmin;

    private EntityManager<ExtensibleSocketConnectorEntity, GenericEntityHeader> mockGenericEntity;
    private ExtensibleSocketConnectorReference reference;

    private static final String version = System.getProperty("java.version");

    @Before
    public void setUp() throws FindException {

        mockFinder = mock(ExternalReferenceFinder.class);
        mockAssertion = mock(ExtensibleSocketConnectorAssertion.class);
        mockEntity = mock(ExtensibleSocketConnectorEntity.class);
        mockAdmin = mock(ExtensibleSocketConnectorEntityAdmin.class);
        mockGenericEntity = mock(EntityManager.class);
        when(mockAssertion.getSocketConnectorGoid()).thenReturn(new Goid(1, 0));
        when(mockFinder.getGenericEntityManager(ExtensibleSocketConnectorEntity.class)).thenReturn(mockGenericEntity);
        when(mockAdmin.find(mockAssertion.getSocketConnectorGoid())).thenReturn(mockEntity);
        when(mockGenericEntity.findByPrimaryKey(mockAssertion.getSocketConnectorGoid())).thenReturn(mockEntity);

        when(mockEntity.getGoid()).thenReturn(new Goid(2, 1));
        when(mockEntity.getName()).thenReturn("name");
        when(mockEntity.isIn()).thenReturn(false);
        when(mockEntity.getHostname()).thenReturn("hostname");
        when(mockEntity.getPort()).thenReturn(2);
        when(mockEntity.isUseSsl()).thenReturn(false);
        when(mockEntity.getSslKeyId()).thenReturn("sslId");
        when(mockEntity.getClientAuthEnum()).thenReturn(SSLClientAuthEnum.DISABLED);
        when(mockEntity.getThreadPoolMin()).thenReturn(1);
        when(mockEntity.getThreadPoolMax()).thenReturn(2);
        when(mockEntity.getBindAddress()).thenReturn("address");
        when(mockEntity.getServiceGoid()).thenReturn(new Goid(3, 1));
        when(mockEntity.getContentType()).thenReturn("content");
        when(mockEntity.getMaxMessageSize()).thenReturn(100);
        when(mockEntity.isEnabled()).thenReturn(true);
        when(mockEntity.getCodecConfiguration()).thenReturn(new MLLPCodecConfiguration());
        when(mockEntity.getExchangePattern()).thenReturn(ExchangePatternEnum.OutIn);

        reference = new ExtensibleSocketConnectorReference(mockFinder, mockAssertion);
    }

    @Test
    public void testValidSerializeToRefElementTestOutBoundWithSSLDisabled() throws Exception {

        String EXPECTED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:References xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\">\n" +
                "    <ExtensibleSocketConnectorReference RefType=\"com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorReference\">\n" +
                "        <GOID>00000000000000020000000000000001</GOID>\n" +
                "        <Name>name</Name>\n" +
                "        <In>false</In>\n" +
                "        <Hostname>hostname</Hostname>\n" +
                "        <Port>2</Port>\n" +
                "        <UseSSL>false</UseSSL>\n" +
                "        <ContentType>content</ContentType>\n" +
                "        <ExchangePattern>OutIn</ExchangePattern>\n" +
                "        <KeepAlive>false</KeepAlive>\n" +
                "        <ListenTimeOut>1</ListenTimeOut>\n" +
                "        <useDnsLookup>false</useDnsLookup>\n" +
                "        <CodecConfiguration>&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                "&lt;java version=\"" + version + "\" class=\"java.beans.XMLDecoder\"&gt;\n" +
                " &lt;object class=\"com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.MLLPCodecConfiguration\"/&gt;\n" +
                "&lt;/java&gt;\n" +
                "</CodecConfiguration>\n" +
                "    </ExtensibleSocketConnectorReference>\n" +
                "</exp:References>";

        when(mockEntity.getGoid()).thenReturn(new Goid(2, 1));
        when(mockEntity.getName()).thenReturn("name");
        when(mockEntity.isIn()).thenReturn(false);
        when(mockEntity.getHostname()).thenReturn("hostname");
        when(mockEntity.getPort()).thenReturn(2);
        when(mockEntity.isUseSsl()).thenReturn(false);
        when(mockEntity.getSslKeyId()).thenReturn("sslId");
        when(mockEntity.getClientAuthEnum()).thenReturn(SSLClientAuthEnum.DISABLED);
        when(mockEntity.getThreadPoolMin()).thenReturn(1);
        when(mockEntity.getThreadPoolMax()).thenReturn(2);
        when(mockEntity.getBindAddress()).thenReturn("address");
        when(mockEntity.getServiceGoid()).thenReturn(new Goid(3, 1));
        when(mockEntity.getContentType()).thenReturn("content");
        when(mockEntity.getMaxMessageSize()).thenReturn(100);
        when(mockEntity.isEnabled()).thenReturn(true);
        when(mockEntity.getCodecConfiguration()).thenReturn(new MLLPCodecConfiguration());
        when(mockEntity.getExchangePattern()).thenReturn(ExchangePatternEnum.OutIn);
        when(mockEntity.getListenTimeout()).thenReturn(1L);
        when(mockEntity.isKeepAlive()).thenReturn(false);
        when(mockEntity.isUseDnsLookup()).thenReturn(false);

        ExtensibleSocketConnectorReference reference = new ExtensibleSocketConnectorReference(mockFinder, mockAssertion);

        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        reference.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);

        assertEquals(EXPECTED_XML, asXml.trim());
    }

    @Test
    public void testValidSerializeToRefElementTestOutBoundWithSSLEnabled() throws Exception {

        String EXPECTED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:References xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\">\n" +
                "    <ExtensibleSocketConnectorReference RefType=\"com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorReference\">\n" +
                "        <GOID>00000000000000020000000000000001</GOID>\n" +
                "        <Name>name</Name>\n" +
                "        <In>false</In>\n" +
                "        <Hostname>hostname</Hostname>\n" +
                "        <Port>2</Port>\n" +
                "        <UseSSL>true</UseSSL>\n" +
                "        <SslKeyID>sslId</SslKeyID>\n" +
                "        <ContentType>content</ContentType>\n" +
                "        <ExchangePattern>OutIn</ExchangePattern>\n" +
                "        <KeepAlive>true</KeepAlive>\n" +
                "        <ListenTimeOut>2</ListenTimeOut>\n" +
                "        <useDnsLookup>true</useDnsLookup>\n" +
                "        <dnsService>serviceName</dnsService>\n" +
                "        <dnsDomainName>domainName</dnsDomainName>\n" +
                "        <CodecConfiguration>&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                "&lt;java version=\"" + version + "\" class=\"java.beans.XMLDecoder\"&gt;\n" +
                " &lt;object class=\"com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.MLLPCodecConfiguration\"/&gt;\n" +
                "&lt;/java&gt;\n" +
                "</CodecConfiguration>\n" +
                "    </ExtensibleSocketConnectorReference>\n" +
                "</exp:References>";

        when(mockEntity.getGoid()).thenReturn(new Goid(2, 1));
        when(mockEntity.getName()).thenReturn("name");
        when(mockEntity.isIn()).thenReturn(false);
        when(mockEntity.getHostname()).thenReturn("hostname");
        when(mockEntity.getPort()).thenReturn(2);
        when(mockEntity.isUseSsl()).thenReturn(true);
        when(mockEntity.getSslKeyId()).thenReturn("sslId");
        when(mockEntity.getClientAuthEnum()).thenReturn(SSLClientAuthEnum.DISABLED);
        when(mockEntity.getThreadPoolMin()).thenReturn(1);
        when(mockEntity.getThreadPoolMax()).thenReturn(2);
        when(mockEntity.getBindAddress()).thenReturn("address");
        when(mockEntity.getServiceGoid()).thenReturn(new Goid(3, 1));
        when(mockEntity.getContentType()).thenReturn("content");
        when(mockEntity.getMaxMessageSize()).thenReturn(100);
        when(mockEntity.isEnabled()).thenReturn(true);
        when(mockEntity.getCodecConfiguration()).thenReturn(new MLLPCodecConfiguration());
        when(mockEntity.getExchangePattern()).thenReturn(ExchangePatternEnum.OutIn);
        when(mockEntity.getListenTimeout()).thenReturn(2L);
        when(mockEntity.isKeepAlive()).thenReturn(true);
        when(mockEntity.getDnsDomainName()).thenReturn("domainName");
        when(mockEntity.getDnsService()).thenReturn("serviceName");
        when(mockEntity.isUseDnsLookup()).thenReturn(true);

        ExtensibleSocketConnectorReference reference = new ExtensibleSocketConnectorReference(mockFinder, mockAssertion);

        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        reference.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);

        assertEquals(EXPECTED_XML, asXml.trim());
    }

    @Test
    public void testVaildSerializeToRefElementTestInboundWithSSLEnabled() throws Exception {
        String EXPECTED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:References xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\">\n" +
                "    <ExtensibleSocketConnectorReference RefType=\"com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorReference\">\n" +
                "        <GOID>00000000000000020000000000000001</GOID>\n" +
                "        <Name>name</Name>\n" +
                "        <In>true</In>\n" +
                "        <Port>2</Port>\n" +
                "        <UseSSL>true</UseSSL>\n" +
                "        <SslKeyID>sslId</SslKeyID>\n" +
                "        <ClientAuth>Required</ClientAuth>\n" +
                "        <ThreadPoolMin>1</ThreadPoolMin>\n" +
                "        <ThreadPoolMax>2</ThreadPoolMax>\n" +
                "        <BindAddress>address</BindAddress>\n" +
                "        <ServiceGOID>00000000000000030000000000000001</ServiceGOID>\n" +
                "        <ContentType>content</ContentType>\n" +
                "        <MaxMessageSize>100</MaxMessageSize>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <CodecConfiguration>&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                "&lt;java version=\"" + version + "\" class=\"java.beans.XMLDecoder\"&gt;\n" +
                " &lt;object class=\"com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.HL7CodecConfiguration\"/&gt;\n" +
                "&lt;/java&gt;\n" +
                "</CodecConfiguration>\n" +
                "    </ExtensibleSocketConnectorReference>\n" +
                "</exp:References>";

        when(mockEntity.getGoid()).thenReturn(new Goid(2, 1));
        when(mockEntity.getName()).thenReturn("name");
        when(mockEntity.isIn()).thenReturn(true);
        when(mockEntity.getHostname()).thenReturn("hostname");
        when(mockEntity.getPort()).thenReturn(2);
        when(mockEntity.isUseSsl()).thenReturn(true);
        when(mockEntity.getSslKeyId()).thenReturn("sslId");
        when(mockEntity.getClientAuthEnum()).thenReturn(SSLClientAuthEnum.REQUIRED);
        when(mockEntity.getThreadPoolMin()).thenReturn(1);
        when(mockEntity.getThreadPoolMax()).thenReturn(2);
        when(mockEntity.getBindAddress()).thenReturn("address");
        when(mockEntity.getServiceGoid()).thenReturn(new Goid(3, 1));
        when(mockEntity.getContentType()).thenReturn("content");
        when(mockEntity.getMaxMessageSize()).thenReturn(100);
        when(mockEntity.isEnabled()).thenReturn(true);
        when(mockEntity.getCodecConfiguration()).thenReturn(new HL7CodecConfiguration());
        when(mockEntity.getExchangePattern()).thenReturn(ExchangePatternEnum.OutIn);

        ExtensibleSocketConnectorReference reference = new ExtensibleSocketConnectorReference(mockFinder, mockAssertion);

        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        reference.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);

        assertEquals(EXPECTED_XML, asXml.trim());


    }

    @Test
    public void testVaildSerializeToRefElementTestInboundWithSSLDisabled() throws Exception {
        String EXPECTED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:References xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\">\n" +
                "    <ExtensibleSocketConnectorReference RefType=\"com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorReference\">\n" +
                "        <GOID>00000000000000020000000000000001</GOID>\n" +
                "        <Name>name</Name>\n" +
                "        <In>true</In>\n" +
                "        <Port>2</Port>\n" +
                "        <UseSSL>false</UseSSL>\n" +
                "        <ThreadPoolMin>1</ThreadPoolMin>\n" +
                "        <ThreadPoolMax>2</ThreadPoolMax>\n" +
                "        <BindAddress>address</BindAddress>\n" +
                "        <ServiceGOID>00000000000000030000000000000001</ServiceGOID>\n" +
                "        <ContentType>content</ContentType>\n" +
                "        <MaxMessageSize>100</MaxMessageSize>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <CodecConfiguration>&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                "&lt;java version=\"" + version + "\" class=\"java.beans.XMLDecoder\"&gt;\n" +
                " &lt;object class=\"com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.HL7CodecConfiguration\"/&gt;\n" +
                "&lt;/java&gt;\n" +
                "</CodecConfiguration>\n" +
                "    </ExtensibleSocketConnectorReference>\n" +
                "</exp:References>";

        when(mockEntity.getGoid()).thenReturn(new Goid(2, 1));
        when(mockEntity.getName()).thenReturn("name");
        when(mockEntity.isIn()).thenReturn(true);
        when(mockEntity.getHostname()).thenReturn("hostname");
        when(mockEntity.getPort()).thenReturn(2);
        when(mockEntity.isUseSsl()).thenReturn(false);
        when(mockEntity.getSslKeyId()).thenReturn("sslId");
        when(mockEntity.getClientAuthEnum()).thenReturn(SSLClientAuthEnum.REQUIRED);
        when(mockEntity.getThreadPoolMin()).thenReturn(1);
        when(mockEntity.getThreadPoolMax()).thenReturn(2);
        when(mockEntity.getBindAddress()).thenReturn("address");
        when(mockEntity.getServiceGoid()).thenReturn(new Goid(3, 1));
        when(mockEntity.getContentType()).thenReturn("content");
        when(mockEntity.getMaxMessageSize()).thenReturn(100);
        when(mockEntity.isEnabled()).thenReturn(true);
        when(mockEntity.getCodecConfiguration()).thenReturn(new HL7CodecConfiguration());
        when(mockEntity.getExchangePattern()).thenReturn(ExchangePatternEnum.OutIn);

        ExtensibleSocketConnectorReference reference = new ExtensibleSocketConnectorReference(mockFinder, mockAssertion);

        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        reference.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);

        assertEquals(EXPECTED_XML, asXml.trim());
    }

    @Test
    public void shouldVerifyWhenReferenceFound() throws Exception {
        Goid goid = new Goid(2, 1);
        ExtensibleSocketConnectorEntity foundEntity = mock(ExtensibleSocketConnectorEntity.class);
        when(foundEntity.getName()).thenReturn("name");
        when(foundEntity.getGoid()).thenReturn(goid);
        when(mockGenericEntity.findByPrimaryKey(goid)).thenReturn(foundEntity);

        assertTrue(reference.verifyReference());
    }

    @Test
    public void shouldNotVerifyWhenReferenceFoundDoesnotMatch() throws Exception {
        Goid goid = new Goid(2, 1);
        ExtensibleSocketConnectorEntity foundEntity = mock(ExtensibleSocketConnectorEntity.class);
        when(foundEntity.getName()).thenReturn("newName");
        when(foundEntity.getGoid()).thenReturn(goid);
        when(mockGenericEntity.findByPrimaryKey(goid)).thenReturn(foundEntity);

        assertFalse(reference.verifyReference());
    }

    @Test
    public void shouldNotVerifyWhenReferenceFoundIsInbound() throws Exception {
        ExtensibleSocketConnectorEntity foundEntity = mock(ExtensibleSocketConnectorEntity.class);
        when(foundEntity.isIn()).thenReturn(true);
        Collection<ExtensibleSocketConnectorEntity> list = new ArrayList<>();
        list.add(foundEntity);
        when(mockGenericEntity.findAll()).thenReturn(list);

        assertFalse(reference.verifyReference());
    }

    @Test
    public void shouldVerifyWhenReferencePartialFoundByName() throws Exception {
        Collection<ExtensibleSocketConnectorEntity> list = new ArrayList<>();
        list.add(mockEntity);
        when(mockGenericEntity.findAll()).thenReturn(list);

        assertTrue(reference.verifyReference());
    }

    @Test
    public void shouldVerifyWhenReferencePartialFoundByProperty() throws Exception {
        Goid goid = new Goid(3, 1);
        ExtensibleSocketConnectorEntity newMockEntity = mock(ExtensibleSocketConnectorEntity.class);
        when(newMockEntity.getName()).thenReturn("newName");
        when(newMockEntity.isIn()).thenReturn(false);
        when(newMockEntity.getGoid()).thenReturn(goid);
        when(newMockEntity.getHostname()).thenReturn("hostname");
        when(newMockEntity.getPort()).thenReturn(2);
        Collection<ExtensibleSocketConnectorEntity> list = new ArrayList<>();
        list.add(newMockEntity);
        when(mockGenericEntity.findAll()).thenReturn(list);

        assertTrue(reference.verifyReference());
    }

    @Test
    public void shouldLocalizeReplaceAssertion() throws Exception {
        Goid goid1 = new Goid(3, 1);
        ExtensibleSocketConnectorAssertion assertion = new ExtensibleSocketConnectorAssertion();
        assertion.setSocketConnectorGoid(new Goid(2, 1));
        reference.setLocalizeReplace(goid1);

        Boolean result = reference.localizeAssertion(assertion);

        Assert.assertEquals(goid1, assertion.getSocketConnectorGoid());
        assertTrue(result);
    }

    @Test
    public void shouldLocalizeIgnoreAssertion() throws Exception {
        ExtensibleSocketConnectorAssertion assertion = new ExtensibleSocketConnectorAssertion();
        reference.setLocalizeIgnore();

        Boolean result = reference.localizeAssertion(assertion);

        assertTrue(result);
    }

    @Test
    public void shouldNotLocalizeDeleteAssertion() throws Exception {
        Goid goid1 = new Goid(2, 1);
        ExtensibleSocketConnectorAssertion assertion = new ExtensibleSocketConnectorAssertion();
        assertion.setSocketConnectorGoid(goid1);
        reference.setLocalizeDelete();

        Boolean result = reference.localizeAssertion(assertion);

        assertFalse(result);
    }

    @Test
    public void parseReferenceFromElementOutboundWithSslDisabled() throws Exception {
        String ESC_REF = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "    <ExtensibleSocketConnectorReference RefType=\"com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorReference\">\n" +
                "        <GOID>00000000000000020000000000000001</GOID>\n" +
                "        <Name>name</Name>\n" +
                "        <In>false</In>\n" +
                "        <Hostname>hostname</Hostname>\n" +
                "        <Port>2</Port>\n" +
                "        <UseSSL>false</UseSSL>\n" +
                "        <ContentType>content</ContentType>\n" +
                "        <ExchangePattern>OutIn</ExchangePattern>\n" +
                "        <CodecConfiguration>&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                "&lt;java version=\"" + version + "\" class=\"java.beans.XMLDecoder\"&gt;\n" +
                " &lt;object class=\"com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.MLLPCodecConfiguration\"/&gt;\n" +
                "&lt;/java&gt;\n" +
                "</CodecConfiguration>\n" +
                "    </ExtensibleSocketConnectorReference>";

        Element element = XmlUtil.parse(ESC_REF).getDocumentElement();
        ExtensibleSocketConnectorReference newReference = (ExtensibleSocketConnectorReference) reference.parseFromElement(mockFinder, element);

        assertEquals(new Goid(2, 1), newReference.getGoid());
        assertEquals("hostname", newReference.getHostname());
        assertEquals("name", newReference.getName());
        assertFalse(newReference.isIn());
        assertNull(newReference.getSslKeyId());
        assertEquals(2, newReference.getPort());
        assertFalse(newReference.isUseSsl());
        assertEquals("content", newReference.getContentType());
        assertEquals(ExchangePatternEnum.OutIn, newReference.getExchangePattern());

    }

    @Test
    public void parseReferenceFromElementInboundWithSslEnabled() throws Exception {
        String ESC_REF = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "    <ExtensibleSocketConnectorReference RefType=\"com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorReference\">\n" +
                "        <GOID>00000000000000020000000000000001</GOID>\n" +
                "        <Name>name</Name>\n" +
                "        <In>true</In>\n" +
                "        <Port>2</Port>\n" +
                "        <UseSSL>true</UseSSL>\n" +
                "        <SslKeyID>sslId</SslKeyID>\n" +
                "        <ClientAuth>REQUIRED</ClientAuth>\n" +
                "        <ThreadPoolMin>1</ThreadPoolMin>\n" +
                "        <ThreadPoolMax>2</ThreadPoolMax>\n" +
                "        <BindAddress>address</BindAddress>\n" +
                "        <ServiceGOID>00000000000000030000000000000001</ServiceGOID>\n" +
                "        <ContentType>content</ContentType>\n" +
                "        <MaxMessageSize>100</MaxMessageSize>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <CodecConfiguration>&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                "&lt;java version=\"" + version + "\" class=\"java.beans.XMLDecoder\"&gt;\n" +
                " &lt;object class=\"com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.HL7CodecConfiguration\"/&gt;\n" +
                "&lt;/java&gt;\n" +
                "</CodecConfiguration>\n" +
                "    </ExtensibleSocketConnectorReference>";


        Element element = XmlUtil.parse(ESC_REF).getDocumentElement();
        ExtensibleSocketConnectorReference newReference = (ExtensibleSocketConnectorReference) reference.parseFromElement(mockFinder, element);

        assertEquals(new Goid(2, 1), newReference.getGoid());
        assertNull(newReference.getHostname());
        assertEquals(2, newReference.getPort());
        assertEquals("name", newReference.getName());
        assertTrue(newReference.isIn());
        assertEquals("sslId", newReference.getSslKeyId());
        assertEquals(SSLClientAuthEnum.REQUIRED, newReference.getClientAuthEnum());
        assertEquals(1, newReference.getThreadPoolMin());
        assertEquals(2, newReference.getThreadPoolMax());
        assertEquals("address", newReference.getBindAddress());
        assertEquals(new Goid(3, 1), newReference.getServiceGoid());
        assertEquals("content", newReference.getContentType());
        assertTrue(newReference.isUseSsl());
        assertEquals(100, newReference.getMaxMessageSize());
        assertTrue(newReference.isEnabled());
    }

    @Test
    public void parseReferenceFromElementOutboundWithSslEnabledAndNewSettings() throws Exception {
        String ESC_REF = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "    <ExtensibleSocketConnectorReference RefType=\"com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorReference\">\n" +
                "        <GOID>00000000000000020000000000000001</GOID>\n" +
                "        <Name>name</Name>\n" +
                "        <In>false</In>\n" +
                "        <Hostname>hostname</Hostname>\n" +
                "        <Port>2</Port>\n" +
                "        <UseSSL>true</UseSSL>\n" +
                "        <SslKeyID>sslId</SslKeyID>\n" +
                "        <ContentType>content</ContentType>\n" +
                "        <ExchangePattern>OutIn</ExchangePattern>\n" +
                "        <KeepAlive>true</KeepAlive>\n" +
                "        <ListenTimeOut>2</ListenTimeOut>\n" +
                "        <useDnsLookup>true</useDnsLookup>\n" +
                "        <dnsService>serviceName</dnsService>\n" +
                "        <dnsDomainName>domainName</dnsDomainName>\n" +
                "        <CodecConfiguration>&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                "&lt;java version=\"" + version + "\" class=\"java.beans.XMLDecoder\"&gt;\n" +
                " &lt;object class=\"com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.MLLPCodecConfiguration\"/&gt;\n" +
                "&lt;/java&gt;\n" +
                "</CodecConfiguration>\n" +
                "    </ExtensibleSocketConnectorReference>";

        Element element = XmlUtil.parse(ESC_REF).getDocumentElement();
        ExtensibleSocketConnectorReference newReference = (ExtensibleSocketConnectorReference) reference.parseFromElement(mockFinder, element);

        assertEquals(new Goid(2, 1), newReference.getGoid());
        assertEquals("name", newReference.getName());
        assertFalse(newReference.isIn());
        assertEquals("hostname", newReference.getHostname());
        assertEquals(2, newReference.getPort());
        assertTrue(newReference.isUseSsl());
        assertEquals("sslId", newReference.getSslKeyId());
        assertNull(newReference.getClientAuthEnum());
        assertEquals("content", newReference.getContentType());
        assertEquals(ExchangePatternEnum.OutIn, newReference.getExchangePattern());
        assertEquals(Boolean.TRUE, newReference.isKeepAlive());
        assertEquals(Boolean.TRUE, newReference.isUseDnsLookup());
        assertEquals(2L, newReference.getListenTimeOut());
        assertEquals("serviceName", newReference.getDnsService());
        assertEquals("domainName", newReference.getDnsDomainName());
    }

    @Test
    public void parseReferenceFromElementOutboundWithSslEnabled() throws Exception {
        String ESC_REF = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "    <ExtensibleSocketConnectorReference RefType=\"com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorReference\">\n" +
                "        <GOID>00000000000000020000000000000001</GOID>\n" +
                "        <Name>name</Name>\n" +
                "        <In>false</In>\n" +
                "        <Hostname>hostname</Hostname>\n" +
                "        <Port>2</Port>\n" +
                "        <UseSSL>true</UseSSL>\n" +
                "        <SslKeyID>sslId</SslKeyID>\n" +
                "        <ContentType>content</ContentType>\n" +
                "        <ExchangePattern>OutIn</ExchangePattern>\n" +
                "        <CodecConfiguration>&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                "&lt;java version=\"" + version + "\" class=\"java.beans.XMLDecoder\"&gt;\n" +
                " &lt;object class=\"com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.MLLPCodecConfiguration\"/&gt;\n" +
                "&lt;/java&gt;\n" +
                "</CodecConfiguration>\n" +
                "    </ExtensibleSocketConnectorReference>";

        Element element = XmlUtil.parse(ESC_REF).getDocumentElement();
        ExtensibleSocketConnectorReference newReference = (ExtensibleSocketConnectorReference) reference.parseFromElement(mockFinder, element);

        assertEquals(new Goid(2, 1), newReference.getGoid());
        assertEquals("name", newReference.getName());
        assertFalse(newReference.isIn());
        assertEquals("hostname", newReference.getHostname());
        assertEquals(2, newReference.getPort());
        assertTrue(newReference.isUseSsl());
        assertEquals("sslId", newReference.getSslKeyId());
        assertNull(newReference.getClientAuthEnum());
        assertEquals("content", newReference.getContentType());
        assertEquals(ExchangePatternEnum.OutIn, newReference.getExchangePattern());
    }

    @Test
    public void parseReferenceFromElementInboundWithSslDisabled() throws Exception {
        String ESC_REF = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "    <ExtensibleSocketConnectorReference RefType=\"com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorReference\">\n" +
                "        <GOID>00000000000000020000000000000001</GOID>\n" +
                "        <Name>name</Name>\n" +
                "        <In>true</In>\n" +
                "        <Port>2</Port>\n" +
                "        <UseSSL>false</UseSSL>\n" +
                "        <ThreadPoolMin>1</ThreadPoolMin>\n" +
                "        <ThreadPoolMax>2</ThreadPoolMax>\n" +
                "        <BindAddress>address</BindAddress>\n" +
                "        <ServiceGOID>00000000000000030000000000000001</ServiceGOID>\n" +
                "        <ContentType>content</ContentType>\n" +
                "        <MaxMessageSize>100</MaxMessageSize>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <CodecConfiguration>&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                "&lt;java version=\"" + version + "\" class=\"java.beans.XMLDecoder\"&gt;\n" +
                " &lt;object class=\"com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.HL7CodecConfiguration\"/&gt;\n" +
                "&lt;/java&gt;\n" +
                "</CodecConfiguration>\n" +
                "    </ExtensibleSocketConnectorReference>";

        Element element = XmlUtil.parse(ESC_REF).getDocumentElement();
        ExtensibleSocketConnectorReference newReference = (ExtensibleSocketConnectorReference) reference.parseFromElement(mockFinder, element);

        assertEquals(new Goid(2, 1), newReference.getGoid());
        assertEquals("name", newReference.getName());
        assertTrue(newReference.isIn());
        assertNull(newReference.getHostname());
        assertEquals(2, newReference.getPort());
        assertFalse(newReference.isUseSsl());
        assertNull(newReference.getSslKeyId());
        assertEquals(1, newReference.getThreadPoolMin());
        assertEquals(2, newReference.getThreadPoolMax());
        assertEquals("address", newReference.getBindAddress());
        assertEquals(new Goid(3, 1), newReference.getServiceGoid());
        assertEquals("content", newReference.getContentType());
        assertEquals(100, newReference.getMaxMessageSize());
        assertTrue(newReference.isEnabled());
    }

    @Test(expected = InvalidDocumentFormatException.class)
    public void parseReferenceFromInvalidRefElement() throws Exception {
        String Invalid_REFERENCE_ELEM = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:References xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\">\n" +
                "    <SomethingElse RefType=\"com.l7tech.external.assertions.somethingElse\">\n" +
                "        <GOID>00000000000000020000000000000001</GOID>\n" +
                "        <Name>name</Name>\n" +
                "        <In>false</In>\n" +
                "        <Hostname>hostname</Hostname>\n" +
                "        <Port>2</Port>\n" +
                "        <UseSSL>false</UseSSL>\n" +
                "        <ContentType>content</ContentType>\n" +
                "        <ExchangePattern>OutIn</ExchangePattern>\n" +
                "        <CodecConfiguration>&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                "&lt;java version=\"" + version + "\" class=\"java.beans.XMLDecoder\"&gt;\n" +
                " &lt;object class=\"com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.MLLPCodecConfiguration\"/&gt;\n" +
                "&lt;/java&gt;\n" +
                "</CodecConfiguration>\n" +
                "    </SomethingElse>\n" +
                "</exp:References>";
        Element element = XmlUtil.parse(Invalid_REFERENCE_ELEM).getDocumentElement();

        reference.parseFromElement(mockFinder, element);
    }
}
