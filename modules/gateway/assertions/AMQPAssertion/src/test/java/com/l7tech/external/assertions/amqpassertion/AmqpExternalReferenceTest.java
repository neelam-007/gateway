package com.l7tech.external.assertions.amqpassertion;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.InvalidDocumentFormatException;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AmqpExternalReferenceTest {

    //    private RouteViaAMQPAssertion mockAssertion;
    private ExternalReferenceFinder mockFinder;
    private SsgActiveConnector mockActiveConnector;

    private Goid mockGoid;


    @Before
    public void setUp() throws FindException {
//        mockAssertion = mock(RouteViaAMQPAssertion.class);
        mockFinder = mock(ExternalReferenceFinder.class);
        mockActiveConnector = mock(SsgActiveConnector.class);

        mockGoid = Goid.DEFAULT_GOID;


    }

    @Test
    public void serializeWithDefaultGoid() throws IOException {
        String EXPECTED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:References xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\"/>";

        AmqpExternalReference reference = new AmqpExternalReference(mockFinder, mockGoid);
        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        reference.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);
        assertEquals(EXPECTED_XML, asXml.trim());
    }

    @Test
    public void serializeWithBasicData() throws IOException {
        Goid specificServiceGoid = new Goid(1, 0);
        String activeConnectorName = "Active Connector Name";

        String EXPECTED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:References xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\">\n" +
                "    <AmqpExternalReference RefType=\"com.l7tech.external.assertions.amqpassertion.AmqpExternalReference\">\n" +
                "        <AMQPServiceGoid>" + specificServiceGoid.toString() + "</AMQPServiceGoid>\n" +
                "        <AMQPName>" + activeConnectorName + "</AMQPName>\n" +
                "    </AmqpExternalReference>\n" +
                "</exp:References>";


        when(mockActiveConnector.getGoid()).thenReturn(specificServiceGoid);
        when(mockActiveConnector.getName()).thenReturn(activeConnectorName);

        AmqpExternalReference reference = new AmqpExternalReference(mockFinder, specificServiceGoid);
        reference.setSsgActiveConnector(mockActiveConnector);

        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        reference.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);
        assertEquals(EXPECTED_XML, asXml.trim());
    }

    @Test
    public void serializeWithParametersSet() throws IOException {
        Goid specificServiceGoid = new Goid(1, 0);
        String activeConnectorName = "Active Connector Name";
        String addresses = "http://server:port";
        String queueName = "testQueue";

        String EXPECTED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:References xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\">\n" +
                "    <AmqpExternalReference RefType=\"com.l7tech.external.assertions.amqpassertion.AmqpExternalReference\">\n" +
                "        <" + AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_SERVICEGOID + ">" + specificServiceGoid.toString() +
                "</" + AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_SERVICEGOID + ">\n" +
                "        <" + AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_NAME + ">" + activeConnectorName +
                "</" + AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_NAME + ">\n" +
                "        <" + AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ADDRESSES + ">" + addresses +
                "</" + AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ADDRESSES + ">\n" +
                "        <" + AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_QUEUE_NAME + ">" + queueName +
                "</" + AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_QUEUE_NAME + ">\n" +
                "    </AmqpExternalReference>\n" +
                "</exp:References>";


        when(mockActiveConnector.getGoid()).thenReturn(specificServiceGoid);
        when(mockActiveConnector.getName()).thenReturn(activeConnectorName);
        when(mockActiveConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ADDRESSES)).thenReturn(addresses);
        when(mockActiveConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_QUEUE_NAME)).thenReturn(queueName);

        AmqpExternalReference reference = new AmqpExternalReference(mockFinder, specificServiceGoid);
        reference.setSsgActiveConnector(mockActiveConnector);

        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        reference.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);
        assertEquals(EXPECTED_XML, asXml.trim());
    }

    @Test
    public void shouldFailVerifyWhenNoReferenceFound() throws InvalidPolicyStreamException {
        AmqpExternalReference reference = new AmqpExternalReference(mockFinder, mockGoid);
        reference.setSsgActiveConnector(mockActiveConnector);
        assertFalse(reference.verifyReference());
    }

    @Test
    public void shouldVerifyWhenActiveConnectorFoundViaPrimaryKey() throws InvalidPolicyStreamException, FindException {
        String addresses = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<java version=\"1.8.0_60\" class=\"java.beans.XMLDecoder\">\n" +
                " <array class=\"java.lang.String\" length=\"1\">\n" +
                "  <void index=\"0\">\n" +
                "   <string>hostname:5672</string>\n" +
                "  </void>\n" +
                " </array>\n" +
                "</java>";

        AmqpExternalReference reference = new AmqpExternalReference(mockFinder, mockGoid);
        reference.setSsgActiveConnector(mockActiveConnector);
        SsgActiveConnector foundMockActiveConnector = mock(SsgActiveConnector.class);
        when(mockActiveConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ADDRESSES)).thenReturn(addresses);
        when(foundMockActiveConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ADDRESSES)).thenReturn(addresses);
        when(mockFinder.findConnectorByPrimaryKey(mockActiveConnector.getGoid())).thenReturn(foundMockActiveConnector);

        assertTrue(reference.verifyReference());
    }

    @Test
    public void shouldVerifyWhenActiveConnectorFoundViaOutboundQueuesAndNamesMatch() throws InvalidPolicyStreamException, FindException {

        String exchangeName = "Exchange Name";
        AmqpExternalReference reference = new AmqpExternalReference(mockFinder, mockGoid);
        reference.setSsgActiveConnector(mockActiveConnector);
        Collection<SsgActiveConnector> amqpConnectors = new ArrayList<>();
        SsgActiveConnector foundMockActiveConnector = mock(SsgActiveConnector.class);

        when(foundMockActiveConnector.getName()).thenReturn(exchangeName);
        when(foundMockActiveConnector.getGoid()).thenReturn(new Goid(1, 2));
        when(mockActiveConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_EXCHANGE_NAME)).thenReturn(exchangeName);
        amqpConnectors.add(foundMockActiveConnector);

        when(mockFinder.findSsgActiveConnectorsByType(AmqpSsgActiveConnector.ACTIVE_CONNECTOR_TYPE_AMQP)).thenReturn(amqpConnectors);

        assertTrue(reference.verifyReference());
    }

    @Test
    public void shouldVerifyWhenActiveConnectorFoundViaMatchingExchangeNameQueueNameAndReplyBehaviour() throws
            InvalidPolicyStreamException, FindException {

        String exchangeName = "Exchange Name";
        String differentExchangeName = "Different Exchange Name";
        String replyBehaviour = "ONE_WAY";
        String amqpQueueName = "Queue1";
        AmqpExternalReference reference = new AmqpExternalReference(mockFinder, mockGoid);
        reference.setSsgActiveConnector(mockActiveConnector);
        Collection<SsgActiveConnector> amqpConnectors = new ArrayList<>();
        SsgActiveConnector foundMockActiveConnector = mock(SsgActiveConnector.class);

        when(foundMockActiveConnector.getName()).thenReturn(exchangeName);
        when(foundMockActiveConnector.getGoid()).thenReturn(new Goid(1, 2));

        when(foundMockActiveConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_EXCHANGE_NAME)).thenReturn(differentExchangeName);
        when(mockActiveConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_EXCHANGE_NAME)).thenReturn(differentExchangeName);
        when(foundMockActiveConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_QUEUE_NAME)).thenReturn(amqpQueueName);
        when(mockActiveConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_QUEUE_NAME)).thenReturn(amqpQueueName);
        when(foundMockActiveConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_OUTBOUND_REPLY_BEHAVIOUR)).thenReturn(replyBehaviour);
        when(mockActiveConnector.getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_OUTBOUND_REPLY_BEHAVIOUR)).thenReturn(replyBehaviour);
        amqpConnectors.add(foundMockActiveConnector);

        when(mockFinder.findSsgActiveConnectorsByType(AmqpSsgActiveConnector.ACTIVE_CONNECTOR_TYPE_AMQP)).thenReturn(amqpConnectors);

        assertTrue(reference.verifyReference());
    }

    @Test
    public void shouldLocalizeDeleteAssertion() throws Exception {
        AmqpExternalReference reference = new AmqpExternalReference(mockFinder, mockGoid);
        reference.setLocalizeDelete();

        RouteViaAMQPAssertion assertion = new RouteViaAMQPAssertion();

        Boolean result = reference.localizeAssertion(assertion);
        // on successful Delete it will return false
        assertFalse(result);
    }

    @Test
    public void shouldLocalizeReplaceAssertion() throws Exception {
        String exchangeName = "Exchange Name";
        AmqpExternalReference reference = new AmqpExternalReference(mockFinder, mockGoid);
        Goid goid = new Goid(1, 2);
        reference.setLocalizeReplace(goid);

        SsgActiveConnector foundMockActiveConnector = mock(SsgActiveConnector.class);
        when(foundMockActiveConnector.getName()).thenReturn(exchangeName);
        when(mockFinder.findConnectorByPrimaryKey(goid)).thenReturn(foundMockActiveConnector);


        RouteViaAMQPAssertion assertion = new RouteViaAMQPAssertion();

        Boolean result = reference.localizeAssertion(assertion);
        assertEquals(goid, assertion.getSsgActiveConnectorGoid());
        assertEquals(exchangeName, assertion.getSsgActiveConnectorName());
        assertTrue(result);
    }

    @Test
    public void shouldLocalizeIgnoreAssertion() throws Exception {
        AmqpExternalReference reference = new AmqpExternalReference(mockFinder, mockGoid);
        reference.setLocalizeIgnore();

        RouteViaAMQPAssertion assertion = new RouteViaAMQPAssertion();

        Boolean result = reference.localizeAssertion(assertion);
        // on successful Delete it will return false
        assertTrue(result);
    }

    @Test
    public void parseReferenceFromElementWithBasicData() throws SAXException, InvalidDocumentFormatException {
        Goid specificServiceGoid = new Goid(1, 0);
        String activeConnectorName = "Active Connector Name";
        String AMQP_REF = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<AmqpExternalReference RefType=\"com.l7tech.external.assertions.amqpassertion.AmqpExternalReference\">\n" +
                "    <AMQPServiceGoid>" + specificServiceGoid.toString() + "</AMQPServiceGoid>\n" +
                "    <AMQPName>" + activeConnectorName + "</AMQPName>\n" +
                "</AmqpExternalReference>";
        Element element = XmlUtil.parse(AMQP_REF).getDocumentElement();

        AmqpExternalReference newReference = AmqpExternalReference.parseFromElement(mockFinder, element);
        assertEquals(specificServiceGoid, newReference.getGoid());
        assertEquals(activeConnectorName, newReference.getSsgActiveConnector().getName());
    }

    @Test
    public void parseReferenceFromElementWithParametersSet() throws SAXException, InvalidDocumentFormatException {
        Goid specificServiceGoid = new Goid(1, 0);
        String activeConnectorName = "Active Connector Name";
        String addresses = "http://server:port";
        String queueName = "testQueue";

        String AMQP_REF = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<AmqpExternalReference RefType=\"com.l7tech.external.assertions.amqpassertion.AmqpExternalReference\">" +
                "        <" + AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_SERVICEGOID + ">" + specificServiceGoid.toString() +
                "</" + AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_SERVICEGOID + ">\n" +
                "        <" + AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_NAME + ">" + activeConnectorName +
                "</" + AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_NAME + ">\n" +
                "        <" + AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ADDRESSES + ">" + addresses +
                "</" + AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ADDRESSES + ">\n" +
                "        <" + AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_QUEUE_NAME + ">" + queueName +
                "</" + AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_QUEUE_NAME + ">\n" +
                "    </AmqpExternalReference>";
        Element element = XmlUtil.parse(AMQP_REF).getDocumentElement();

        AmqpExternalReference newReference = AmqpExternalReference.parseFromElement(mockFinder, element);
        assertEquals(specificServiceGoid, newReference.getGoid());
        assertEquals(activeConnectorName, newReference.getSsgActiveConnector().getName());
        assertEquals(addresses, newReference.getSsgActiveConnector().getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ADDRESSES));
        assertEquals(queueName, newReference.getSsgActiveConnector().getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_QUEUE_NAME));

    }
}
