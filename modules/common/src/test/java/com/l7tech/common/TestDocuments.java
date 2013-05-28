package com.l7tech.common;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
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
    public static final String DIR = "com/l7tech/common/resources/";
    public static final String TEST_SOAP_XML = DIR + "GetLastTradePriceSoapRequest.xml";
    public static final String WSDL2PORTS = DIR + "xmltoday-delayed-quotes-2ports.wsdl";
    public static final String WSDL = DIR + "StockQuoteService.wsdl";
    public static final String WSDL_STOCK_QUOTE = DIR + "StockQuoteService.wsdl";
    public static final String WSDL_STOCK_QUOTE_INVALID_USE = DIR + "StockQuoteServiceInvalidUse.wsdl";
    public static final String WSDL_DOC_LITERAL = DIR + "GeoServe.wsdl";
    public static final String WSDL_DOC_LITERAL2 = DIR + "QuoteService.wsdl";
    public static final String WSDL_DOC_LITERAL3 = DIR + "DeadOrAlive.wsdl";
    public static final String WSDL_RPC_LITERAL = DIR + "whitemesa_rpc-lit.wsdl";
    public static final String WSDL2SERVICES = DIR + "xmltoday-delayed-quotes-2services.wsdl";
    public static final String BUG6944_WSDLS_WITH_CIRCULAR_IMPORTS_A_IMPORTS_B = DIR + "bug6944-wsdls-with-circular-imports-A_Imports_B.wsdl";
    public static final String BUG6944_WSDLS_WITH_CIRCULAR_IMPORTS_B_IMPORTS_A = DIR + "bug6944-wsdls-with-circular-imports-B_Imports_A.wsdl";
    public static final String BUG9848_CIRCULAR_WCF_WSDL1 = DIR + "wsdl1.wsdl";
    public static final String BUG9848_CIRCULAR_WCF_WSDL2 = DIR + "wsdl2.wsdl";
    public static final String PLACEORDER_CLEARTEXT = DIR + "PlaceOrder_cleartext.xml";
    public static final String PLACEORDER_CLEARTEXT_S12 = DIR + "PlaceOrder_cleartext_soap12.xml";
    public static final String PLACEORDER_CLEARTEXT_ONELINE = DIR + "PlaceOrder_cleartext_oneline.xml";
    public static final String PLACEORDER_WITH_MAJESTY = DIR + "PlaceOrder_with_XmlRequestSecurity.xml";
    public static final String PLACEORDER_KEYS = DIR + "PlaceOrder_with_XmlRequestSecurity_keys.properties";
    public static final String BUG_763_MONSTER_POLICY = DIR + "bug763MonsterPolicy.xml";
    public static final String BUG_3736_STR_TRANSFORM_REQUEST = DIR + "bug3736-str-transform.xml";
    public static final String BUG_7170_STR_TRANSFORM_X509DATA_REQUEST = DIR + "bug7170-str-transform-x509data.xml";
    public static final String BUG_3611_SIGNATURE_INCLUSIVE_NAMESPACES = DIR + "bug3611-signature-inclusive-namespaces.xml";
    public static final String WAREHOUSE_SECURED_POLICY = DIR + "WarehouseSecuredPolicy.xml";
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
    public static final String SAMPLE_SIGNED_SAML_HOLDER_OF_KEY_REQUEST = DIR + "sampleSignedSamlHolderOfKeyRequest.xml";
    public static final String GOOGLESEARCH_RESPONSE = DIR + "googlesearch-response.xml";
    public static final String NON_SOAP_REQUEST = DIR + "nonSoapRequest.xml";
    public static final String BUG_3747_DSIG_XPATH = DIR + "bug3747_dsig_xpath.xml";
    public static final String BUG_3754_PING_REQ_VORDEL_SIGNED = DIR + "ping-req-vordel-signed.xml";
    public static final String WAREHOUSE_REQUEST_UNKNOWN_BST = DIR + "warehouseRequestUnknownBST.xml";
    public static final String WAREHOUSE_REQUEST_UNKNOWN_MUSTUNDERSTAND = DIR + "warehouseRequestUnknownMustUnderstand.xml";
    public static final String WAREHOUSE_REQUEST_MULTIPLE_TIMESTAMP_SIGS = DIR + "warehouseRequestMultipleTimestampSignatures.xml";
    public static final String WAREHOUSE_REQUEST_OUT_OF_ORDER_HEADER = DIR + "warehouseRequestOutOfOrderHeader.xml";
    public static final String WAREHOUSE_REQUEST_XPOINTER_REFERENCE = DIR + "warehouseRequestXPointerRef.xml";
    public static final String WAREHOUSE_REQUEST_SAML11_DERIVED_KEYS = DIR + "warehouseRequestSAML11DerivedKeys.xml";
    public static final String WAREHOUSE_REQUEST_SAML11_DERIVED_KEYS_NO_TOKEN = DIR + "warehouseRequestSAML11DerivedKeysNoToken.xml";
    public static final String WAREHOUSE_REQUEST_SAML20_DERIVED_KEYS = DIR + "warehouseRequestSAML20DerivedKeys.xml";
    public static final String WAREHOUSE_REQUEST_SAML20_DERIVED_KEYS_NO_TOKEN = DIR + "warehouseRequestSAML20DerivedKeysNoToken.xml";

    public static final String WRAPED_L7ACTOR = DIR + "soapRequestWithUsernameTokenAndWrappedL7Actors.xml";
    public static final String MULTIPLE_WRAPED_L7ACTOR = DIR + "soapRequestWithUsernameTokenAndMultipleL7Actors.xml";

    public static final String DOTNET_SEC_TOK_REQ = DIR + "dotNetSecurityTokenRequest.xml";

    public static final String FIM2005APR_CLIENT_RST = DIR + "fim/fim2005apr_client_rst.xml";
    public static final String FIM2005APR_CLIENT_RSTR = DIR + "fim/fim2005apr_client_rstr.xml";
    public static final String FIM2005APR_CLIENT_REQ = DIR +"fim/fim2005apr_client_req.xml";
    public static final String FIM2005APR_SERVER_RST = DIR + "fim/fim2005apr_server_rst.xml";
    public static final String FIM2005APR_SERVER_RSTR = DIR + "fim/fim2005apr_server_rstr.xml";

    public static final String ECDSA_SHA256_REQUEST = DIR + "ecc/ecdsa_sha256_request.xml";
    public static final String ECDSA_SHA384_REQUEST = DIR + "ecc/ecdsa_sha384_request.xml";

    public static final String WSS2005JUL_REQUEST = DIR + "wssInterop/regeneratedRequest.xml";
    public static final String WSS2005JUL_REQUEST_ORIG = DIR + "wssInterop2005July_request.xml";
    public static final String WSS2005JUL_RESPONSE = DIR + "wssInterop2005July_response.xml";
    public static final String WSSKEYSTORE_ALICE = DIR + "wssInterop/alice.pfx";
    public static final String WSSKEYSTORE_BOB   = DIR + "wssInterop/bob.pfx";
    public static final String WSSKEYSTORE_WSSIP   = DIR + "wssInterop/wssip.pfx";

    public static final String GOOGLESPELLREQUEST_SIGNED_ISSUERSERIAL = DIR + "googleSpellRequestWithIssuerSerialSignature.xml";

    private static final String ETTK_KS = DIR + "ibmEttkKeystore.db";
    private static final String ETTK_KS_PROPERTIES = DIR + "ibmEttkKeystore.properties";
    private static final String SSL_KS = DIR + "rikerssl.ks";
    private static final String SSL_CER = DIR + "rikerssl.cer";
    private static final String FIM_SIGNER_KS = DIR + "fim/fimsigner.p12";

    public static final String WEBLOGIC_WSDL = DIR + "weblogic_wsdl.xml";

    public static final String NCES_REQ_WITH_CANCELED_XMLNS = DIR + "nces/nces_req_with_canceled_xmlns.xml";

    public static final String BUG_7685_SPLOIT_XML = DIR + "bug7685_sploit_xml.dat";
    

    public static class SecurityPolicies {
        private final String ROUND3_MS_WSDL = "wssp_interop_apr_2006/round3_ms.wsdl";
        private final String ROUND3_IBM_WSDL = "wssp_interop_apr_2006/round3_ibm.wsdl";
        private final String ROUND3_BEA_WSDL = "wssp_interop_apr_2006/round3_bea.wsdl";

        public InputStream getRound3MsWsdl() throws FileNotFoundException {
            return getInputStream(DIR + ROUND3_MS_WSDL);
        }

        public InputStream getRound3IbmWsdl() throws FileNotFoundException {
            return getInputStream(DIR + ROUND3_IBM_WSDL);
        }

        public InputStream getRound3BeaWsdl() throws FileNotFoundException {
            return getInputStream(DIR + ROUND3_BEA_WSDL);
        }
    }

    private static final SecurityPolicies SECURITYPOLICIES = new SecurityPolicies();


    private TestDocuments() { }

    public static SecurityPolicies getSecurityPolicies() {
        return SECURITYPOLICIES;
    }

    public static Document getTestDocument(String resourcetoread)
      throws IOException, SAXException {
        InputStream i = getInputStream(resourcetoread);
        return XmlUtil.parse(i);
    }

    public static String getTestDocumentAsXml(String resourcetoread) throws IOException, SAXException {
        InputStream i = getInputStream(resourcetoread);
        try {
            return new String(IOUtils.slurpStream(i));
        } finally {
            ResourceUtils.closeQuietly(i);
        }
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
        if (resourcetoread.contains("//"))
            throw new FileNotFoundException("Double slash in path will not work on some platforms (even if test passes on others)");
        ClassLoader cl = TestDocuments.class.getClassLoader();
        InputStream i = cl.getResourceAsStream(resourcetoread);
        if (i == null) {
            throw new FileNotFoundException(resourcetoread);
        }
        return i;
    }

    private static X509Certificate wssInteropAliceCert = null;
    private static PrivateKey wssInteropAliceKey = null;
    private static X509Certificate[] wssInteropAliceChain = null;
    private static X509Certificate wssInteropBobCert = null;
    private static PrivateKey wssInteropBobKey = null;
    private static X509Certificate[] wssInteropBobChain = null;
    private static X509Certificate wssInteropWssIpCert = null;
    private static PrivateKey wssInteropWssIpKey = null;
    private static synchronized void initWssInteropCerts() throws Exception {
        if (wssInteropAliceCert != null &&
            wssInteropAliceKey != null &&
            wssInteropBobCert != null &&
            wssInteropBobKey != null &&
            wssInteropWssIpCert != null &&
            wssInteropWssIpKey != null)
            return;

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(getInputStream(WSSKEYSTORE_ALICE), "password".toCharArray());
        final String aliceAlias = ks.aliases().nextElement();
        wssInteropAliceCert = (X509Certificate)ks.getCertificate(aliceAlias);
        wssInteropAliceKey = (PrivateKey)ks.getKey(aliceAlias, "password".toCharArray());
        wssInteropAliceChain = toX509Certificate(ks.getCertificateChain(aliceAlias));

        ks.load(getInputStream(WSSKEYSTORE_BOB), "password".toCharArray());
        final String bobAlias = ks.aliases().nextElement();
        wssInteropBobCert = (X509Certificate)ks.getCertificate(bobAlias);
        wssInteropBobKey = (PrivateKey)ks.getKey(bobAlias, "password".toCharArray());
        wssInteropBobChain = toX509Certificate(ks.getCertificateChain(bobAlias));

        ks.load(getInputStream(WSSKEYSTORE_WSSIP), "password".toCharArray());
        final String wssIpAlias = ks.aliases().nextElement();
        wssInteropWssIpCert = (X509Certificate)ks.getCertificate(wssIpAlias);
        wssInteropWssIpKey = (PrivateKey)ks.getKey(wssIpAlias, "password".toCharArray());

        if (wssInteropAliceCert == null ||
            wssInteropAliceKey == null ||
            wssInteropBobCert == null ||
            wssInteropBobKey == null ||
            wssInteropWssIpCert == null ||
            wssInteropWssIpKey == null)
            throw new IOException("Unable to find all keys and certs in the wss interop keystores");
    }

    public static X509Certificate[] toX509Certificate( final Certificate[] certificates ) {
        X509Certificate[] x509certs = null;

        if ( certificates != null ) {
            x509certs = new X509Certificate[certificates.length];
            for ( int c=0; c<certificates.length; c++ ) {
                x509certs[c] = (X509Certificate) certificates[c];
            }
        }

        return x509certs;
    }

    public static X509Certificate getWssInteropAliceCert() throws Exception {
        initWssInteropCerts();
        return wssInteropAliceCert;
    }

    public static PrivateKey getWssInteropAliceKey() throws Exception {
        initWssInteropCerts();
        return wssInteropAliceKey;
    }

    public static X509Certificate[] getWssInteropAliceChain() throws Exception {
        initWssInteropCerts();
        return wssInteropAliceChain;
    }

    public static X509Certificate getWssInteropBobCert() throws Exception {
        initWssInteropCerts();
        return wssInteropBobCert;
    }

    public static PrivateKey getWssInteropBobKey() throws Exception {
        initWssInteropCerts();
        return wssInteropBobKey;
    }

    public static X509Certificate[] getWssInteropBobChain() throws Exception {
        initWssInteropCerts();
        return wssInteropBobChain;
    }

    public static X509Certificate getWssInteropIpCert() throws Exception {
        initWssInteropCerts();
        return wssInteropWssIpCert;
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

    private static PrivateKey expiredDotNetServerPrivateKey = null;
    public static synchronized PrivateKey getExpiredServerPrivateKey() throws Exception {
        if (expiredDotNetServerPrivateKey != null) return expiredDotNetServerPrivateKey;
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
        return expiredDotNetServerPrivateKey = output;
    }

    private static X509Certificate expiredDotNetServerCertificate = null;
    public static synchronized X509Certificate getExpiredServerCertificate() throws Exception {
        if (expiredDotNetServerCertificate != null) return expiredDotNetServerCertificate;
        InputStream fis = getInputStream(SSL_CER);
        byte[] certbytes;
        try {
            certbytes = IOUtils.slurpStream(fis, 16384);
        } finally {
            fis.close();
        }
        // construct the x509 based on the bytes
        return expiredDotNetServerCertificate = CertUtils.decodeCert(certbytes);
    }

    // Old one expired -- use Franco one instead now
    public static X509Certificate getDotNetServerCertificate() throws Exception {
        return getFrancoCertificate();
    }

    private static X509Certificate francoCertificate = null;
    public static synchronized X509Certificate getFrancoCertificate() throws Exception {
        if (francoCertificate != null) return francoCertificate;
        InputStream fis = getInputStream(DIR + "franco.cer");
        byte[] certbytes;
        try {
            certbytes = IOUtils.slurpStream(fis, 16384);
        } finally {
            fis.close();
        }
        // construct the x509 based on the bytes
        return francoCertificate = CertUtils.decodeCert(certbytes);
    }

    // Old one expired -- use Franco one instead now
    public static PrivateKey getDotNetServerPrivateKey() throws Exception {
        return getFrancoPrivateKey();
    }

    private static PrivateKey francoPrivateKey = null;
    public static synchronized PrivateKey getFrancoPrivateKey() throws Exception {
        if (francoPrivateKey != null) return francoPrivateKey;
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        InputStream fis = getInputStream(DIR + "franco.ks");
        final String passwd = "password";
        keyStore.load(fis, passwd.toCharArray());
        fis.close();
        //final String alias = "tomcat";
        PrivateKey output = (PrivateKey)keyStore.getKey("tomcat", passwd.toCharArray());
        return francoPrivateKey = output;
    }

    /** @return the SecretKey used in the .NET WS-SC derived key token examples. */
    public static byte[] getDotNetSecureConversationSharedSecret() {
        return new byte[] {5,2,4,5,
                           8,7,9,6,
                           32,4,1,55,
                           8,7,77,7};
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

    public static KeyStore getMockSSLServerKeyStore() throws Exception {
        //Created by  keytool -genkey -alias alias -keypass 7layer] -keystore layer7.keystore -storepass 7layer]
        char[] password = "7layer]".toCharArray();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        InputStream is = TestDocuments.getInputStream(TestDocuments.DIR + "layer7.db");
        keyStore.load(is, password);
        return keyStore;
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
