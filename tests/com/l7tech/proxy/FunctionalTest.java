package com.l7tech.proxy;

import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgManagerStub;
import com.l7tech.proxy.datamodel.PolicyManager;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TrueAssertion;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.axis.AxisEngine;
import org.apache.axis.client.Call;
import org.apache.axis.encoding.DeserializationContextImpl;
import org.apache.axis.message.*;
import org.apache.axis.soap.SOAPConstants;
import org.mortbay.util.MultiException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.soap.SOAPException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.io.IOException;

/**
 * Test message processing.
 * User: mike
 * Date: Jun 5, 2003
 * Time: 12:05:43 PM
 */
public class FunctionalTest extends TestCase {
    private static final String pingNamespace = "http://services.l7tech.com/soap/demos/Ping";
    private static final String pingPrefix = "p";
    private static final String ssg0ProxyEndpoint = "ssg0";
    private static final int DEFAULT_PORT = 5555;
    private static final int MIN_THREADS = 4;
    private static final int MAX_THREADS = 20;

    private SsgFaker ssgFaker;
    private ClientProxy clientProxy;
    private SsgManagerStub ssgManager;
    private String ssgUrl;
    private String proxyUrl;

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
    protected void setUp() throws MultiException {
        destroyFaker();
        destroyProxy();

        // Start the fake SSG
        ssgFaker = new SsgFaker();
        ssgUrl = ssgFaker.start();

        // Configure the client proxy
        ssgManager = new SsgManagerStub();
        ssgManager.clear();
        Ssg ssgFake = ssgManager.createSsg();
        ssgFake.setName("SSG Faker");
        ssgFake.setLocalEndpoint(ssg0ProxyEndpoint);
        ssgFake.setServerUrl(ssgUrl);
        ssgManager.add(ssgFake);

        // Make a do-nothing PolicyManager
        MessageProcessor messageProcessor = new MessageProcessor(new PolicyManager() {
            public Assertion getPolicy(Ssg ssg) throws IOException {
                return new TrueAssertion();
            }
        });

        // Start the client proxy
        clientProxy = new ClientProxy(ssgManager, messageProcessor, DEFAULT_PORT, MIN_THREADS, MAX_THREADS);
        proxyUrl = clientProxy.start().toString();
    }

    /** Shuts down the SSG Faker and the Client Proxy. */
    protected void tearDown() {
        destroyFaker();
        destroyProxy();
    }

    /**
     * Bounce a message off of the echo server, going through the client proxy.
     */
    public void testSimplePing() throws RemoteException, SOAPException, MalformedURLException {
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
        String payload = "ping 1 2 3";
        reqBe.addChildElement("pingData").addTextNode(payload);
        reqEnvelope.addBodyElement(reqBe);

        Call call = new Call(proxyUrl + ssg0ProxyEndpoint);
        SOAPEnvelope responseEnvelope = call.invoke(reqEnvelope);

        System.out.println("Client:  I Sent: " + reqEnvelope);
        System.out.println("Client:  I Got back: " + responseEnvelope);
        MessageElement re = (MessageElement)responseEnvelope.getBody().getChildElements().next();
        MessageElement rec = (MessageElement)re.getChildren().get(0);
        assertTrue(rec.getValue().equals(payload));
    }
}
