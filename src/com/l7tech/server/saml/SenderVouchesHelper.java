package com.l7tech.server.saml;

import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.SoapMsgSigner;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.identity.User;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import x0Assertion.oasisNamesTcSAML1.*;

import javax.xml.soap.*;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Class SenderVouchesHelper.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
class SenderVouchesHelper {
    static final Logger log = Logger.getLogger(SenderVouchesHelper.class.getName());
    static final int DEFAULT_EXPIRY_MINUTES = 5;
    private int expiryMinutes = DEFAULT_EXPIRY_MINUTES;
    private boolean includeGroupStatement = true;
    private User user;
    private String issuer;
    private SignerInfo signerInfo;
    private SOAPMessage soapMessage;

    SenderVouchesHelper(SOAPMessage sm, User user, boolean includeGroupStatement,
                        int expiryMinutes, String issuer, SignerInfo signer) {
        if (sm == null || user == null || issuer == null || signer == null || expiryMinutes <= 0) {
            throw new IllegalArgumentException();
        }

        this.user = user;
        this.includeGroupStatement = includeGroupStatement;
        this.expiryMinutes = expiryMinutes;
        this.issuer = issuer;
        this.signerInfo = signer;
        this.soapMessage = sm;
    }

    /**
     * attach the assertion header to the <code>SOAPMEssage</code>
     * @param sign if true the
     */
    void attachAssertion(boolean sign)
      throws IOException, SAXException, SignatureException {
        Document doc = createAssertion();

        try {
            doc.getDocumentElement().setAttribute("Id", "SamlTicket");
            SoapMsgSigner signer = new SoapMsgSigner();
            signer.signEnvelope(doc, signerInfo.getPrivate(), signerInfo.getCertificate());
            attachAssertionHeader(soapMessage, doc);
        } catch (Exception e) {
            SignatureException ex =  new SignatureException("error signing the saml ticket");
            ex.initCause(e);
            throw ex;
        }
    }

    int getExpiryMinutes() {
        return expiryMinutes;
    }

    boolean isIncludeGroupStatement() {
        return includeGroupStatement;
    }

    User getUser() {
        return user;
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
        AssertionDocument assertionDocument = AssertionDocument.Factory.newInstance();
        AssertionType assertion = AssertionType.Factory.newInstance();
        Calendar now = Calendar.getInstance();

        assertion.setMinorVersion(new BigInteger("0"));
        assertion.setMajorVersion(new BigInteger("1"));
        assertion.setAssertionID(Long.toHexString(System.currentTimeMillis()));
        assertion.setIssuer(issuer);
        assertion.setIssueInstant(now);

        ConditionsType ct = ConditionsType.Factory.newInstance();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        ct.setNotBefore(calendar);
        Calendar c2 = (Calendar)calendar.clone();
        c2.roll(Calendar.MINUTE, expiryMinutes);
        ct.setNotOnOrAfter(c2);
        assertion.setConditions(ct);

        AuthenticationStatementType at = assertion.addNewAuthenticationStatement();
        at.setAuthenticationMethod("urn:oasis:names:tc:SAML:1.0:am:password");
        at.setAuthenticationInstant(now);
        SubjectType subject = at.addNewSubject();
        NameIdentifierType ni = subject.addNewNameIdentifier();
        ni.setStringValue(user.getName());
        SubjectConfirmationType st = subject.addNewSubjectConfirmation();
        st.addConfirmationMethod("urn:oasis:names:tc:SAML:1.0:cm:sender-vouches");


        StringWriter sw = new StringWriter();
        assertionDocument.setAssertion(assertion);

        XmlOptions xo = new XmlOptions();
        Map namespaces = new HashMap();
        namespaces.put("saml", "urn:oasis:names:tc:SAML:1.0:assertion");
        xo.setSaveImplicitNamespaces(namespaces);
        /*
        xo.setSavePrettyPrint();
        xo.setSavePrettyPrintIndent(2);
        xo.setLoadLineNumbers();
        */
        assertionDocument.save(sw, xo);
        return XmlUtil.stringToDocument(sw.toString());
    }
}
