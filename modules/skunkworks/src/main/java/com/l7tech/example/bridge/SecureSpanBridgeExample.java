/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.example.bridge;

import com.l7tech.proxy.SecureSpanBridge;
import com.l7tech.proxy.SecureSpanBridgeFactory;
import com.l7tech.proxy.SecureSpanBridgeOptions;
import org.apache.xml.serialize.DOMSerializer;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.SerializerFactory;
import org.apache.xml.serialize.Method;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Example code that uses the SecureSpan Bridge API.
 */
public class SecureSpanBridgeExample {

    /** A "placeOrder" request message to the ACME Warehouse sample SOAP service. */
    private static final String PLACEORDER_MESSAGE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
      "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
      "    <soap:Body>\n" +
      "        <placeOrder xmlns=\"http://warehouse.acme.com/ws\">\n" +
      "            <productid>111111116</productid>\n" +
      "            <amount>1000</amount>\n" +
      "            <price>1230</price>\n" +
      "            <accountid>334</accountid>\n" +
      "        </placeOrder>\n" +
      "    </soap:Body>\n" +
      "</soap:Envelope>\n";


    /** The SOAPAction header corresponding to the above message, including surrounding quote marks. */
    private static final String PLACEORDER_SOAPACTION = "\"http://warehouse.acme.com/ws/placeOrder\"";

    /** A Layer 7 security policy that calls for HTTP Basic over SSL. */
    private static final String POLICY_XML_HTTPBASIC_SSL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:HttpBasic/>\n" +
            "        <L7p:SslAssertion/>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    private static void testStaticPolicy(String host, String username, char[] password) {
        try {
            System.out.println("Attempting request:  static policy");

            SecureSpanBridgeOptions options = new SecureSpanBridgeOptions(host, username, password);
            SecureSpanBridge bridge = SecureSpanBridgeFactory.createSecureSpanBridge(options);

            bridge.setStaticPolicy(POLICY_XML_HTTPBASIC_SSL);

            SecureSpanBridge.Result result = bridge.send(PLACEORDER_SOAPACTION, PLACEORDER_MESSAGE);
            System.out.println("Got back HTTP status " + result.getHttpStatus());
            System.out.println("Got back envelope:\n");
            documentToStream(result.getResponse(), System.out);

            System.out.println("\nRequest succeeded.");
        } catch (Exception e) {
            System.out.println("Request failed: " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }

    private static void testAutomaticCerts(String host, String username, char[] password) {
        try {
            System.out.println("Attempting request: automatic server certificate discovery / client certificate signing request");

            SecureSpanBridgeOptions options = new SecureSpanBridgeOptions(host, username, password);
            SecureSpanBridge bridge = SecureSpanBridgeFactory.createSecureSpanBridge(options);

            // Force server cert discovery, and the client certificate signing request, to be attempted
            // early so we'll know we're ready to go
            bridge.ensureCertificatesAreAvailable();

            X509Certificate cert = bridge.getClientCert();
            System.out.println("Our Bridge client certificate: " + cert);

            PrivateKey key = bridge.getClientCertPrivateKey();
            System.out.println("Our Bridge private key: " + key);

            X509Certificate ssgCert = bridge.getServerCert();
            System.out.println("The Gateway's SSL certificate: " + ssgCert);

            // Send a request to the Gateway through the Bridge.
            // The Bridge will automatically discover and cache service policies as indicated by the Gateway.
            SecureSpanBridge.Result result = bridge.send(PLACEORDER_SOAPACTION, PLACEORDER_MESSAGE);
            System.out.println("Got back HTTP status " + result.getHttpStatus());
            System.out.println("Got back envelope:\n");
            documentToStream(result.getResponse(), System.out);

            System.out.println("\nRequest succeeded.");
        } catch (Exception e) {
            System.out.println("Request failed: " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }

    /** Serialize the specified DOM Node to the specified OutputStream. */
    private static void documentToStream(Document doc, OutputStream out) throws IOException {
        OutputFormat of = new OutputFormat();
        of.setIndenting(true);
        of.setOmitXMLDeclaration(true);
        SerializerFactory sfac = SerializerFactory.getSerializerFactory(Method.XML);
        DOMSerializer ser = sfac.makeSerializer(out, of).asDOMSerializer();
        ser.serialize(doc);
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: testbridge gatewayhost username password");
            System.exit(1);
        }
        String host = args[0];
        String username = args[1];
        char[] password = args[2].toCharArray();

        testAutomaticCerts(host, username, password);

        testStaticPolicy(host, username, password);
    }
}
