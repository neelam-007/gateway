package com.l7tech.common.xml;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;

import com.l7tech.common.util.XmlUtil;

/**
 * The class is a container for test documents, SOAP tmessages etc
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public final class TestDocuments {
    private static final String DIR = "com/l7tech/service/resources/";
    private static final String PDIR = "com/l7tech/policy/resources/";
    public static final String TEST_SOAP_XML = DIR + "GetLastTradePriceSoapRequest.xml";
    public static final String WSDL2PORTS = DIR + "xmltoday-delayed-quotes-2ports.wsdl";
    public static final String WSDL = DIR + "StockQuoteService.wsdl";
    public static final String WSDL_DOC_LITERAL = DIR + "GeoServe.wsdl";
    public static final String WSDL_DOC_LITERAL2 = DIR + "QuoteService.wsdl";
    public static final String WSDL_DOC_LITERAL3 = DIR + "DeadOrAlive.wsdl";
    public static final String WSDL2SERVICES = DIR + "xmltoday-delayed-quotes-2services.wsdl";
    public static final String PLACEORDER_CLEARTEXT = DIR + "PlaceOrder_cleartext.xml";
    public static final String PLACEORDER_WITH_MAJESTY = DIR + "PlaceOrder_with_XmlRequestSecurity.xml";
    public static final String PLACEORDER_KEYS = DIR + "PlaceOrder_with_XmlRequestSecurity_keys.properties";
    public static final String BUG_763_MONSTER_POLICY = PDIR + "bug763MonsterPolicy.xml";
    public static final String DOTNET_SIGNED_REQUEST = DIR + "dotNetSignedSoapRequest.xml";
    public static final String DOTNET_SIGNED_TAMPERED_REQUEST = DIR + "dotNetSignedTamperedSoapRequest.xml";
    public static final String DOTNET_ENCRYPTED_REQUEST = DIR + "dotNetSignedAndEncryptedRequest.xml";
    public static final String DOTNET_SIGNED_USING_DERIVED_KEY_TOKEN = DIR + "dotNetSignedUsingDerivedKeyToken.xml";
    public static final String DOTNET_ENCRYPTED_USING_DERIVED_KEY_TOKEN = DIR + "dotNetSignedAndEncryptedUsingDKFromSCT.xml";
    public static final String SSL_KS = DIR + "rikerssl.ks";
    public static final String SSL_CER = DIR + "rikerssl.cer";

    private TestDocuments() { }

    public static Document getTestDocument(String resourcetoread)
      throws IOException, SAXException {
        InputStream i = getInputStream(resourcetoread);
        return XmlUtil.parse(i);
    }

    public static InputStream getInputStream(String resourcetoread) throws FileNotFoundException {
        if (resourcetoread == null) {
            resourcetoread = TestDocuments.TEST_SOAP_XML;
        }
        ClassLoader cl = TestDocuments.class.getClassLoader();
        InputStream i = cl.getResourceAsStream(resourcetoread);
        if (i == null) {
            throw new FileNotFoundException(resourcetoread);
        }
        return i;
    }
}
