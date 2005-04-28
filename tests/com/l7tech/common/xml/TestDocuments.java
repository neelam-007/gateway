package com.l7tech.common.xml;

import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Properties;

/**
 * The class is a container for test documents, SOAP tmessages etc
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public final class TestDocuments {
    public static final String DIR = "com/l7tech/service/resources/";
    private static final String PDIR = "com/l7tech/policy/resources/";
    public static final String TEST_SOAP_XML = DIR + "GetLastTradePriceSoapRequest.xml";
    public static final String WSDL2PORTS = DIR + "xmltoday-delayed-quotes-2ports.wsdl";
    public static final String WSDL = DIR + "StockQuoteService.wsdl";
    public static final String WSDL_STOCK_QUOTE = DIR + "StockQuoteService.wsdl";
    public static final String WSDL_DOC_LITERAL = DIR + "GeoServe.wsdl";
    public static final String WSDL_DOC_LITERAL2 = DIR + "QuoteService.wsdl";
    public static final String WSDL_DOC_LITERAL3 = DIR + "DeadOrAlive.wsdl";
    public static final String WSDL2SERVICES = DIR + "xmltoday-delayed-quotes-2services.wsdl";
    public static final String PLACEORDER_CLEARTEXT = DIR + "PlaceOrder_cleartext.xml";
    public static final String PLACEORDER_WITH_MAJESTY = DIR + "PlaceOrder_with_XmlRequestSecurity.xml";
    public static final String PLACEORDER_KEYS = DIR + "PlaceOrder_with_XmlRequestSecurity_keys.properties";
    public static final String BUG_763_MONSTER_POLICY = PDIR + "bug763MonsterPolicy.xml";
    public static final String WAREHOUSE_SECURED_POLICY = PDIR + "WarehouseSecuredPolicy.xml";
    public static final String DOTNET_SIGNED_REQUEST = DIR + "dotNetSignedSoapRequest.xml";
    public static final String DOTNET_SIGNED_REQUEST2 = DIR + "dotNetWseSignedRequest2.xml";
    public static final String DOTNET_SIGNED_TAMPERED_REQUEST = DIR + "dotNetSignedTamperedSoapRequest.xml";
    public static final String DOTNET_ENCRYPTED_REQUEST = DIR + "dotNetSignedAndEncryptedRequest.xml";
    public static final String DOTNET_SIGNED_USING_DERIVED_KEY_TOKEN = DIR + "dotNetSignedUsingDerivedKeyToken.xml";
    public static final String DOTNET_ENCRYPTED_USING_DERIVED_KEY_TOKEN = DIR + "dotNetSignedAndEncryptedUsingDKFromSCT.xml";
    public static final String DOTNET_USERNAME_TOKEN = DIR + "dotNetSoapRequestWithUsernameToken.xml";
    public static final String SOAP_WITH_ATTACHMENTS_REQUEST = DIR + "SOAPWithAttachmentsRequest.txt";
    public static final String ETTK_SIGNED_REQUEST = DIR + "ibmEttkSignedRequest.xml";
    public static final String ETTK_ENCRYPTED_REQUEST = DIR + "ibmEttkEncryptedRequest.xml";
    public static final String ETTK_SIGNED_ENCRYPTED_REQUEST = DIR + "ibmEttkSignedEncryptedRequest.xml";
    public static final String WEBSPHERE_SIGNED_REQUEST = DIR + "websphereSignedRequest.xml";
    public static final String BRIDGE_FAILING_SIGNED_REQUEST = DIR + "agentSignedRequest.xml";
    public static final String SAMPLE_SIGNED_SAML_HOLDER_OF_KEY_REQUEST = DIR + "sampleSignedSamlHolderOfKeyRequest.xml";
    public static final String GOOGLESEARCH_RESPONSE = DIR + "googlesearch-response.xml";
    public static final String NON_SOAP_REQUEST = DIR + "nonSoapRequest.xml";

    public static final String WRAPED_L7ACTOR = DIR + "soapRequestWithUsernameTokenAndWrappedL7Actors.xml";
    public static final String MULTIPLE_WRAPED_L7ACTOR = DIR + "soapRequestWithUsernameTokenAndMultipleL7Actors.xml";

    public static final String DOTNET_SEC_TOK_REQ = DIR + "dotNetSecurityTokenRequest.xml";

    public static final String FIM2005APR_CLIENT_RST = DIR + "fim/fim2005apr_client_rst.xml";
    public static final String FIM2005APR_CLIENT_RSTR = DIR + "fim/fim2005apr_client_rstr.xml";
    public static final String FIM2005APR_CLIENT_REQ = DIR +"fim/fim2005apr_client_req.xml";
    public static final String FIM2005APR_SERVER_RST = DIR + "fim/fim2005apr_server_rst.xml";
    public static final String FIM2005APR_SERVER_RSTR = DIR + "fim/fim2005apr_server_rstr.xml";


    private static final String ETTK_KS = DIR + "ibmEttkKeystore.db";
    private static final String ETTK_KS_PROPERTIES = DIR + "ibmEttkKeystore.properties";
    private static final String SSL_KS = DIR + "rikerssl.ks";
    private static final String SSL_CER = DIR + "rikerssl.cer";
    private static final String FIM_SIGNER_KS = DIR + "fim/fimsigner.p12";

    public static final String WEBLOGIC_WSDL = DIR + "weblogic_wsdl.xml";

    private TestDocuments() { }

    public static Document getTestDocument(String resourcetoread)
      throws IOException, SAXException {
        InputStream i = getInputStream(resourcetoread);
        return XmlUtil.parse(i);
    }

    public static String getTestDocumentAsXml(String resourcetoread)
      throws IOException, SAXException {
       return XmlUtil.nodeToString(getTestDocument(resourcetoread));
    }

    public static URL getTestDocumentURL(String resource)
      throws IOException {
        if (resource == null) {
            throw new IllegalArgumentException();
        }
        ClassLoader cl = TestDocuments.class.getClassLoader();
        URL url = cl.getResource(resource);
        if (url == null) {
            throw new FileNotFoundException(resource);
        }
        return url;
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
        return dotNetServerCertificate = CertUtils.decodeCert(certbytes);
    }

    private static X509Certificate francoCertificate = null;
    public static synchronized X509Certificate getFrancoCertificate() throws Exception {
        if (francoCertificate != null) return francoCertificate;
        InputStream fis = getInputStream(DIR + "franco.cer");
        byte[] certbytes;
        try {
            certbytes = HexUtils.slurpStream(fis, 16384);
        } finally {
            fis.close();
        }
        // construct the x509 based on the bytes
        return francoCertificate = CertUtils.decodeCert(certbytes);
    }

    private static PrivateKey francoPrivateKey = null;
    public static synchronized PrivateKey getFrancoPrivateKey() throws Exception {
        if (francoPrivateKey != null) return francoPrivateKey;
        KeyStore keyStore = KeyStore.getInstance("BCPKCS12");
        InputStream fis = getInputStream(DIR + "franco.ks");
        final String passwd = "blahblah";
        keyStore.load(fis, passwd.toCharArray());
        fis.close();
        //final String alias = "tomcat";
        PrivateKey output = (PrivateKey)keyStore.getKey("tomcat", passwd.toCharArray());
        return francoPrivateKey = output;
    }

    /** @return the SecretKey used in the .NET WS-SC derived key token examples. */
    public static SecretKey getDotNetSecureConversationSharedSecret() {
        return new SecretKey() {
            public String getAlgorithm() { return "ANY"; }
            public String getFormat() { return "RAW"; }
            public byte[] getEncoded() {
                return new byte[] {5,2,4,5,
                                   8,7,9,6,
                                   32,4,1,55,
                                   8,7,77,7};
            }
        };
    }


    private static X509Certificate fimValidationCertificate = null;
    public static synchronized X509Certificate getFimValidationCertificate() {
        if (fimValidationCertificate != null) return fimValidationCertificate;
        ByteArrayInputStream bais = new ByteArrayInputStream(FIM2005APR_VALIDATION_CERT_PEM.getBytes());
        try {
            return fimValidationCertificate = (X509Certificate)CertUtils.getFactory().generateCertificate(bais);
        } catch (CertificateException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    private static KeyStore fimSigningKeyStore = null;
    private static synchronized KeyStore getFimSigningKeyStore() throws Exception {
        if (fimSigningKeyStore != null) return fimSigningKeyStore;
        String keystorePassword = "foo";
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        InputStream fis = null;
        try {
            fis = getInputStream(FIM_SIGNER_KS);
            keyStore.load(fis, keystorePassword.toCharArray());
        } finally {
            if (fis != null)
                fis.close();
        }
        return fimSigningKeyStore = keyStore;
    }

    private static X509Certificate fimSigningCertificate = null;
    public static synchronized X509Certificate getFimSigningCertificate() throws Exception {
        if (fimSigningCertificate != null) return fimSigningCertificate;
        return fimSigningCertificate = (X509Certificate)getFimSigningKeyStore().getCertificate("ws-trust-test-cert");
    }

    private static RSAPrivateKey fimSigningPrivateKey = null;
    public static synchronized RSAPrivateKey getFimSigningPrivateKey() throws Exception {
        if (fimSigningPrivateKey != null) return fimSigningPrivateKey;
        return fimSigningPrivateKey = (RSAPrivateKey)getFimSigningKeyStore().getKey("ws-trust-test-cert", "foo".toCharArray());
    }

    public static final String FIM2005APR_VALIDATION_CERT_PEM = "-----BEGIN CERTIFICATE-----\n" +
            "MIICBzCCAXCgAwIBAgIEQH26vjANBgkqhkiG9w0BAQQFADBIMQswCQYDVQQGEwJVUzEPMA0GA1UE\n" +
            "ChMGVGl2b2xpMQ4wDAYDVQQLEwVUQU1lQjEYMBYGA1UEAxMPZmltZGVtby5pYm0uY29tMB4XDTA0\n" +
            "MDQxNDIyMjcxMFoXDTE3MTIyMjIyMjcxMFowSDELMAkGA1UEBhMCVVMxDzANBgNVBAoTBlRpdm9s\n" +
            "aTEOMAwGA1UECxMFVEFNZUIxGDAWBgNVBAMTD2ZpbWRlbW8uaWJtLmNvbTCBnzANBgkqhkiG9w0B\n" +
            "AQEFAAOBjQAwgYkCgYEAiZ0D1X6rk8+ZwNBTVZt7C85m421a8A52Ksjw40t+jNvbLYDp/W66AMMY\n" +
            "D7rB5qgniZ5K1p9W8ivM9WbPxc2u/60tFPg0e/Q/r/fxegW1K1umnay+5MaUvN3p4XUCRrfg79Ov\n" +
            "urvXQ7GZa1/wOp5vBIdXzg6i9CVAqL29JGi6GYUCAwEAATANBgkqhkiG9w0BAQQFAAOBgQBXiAhx\n" +
            "m91I4m+g3YX+dyGc352TSKO8HvAIBkHHFFwIkzhNgO+zLhxg5UMkOg12X9ucW7leZ1IB0Z6+JXBr\n" +
            "XIWmU3UPum+QxmlaE0OG9zhp9LEfzsE5+ff+7XpS0wpJklY6c+cqHj4aTGfOhSE6u7BLdI26cZNd\n" +
            "zxdhikBMZPgdyQ==\n" +
            "-----END CERTIFICATE-----";

}
