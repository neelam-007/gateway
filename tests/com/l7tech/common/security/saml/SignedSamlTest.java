package com.l7tech.common.security.saml;

import com.ibm.xml.dsig.*;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.xmlbeans.XmlOptions;
import org.w3.x2000.x09.xmldsig.KeyInfoType;
import org.w3.x2000.x09.xmldsig.X509DataType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import x0Assertion.oasisNamesTcSAML1.*;

import javax.xml.namespace.QName;
import java.io.StringWriter;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author alex
 * @version $Revision$
 */
public class SignedSamlTest extends TestCase {
    /**
     * test <code>SignedSamlTest</code> constructor
     */
    public SignedSamlTest( String name ) {
        super( name );
    }

    /**
     * create the <code>TestSuite</code> for the SignedSamlTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite( SignedSamlTest.class );
        return suite;
    }

    public void setUp() throws Exception {
        KeystoreUtils ku = KeystoreUtils.getInstance();

        final char[] caPassChars = ku.getRootKeystorePasswd().toCharArray();
        KeyStore caKeyStore = KeystoreUtils.getKeyStore(ku.getRootKeystorePath(), caPassChars, ku.getKeyStoreType());
        caPrivateKey = (PrivateKey)caKeyStore.getKey(CA_ALIAS, caPassChars);
        Certificate[] certs = caKeyStore.getCertificateChain(CA_ALIAS);
        X509Certificate[] caCerts = new X509Certificate[certs.length];
        for ( int i = 0; i < certs.length; i++ ) {
            Certificate cert = certs[i];
            caCerts[i] = (X509Certificate)cert;
        }
        caCertChain = caCerts;
        caPublicKey = caCerts[0].getPublicKey();

        final char[] clientPassChars = CLIENT_PASS.toCharArray();
        KeyStore clientKeystore = KeystoreUtils.getKeyStore(CLIENT_KEYSTORE, clientPassChars, "PKCS12");
        clientPrivateKey = (PrivateKey)clientKeystore.getKey(CLIENT_ALIAS, clientPassChars);
        certs = clientKeystore.getCertificateChain(CLIENT_ALIAS);
        final X509Certificate[] clientCerts = new X509Certificate[certs.length];
        for ( int i = 0; i < certs.length; i++ ) {
            Certificate cert = certs[i];
            clientCerts[i] = (X509Certificate)cert;
        }
        clientCertChain = clientCerts;
        clientPublicKey = clientCertChain[0].getPublicKey();
    }

    public void testKeys() throws Exception {
        System.out.println("CA private key: " + caPrivateKey);
        System.out.println("CA public key: " + caPublicKey);
        System.out.println("CA cert: " + caCertChain[0]);

        System.out.println("Client private key: " + clientPrivateKey);
        System.out.println("Client public key: " + clientPublicKey);
        System.out.println("Client cert: " + clientCertChain[0]);
    }

    private AssertionType getUnsignedHolderOfKeyAssertion() throws Exception {
        final String clientDn = clientCertChain[0].getSubjectDN().getName();
        final Map clientCertDnMap = CertUtils.dnToAttributeMap(clientDn);
        final String clientCertCn = (String)((List)clientCertDnMap.get("CN")).get(0);
        System.out.println("Client CN = " + clientCertCn);

        final String caDn = caCertChain[0].getSubjectDN().getName();
        final Map caCertDnMap = CertUtils.dnToAttributeMap(caDn);
        final String caCertCn = (String)((List)caCertDnMap.get("CN")).get(0);
        System.out.println("CA CN = " + caCertCn);

        final SubjectType samlSubject = SubjectType.Factory.newInstance();
        NameIdentifierType nameIdentifier = samlSubject.addNewNameIdentifier();
        nameIdentifier.setNameQualifier(caCertCn);
        nameIdentifier.setFormat(Constants.NAMEIDENTIFIER_X509_SUBJECT);
        nameIdentifier.setStringValue(clientDn);

        SubjectConfirmationType samlSubjectConfirmation = samlSubject.addNewSubjectConfirmation();
        samlSubjectConfirmation.addConfirmationMethod(Constants.CONFIRMATION_HOLDER_OF_KEY);
        KeyInfoType samlKeyInfo = samlSubjectConfirmation.addNewKeyInfo();

        X509DataType samlX509 = samlKeyInfo.addNewX509Data();
        samlX509.addX509SubjectName(clientDn);
        samlX509.addX509Certificate(clientCertChain[0].getEncoded());

        AssertionType samlAssertion = AssertionType.Factory.newInstance();
        samlAssertion.setAssertionID(ASSERTION_ID);

        final Calendar cal = Calendar.getInstance();

        AuthenticationStatementType samlAuthStatement = samlAssertion.addNewAuthenticationStatement();
        samlAuthStatement.setAuthenticationInstant(cal);
        samlAuthStatement.setSubject(samlSubject);
        samlAuthStatement.setAuthenticationMethod(Constants.X509_PKI_AUTHENTICATION);
        samlAuthStatement.addNewSubjectLocality().setIPAddress(addressToString(InetAddress.getLocalHost()));

        ConditionsType conditions = samlAssertion.addNewConditions();
        conditions.setNotBefore(cal);
        cal.add(Calendar.SECOND, EXPIRY_SECONDS);
        conditions.setNotOnOrAfter(cal);

        return samlAssertion;
    }

    private String addressToString(InetAddress address) {
        StringBuffer sb = new StringBuffer();
        byte[] bytes = address.getAddress();
        for ( int i = 0; i < bytes.length; i++ ) {
            byte b = bytes[i];
            sb.append(b & 0xff);
            if ( i < bytes.length-1 ) sb.append(".");
        }
        return sb.toString();
    }

    public void testSignedHolderOfKey() throws Exception {
        final AssertionType samlAssertion = getUnsignedHolderOfKeyAssertion();

        final HashMap prefixMap = new HashMap();
        prefixMap.put(Constants.NS_SAML, "saml");
        prefixMap.put(SoapUtil.DIGSIG_URI, "ds");

        final XmlOptions xmlOptions = new XmlOptions();
        xmlOptions.setSaveSyntheticDocumentElement(new QName(Constants.NS_SAML, Constants.ELEMENT_ASSERTION));
        xmlOptions.setSavePrettyPrint();
        xmlOptions.setSaveSuggestedPrefixes(prefixMap);

        StringWriter stringWriter = new StringWriter();
        samlAssertion.save(stringWriter, xmlOptions);
        String s = stringWriter.toString();
        final Document assertionDoc = XmlUtil.stringToDocument(s);
        String s2 = XmlUtil.nodeToFormattedString(assertionDoc);
        System.out.println("Before signing: " + s2);
        stringWriter.close();

        TemplateGenerator template = new TemplateGenerator( assertionDoc, XSignature.SHA1,
                                                            Canonicalizer.EXCLUSIVE, SignatureMethod.RSA);
        template.setPrefix("ds");
        Reference ref = template.createReference("#" + ASSERTION_ID);
        ref.addTransform(Transform.ENVELOPED);
        ref.addTransform(Transform.C14N_EXCLUSIVE);
        template.addReference(ref);

        SignatureContext context = new SignatureContext();
        context.setIDResolver(new IDResolver() {
            public Element resolveID( Document document, String s ) {
                if (ASSERTION_ID.equals(s))
                    return assertionDoc.getDocumentElement();
                else
                    throw new IllegalArgumentException("I don't know how to find " + s);
            }
        });

        final Element signatureElement = template.getSignatureElement();
        assertionDoc.getDocumentElement().appendChild(signatureElement);
        KeyInfo keyInfo = new KeyInfo();
        KeyInfo.X509Data x509 = new KeyInfo.X509Data();
        x509.setCertificate(caCertChain[0]);
        x509.setParameters(caCertChain[0], false, false, true);
        keyInfo.setX509Data(new KeyInfo.X509Data[] { x509 });

        signatureElement.appendChild(keyInfo.getKeyInfoElement(assertionDoc));

        context.sign(signatureElement, caPrivateKey);

        String s3 = XmlUtil.nodeToFormattedString(assertionDoc);
        System.out.println("After signing: " + s3);
    }

    /**
     * Test <code>SignedSamlTest</code> main.
     */
    public static void main( String[] args ) throws
                                             Throwable {
        junit.textui.TestRunner.run( suite() );
    }

    private PrivateKey caPrivateKey;
    private PublicKey caPublicKey;
    private X509Certificate[] caCertChain;

    private PrivateKey clientPrivateKey;
    private PublicKey clientPublicKey;
    private X509Certificate[] clientCertChain;

    private static final String CLIENT_ALIAS = "clientCert";
    private static final String CLIENT_PASS = "asdfasdf";
    private static final String CLIENT_KEYSTORE = System.getProperty("user.home") +
                                                  "/.l7tech/key1.p12";
    private static final String ASSERTION_ID = "mySamlAssertion";
    private static final String CA_ALIAS = "ssgroot";
    private static final int EXPIRY_SECONDS = 2 * 60;
}