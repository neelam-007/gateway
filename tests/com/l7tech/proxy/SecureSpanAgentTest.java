/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.HexUtils;
import org.apache.log4j.Category;

import java.security.cert.X509Certificate;
import java.security.PrivateKey;

/**
 * Test the API version of the Agent.
 * @author mike
 * @version 1.0
 */
public class SecureSpanAgentTest {
    private static final Category log = Category.getInstance(SecureSpanAgentTest.class);
    private static final String PLACEORDER_SOAPACTION = "\"http://warehouse.acme.com/ws/placeOrder\"";
    private static final String PLACEORDER_MESSAGE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"+
            "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"+
            "    <soap:Body>\n"+
            "        <placeOrder xmlns=\"http://warehouse.acme.com/ws\">\n"+
            "            <productid>224011405</productid>\n"+
            "            <amount>1000</amount>\n"+
            "            <price>1230</price>\n"+
            "            <accountid>334</accountid>\n"+
            "        </placeOrder>\n"+
            "    </soap:Body>\n"+
            "</soap:Envelope>\n";
    private static final String SAML4 = "com/l7tech/common/security/saml/saml4.xml";

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: testagent gatewayhost username password");
            System.exit(1);
        }
        int i = 0;
        String host = args[i++];
        String username = args[i++];
        char[] password = args[i++].toCharArray();

        SecureSpanAgentOptions options = new SecureSpanAgentOptions(host, username, password);
        SecureSpanAgent agent = SecureSpanAgentFactory.createSecureSpanAgent(options);
        agent.ensureCertificatesAreAvailable();

        X509Certificate cert = agent.getClientCert();
        log.info("Our client certificate: " + cert);

        PrivateKey key = agent.getClientCertPrivateKey();
        log.info("Our private key: " + key);

        X509Certificate ssgCert = agent.getServerCert();
        log.info("The Gateway's CA certificate: " + ssgCert);

        String soapaction = PLACEORDER_SOAPACTION;
        String message = PLACEORDER_MESSAGE;
        //HexUtils.Slurpage slurpage = HexUtils.slurpUrl(SecureSpanAgentTest.class.getClassLoader().getResource(SAML4));
        //String message = new String(slurpage.bytes);
        SecureSpanAgent.Result result = agent.send(soapaction, message);
        log.info("Got back http status " + result.getHttpStatus());
        log.info("Got back envelope:\n" + XmlUtil.documentToString(result.getResponse()));
    }
}
