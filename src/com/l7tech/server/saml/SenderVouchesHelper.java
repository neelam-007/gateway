package com.l7tech.server.saml;

import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.identity.User;
import com.ibm.xml.dsig.*;
import com.ibm.xml.dsig.util.AdHocIDResolver;
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
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.cert.X509Certificate;
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
            signEnvelope(doc, signerInfo.getPrivate(), signerInfo.getCertificateChain());
            Element secElement = SoapUtil.getOrMakeSecurityElement(soapMessage);
            if ( secElement == null ) {
                throw new SAXException("Can't attach SAML token to non-SOAP message");
            }
            SoapUtil.importNode(soapMessage, doc, secElement);
            NodeList list = secElement.getElementsByTagNameNS(Constants.NS_SAML, "Assertion");
            if (list.getLength() == 0) {
                throw new IOException("Cannot locate the saml assertion in \n"+XmlUtil.nodeToString(soapMessage));
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
            signEnvelope(soapMessage, signerInfo.getPrivate(), signerInfo.getCertificateChain());
        } catch (Exception e) {
            SignatureException ex = new SignatureException("error signing the saml ticket");
            ex.initCause(e);
            throw ex;
        }
    }

    // TODO move this XML document signing utility into a standalone class in common/security/xml

    /**
     * Appends a soap message with a digital signature of it's entire envelope.
     * <p/>
     * If the envelope already has as Id attribute, it's value will be used to refer to the envelope within the
     * SignedInfo element. Otherwise, an Id of value DEF_ENV_TAG will be used.
     *
     * @param soapMsg    the xml document containing the soap message expected to contain at least a soapenvelope element.
     *                   this document contains the signature when at return time.
     * @param privateKey the private key of the signer if imlpements RSAPrivateKey signature method will be
     *                   http://www.w3.org/2000/09/xmldsig#rsa-sha1, if privateKey implements DSAPrivateKey, signature method will be
     *                   http://www.w3.org/2000/09/xmldsig#dsa-sha1.
     * @param certChain  the signer's cert chain
     * @throws com.ibm.xml.dsig.SignatureStructureException
     *
     * @throws com.ibm.xml.dsig.XSignatureException
     *
     */
    private static void signEnvelope(Document soapMsg, PrivateKey privateKey, X509Certificate[] certChain)
      throws SignatureStructureException, XSignatureException
    {
        // is the envelope already ided?
        String id = soapMsg.getDocumentElement().getAttribute("Id");

        if (id == null || id.length() < 1) {
            id = "envId";
        }
        signElement(soapMsg, soapMsg.getDocumentElement(), id, privateKey, certChain);
    }

    /**
     * Sign the document element using the private key and Embed the <code>X509Certificate</code> into
     * the XML document.
     *
     * @param document    the xml document containing the element to sign.
     * @param messagePart        the document element
     * @param referenceId the signature reference ID attreibute value
     * @param privateKey  the private key of the signer if imlpements RSAPrivateKey signature method will be
     *                    http://www.w3.org/2000/09/xmldsig#rsa-sha1, if privateKey implements DSAPrivateKey, signature method will be
     *                    http://www.w3.org/2000/09/xmldsig#dsa-sha1.
     * @param certChain        the signer's cert chain
     * @throws com.ibm.xml.dsig.SignatureStructureException
     *
     * @throws com.ibm.xml.dsig.XSignatureException
     *
     * @throws IllegalArgumentException if any of the parameters i <b>null</b>
     */
    public static void signElement(Document document, final Element messagePart, String referenceId, PrivateKey privateKey, X509Certificate[] certChain)
      throws SignatureStructureException, XSignatureException {

        if (document == null || messagePart == null | referenceId == null ||
          privateKey == null || certChain == null || certChain.length == 0) {
            throw new IllegalArgumentException();
        }

        String id = messagePart.getAttribute(referenceId);
        if (id == null || "".equals(id)) {
            id = referenceId;
            messagePart.setAttribute("Id", referenceId);
        }

        // set the appropriate signature method
        String signaturemethod = getSignatureMethod(privateKey);

        // Create signature template and populate with appropriate transforms. Reference is to SOAP Envelope
        TemplateGenerator template = new TemplateGenerator(document, XSignature.SHA1, Canonicalizer.W3C2, signaturemethod);
        template.setPrefix("ds");
        Reference ref = template.createReference("#" + id);
        ref.addTransform(Transform.ENVELOPED);
        ref.addTransform(Transform.W3CC14N2);
        template.addReference(ref);
        Element emptySignatureElement = template.getSignatureElement();

        // Signature is inserted in Header/Security, as per WS-S
        Element securityHeaderElement = SoapUtil.getOrMakeSecurityElement(document);

        Element signatureElement = (Element)securityHeaderElement.appendChild(emptySignatureElement);

        // Include KeyInfo element in signature and embed cert into subordinate X509Data element
        KeyInfo keyInfo = new KeyInfo();
        KeyInfo.X509Data x509Data = new KeyInfo.X509Data();
        x509Data.setCertificate(certChain[0]);
        x509Data.setParameters(certChain[0], true, true, true);
        keyInfo.setX509Data(new KeyInfo.X509Data[]{x509Data});
        keyInfo.insertTo(signatureElement, "ds");

        // Setup context and sign document
        SignatureContext sigContext = new SignatureContext();
        AdHocIDResolver idResolver = new AdHocIDResolver(document);
        sigContext.setIDResolver(idResolver);

        sigContext.sign(signatureElement, privateKey);
    }

    private static String getSignatureMethod(PrivateKey privateKey) {
        String signaturemethod;
        if (privateKey instanceof RSAPrivateKey)
            signaturemethod = SignatureMethod.RSA;
        else if (privateKey instanceof DSAPrivateKey)
            signaturemethod = SignatureMethod.DSA;
        else {
            throw new IllegalArgumentException("Unsupported private key type: " + privateKey.getClass().getName());
        }
        return signaturemethod;
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
        at.setAuthenticationMethod(Constants.PASSWORD_AUTHENTICATION);
        at.setAuthenticationInstant(now);
        SubjectType subject = at.addNewSubject();
        NameIdentifierType ni = subject.addNewNameIdentifier();
        ni.setStringValue(user.getName());
        SubjectConfirmationType st = subject.addNewSubjectConfirmation();
        st.addConfirmationMethod(Constants.CONFIRMATION_SENDER_VOUCHES);


        StringWriter sw = new StringWriter();
        assertionDocument.setAssertion(assertion);

        XmlOptions xo = new XmlOptions();
        Map namespaces = new HashMap();
        namespaces.put(Constants.NS_SAML, Constants.NS_SAML_PREFIX);
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
