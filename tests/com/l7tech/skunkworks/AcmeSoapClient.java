/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.Key;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.w3c.dom.Document;
import com.l7tech.util.XmlUtil;
import com.l7tech.common.security.xml.XmlMangler;
import com.l7tech.common.security.AesKey;

/**
 *
 * User: mike
 * Date: Sep 2, 2003
 * Time: 11:11:12 AM
 */
public class AcmeSoapClient extends TestCase {
    private static Logger log = Logger.getLogger(AcmeSoapClient.class.getName());
    public static final String TEST_URL = "http://192.168.1.118/ACMEWarehouseWS/Service1.asmx";

    public AcmeSoapClient(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(AcmeSoapClient.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private static AesKey generateKey() throws NoSuchAlgorithmException {
        SecureRandom rand = SecureRandom.getInstance("SHA1PRNG");
        byte[] keyBytes = new byte[32];
        rand.nextBytes(keyBytes);
        return new AesKey(keyBytes);
    }

    public static final String SIMPLE_REQ =
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"" +
                        " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                        "    <soap:Body>\n" +
                        "        <getProductDetails xmlns=\"http://warehouse.acme.com/ws\">\n" +
                        "            <productid>812673109</productid>\n" +
                        "        </getProductDetails>\n" +
                        "    </soap:Body>\n" +
                        "</soap:Envelope>\n";

    public void testSimpleRequest() throws Exception {
        HttpClient client = new HttpClient();
        PostMethod pm = new PostMethod(TEST_URL);
        Document req = XmlUtil.stringToDocument(SIMPLE_REQ);
        pm.setRequestBody(XmlUtil.documentToString(req));
        log.info("Sending request: " + pm.getRequestBodyAsString());
        pm.addRequestHeader("SOAPAction", "\"http://warehouse.acme.com/ws/getProductDetails\"");
        pm.addRequestHeader("Content-Type", "text/xml");
        int result = client.executeMethod(pm);
        log.info("Post completed with status " + result);
        log.info("Got response: " + pm.getResponseBodyAsString());
    }

    public void testDecryptedRequest() throws Exception {
        HttpClient client = new HttpClient();
        PostMethod pm = new PostMethod(TEST_URL);
        Document req = XmlUtil.stringToDocument(SIMPLE_REQ);
        Key key = generateKey();
        XmlMangler.encryptXml(req, key.getEncoded(), "mine");
        Document creq = XmlUtil.stringToDocument(XmlUtil.documentToString(req));
        XmlMangler.decryptXml(creq, key);

        pm.setRequestBody(XmlUtil.documentToString(creq));
        log.info("Sending request: " + pm.getRequestBodyAsString());
        pm.addRequestHeader("SOAPAction", "\"http://warehouse.acme.com/ws/getProductDetails\"");
        pm.addRequestHeader("Content-Type", "text/xml");
        int result = client.executeMethod(pm);
        log.info("Post completed with status " + result);
        log.info("Got response: " + pm.getResponseBodyAsString());
    }

}
