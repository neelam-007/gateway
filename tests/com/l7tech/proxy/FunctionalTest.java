package com.l7tech.proxy;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.axis.AxisEngine;
import org.apache.axis.client.Call;
import org.apache.axis.encoding.DeserializationContextImpl;
import org.apache.axis.message.*;
import org.apache.axis.soap.SOAPConstants;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.soap.SOAPException;
import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.rmi.RemoteException;

/**
 * Test message processing.
 * User: mike
 * Date: Jun 5, 2003
 * Time: 12:05:43 PM
 */
public class FunctionalTest extends TestCase {
    private SsgFaker ssgFaker;
    private String ssgUrl;
    private String pingNamespace = "http://services.l7tech.com/soap/demos/Ping";
    private String pingPrefix = "p";

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

    protected void setUp() {
        destroyFaker();
        ssgFaker = new SsgFaker();
        ssgUrl = ssgFaker.start();
    }

    protected void tearDown() {
        destroyFaker();
    }


    /**
     * Bounce a message of the echo server and verify that it worked.
     */
    public void testSimpleMessage() throws RemoteException, SOAPException, MalformedURLException {
        SOAPEnvelope reqEnvelope = new SOAPEnvelope();

        SOAPHeader reqHeader = new SOAPHeader(pingNamespace,
                                              "/ping",
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

        Call call = new Call(ssgUrl);
        SOAPEnvelope responseEnvelope = call.invoke(reqEnvelope);

        System.out.println("Sent: " + reqEnvelope);
        System.out.println("Got back: " + responseEnvelope);
        MessageElement re = (MessageElement)responseEnvelope.getBody().getChildElements().next();
        MessageElement rec = (MessageElement)re.getChildren().get(0);
        assertTrue(rec.getValue().equals(payload));
    }
}
