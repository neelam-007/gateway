package com.l7tech.proxy;

import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.proxy.datamodel.PolicyManagerStub;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgManagerStub;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.processor.MessageProcessor;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.axis.AxisEngine;
import org.apache.axis.AxisFault;
import org.apache.axis.client.Call;
import org.apache.axis.encoding.DeserializationContextImpl;
import org.apache.axis.message.MessageElement;
import org.apache.axis.message.SOAPBodyElement;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.message.SOAPHandler;
import org.apache.axis.message.SOAPHeader;
import org.apache.axis.soap.SOAPConstants;
import org.apache.log4j.Category;
import org.mortbay.util.MultiException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.soap.SOAPException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Test message processing.
 * User: mike
 * Date: Jun 5, 2003
 * Time: 12:05:43 PM
 */
public class FunctionalTest extends TestCase {
    private static final Category log = Category.getInstance(FunctionalTest.class);
    private static final String pingNamespace = "http://services.l7tech.com/soap/demos/Ping";
    private static final String pingPrefix = "p";
    private static final String ssg0ProxyEndpoint = "ssg0";
    private static final int DEFAULT_PORT = 5555;
    private static final int MIN_THREADS = 4;
    private static final int MAX_THREADS = 20;

    private SsgFaker ssgFaker;
    private ClientProxy clientProxy;
    private SsgManagerStub ssgManager;
    private PolicyManagerStub policyManager = new PolicyManagerStub();
    private String ssgUrl;
    private String proxyUrl;
    private Ssg ssgFake;

    public FunctionalTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(FunctionalTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private void destroyFaker() {
        if (ssgFaker != null) {
            try {
                ssgFaker.destroy();
            } catch (IllegalStateException e) {
            }
            ssgFaker = null;
        }
    }

    private void destroyProxy() {
        if (clientProxy != null)
            clientProxy.destroy();
        clientProxy = null;
    }

    /** Starts up the SSG Faker and the Client Proxy. */
    protected void setUp() throws MultiException, IOException, KeyManagementException, NoSuchAlgorithmException, NoSuchProviderException {
        destroyFaker();
        destroyProxy();

        // Start the fake SSG
        ssgFaker = new SsgFaker();
        ssgUrl = ssgFaker.start();

        // Configure the client proxy
        ssgManager = new SsgManagerStub();
        ssgManager.clear();
        ssgFake = ssgManager.createSsg();
        ssgFake.setName("SSG Faker");
        ssgFake.setLocalEndpoint(ssg0ProxyEndpoint);
        ssgFake.setSsgAddress(new URL(ssgUrl).getHost());
        ssgFake.setSsgPort(new URL(ssgUrl).getPort());
        ssgManager.add(ssgFake);

        // Make a do-nothing PolicyManager
        policyManager = new PolicyManagerStub();
        policyManager.setPolicy(new Policy(new TrueAssertion(), "testpolicy"));
        MessageProcessor messageProcessor = new MessageProcessor(policyManager);

        // Start the client proxy
        clientProxy = new ClientProxy(ssgManager, messageProcessor, DEFAULT_PORT, MIN_THREADS, MAX_THREADS);
        proxyUrl = clientProxy.start().toString();
    }

    /** Shuts down the SSG Faker and the Client Proxy. */
    protected void tearDown() {
        destroyFaker();
        destroyProxy();
    }

    private SOAPEnvelope makePingRequest(String payload) throws AxisFault, SOAPException {
        SOAPEnvelope reqEnvelope = new SOAPEnvelope();

        SOAPHeader reqHeader = new SOAPHeader(pingNamespace,
                                              "/ssgFaker",
                                              pingPrefix,
                                              new AttributesImpl(),
                                              new DeserializationContextImpl(AxisEngine.getCurrentMessageContext(),
                                                                             new SOAPHandler()),
                                              SOAPConstants.SOAP12_CONSTANTS);
        reqHeader.setNamespaceURI(pingNamespace);
        reqEnvelope.setHeader(reqHeader);

        SOAPBodyElement reqBe = new SOAPBodyElement();
        reqBe.setNamespaceURI(pingNamespace + "#ping");
        reqBe.setName("ping");
        reqBe.addChildElement("pingData").addTextNode(payload);
        reqEnvelope.addBodyElement(reqBe);
        return reqEnvelope;
    }

    /**
     * Bounce a message off of the echo server, going through the client proxy.
     */
    public void testSimplePing() throws RemoteException, SOAPException, MalformedURLException {
        String payload = "ping 1 2 3";
        SOAPEnvelope reqEnvelope = makePingRequest(payload);

        policyManager.setPolicy(new Policy(new TrueAssertion(), "testpolicy"));
        ssgFake.setSsgFile("/soap/ssg");

        Call call = new Call(proxyUrl + ssg0ProxyEndpoint);
        SOAPEnvelope responseEnvelope = call.invoke(reqEnvelope);

        System.out.println("Client:  I Sent: " + reqEnvelope);
        System.out.println("Client:  I Got back: " + responseEnvelope);
        MessageElement re = (MessageElement)responseEnvelope.getBody().getChildElements().next();
        MessageElement rec = (MessageElement)re.getChildren().get(0);
        assertTrue(rec.getValue().equals(payload));
    }

    public void testBasicAuthPing() throws SOAPException, RemoteException, MalformedURLException {
        String payload = "ping 1 2 3";
        SOAPEnvelope reqEnvelope = makePingRequest(payload);

        policyManager.setPolicy(new Policy(new HttpBasic(), "testpolicy"));
        URL url = new URL(ssgUrl);
        ssgFake.setSsgAddress(url.getHost());
        ssgFake.setSsgPort(url.getPort());
        ssgFake.setSsgFile("/soap/ssg/basicauth");
        ssgFake.setUsername("testuser");
        ssgFake.password("testpassword".toCharArray());

        Call call = new Call(proxyUrl + ssg0ProxyEndpoint);
        SOAPEnvelope responseEnvelope = call.invoke(reqEnvelope);

        System.out.println("Client:  I Sent: " + reqEnvelope);
        System.out.println("Client:  I Got back: " + responseEnvelope);
        MessageElement re = (MessageElement)responseEnvelope.getBody().getChildElements().next();
        MessageElement rec = (MessageElement)re.getChildren().get(0);
        assertTrue(rec.getValue().equals(payload));
    }

    public void testSsgFault() throws Exception {
        String payload = "ping 1 2 3";
        SOAPEnvelope reqEnvelope = makePingRequest(payload);

        URL url = new URL(ssgUrl);
        ssgFake.setSsgAddress(url.getHost());
        ssgFake.setSsgPort(url.getPort());
        ssgFake.setSsgFile("/soap/ssg/throwfault");

        boolean threwAxisFault = false;
        try {
            Call call = new Call(proxyUrl + ssg0ProxyEndpoint);
            call.invoke(reqEnvelope);
        } catch (AxisFault e) {
            threwAxisFault = true;
            log.error("EndUserClient: SOAP call to CP threw expected exception: " + e.getClass().getName());
        }
        assertTrue(threwAxisFault);

    }

    public void testSslBasicAuthPing() throws SOAPException, RemoteException, MalformedURLException {
        /* TODO not working atm
        String payload = "ping 1 2 3";
        SOAPEnvelope reqEnvelope = makePingRequest(payload);

        policyManager.setPolicy(new ExactlyOneAssertion(Arrays.asList(new Assertion[] {
            new AllAssertion(Arrays.asList(new Assertion[] {
                new SslAssertion(),
                new HttpBasic()
            })),
            new HttpDigest()
        })));

        Call call = new Call(proxyUrl + ssg0ProxyEndpoint);
        SOAPEnvelope responseEnvelope = call.invoke(reqEnvelope);

        System.out.println("Client:  I Sent: " + reqEnvelope);
        System.out.println("Client:  I Got back: " + responseEnvelope);
        MessageElement re = (MessageElement)responseEnvelope.getBody().getChildElements().next();
        MessageElement rec = (MessageElement)re.getChildren().get(0);
        assertTrue(rec.getValue().equals(payload));     */
    }
}
