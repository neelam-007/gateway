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
public class TestDocuments {
    public static final String TEST_SOAP_XML = "com/l7tech/service/resources/GetLastTradePriceSoapRequest.xml";
    public static final String WSDL2PORTS = "com/l7tech/service/resources/xmltoday-delayed-quotes-2ports.wsdl";
    public static final String WSDL = "com/l7tech/service/resources/StockQuoteService.wsdl";
    public static final String WSDL_DOC_LITERAL = "com/l7tech/service/resources/GeoServe.wsdl";
    public static final String WSDL_DOC_LITERAL2 = "com/l7tech/service/resources/QuoteService.wsdl";
    public static final String WSDL2SERVICES = "com/l7tech/service/resources/xmltoday-delayed-quotes-2services.wsdl";

    public Document getTestDocument(String resourcetoread)
      throws IOException, SAXException {
        InputStream i = getInputStream(resourcetoread);
        return XmlUtil.parse(i);
    }

    public InputStream getInputStream(String resourcetoread) throws FileNotFoundException {
        if (resourcetoread == null) {
            resourcetoread = TestDocuments.TEST_SOAP_XML;
        }
        ClassLoader cl = getClass().getClassLoader();
        InputStream i = cl.getResourceAsStream(resourcetoread);
        if (i == null) {
            throw new FileNotFoundException(resourcetoread);
        }
        return i;
    }
}
