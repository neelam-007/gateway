package com.l7tech.server.saml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.apache.xmlbeans.XmlOptions;
import org.xml.sax.SAXException;

import javax.xml.soap.*;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import x0Assertion.oasisNamesTcSAML1.AssertionDocument;
import x0Assertion.oasisNamesTcSAML1.AssertionType;
import x0Assertion.oasisNamesTcSAML1.ConditionsType;
import x0Assertion.oasisNamesTcSAML1.SubjectStatementAbstractType;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.io.StringWriter;
import java.io.IOException;

/**
 * Class SenderVouchesHelper.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
class SenderVouchesHelper {
    static final Logger log = Logger.getLogger(SenderVouchesHelper.class.getName());

    private int expiryMinutes;
    private AssertionDocument assertionDocument;

    int getExpiryMinutes() {
        return expiryMinutes;
    }

    void setExpiryMinutes(int expiryMinutes) {
        this.expiryMinutes = expiryMinutes;
    }

    /**
     * Attach the assertion the the soap message
     *
     * todo: check if the security header already exists
     * @param sm the soap message
     * @param assertionDom the assertion as dom document
     * @throws SOAPException on soap message processing error
     */
    private void attachAssertionHeader(SOAPMessage sm, Document assertionDom)
      throws SOAPException {
        SOAPEnvelope envelope = sm.getSOAPPart().getEnvelope();
        envelope.addNamespaceDeclaration("wsse", "http://schemas.xmlsoap.org/ws/2002/xx/secext");
        envelope.addNamespaceDeclaration("ds", "http://www.w3.org/2000/09/xmldsig#");
        SOAPHeader sh = envelope.getHeader();
        if (sh == null) {
            sh = envelope.addHeader();
        }
        Element domNode = assertionDom.getDocumentElement();
        SOAPHeaderElement she = null;
        SOAPFactory sf = SOAPFactory.newInstance();
        Name headerName = sf.createName("Security", "wsse", "http://schemas.xmlsoap.org/ws/2002/xx/secext");

        she = sh.addHeaderElement(headerName);
        Name assertionName = sf.createName(domNode.getLocalName(), domNode.getPrefix(), domNode.getNamespaceURI());

        SOAPElement assertionElement = she.addChildElement(assertionName);
        SoapUtil.domToSOAPElement(assertionElement, domNode);
    }


    Document createAssertion() throws IOException, SAXException {
        assertionDocument = AssertionDocument.Factory.newInstance();
        AssertionType assertion = AssertionType.Factory.newInstance();

        assertion.setMinorVersion(new BigInteger("0"));
        assertion.setMajorVersion(new BigInteger("1"));
        assertion.setAssertionID(Long.toHexString(System.currentTimeMillis()));
        assertion.setIssuer("ssg");
        assertion.setIssueInstant(Calendar.getInstance());
        ConditionsType ct = ConditionsType.Factory.newInstance();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        ct.setNotBefore(calendar);
        Calendar c2 = (Calendar)calendar.clone();
        c2.roll(Calendar.MINUTE, expiryMinutes);
        ct.setNotOnOrAfter(c2);
        assertion.setConditions(ct);
        SubjectStatementAbstractType ss = assertion.addNewSubjectStatement();
        StringWriter sw = new StringWriter();
        assertionDocument.setAssertion(assertion);

        XmlOptions xo = new XmlOptions();
        Map namespaces = new HashMap();
        namespaces.put("saml", "urn:oasis:names:tc:SAML:1.0:assertion");
        xo.setSaveImplicitNamespaces(namespaces);
        xo.setSavePrettyPrint();
        xo.setSavePrettyPrintIndent(2);
        xo.setLoadLineNumbers();

        assertionDocument.save(sw, xo);
        return XmlUtil.stringToDocument(sw.toString());
    }
}
