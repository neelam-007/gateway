package com.l7tech.common.xml;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.ByteArrayInputStream;
import java.util.Properties;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.HexUtils;

/**
 * The class is a container for test documents, SOAP tmessages etc
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public final class TestDocuments {
    private static final String DIR = "com/l7tech/service/resources/";
    private static final String PDIR = "com/l7tech/policy/resources/";
    public static final String TEST_SOAP_XML = DIR + "GetLastTradePriceSoapRequest.xml";
    public static final String WSDL2PORTS = DIR + "xmltoday-delayed-quotes-2ports.wsdl";
    public static final String WSDL = DIR + "StockQuoteService.wsdl";
    public static final String WSDL_DOC_LITERAL = DIR + "GeoServe.wsdl";
    public static final String WSDL_DOC_LITERAL2 = DIR + "QuoteService.wsdl";
    public static final String WSDL_DOC_LITERAL3 = DIR + "DeadOrAlive.wsdl";
    public static final String WSDL2SERVICES = DIR + "xmltoday-delayed-quotes-2services.wsdl";
    public static final String PLACEORDER_CLEARTEXT = DIR + "PlaceOrder_cleartext.xml";
    public static final String PLACEORDER_WITH_MAJESTY = DIR + "PlaceOrder_with_XmlRequestSecurity.xml";
    public static final String PLACEORDER_KEYS = DIR + "PlaceOrder_with_XmlRequestSecurity_keys.properties";
    public static final String BUG_763_MONSTER_POLICY = PDIR + "bug763MonsterPolicy.xml";
    public static final String DOTNET_SIGNED_REQUEST = DIR + "dotNetSignedSoapRequest.xml";
    public static final String DOTNET_SIGNED_TAMPERED_REQUEST = DIR + "dotNetSignedTamperedSoapRequest.xml";
    public static final String DOTNET_ENCRYPTED_REQUEST = DIR + "dotNetSignedAndEncryptedRequest.xml";
    public static final String DOTNET_SIGNED_USING_DERIVED_KEY_TOKEN = DIR + "dotNetSignedUsingDerivedKeyToken.xml";
    public static final String DOTNET_ENCRYPTED_USING_DERIVED_KEY_TOKEN = DIR + "dotNetSignedAndEncryptedUsingDKFromSCT.xml";
    public static final String SOAP_WITH_ATTACHMENTS_REQUEST = DIR + "SOAPWithAttachmentsRequest.txt";
    public static final String DOTNET_SCT_REQUEST = DIR + "dotNetSCTRequest.xml";
    public static final String ETTK_SIGNED_REQUEST = DIR + "ibmEttkSignedRequest.xml";
    public static final String ETTK_ENCRYPTED_REQUEST = DIR + "ibmEttkEncryptedRequest.xml";
    public static final String ETTK_SIGNED_ENCRYPTED_REQUEST = DIR + "ibmEttkSignedEncryptedRequest.xml";
    public static final String ETTK_KS = DIR + "ibmEttkKeystore.db";
    public static final String ETTK_KS_PROPERTIES = DIR + "ibmEttkKeystore.properties";
    public static final String SSL_KS = DIR + "rikerssl.ks";
    public static final String SSL_CER = DIR + "rikerssl.cer";

    private TestDocuments() { }

    public static Document getTestDocument(String resourcetoread)
      throws IOException, SAXException {
        InputStream i = getInputStream(resourcetoread);
        return XmlUtil.parse(i);
    }

    public static InputStream getInputStream(String resourcetoread) throws FileNotFoundException {
        if (resourcetoread == null) {
            resourcetoread = TestDocuments.TEST_SOAP_XML;
        }
        ClassLoader cl = TestDocuments.class.getClassLoader();
        InputStream i = cl.getResourceAsStream(resourcetoread);
        if (i == null) {
            throw new FileNotFoundException(resourcetoread);
        }
        return i;
    }

    private static Properties ettkKeystoreProperties = null;
    private static synchronized Properties getEttkKeystoreProperties() throws IOException {
        if (ettkKeystoreProperties != null) return ettkKeystoreProperties;
        Properties ksp = new Properties();
        ksp.load(getInputStream(ETTK_KS_PROPERTIES));
        return ettkKeystoreProperties = ksp;
    }

    private static KeyStore ettkKeystore = null;
    private static synchronized KeyStore getEttkKeystore() throws IOException, GeneralSecurityException {
        if (ettkKeystore != null) return ettkKeystore;
        Properties ksp = getEttkKeystoreProperties();
        String keystorePassword = ksp.getProperty("keystore.storepass");
        KeyStore keyStore = KeyStore.getInstance(ksp.getProperty("keystore.type"));
        InputStream fis = null;
        try {
            fis = getInputStream(ETTK_KS);
            keyStore.load(fis, keystorePassword.toCharArray());
        } finally {
            if (fis != null)
                fis.close();
        }
        return ettkKeystore = keyStore;
    }

    private static PrivateKey ettkServerPrivateKey = null;
    public static synchronized PrivateKey getEttkServerPrivateKey() throws GeneralSecurityException, IOException
    {
        if (ettkServerPrivateKey != null) return ettkServerPrivateKey;
        KeyStore keyStore = getEttkKeystore();
        Properties ksp = getEttkKeystoreProperties();
        String serverAlias = ksp.getProperty("keystore.server.alias");
        String serverKeyPassword = ksp.getProperty("keystore.server.keypass");
        return ettkServerPrivateKey = (PrivateKey)keyStore.getKey(serverAlias, serverKeyPassword.toCharArray());
    }

    private static X509Certificate ettkServerCertificate = null;
    public static synchronized X509Certificate getEttkServerCertificate() throws IOException, GeneralSecurityException {
        if (ettkServerCertificate != null) return ettkServerCertificate;
        KeyStore keyStore = getEttkKeystore();
        Properties ksp = getEttkKeystoreProperties();
        String serverAlias = ksp.getProperty("keystore.server.alias");
        return ettkServerCertificate = (X509Certificate)keyStore.getCertificate(serverAlias);
    }

    private static PrivateKey ettkClientPrivateKey = null;
    public static synchronized PrivateKey getEttkClientPrivateKey() throws GeneralSecurityException, IOException
    {
        if (ettkClientPrivateKey != null) return ettkClientPrivateKey;
        KeyStore keyStore = getEttkKeystore();
        Properties ksp = getEttkKeystoreProperties();
        String clientAlias = ksp.getProperty("keystore.client.alias");
        String clientKeyPassword = ksp.getProperty("keystore.client.keypass");
        return ettkClientPrivateKey = (PrivateKey)keyStore.getKey(clientAlias, clientKeyPassword.toCharArray());
    }

    private static X509Certificate ettkClientCertificate = null;
    public static synchronized X509Certificate getEttkClientCertificate() throws GeneralSecurityException, IOException {
        if (ettkClientCertificate != null) return ettkClientCertificate;
        KeyStore keyStore = getEttkKeystore();
        Properties ksp = getEttkKeystoreProperties();
        String clientAlias = ksp.getProperty("keystore.client.alias");
        return ettkClientCertificate = (X509Certificate)keyStore.getCertificate(clientAlias);
    }

    private static PrivateKey dotNetServerPrivateKey = null;
    public static synchronized PrivateKey getDotNetServerPrivateKey() throws Exception {
        if (dotNetServerPrivateKey != null) return dotNetServerPrivateKey;
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream fis = getInputStream(SSL_KS);
        //fis = FileUtils.loadFileSafely(sslkeystorepath);
        final String RIKER_KEYSTORE_PASSWORD = "blahblah";
        keyStore.load(fis, RIKER_KEYSTORE_PASSWORD.toCharArray());
        fis.close();
        final String RIKER_PRIVATE_KEY_PASSWORD = "blahblah";
        final String RIKER_PRIVATE_KEY_ALIAS = "tomcat";
        PrivateKey output = (PrivateKey)keyStore.getKey(RIKER_PRIVATE_KEY_ALIAS,
                                                        RIKER_PRIVATE_KEY_PASSWORD.toCharArray());
        return dotNetServerPrivateKey = output;
    }

    private static X509Certificate dotNetServerCertificate = null;
    public static synchronized X509Certificate getDotNetServerCertificate() throws Exception {
        if (dotNetServerCertificate != null) return dotNetServerCertificate;
        InputStream fis = getInputStream(SSL_CER);
        byte[] certbytes;
        try {
            certbytes = HexUtils.slurpStream(fis, 16384);
        } finally {
            fis.close();
        }
        // construct the x509 based on the bytes
        return dotNetServerCertificate = (X509Certificate)(CertificateFactory.getInstance("X.509").
                                                    generateCertificate(new ByteArrayInputStream(certbytes)));
    }
}
