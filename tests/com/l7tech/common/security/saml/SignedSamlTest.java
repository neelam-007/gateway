package com.l7tech.common.security.saml;

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
import com.l7tech.server.saml.SamlAssertionHelper;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Document;

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

        SamlAssertionHelper.signAssertion(assertionDoc, caPrivateKey, caCertChain);

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
}