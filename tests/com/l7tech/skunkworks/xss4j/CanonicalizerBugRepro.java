/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.xss4j;

import com.ibm.xml.dsig.Canonicalizer;
import com.ibm.xml.dsig.transform.ExclusiveC11r;
import com.ibm.xml.dsig.transform.ExclusiveC11rWC;
import com.ibm.xml.dsig.transform.W3CCanonicalizer;
import com.ibm.xml.dsig.transform.W3CCanonicalizer2WC;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.ByteArrayInputStream;

/**
 * Test case demonstrating that both xs4j "exclusive" canonicalizers fail for unknown reasons
 * when canonicalizing the "productid" element in our placeorder_cleartext sample message.
 * Some canonicalizer (ExclusiveC11r and ExclusiveC11rWC) throw an exception when canonicalize
 * certain elements.
 */
public class CanonicalizerBugRepro {
    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String APP_NS = "http://warehouse.acme.com/ws";
    public static final String TEST_DOC = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope\n" +
            "    xmlns:soapenv=\"" + SOAP_NS + "\"\n" +
            "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "    <soapenv:Body>\n" +
            "        <ns1:placeOrder\n" +
            "            soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:ns1=\"" + APP_NS + "\">\n" +
            "            <productid xsi:type=\"xsd:long\">-9206260647417300294</productid>\n" +
            "            <amount xsi:type=\"xsd:long\">1</amount>\n" +
            "            <price xsi:type=\"xsd:float\">5.0</price>\n" +
            "            <accountid xsi:type=\"xsd:long\">228</accountid>\n" +
            "        </ns1:placeOrder>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    public static void main(String[] args) {
        Canonicalizer[] c = new Canonicalizer[] {
            new W3CCanonicalizer(),
            new W3CCanonicalizer2WC(),
            new ExclusiveC11r(),
            new ExclusiveC11rWC(),
        };

        for (int i = 0; i < c.length; i++) {
            Canonicalizer canonicalizer = c[i];
            try {
                System.out.println("\n*** Testing: " + canonicalizer.getClass() + "\n");
                useCanon(canonicalizer);
                System.out.println("\n\n*** Test succeeded: " + canonicalizer.getClass() + "\n");
            } catch (Exception e) {
                System.out.println("\n");
                e.printStackTrace(System.out);
                System.out.println("\n*** Test failed: " + canonicalizer.getClass() + "\n");
            }
        }
    }

    private static void useCanon(Canonicalizer canon) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        ByteArrayInputStream bais = new ByteArrayInputStream(TEST_DOC.getBytes());
        Document message = builder.parse(bais);
        assertNotNull(message);
        Element body = (Element)message.getElementsByTagNameNS(SOAP_NS, "Body").item(0);
        assertNotNull(body);
        Element placeOrder = (Element)message.getElementsByTagNameNS(APP_NS, "placeOrder").item(0);
        assertNotNull(placeOrder);
        Element productid = (Element)message.getElementsByTagName("productid").item(0);
        assertNotNull(productid);

        canon.canonicalize(message, System.out);
        canon.canonicalize(body, System.out);
        canon.canonicalize(placeOrder, System.out);
        canon.canonicalize(productid, System.out);  // NPE here if using exclusive c11r

        System.out.println("Node serialized ok.");
    }

    private static void assertNotNull(Object o) {
        if (o == null) throw new NullPointerException("assertion failed");
    }
}
