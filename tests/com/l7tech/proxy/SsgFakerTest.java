package com.l7tech.proxy;

import com.l7tech.common.http.GenericHttpException;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import org.junit.After;
import static org.junit.Assert.assertEquals;
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
 * Test the SsgFaker.
 */
public class SsgFakerTest {
    protected static final Logger logger = Logger.getLogger(SsgFakerTest.class.getName());
    private SsgFaker ssgFaker;
    private String ssgUrl;

    @Before
    protected void setUp() throws Exception {
        ResourceUtils.closeQuietly(ssgFaker);
        ssgFaker = new SsgFaker();
        ssgUrl = ssgFaker.start();
    }

    @After
    protected void tearDown() {
        ResourceUtils.closeQuietly(ssgFaker);
    }

    private void sendPing(String payload, Document reqEnvelope) throws SAXException, IOException, InvalidDocumentFormatException {
        Document responseEnvelope = sendXml(reqEnvelope);

        logger.info("Client:  I Sent: " + XmlUtil.nodeToFormattedString(reqEnvelope));
        logger.info("Client:  I Got back: " + XmlUtil.nodeToFormattedString(responseEnvelope));

        Element respPing = SoapUtil.getPayloadElement(responseEnvelope);
        Element respPayloadEl = XmlUtil.findFirstChildElement(respPing);
        String respText = XmlUtil.getTextValue(respPayloadEl);
        assertEquals(respText, payload);
    }

    private Document sendXml(Document reqEnvelope) throws MalformedURLException, GenericHttpException, SAXException {
        SimpleHttpClient httpClient = new SimpleHttpClient(new UrlConnectionHttpClient());
        URL url = new URL(ssgUrl);
        SimpleHttpClient.SimpleXmlResponse response = httpClient.postXml(new GenericHttpRequestParams(url), reqEnvelope);
        return response.getDocument();
    }

    @Test
    public void testSsgFaker() throws Exception {
        String payload = "ping 1 2 3";
        sendPing(payload, SsgFaker.makePingRequest(payload));
    }
}
