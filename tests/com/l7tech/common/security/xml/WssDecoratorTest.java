/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class WssDecoratorTest extends TestCase {
    private static Logger log = Logger.getLogger(WssDecoratorTest.class.getName());

    public WssDecoratorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(WssDecoratorTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private static class Context {
        Document message;
        String soapNs;
        Element body;
        String payloadNs;
        Element payload;
        Element price;
        Element amount;
        Element productid;
        Element accountid;

        Context() throws IOException, SAXException {
            message = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
            soapNs = message.getDocumentElement().getNamespaceURI();
            body = (Element)message.getElementsByTagNameNS(soapNs, SoapUtil.BODY_EL_NAME).item(0);
            payload = XmlUtil.findFirstChildElement(body);
            payloadNs = payload.getNamespaceURI();
            price = (Element)message.getElementsByTagNameNS("", "price").item(0);
            amount = (Element)message.getElementsByTagNameNS("", "amount").item(0);
            productid = (Element)message.getElementsByTagNameNS("", "productid").item(0);
            accountid = (Element)message.getElementsByTagNameNS("", "accountid").item(0);
        }
    }

    public void testSimpleDecoration() throws Exception {
        Context c = new Context();
        WssDecorator decorator = new WssDecoratorImpl();

        log.info("Before decoration:" + XmlUtil.documentToFormattedString(c.message));

        decorator.decorateMessage(c.message,
                                  TestDocuments.getDotNetServerCertificate(),
                                  TestDocuments.getEttkClientCertificate(),
                                  TestDocuments.getEttkClientPrivateKey(),
                                  new Element[0],
                                  new Element[0]);

        log.info("Decorated message:" + XmlUtil.documentToFormattedString(c.message));
    }

    public void testWrappedSecurityHeader() throws Exception {
        Context c = new Context();
        WssDecorator decorator = new WssDecoratorImpl();

        Element sec = SoapUtil.getOrMakeSecurityElement(c.message);
        final String privUri = "http://example.com/ws/security/stuff";
        Element privateStuff = c.message.createElementNS(privUri, "privateStuff");
        privateStuff.setPrefix("priv");
        privateStuff.setAttribute("xmlns:priv", privUri);
        sec.appendChild(privateStuff);

        log.info("Before decoration:" + XmlUtil.documentToFormattedString(c.message));

        decorator.decorateMessage(c.message,
                                  TestDocuments.getDotNetServerCertificate(),
                                  TestDocuments.getEttkClientCertificate(),
                                  TestDocuments.getEttkClientPrivateKey(),
                                  new Element[0],
                                  new Element[0]);

        log.info("Decorated message:" + XmlUtil.documentToFormattedString(c.message));
    }

    public void testSingleEncryptionMultipleSignature() throws Exception {
        Context c = new Context();
        WssDecorator decorator = new WssDecoratorImpl();

        log.info("Before decoration:" + XmlUtil.documentToFormattedString(c.message));

        decorator.decorateMessage(c.message,
                                  TestDocuments.getDotNetServerCertificate(),
                                  TestDocuments.getEttkClientCertificate(),
                                  TestDocuments.getEttkClientPrivateKey(),
                                  new Element[] { c.productid,  c.accountid },
                                  new Element[] { c.body });

        log.info("Decorated message:" + XmlUtil.documentToFormattedString(c.message));
    }
}
