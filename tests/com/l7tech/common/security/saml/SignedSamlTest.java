package com.l7tech.common.security.saml;

import com.ibm.xml.dsig.*;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.server.saml.HolderOfKeyHelper;
import com.l7tech.server.saml.SamlAssertionGenerator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.net.InetAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
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
        caPrivateKey = TestDocuments.getEttkServerPrivateKey();
        caCertChain = new X509Certificate[] { TestDocuments.getEttkServerCertificate() };
        caPublicKey = caCertChain[0].getPublicKey();

        clientPrivateKey = TestDocuments.getEttkClientPrivateKey();
        clientCertChain = new X509Certificate[] { TestDocuments.getEttkClientCertificate() };
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

    private Document getUnsignedHolderOfKeyAssertion() throws Exception {
        final String clientDn = clientCertChain[0].getSubjectDN().getName();
        final Map clientCertDnMap = CertUtils.dnToAttributeMap(clientDn);
        final String clientCertCn = (String)((List)clientCertDnMap.get("CN")).get(0);
        System.out.println("Client CN = " + clientCertCn);

        final String caDn = caCertChain[0].getSubjectDN().getName();
        final Map caCertDnMap = CertUtils.dnToAttributeMap(caDn);
        final String caCertCn = (String)((List)caCertDnMap.get("CN")).get(0);
        System.out.println("CA CN = " + caCertCn);

        LoginCredentials creds = new LoginCredentials(null, null, CredentialFormat.CLIENTCERT, RequestWssX509Cert.class, null, clientCertChain[0]);
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setExpiryMinutes(80);
        samlOptions.setClientAddress(InetAddress.getLocalHost());
        Document doc = new DocumentImpl();
        SignerInfo si = new SignerInfo(caPrivateKey, caCertChain);
        HolderOfKeyHelper hh = new HolderOfKeyHelper(doc, samlOptions, creds, si);
        Document samlDoc = hh.createAssertion();
        return samlDoc;
    }

    public void testSignedHolderOfKey() throws Exception {
        final Document assertionDoc = getUnsignedHolderOfKeyAssertion();

        final HashMap prefixMap = new HashMap();
        prefixMap.put(Constants.NS_SAML, "saml");
        prefixMap.put(SoapUtil.DIGSIG_URI, "ds");

        final XmlOptions xmlOptions = new XmlOptions();
        xmlOptions.setSaveSyntheticDocumentElement(new QName(Constants.NS_SAML, Constants.ELEMENT_ASSERTION));
        xmlOptions.setSavePrettyPrint();
        xmlOptions.setSaveAggresiveNamespaces();
        xmlOptions.setSaveSuggestedPrefixes(prefixMap);

        String s2 = XmlUtil.nodeToFormattedString(assertionDoc);
        System.out.println("Before signing: " + s2);

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
}