/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.saml;

import com.ibm.xml.dsig.*;
import com.ibm.xml.dsig.util.AdHocIDResolver;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.HexUtils;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.credential.http.HttpCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import org.apache.xmlbeans.XmlOptions;
import org.w3.x2000.x09.xmldsig.KeyInfoType;
import org.w3.x2000.x09.xmldsig.X509DataType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import x0Assertion.oasisNamesTcSAML1.*;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.*;

/**
 * Abstract superclass of specific Subject/ConfirmationMethod implementations
 *
 * @author alex
 * @version $Revision$
 */
public abstract class SamlAssertionHelper {
    SamlAssertionHelper(Document soapDom, SamlAssertionGenerator.Options options, LoginCredentials credentials, SignerInfo signer ) {
        if (signer == null || options.getExpiryMinutes() <= 0) {
            throw new IllegalArgumentException();
        }

        this.options = options;
        this.credentials = credentials;
        this.signerInfo = signer;
        this.soapMessage = soapDom;
        utcTimeZone = TimeZone.getTimeZone("UTC");
    }

    /**
     * attach the assertion header to the soap message
     *
     * @param sign if true the
     */
    void attachAssertion(boolean sign)
      throws IOException, SAXException, SignatureException, CertificateException {
        Document doc = createAssertion(null);

        try {
            doc.getDocumentElement().setAttribute("Id", "SamlTicket"); // TODO use AssertionID and better values, if at all
            signEnvelope(doc, signerInfo.getPrivate(), signerInfo.getCertificateChain());
            Element secElement = SoapUtil.getOrMakeSecurityElement(soapMessage);
            if ( secElement == null ) {
                throw new SAXException("Can't attach SAML token to non-SOAP message");
            }
            SoapUtil.importNode(soapMessage, doc, secElement);
            NodeList list = secElement.getElementsByTagNameNS(SamlConstants.NS_SAML, "Assertion");
            if (list.getLength() == 0) {
                throw new IOException("Cannot locate the saml assertion in \n"+XmlUtil.nodeToString(soapMessage));
            }
        } catch (Exception e) {
            SignatureException ex = new SignatureException("error signing the saml ticket");
            ex.initCause(e);
            throw ex;
        }
    }

    public Document createSignedAssertion() throws IOException, SAXException, SignatureException, CertificateException {
        Document doc = createAssertion(null);
        signAssertion(doc, signerInfo.getPrivate(), signerInfo.getCertificateChain());
        return doc;
    }

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
    protected void signEnvelope(Document soapMsg, PrivateKey privateKey, X509Certificate[] certChain)
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
    private void signElement(Document document, final Element messagePart, String referenceId, PrivateKey privateKey, X509Certificate[] certChain)
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
     * Create a SAML assertion.
     * @return the new assertion, not yet signed
     * @param assertionId what to use as value of AssertionID in created element.  If null, one will be made up.
     * @throws IOException
     * @throws SAXException
     * @throws CertificateException
     */
    abstract Document createAssertion(String assertionId) throws IOException, SAXException, CertificateException;

    /**
     * sign te while document (envelope).
     */
    void signEnvelope()
      throws IOException, SAXException, SignatureException {
        try {
            signEnvelope(soapMessage, signerInfo.getPrivate(), signerInfo.getCertificateChain());
        } catch (Exception e) {
            SignatureException ex = new SignatureException("error signing the saml ticket");
            ex.initCause(e);
            throw ex;
        }
    }

    protected AssertionType getGenericAssertion( Calendar now, String assertionId ) {
        final String caDn = signerInfo.getCertificateChain()[0].getSubjectDN().getName();
        Map caMap = CertUtils.dnToAttributeMap(caDn);
        String caCn = (String)((List)caMap.get("CN")).get(0);

        AssertionType assertion = AssertionType.Factory.newInstance();

        assertion.setMinorVersion(BigInteger.ONE);
        assertion.setMajorVersion(BigInteger.ONE);
        if (assertionId == null)
            assertion.setAssertionID(generateAssertionId(null));
        else
            assertion.setAssertionID(assertionId);
        assertion.setIssuer(caCn);
        assertion.setIssueInstant(now);

        ConditionsType ct = ConditionsType.Factory.newInstance();
        Calendar calendar = Calendar.getInstance(utcTimeZone);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        ct.setNotBefore(calendar);
        Calendar c2 = (Calendar)calendar.clone();
        c2.roll(Calendar.MINUTE, options.getExpiryMinutes());
        ct.setNotOnOrAfter(c2);
        assertion.setConditions(ct);
        return assertion;
    }

    public static Document assertionToDocument( AssertionType assertion ) throws IOException, SAXException {
        AssertionDocument assertionDocument = AssertionDocument.Factory.newInstance();
        StringWriter sw = new StringWriter();
        assertionDocument.setAssertion(assertion);

        XmlOptions xo = new XmlOptions();
        Map namespaces = new HashMap();
        namespaces.put(SamlConstants.NS_SAML, SamlConstants.NS_SAML_PREFIX);
        xo.setSaveSuggestedPrefixes(namespaces);
        /*
        xo.setSavePrettyPrint();
        xo.setSavePrettyPrintIndent(2);
        xo.setLoadLineNumbers();
        */
        assertionDocument.save(sw, xo);
        return XmlUtil.stringToDocument(sw.toString());
    }

    protected AuthenticationStatementType attachAuthenticationStatement(AssertionType assertion, Calendar now) throws CertificateEncodingException {
        final Class credentialSourceClass = credentials.getCredentialSourceAssertion();

        long authInstant = credentials.getAuthInstant();
        if (authInstant == 0) authInstant = System.currentTimeMillis();
        now.setTimeInMillis(authInstant);

        AuthenticationStatementType authStatement = assertion.addNewAuthenticationStatement();
        authStatement.setAuthenticationInstant(now);

        SubjectType subject = authStatement.addNewSubject();
        NameIdentifierType ni = subject.addNewNameIdentifier();
        if (credentials.getFormat().isClientCert()) {
            X509Certificate cert = (X509Certificate)credentials.getPayload();
            ni.setFormat(SamlConstants.NAMEIDENTIFIER_X509_SUBJECT);
            final String dn = cert.getSubjectDN().getName();
            ni.setStringValue(dn);
            SubjectConfirmationType sc = subject.addNewSubjectConfirmation();
            KeyInfoType keyInfo = sc.addNewKeyInfo();
            X509DataType x509 = keyInfo.addNewX509Data();
            x509.addX509SubjectName(dn);
            x509.addX509Certificate(cert.getEncoded());
        } else {
            // TODO add email address etc.
            ni.setFormat(SamlConstants.NAMEIDENTIFIER_UNSPECIFIED);
        }
        if (ni.getStringValue() == null && credentials.getLogin() != null)
            ni.setStringValue(credentials.getLogin());
        InetAddress clientAddress = options.getClientAddress();
        if (clientAddress != null) {
            final SubjectLocalityType subjectLocality = authStatement.addNewSubjectLocality();
            subjectLocality.setIPAddress(HolderOfKeyHelper.addressToString(clientAddress));
            subjectLocality.setDNSAddress(clientAddress.getCanonicalHostName());
        }

        String authMethod = null;
        if (credentialSourceClass.isAssignableFrom(HttpClientCert.class)) {
            authMethod = SamlConstants.SSL_TLS_CERTIFICATE_AUTHENTICATION;
        } else if (credentialSourceClass.isAssignableFrom(RequestWssX509Cert.class)) {
            authMethod = SamlConstants.XML_DSIG_AUTHENTICATION;
        } else if (credentialSourceClass.isAssignableFrom(HttpCredentialSourceAssertion.class) ||
                   credentialSourceClass.isAssignableFrom(WssBasic.class)) {
            authMethod = SamlConstants.PASSWORD_AUTHENTICATION;
        } else {
            authMethod = SamlConstants.UNSPECIFIED_AUTHENTICATION;
        }

        authStatement.setAuthenticationMethod(authMethod);

        return authStatement;
    }

    public static void signAssertion(final Document assertionDoc, PrivateKey signingKey,
                                     X509Certificate[] signingCertChain) throws SignatureException {
        TemplateGenerator template = new TemplateGenerator( assertionDoc, XSignature.SHA1,
                                                            Canonicalizer.EXCLUSIVE, SignatureMethod.RSA);
        final String id = assertionDoc.getDocumentElement().getAttribute(SamlConstants.ATTR_ASSERTION_ID);
        template.setPrefix("ds");
        Reference ref = template.createReference("#" + id);
        ref.addTransform(Transform.ENVELOPED);
        ref.addTransform(Transform.C14N_EXCLUSIVE);
        template.addReference(ref);

        SignatureContext context = new SignatureContext();
        context.setIDResolver(new IDResolver() {
            public Element resolveID( Document document, String s ) {
                if (id.equals(s))
                    return assertionDoc.getDocumentElement();
                else
                    throw new IllegalArgumentException("I don't know how to find " + s);
            }
        });

        final Element signatureElement = template.getSignatureElement();
        assertionDoc.getDocumentElement().appendChild(signatureElement);
        KeyInfo keyInfo = new KeyInfo();
        KeyInfo.X509Data x509 = new KeyInfo.X509Data();
        x509.setCertificate(signingCertChain[0]);
        x509.setParameters(signingCertChain[0], false, false, true);
        keyInfo.setX509Data(new KeyInfo.X509Data[] { x509 });

        signatureElement.appendChild(keyInfo.getKeyInfoElement(assertionDoc));

        try {
            context.sign(signatureElement, signingKey);
        } catch ( XSignatureException e ) {
            throw new SignatureException(e.getMessage());
        }
    }

    public static String generateAssertionId(String prefix) {
        if (prefix == null) prefix = "SamlAssertion";
        byte[] disambig = new byte[16];
        random.nextBytes(disambig);
        return prefix + "-" + HexUtils.hexDump(disambig);
    }

    protected SignerInfo signerInfo;
    protected Document soapMessage;
    protected SamlAssertionGenerator.Options options;
    protected LoginCredentials credentials;
    protected final TimeZone utcTimeZone;
    private static final SecureRandom random = new SecureRandom();
}
