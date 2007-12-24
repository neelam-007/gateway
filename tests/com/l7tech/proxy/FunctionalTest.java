package com.l7tech.proxy;

import com.l7tech.common.http.GenericHttpException;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.proxy.datamodel.Policy;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Fire up a Client Proxy instance against a fake SSG and try sending messages through it.
 */
public class FunctionalTest {
    private static final Logger log = Logger.getLogger(FunctionalTest.class.getName());
    private static final String pingPrefix = "p";

    private BridgeTestHarness bt;


    /**
     * Starts up the SSG Faker and the Client Proxy.
     */
    @Before
    public void setUp() throws Exception {
        ResourceUtils.closeQuietly(bt);
        bt = new BridgeTestHarness();
        bt.reset();
    }

    /**
     * Shuts down the SSG Faker and the Client Proxy.
     */
    @After
    public void tearDown() {
        if (bt != null) bt.close();
        bt = null;
    }

    /*
     * Bounce a message off of the echo server, going through the client proxy.            kk
     */
    @Test
    public void testSimplePing() throws Exception {
        String payload = "ping 1 2 3";
        Document reqEnvelope = SsgFaker.makePingRequest(payload);

        bt.policyManager.setPolicy(new Policy(new TrueAssertion(), "testpolicy"));
        bt.ssgFake.setSsgFile("/soap/ssg");
        bt.ssgFake.getRuntime().setPolicyManager(bt.policyManager);

        sendPing(payload, reqEnvelope);
    }

    private void sendPing(String payload, Document reqEnvelope) throws SAXException, IOException, InvalidDocumentFormatException {
        Document responseEnvelope = sendXml(reqEnvelope);

        log.info("Client:  I Sent: " + XmlUtil.nodeToFormattedString(reqEnvelope));
        log.info("Client:  I Got back: " + XmlUtil.nodeToFormattedString(responseEnvelope));

        Element respPing = SoapUtil.getPayloadElement(responseEnvelope);
        Element respPayloadEl = XmlUtil.findFirstChildElement(respPing);
        String respText = XmlUtil.getTextValue(respPayloadEl);
        assertEquals(respText, payload);
    }

    private Document sendXml(Document reqEnvelope) throws MalformedURLException, GenericHttpException, SAXException {
        SimpleHttpClient httpClient = new SimpleHttpClient(new UrlConnectionHttpClient());
        URL url = new URL(bt.proxyUrl + bt.ssg0ProxyEndpoint);
        SimpleHttpClient.SimpleXmlResponse response = httpClient.postXml(new GenericHttpRequestParams(url), reqEnvelope);
        return response.getDocument();
    }

    @Test
    public void testBasicAuthPing() throws Exception {
        String payload = "ping 1 2 3";
        Document reqEnvelope = SsgFaker.makePingRequest(payload);

        bt.policyManager.setPolicy(new Policy(new HttpBasic(), "testpolicy"));
        URL url = new URL(bt.ssgUrl);
        bt.ssgFake.getRuntime().setPolicyManager(bt.policyManager);
        bt.ssgFake.setSsgAddress(url.getHost());
        bt.ssgFake.setSsgPort(url.getPort());
        bt.ssgFake.setSsgFile("/soap/ssg/basicauth");
        bt.ssgFake.setUsername("testuser");
        bt.ssgFake.getRuntime().setCachedPassword("testpassword".toCharArray());

        sendPing(payload, reqEnvelope);
    }

    @Test
    public void testSsgFault() throws Exception {
        String payload = "ping 1 2 3";
        Document reqEnvelope = SsgFaker.makePingRequest(payload);

        bt.policyManager.setPolicy(null);
        URL url = new URL(bt.ssgUrl);
        bt.ssgFake.getRuntime().setPolicyManager(bt.policyManager);
        bt.ssgFake.setSsgAddress(url.getHost());
        bt.ssgFake.setSsgPort(url.getPort());
        bt.ssgFake.setSsgFile("/soap/ssg/throwfault");

        Document resp = sendXml(reqEnvelope);
        assertTrue(SoapFaultUtils.gatherSoapFaultDetail(resp) != null);
    }
}
