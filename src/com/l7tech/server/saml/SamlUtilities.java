package com.l7tech.server.saml;

import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.saml.InvalidAssertionException;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import x0Protocol.oasisNamesTcSAML1.ResponseDocument;
import x0Protocol.oasisNamesTcSAML1.ResponseType;
import x0Protocol.oasisNamesTcSAML1.StatusCodeType;
import x0Protocol.oasisNamesTcSAML1.StatusType;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPBody;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.text.DateFormat;

import x0Assertion.oasisNamesTcSAML1.AssertionType;
import x0Assertion.oasisNamesTcSAML1.ConditionsType;

/**
 * Public class with saml protocol utility methods.
 * @author emil
 * @version 5-Aug-2004
 */
public class SamlUtilities {
    private static final Logger logger = Logger.getLogger(SamlUtilities.class.getName());

    /**
     * Tests if the response document has a success status.
     *
     * @param rdoc the response document
     * @return whether the response document has a SAML status code "samlp:Success"
     * @throws IllegalArgumentException if the response is malformed
     */
    static boolean isSuccess(ResponseDocument rdoc) throws IllegalArgumentException {
        ResponseType rt = rdoc.getResponse();
        if (rt == null) {
            throw new IllegalArgumentException();
        }

        StatusType status = rt.getStatus();
        if (status == null) {
            throw new IllegalArgumentException();
        }
        StatusCodeType scode = status.getStatusCode();
        if (scode == null) {
            throw new IllegalArgumentException();
        }

        QName qn = scode.getValue();
        return SamlConstants.STATUS_SUCCESS.equals(qn.getLocalPart());
    }

    public static XmlOptions xmlOptions() {
        XmlOptions options = new XmlOptions();
        Map prefixes = new HashMap();
        prefixes.put(SamlConstants.NS_SAMLP, SamlConstants.NS_SAMLP_PREFIX);
        prefixes.put(SamlConstants.NS_SAML, SamlConstants.NS_SAML_PREFIX);
        options.setSaveSuggestedPrefixes(prefixes);
        return options;
    }

    static SOAPMessage asSoapMessage(XmlObject doc) throws SOAPException {
        SOAPMessage sm = SoapUtil.makeMessage();
        SOAPBody body = sm.getSOAPPart().getEnvelope().getBody();

        final Document document = (Document)doc.newDomNode(SamlUtilities.xmlOptions());
        final Element documentElement = document.getDocumentElement();

        SoapUtil.domToSOAPElement(body, documentElement);

        return sm;
    }

    static Document asDomSoapMessage(XmlObject doc) throws SOAPException, IOException, SAXException {
        SOAPMessage sm = asSoapMessage(doc);
        String strMsg = SoapUtil.soapMessageToString(sm, "UTF-8");
        Document domDocument = XmlUtil.stringToDocument(strMsg);

        return domDocument;
    }

    public static boolean validateIntervalConditions(AssertionType at)
      throws InvalidAssertionException
    {
        checkNonNullAssertionElement("Assertion", at);
        Calendar now = Calendar.getInstance();
        now.setTimeZone(TimeZone.getTimeZone("GMT")); // spec says UTC, that is GMT for our purpose
        now.setTime(new Date());
        ConditionsType type = at.getConditions();
        checkNonNullAssertionElement("Conditions", type);
        Calendar notBefore = type.getNotBefore();
        checkNonNullAssertionElement("Not Before", notBefore);
        Calendar notAfter = type.getNotOnOrAfter();
        checkNonNullAssertionElement("Not After", notAfter);
        final boolean retb = (notBefore.before(now) && notAfter.after(now));

        if (!retb && logger.getLevel().intValue() <= Level.INFO.intValue()) {
            StringBuffer sb = new StringBuffer();
            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            sb.append("Date/time range check failed").append("\n")
              .append("Time Now is:"+df.format(now.getTime())).append("\n")
              .append("Not Before is:"+df.format(notBefore.getTime())).append("\n")
              .append("Not After is:"+df.format(notAfter.getTime()));
            logger.info(sb.toString());

        }
        return retb;
    }

    /**
     * helper that checks assertion elements for <b>null</b>
     * @param element the element to check
     * @throws com.l7tech.common.security.saml.InvalidAssertionException if null
     */
    private static void checkNonNullAssertionElement(String name, Object element)
      throws InvalidAssertionException {
        if (element == null) {
            name = (name == null) ? "" : name;
            throw new InvalidAssertionException("The required element '"+name+" ' is null");
        }
    }

}
