package com.l7tech.server.saml;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MessageNotSoapException;
import org.apache.xmlbeans.XmlException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import x0Protocol.oasisNamesTcSAML1.ResponseDocument;

import javax.xml.soap.*;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author emil
 * @version 5-Aug-2004
 */
class SoapResponseGenerator {
    private static final Logger logger = Logger.getLogger(SoapResponseGenerator.class.getName());

    private MessageFactory msgFactory;

    SoapResponseGenerator() {
        try {
            msgFactory = MessageFactory.newInstance();
        } catch (SOAPException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the response document from the SOAP inputstream
     *
     * @param in the soap input stream
     * @return the response document
     * @throws XmlException
     * @throws InvalidDocumentFormatException
     * @throws IOException
     * @throws SAXException
     */
    ResponseDocument fromSoapInputStream(InputStream in)
      throws XmlException, InvalidDocumentFormatException, IOException, SAXException {
        if (in == null) {
            throw new IllegalArgumentException();
        }
        return fromSoapMessage(XmlUtil.parse(in));
    }


    /**
     * Locate the saml reponse in the SOAP message <code>Document</code>.
     *
     * @param doc the document
     * @return the response document
     * @throws XmlException                   on parsing error
     * @throws InvalidDocumentFormatException on invalid document format
     */
    ResponseDocument fromSoapMessage(Document doc)
      throws XmlException, InvalidDocumentFormatException {
        if (doc == null) {
            throw new IllegalArgumentException();
        }
        Element bodyElement = SoapUtil.getBodyElement(doc);
        if (bodyElement == null) {
            throw new MessageNotSoapException();
        }
        Node firstChild = bodyElement.getFirstChild();
        if (firstChild == null) {
            throw new InvalidDocumentFormatException("Empty Body element");
        }
        return ResponseDocument.Factory.parse(firstChild);
    }

    Document streamSoapMessage(ResponseDocument doc)
      throws SOAPException, IOException, SAXException {
        return SamlUtilities.asDomSoapMessage(doc);
    }

    public void streamSoapMessage(ResponseDocument response, ServletOutputStream outputStream)
      throws IOException, SAXException, SOAPException {
        final Document document = SamlUtilities.asDomSoapMessage(response);

        XmlUtil.nodeToOutputStream(document, outputStream);
    }

    /**
     * Stream soap fault code into the output stream for a given exception.
     *
     * @param e  the exception
     * @param os the outputr stream
     */
    void streamFault(String faultString, Exception e, OutputStream os) {
        logger.log(Level.WARNING, "Returning SOAP fault " + faultString, e);
        try {
            String fault = SoapFaultUtils.generateRawSoapFault(SoapFaultUtils.FC_SERVER, faultString, e.getMessage(), "");
            os.write(fault.getBytes());
        } catch (IOException se) {
            se.printStackTrace();
            throw new RuntimeException(se);
        } catch (SAXException se) {
            e.printStackTrace();
            throw new RuntimeException(se);
        }
    }
}
