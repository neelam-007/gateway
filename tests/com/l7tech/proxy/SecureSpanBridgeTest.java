/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.message.PolicyApplicationContext;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * Test the API version of the Bridge.
 *
 * @author mike
 * @version 1.0
 */
public class SecureSpanBridgeTest {
    private static final Logger log = Logger.getLogger(SecureSpanBridgeTest.class.getName());
    private static final String PLACEORDER_SOAPACTION = "\"http://warehouse.acme.com/ws/placeOrder\"";
    private static final String PLACEORDER_MESSAGE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
      "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
      "    <soap:Body>\n" +
      "        <placeOrder xmlns=\"http://warehouse.acme.com/ws\">\n" +
      "            <productid>224011405</productid>\n" +
      "            <amount>1000</amount>\n" +
      "            <price>1230</price>\n" +
      "            <accountid>334</accountid><blah/><foo/><blatch>asdfasdfdsf</blatch>\n" +
      "        </placeOrder>\n" +
      "    </soap:Body>\n" +
      "</soap:Envelope>\n";
    private static final String SAML4 = "com/l7tech/common/security/saml/saml4.xml";

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: testbridge gatewayhost username password");
            System.exit(1);
        }

        System.setProperty(SecureSpanBridgeFactory.PROPERTY_MESSAGE_INTERCEPTOR, MyInterceptor.class.getName());

        int i = 0;
        String host = args[i++];
        String username = args[i++];
        char[] password = args[i++].toCharArray();

        SecureSpanBridgeOptions options = new SecureSpanBridgeOptions(host, username, password);
        SecureSpanBridge bridge = SecureSpanBridgeFactory.createSecureSpanBridge(options);
        bridge.ensureCertificatesAreAvailable();

        X509Certificate cert = bridge.getClientCert();
        log.info("Our client certificate: " + cert);

        PrivateKey key = bridge.getClientCertPrivateKey();
        log.info("Our private key: " + key);

        X509Certificate ssgCert = bridge.getServerCert();
        log.info("The Gateway's CA certificate: " + ssgCert);

        String soapaction = PLACEORDER_SOAPACTION;
        String message = PLACEORDER_MESSAGE;
        //HexUtils.Slurpage slurpage = HexUtils.slurpUrl(SecureSpanBridgeTest.class.getClassLoader().getResource(SAML4));
        //String message = new String(slurpage.bytes);
        SecureSpanBridge.Result result = bridge.send(soapaction, message);
        log.info("Got back http status " + result.getHttpStatus());
        log.info("Got back envelope:\n" + XmlUtil.nodeToString(result.getResponse()));

        // Test static policy
        Assertion staticAss = new AllAssertion(Arrays.asList(new Assertion[] {
            new HttpBasic(),
            new TrueAssertion(),
        }));
        bridge.setStaticPolicy(WspWriter.getPolicyXml(staticAss));
        result = bridge.send(soapaction, message);
        log.info("Got back http status " + result.getHttpStatus());
        log.info("Got back envelope:\n" + XmlUtil.nodeToString(result.getResponse()));
    }

    public static class MyInterceptor implements RequestInterceptor {
        public void onFrontEndRequest(PolicyApplicationContext context) {
        }

        public void onFrontEndReply(PolicyApplicationContext context) {
        }

        public void onBackEndRequest(PolicyApplicationContext context, List<HttpHeader> headersSent) {
            try {
                log.info("\n\n\n\n*******Sending request: " + new String(context.getRequest().getMimeKnob().getFirstPart().getBytesIfAlreadyAvailable()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void onBackEndReply(PolicyApplicationContext context) {
        }

        public void onMessageError(Throwable t) {
        }

        public void onReplyError(Throwable t) {
        }

        public void onPolicyUpdated(Ssg ssg, PolicyAttachmentKey binding, Policy policy) {
        }

        public void onPolicyError(Ssg ssg, PolicyAttachmentKey binding, Throwable error) {
        }
    }
}
