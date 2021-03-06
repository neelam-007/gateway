/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks;

//import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.test.BenchmarkRunner;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.util.logging.Logger;

/**
 * Test DOM parsing cost vs. cloning cost
 */
public class DomPerfTest {
    private static final Logger logger = Logger.getLogger(DomPerfTest.class.getName());

    public static void main(String[] args) throws Exception {
        final Document origDoc = XmlUtil.stringAsDocument( DOC );
        final String origXml = XmlUtil.nodeToString(origDoc);
        logger.info("Test document size = " + origXml.length());
        
        Runnable testClone = new Runnable() {
            public void run() {
                Document newDoc = (Document)origDoc.cloneNode(true);
            }
        };
        BenchmarkRunner benchClone = new BenchmarkRunner(testClone, 20000, "testClone");
        benchClone.setThreadCount(1);
        benchClone.run();

        Runnable testParse = new Runnable() {
            public void run() {
                try {
                    Document newDoc = XmlUtil.stringToDocument(origXml);
                } catch (SAXException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        BenchmarkRunner benchParse = new BenchmarkRunner(testParse, 20000, "testParse");
        benchParse.setThreadCount(1);
        benchParse.run();
    }

    private static final String DOC = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<soap:Envelope  xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/03/addressing\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"><soap:Header><wsa:Action wsu:Id=\"Id-9638da50-397b-400d-986a-4a6bd1638873\">http://warehouse.acme.com/ws/listProducts</wsa:Action><wsa:MessageID wsu:Id=\"Id-298f96cb-ba73-4476-9d67-852d2d125db2\">uuid:8c7dac90-96bd-4313-bfab-c4cf7ff91420</wsa:MessageID><wsa:ReplyTo wsu:Id=\"Id-975e0b00-f762-4292-a36e-ea9ab28a326c\"><wsa:Address>http://schemas.xmlsoap.org/ws/2004/03/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:To wsu:Id=\"Id-2ca284eb-03be-4387-a0a9-9745acfd9992\">http://riker:8888/ACMEWarehouseWS/Service1.asmx</wsa:To><wsse:Security soap:mustUnderstand=\"1\"><wsu:Timestamp wsu:Id=\"Timestamp-e70f7ab2-8d58-4a25-b5cf-349ab666ca4b\"><wsu:Created>2004-06-15T20:51:27Z</wsu:Created><wsu:Expires>2004-06-15T20:56:27Z</wsu:Expires></wsu:Timestamp><wsse:BinarySecurityToken ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\" EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" wsu:Id=\"SecurityToken-0791226c-f64b-4ccd-bfd5-2a8706fceecc\">MIIBxDCCAW6gAwIBAgIQxUSXFzWJYYtOZnmmuOMKkjANBgkqhkiG9w0BAQQFADAWMRQwEgYDVQQDEwtSb290IEFnZW5jeTAeFw0wMzA3MDgxODQ3NTlaFw0zOTEyMzEyMzU5NTlaMB8xHTAbBgNVBAMTFFdTRTJRdWlja1N0YXJ0Q2xpZW50MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC+L6aB9x928noY4+0QBsXnxkQE4quJl7c3PUPdVu7k9A02hRG481XIfWhrDY5i7OEB7KGW7qFJotLLeMec/UkKUwCgv3VvJrs2nE9xO3SSWIdNzADukYh+Cxt+FUU6tUkDeqg7dqwivOXhuOTRyOI3HqbWTbumaLdc8jufz2LhaQIDAQABo0swSTBHBgNVHQEEQDA+gBAS5AktBh0dTwCNYSHcFmRjoRgwFjEUMBIGA1UEAxMLUm9vdCBBZ2VuY3mCEAY3bACqAGSKEc+41KpcNfQwDQYJKoZIhvcNAQEEBQADQQAfIbnMPVYkNNfX1tG1F+qfLhHwJdfDUZuPyRPucWF5qkh6sSdWVBY5sT/txBnVJGziyO8DPYdu2fPMER8ajJfl</wsse:BinarySecurityToken><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" /><SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\" /><Reference URI=\"#Id-9638da50-397b-400d-986a-4a6bd1638873\"><Transforms><Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" /></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>YwMIXiIguuR4YDgEQ/AKy+UkIZU=</DigestValue></Reference><Reference URI=\"#Id-298f96cb-ba73-4476-9d67-852d2d125db2\"><Transforms><Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" /></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>B3Eunghwy6JUOB5qaujYJsQlyak=</DigestValue></Reference><Reference URI=\"#Id-975e0b00-f762-4292-a36e-ea9ab28a326c\"><Transforms><Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" /></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>iKp+WhBzfyFB31XL1oDkLjY0vO0=</DigestValue></Reference><Reference URI=\"#Id-2ca284eb-03be-4387-a0a9-9745acfd9992\"><Transforms><Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" /></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>DiYiTvNUWQ9z/py45L14uFF1ZeY=</DigestValue></Reference><Reference URI=\"#Timestamp-e70f7ab2-8d58-4a25-b5cf-349ab666ca4b\"><Transforms><Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" /></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>3Md95ql80DzfKBCQpOjtoKHSEDU=</DigestValue></Reference><Reference URI=\"#Id-3a85394b-e6dc-469d-aa5a-dba4d787c035\"><Transforms><Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" /></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>nf2gLmqWGE565FzV4WrM5fGjnYU=</DigestValue></Reference></SignedInfo><SignatureValue>NEpFkTel0OTDcGTTDVhSDtlZg2K1vmVJew1mdNqXPbgzdAKzMIfj5E/B4VYwunnLZ7VW7v7c1oHuLs6mxim2zE5g3urDHy+3PRdnC+RooLh3hxE21292QqVLHwfb5lMB1CchRNZI5sPS09EioOHZ1VBl3BeOPXBIV7Ak3TmZm1o=</SignatureValue><KeyInfo><wsse:SecurityTokenReference><wsse:Reference URI=\"#SecurityToken-0791226c-f64b-4ccd-bfd5-2a8706fceecc\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\" /></wsse:SecurityTokenReference></KeyInfo></Signature></wsse:Security></soap:Header><soap:Body wsu:Id=\"Id-3a85394b-e6dc-469d-aa5a-dba4d787c035\"><listProducts xmlns=\"http://warehouse.acme.com/ws\" /></soap:Body></soap:Envelope>";
}
