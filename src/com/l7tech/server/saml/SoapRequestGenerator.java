package com.l7tech.server.saml;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MessageNotSoapException;
import org.apache.xmlbeans.XmlException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import x0Protocol.oasisNamesTcSAML1.RequestDocument;

import javax.xml.soap.SOAPException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Class with support methods that deal with creting saml requests as SOAP messages.
 *
 * @author emil
 */
class SoapRequestGenerator {

    SoapRequestGenerator() {
    }

    RequestDocument fromSoapInputStream(InputStream in)
      throws XmlException, InvalidDocumentFormatException, IOException, SAXException {
        if (in == null) {
            throw new IllegalArgumentException();
        }
        return fromSoapMessage(XmlUtil.parse(in));
    }

    /**
     * Locate the saml request in the SOAP message <code>Document</code>.
     *
     * @param doc the document
     * @return the requerst document
     * @throws XmlException                   on parsing error
     * @throws InvalidDocumentFormatException on invalid document format
     */
    RequestDocument fromSoapMessage(Document doc)
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
        return RequestDocument.Factory.parse(firstChild);
    }

    /**
     * Create the soap message for the
     *
     * @param document
     * @return
     * @throws SOAPException
     */
    Document asSoapMessage(RequestDocument document)
      throws SOAPException, IOException, SAXException {
        return SamlUtilities.asDomSoapMessage(document);
    }
}
