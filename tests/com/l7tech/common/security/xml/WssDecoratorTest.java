/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;
import java.security.cert.X509Certificate;
import java.security.PrivateKey;

import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.SoapUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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

    public void testSimpleDecoration() throws Exception {
        WssDecorator wssDecorator = new WssDecoratorImpl();

        Document soapMsg = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);

        log.info("Before decoration:" + XmlUtil.documentToString(soapMsg));

        X509Certificate serverCert = TestDocuments.getEttkServerCertificate();
        //PrivateKey serverKey = TestDocuments.getEttkServerPrivateKey();
        X509Certificate clientCert = TestDocuments.getEttkClientCertificate();
        PrivateKey clientKey = TestDocuments.getEttkClientPrivateKey();

        String soapNs = soapMsg.getDocumentElement().getNamespaceURI();
        Element body = (Element)soapMsg.getElementsByTagNameNS(soapNs, SoapUtil.BODY_EL_NAME).item(0);
        Element payload = XmlUtil.findFirstChildElement(body);
        String payloadNs = payload.getNamespaceURI();
        Element price = (Element)soapMsg.getElementsByTagNameNS(payloadNs, "price").item(0);
        Element amount = (Element)soapMsg.getElementsByTagNameNS(payloadNs, "amount").item(0);
        Element productid = (Element)soapMsg.getElementsByTagNameNS(payloadNs, "productid").item(0);
        Element accountid = (Element)soapMsg.getElementsByTagNameNS(payloadNs, "accountid").item(0);

        Element[] tocrypt = {
            productid,
            accountid
        };

        Element[] tosign = {
            body
        };

        Document decoratedMsg = wssDecorator.decorateMessage(soapMsg,
                                                             serverCert,
                                                             clientCert,
                                                             clientKey,
                                                             tocrypt,
                                                             tosign);

        log.info("Decorated message:" + XmlUtil.documentToString(decoratedMsg));


    }
}
