package com.l7tech.server.saml;

import com.l7tech.common.security.saml.Constants;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.WssDecorator;
import com.l7tech.common.security.xml.WssDecoratorImpl;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.XpathEvaluator;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import x0Protocol.oasisNamesTcSAML1.ResponseDocument;
import x0Protocol.oasisNamesTcSAML1.ResponseType;
import x0Protocol.oasisNamesTcSAML1.StatusCodeType;
import x0Protocol.oasisNamesTcSAML1.StatusType;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;

/**
 * Package private class that contains methods for creating responses, precanned responses
 * and utility methods that deal with saml responses.
 *
 * @author emil
 */
class Responses {

    /**
     * Create the SOAP message, inclding the SAML response, signing the document if the
     * <code>SignerInfo</code> is not null.
     *
     * @param rdoc the response document
     * @param si   the signer info
     * @return the SOAP message as a DOM document containing the saml response, optionally signed
     * @throws SOAPException                  on soap error
     * @throws IOException                    on io error
     * @throws SAXException                   on SAX parser error
     * @throws InvalidDocumentFormatException if document is invalid format
     * @throws GeneralSecurityException       thrown on security related error
     * @throws WssDecorator.DecoratorException
     *
     */
    static Document asSoapMessage(ResponseDocument rdoc, SignerInfo si)
      throws SOAPException, IOException, SAXException, InvalidDocumentFormatException,
      GeneralSecurityException, WssDecorator.DecoratorException {
        SOAPMessage sm = SoapUtil.makeMessage();
        SOAPBody body = sm.getSOAPPart().getEnvelope().getBody();

        final Document document = (Document)rdoc.newDomNode(Utilities.xmlOptions());
        final Element documentElement = document.getDocumentElement();
        SoapUtil.domToSOAPElement(body, documentElement);
        String strMsg = SoapUtil.soapMessageToString(sm, "UTF-8");
        Document domDocument = XmlUtil.stringToDocument(strMsg);

        if (si != null) {
            WssDecorator wssd = getWssDecorator();
            WssDecorator.DecorationRequirements wssRequirements = new WssDecorator.DecorationRequirements();
            wssRequirements.setSenderCertificate(si.getCertificateChain()[0]);
            wssRequirements.setSenderPrivateKey(si.getPrivate());
            try {
                final Map namespaces = XpathEvaluator.getNamespaces(sm);
                List list = (List)XpathEvaluator.newEvaluator(domDocument, namespaces).evaluate("//soapenv:Envelope/soapenv:Body/samlp:Response");
                if (list.isEmpty()) {
                    throw new InvalidDocumentFormatException("Could not find the samlp:Response :" + strMsg);
                }
                Element element = (Element)list.get(0);
                wssRequirements.getElementsToSign().add(element);
                wssd.decorateMessage(domDocument, wssRequirements);
            } catch (JaxenException e) {
                throw new SOAPException(e);
            }
        }

        return domDocument;
    }

    /**
     * Precanned response that indicates not implemented functionality. It
     * is a empty response with the status code 'Responder' (see saml1.1 core 3.4.3.1).
     *
     * @param detail message - optional message , on <b>null</b> the default
     *               "Not Implemented is returned"
     * @return the precanned 'not implemented' <code>ResponseDocument</code>
     */
    static ResponseDocument getNotImplementedResponse(String detail) {
        ResponseType notImplemented = ResponseType.Factory.newInstance(Utilities.xmlOptions());
        StatusType status = notImplemented.addNewStatus();
        StatusCodeType scode = status.addNewStatusCode();

        scode.setValue(new QName(Constants.NS_SAMLP, Constants.STATUS_RESPONDER));
        String msg = detail;
        if (msg == null) {
            msg = "Not implemented in this provider/version";
        }
        status.setStatusMessage(msg);
        ResponseDocument rdoc = ResponseDocument.Factory.newInstance();
        rdoc.setResponse(notImplemented);
        return rdoc;
    }


    /**
     * Precanned empty response that contains no assertions with the 'Success' status.
     * Used in scenarios such as SAML authority unable provide an assertion with any
     * statements satisfying the constraints expressed by a query (saml1.1 core 3.4.4).
     *
     * @param detail message
     * @return the precanned empty  <code>ResponseDocument</code>
     */
    static ResponseDocument getEmptySuccess(String detail) {
        ResponseType empty = ResponseType.Factory.newInstance(Utilities.xmlOptions());
        StatusType status = empty.addNewStatus();
        StatusCodeType scode = status.addNewStatusCode();
        scode.setValue(new QName(Constants.NS_SAMLP, Constants.STATUS_SUCCESS));
        if (detail != null) {
            status.setStatusMessage(detail);
        }
        ResponseDocument rdoc = ResponseDocument.Factory.newInstance();
        rdoc.setResponse(empty);
        return rdoc;
    }

    /**
     * Precanned bad request response that contains no assertions with the 'Success' status.
     * Used in scenarios such as bad message received.
     *
     * @param detail message
     * @return the precanned empty  <code>ResponseDocument</code>
     */
    static ResponseDocument getBadRequest(String detail) {
        ResponseType empty = ResponseType.Factory.newInstance(Utilities.xmlOptions());
        StatusType status = empty.addNewStatus();
        StatusCodeType scode = status.addNewStatusCode();
        scode.setValue(new QName(Constants.NS_SAMLP, Constants.STATUS_SUCCESS));
        if (detail != null) {
            status.setStatusMessage(detail);
        }
        ResponseDocument rdoc = ResponseDocument.Factory.newInstance();
        rdoc.setResponse(empty);
        return rdoc;
    }


    private static WssDecorator wssDecorator;

    private static synchronized WssDecorator getWssDecorator() {
        if (wssDecorator != null) return wssDecorator;
        return wssDecorator = new WssDecoratorImpl();
    }

    /**
     * cannot instantiate this class
     */
    private Responses() {
    }


}
