package com.l7tech.server.saml;

import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.SoapMsgSigner;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.identity.User;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import x0Assertion.oasisNamesTcSAML1.*;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Class <code>SenderVouchesHelper</code> is the package private class
 * that provisions the sender voucher saml scenario.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
class SenderVouchesHelper {
    static final Logger log = Logger.getLogger(SenderVouchesHelper.class.getName());
    static final int DEFAULT_EXPIRY_MINUTES = 5;
    private int expiryMinutes = DEFAULT_EXPIRY_MINUTES;
    private boolean includeGroupStatement = true;
    private User user;
    private SignerInfo signerInfo;
    private Document soapMessage;
    private static final String CONFIRMATION_SENDER_VOUCHES = "urn:oasis:names:tc:SAML:1.0:cm:sender-vouches";
    private static final String PASSWORD_AUTHENTICATION = "urn:oasis:names:tc:SAML:1.0:am:password";
    //todo: add other authentication types (cert, signature etc)
    private static final String NS_SAML = "urn:oasis:names:tc:SAML:1.0:assertion";
    private static final String NS_SAML_PREFIX = "saml";

    /**
     * Instantiate the sender voucher helper
     * 
     * @param soapDom               the soap message as a dom.w3c.org document
     * @param user                  the user that
     * @param includeGroupStatement include the group statement
     * @param expiryMinutes         the saml ticket expiry timeout (default 5 minutes)
     * @param signer                the signer
     */
    SenderVouchesHelper(Document soapDom, User user,
                        boolean includeGroupStatement,
                        int expiryMinutes, SignerInfo signer) {
        if (soapDom == null || user == null ||
          signer == null || expiryMinutes <= 0) {
            throw new IllegalArgumentException();
        }

        this.user = user;
        this.includeGroupStatement = includeGroupStatement;
        this.expiryMinutes = expiryMinutes;
        this.signerInfo = signer;
        this.soapMessage = soapDom;
    }

    /**
     * attach the assertion header to the soap message
     * 
     * @param sign if true the
     */
    void attachAssertion(boolean sign)
      throws IOException, SAXException, SignatureException {
        Document doc = createAssertion();

        try {
            doc.getDocumentElement().setAttribute("Id", "SamlTicket");
            SoapMsgSigner.signEnvelope(doc, signerInfo.getPrivate(), signerInfo.getCertificateChain());
            Element secElement = SoapUtil.getOrMakeSecurityElement(soapMessage);
            if ( secElement == null ) {
                throw new SAXException("Can't attach SAML token to non-SOAP message");
            }
            SoapUtil.importNode(soapMessage, doc, secElement);
            NodeList list = secElement.getElementsByTagNameNS(NS_SAML, "Assertion");
            if (list.getLength() == 0) {
                throw new IOException("Cannot locate the saml assertion in \n"+XmlUtil.documentToString(soapMessage));
            }
            Node node = list.item(0);

        } catch (Exception e) {
            SignatureException ex = new SignatureException("error signing the saml ticket");
            ex.initCause(e);
            throw ex;
        }
    }

    /**
     * sign te while document (envelope).
     */
    void signEnvleope()
      throws IOException, SAXException, SignatureException {
        try {
            SoapMsgSigner.signEnvelope(soapMessage, signerInfo.getPrivate(), signerInfo.getCertificateChain());
        } catch (Exception e) {
            SignatureException ex = new SignatureException("error signing the saml ticket");
            ex.initCause(e);
            throw ex;
        }
    }


    /**
     * Attach the assertion the the soap message
     * <p/>
     * todo: check if the security header already exists
     * 
     * @param soapDom      dom.w3c.org the soap message
     * @param assertionDom the assertion as dom document
     */
    private void attachAssertionHeader(Document soapDom, Document assertionDom) {

    }

    /**
     * create saml sender vouches assertion
     * 
     * @return the saml assertion as a dom.w3c.org document
     * @throws IOException  
     * @throws SAXException 
     */
    Document createAssertion() throws IOException, SAXException {
        AssertionDocument assertionDocument = AssertionDocument.Factory.newInstance();
        AssertionType assertion = AssertionType.Factory.newInstance();
        Calendar now = Calendar.getInstance();

        assertion.setMinorVersion(new BigInteger("0"));
        assertion.setMajorVersion(new BigInteger("1"));
        assertion.setAssertionID(Long.toHexString(System.currentTimeMillis()));
        assertion.setIssuer(signerInfo.getCertificateChain()[0].getSubjectDN().getName()); // TODO is it OK to use the first cert in the chain?
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
        at.setAuthenticationMethod(PASSWORD_AUTHENTICATION);
        at.setAuthenticationInstant(now);
        SubjectType subject = at.addNewSubject();
        NameIdentifierType ni = subject.addNewNameIdentifier();
        ni.setStringValue(user.getName());
        SubjectConfirmationType st = subject.addNewSubjectConfirmation();
        st.addConfirmationMethod(CONFIRMATION_SENDER_VOUCHES);


        StringWriter sw = new StringWriter();
        assertionDocument.setAssertion(assertion);

        XmlOptions xo = new XmlOptions();
        Map namespaces = new HashMap();
        namespaces.put(NS_SAML, NS_SAML_PREFIX);
        xo.setSaveSuggestedPrefixes(namespaces);
        /*
        xo.setSavePrettyPrint();
        xo.setSavePrettyPrintIndent(2);
        xo.setLoadLineNumbers();
        */
        assertionDocument.save(sw, xo);
        return XmlUtil.stringToDocument(sw.toString());
    }
}
