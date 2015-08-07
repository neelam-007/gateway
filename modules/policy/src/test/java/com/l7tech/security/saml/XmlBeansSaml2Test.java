package com.l7tech.security.saml;

import com.l7tech.security.token.http.TlsClientCertToken;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.TestDocuments;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.token.http.HttpBasicToken;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Document;
import x0Assertion.oasisNamesTcSAML2.AssertionDocument;
import x0Assertion.oasisNamesTcSAML2.AssertionType;
import x0Assertion.oasisNamesTcSAML2.ConditionsType;

import java.net.URL;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

public class XmlBeansSaml2Test  {

    //- PUBLIC

    /**
     *
     */
    @Before
    public void setUp() throws Exception {
        caPrivateKey = TestDocuments.getEttkServerPrivateKey();
        caCertChain = new X509Certificate[]{TestDocuments.getEttkServerCertificate()};
        caPublicKey = caCertChain[0].getPublicKey();

        clientPrivateKey =  TestDocuments.getEttkClientPrivateKey();
        clientCertChain = new X509Certificate[]{TestDocuments.getEttkClientCertificate()};
        clientPublicKey = clientCertChain[0].getPublicKey();
    }

    /**
     *
     */
    @Test
    public void testParseAssertionFromFile() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        URL assertionUrl = cl.getResource("com/l7tech/security/saml/saml_2_a.xml");
        assertTrue("Could not locate the resource", assertionUrl != null);

        XmlOptions xo = new XmlOptions();
        xo.setLoadLineNumbers();
        AssertionDocument doc = AssertionDocument.Factory.parse(assertionUrl, xo);
        //System.out.println(doc.getAssertion());
        xo = new XmlOptions();
        Collection errors = new ArrayList();
        xo.setErrorListener(errors);
        System.out.println("The document is " + (doc.validate(xo) ? "valid" : "invalid"));
        for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
            XmlError xerr = (XmlError)iterator.next();
            System.out.println(xerr);
        }

        AssertionType at = doc.getAssertion();
        ConditionsType type = at.getConditions();
        Calendar notBefore = type.getNotBefore();
        Calendar notAfter = type.getNotOnOrAfter();
        System.out.println("The not before is: " + notBefore);
        System.out.println("The not after is: " + notAfter);
    }

    /**
     *
     */
    @Test
    public void testParseAttributeStmtAssertionFromFile() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        URL assertionUrl = cl.getResource("com/l7tech/security/saml/saml_2_b.xml");
        assertTrue("Could not locate the resource", assertionUrl != null);

        XmlOptions xo = new XmlOptions();
        xo.setLoadLineNumbers();
        AssertionDocument doc = AssertionDocument.Factory.parse(assertionUrl, xo);
        //System.out.println(doc.getAssertion());
        xo = new XmlOptions();
        Collection errors = new ArrayList();
        xo.setErrorListener(errors);
        System.out.println("The document is " + (doc.validate(xo) ? "valid" : "invalid"));
        for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
            XmlError xerr = (XmlError)iterator.next();
            System.out.println(xerr);
        }

        AssertionType at = doc.getAssertion();
        ConditionsType type = at.getConditions();
        Calendar notBefore = type.getNotBefore();
        Calendar notAfter = type.getNotOnOrAfter();


        System.out.println("The not before is: " + notBefore);
        System.out.println("The not after is: " + notAfter);
        System.out.println("The attr name is: " + at.getAttributeStatementArray(0).getAttributeArray(0).getName());
    }

    /**
     *
     */
    @Test
    public void testParseAuthenticationStmtAssertionFromFile() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        URL assertionUrl = cl.getResource("com/l7tech/security/saml/saml_2_c.xml");
        assertTrue("Could not locate the resource", assertionUrl != null);

        XmlOptions xo = new XmlOptions();
        xo.setLoadLineNumbers();
        AssertionDocument doc = AssertionDocument.Factory.parse(assertionUrl, xo);
        //System.out.println(doc.getAssertion());
        xo = new XmlOptions();
        Collection errors = new ArrayList();
        xo.setErrorListener(errors);
        System.out.println("The document is " + (doc.validate(xo) ? "valid" : "invalid"));
        for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
            XmlError xerr = (XmlError)iterator.next();
            System.out.println(xerr);
        }

        AssertionType at = doc.getAssertion();
        ConditionsType type = at.getConditions();
        Calendar notBefore = type.getNotBefore();
        Calendar notAfter = type.getNotOnOrAfter();

        System.out.println("The not before is: " + notBefore);
        System.out.println("The not after is: " + notAfter);
        System.out.println("Auth instant is: " + at.getAuthnStatementArray(0).getAuthnInstant());
        System.out.println("Auth type is: " + at.getAuthnStatementArray(0).getAuthnContext().getAuthnContextClassRef());
    }

    /**
     *
     */
    @Test
    public void testGenerateAuthnStmtPasswordl() throws Exception {
        LoginCredentials creds = LoginCredentials.makeLoginCredentials(new HttpBasicToken("test", "pass".toCharArray()), HttpBasic.class);
        generateAuthenticationStatement(creds);
    }

    /**
     *
     */
    @Test
    public void testGenerateAuthnStmtTLS() throws Exception {
        LoginCredentials creds = LoginCredentials.makeLoginCredentials(new TlsClientCertToken(clientCertChain[0]), SslAssertion.class);
        generateAuthenticationStatement(creds);
    }

    /**
     *
     */
    @Test
    public void testGenerateAuthnStmtDigSig() throws Exception {
        LoginCredentials creds = LoginCredentials.makeLoginCredentials(new TlsClientCertToken(clientCertChain[0]), SslAssertion.class);
        generateAuthenticationStatement(creds);
    }

    //- PRIVATE

    private PrivateKey caPrivateKey;
    private PublicKey caPublicKey;
    private X509Certificate[] caCertChain;

    private PrivateKey clientPrivateKey;
    private PublicKey clientPublicKey;
    private X509Certificate[] clientCertChain;

    /**
     *
     */
    private void generateAuthenticationStatement(LoginCredentials creds) throws Exception {
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setClientAddress(InetAddressUtil.getLocalHost());
        samlOptions.setSignAssertion(false);
        samlOptions.setVersion(SamlAssertionGenerator.Options.VERSION_2);
        SignerInfo si = new SignerInfo(caPrivateKey, caCertChain);
        SubjectStatement subjectStatement = SubjectStatement.createAuthenticationStatement(creds, SubjectStatement.SENDER_VOUCHES, KeyInfoInclusionType.STR_THUMBPRINT, NameIdentifierInclusionType.FROM_CREDS, null, null, null, null);
        subjectStatement.setKeyInfo(creds.getClientCert());
        SamlAssertionGenerator generator = new SamlAssertionGenerator(si);

        Document assertionDocument = generator.createAssertion(subjectStatement, samlOptions);

        AssertionDocument.Factory.parse(assertionDocument);

        System.out.println( XmlUtil.nodeToFormattedString(assertionDocument));
    }
}
