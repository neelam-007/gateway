package com.l7tech.security.prov.rsa;

import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.Security;
import java.security.Provider;
import java.io.ByteArrayInputStream;

/**
 *
 */
public class RsaReproCertificateFactoryVersionBug {
    private static final String CLASSNAME_PROVIDER = "com.rsa.jsafe.provider.JsafeJCE";
    private static final String CERTIFICATE = "-----BEGIN CERTIFICATE-----\n" +
            "MIIB3DCCAUWgAwIBAgIDGdGfMA0GCSqGSIb3DQEBBQUAMBExDzANBgNVBAMTBnNq\n" +
            "b25lczAeFw0wOTA1MTQyMjUwNTlaFw0xNDA1MTMyMjUwNTlaMBExDzANBgNVBAMT\n" +
            "BnNqb25lczCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEA9pAY/Nxfwu/ddNsA\n" +
            "zl/PqlBmRp2XIeaLmtN24/w+JwS8l9yzSiCquU7HRU/Ru4cMQJJkZWVK0BpLPJy4\n" +
            "FkMq/OdLzwpoQCs+WnmgRQjFYLMWbz64BqkGzDNJqtcdpJ9Z9I62ghhMb9aycAMg\n" +
            "POBvVaf1X/bxS7YQnrG847ICEpUCAwEAAaNCMEAwHQYDVR0OBBYEFERtS4O46o8M\n" +
            "A/KmMmnGbOdMfRFpMB8GA1UdIwQYMBaAFERtS4O46o8MA/KmMmnGbOdMfRFpMA0G\n" +
            "CSqGSIb3DQEBBQUAA4GBAJLfpagKIV7FUy84YdxZed2zgQQ3aUaeNHZitHk1gnzm\n" +
            "9qBNVKQ1ixmPwDrl7jkfY7f1k6eeqXah5vxcAqNhgmkl60rqBrkyWNZKvd5zcduG\n" +
            "X1ll57iLyQo/apOGAW0ObyPA7MbhewVTr/LoLHpgb5nQ7R0WVxpYDOMf3hOiDRSa\n" +
            "-----END CERTIFICATE-----";

    public static void main( String[] args ) throws Exception {
        generateAndPrint();

        Provider provider = (Provider)RsaReproCertificateFactoryVersionBug.class.getClassLoader().loadClass(CLASSNAME_PROVIDER).newInstance();
        Security.insertProviderAt(provider, 1);

        generateAndPrint();
    }

    private static void generateAndPrint() throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance( "X.509" );
        Certificate cert = factory.generateCertificate( new ByteArrayInputStream(CERTIFICATE.getBytes( "UTF-8" )) );
        System.out.println( factory.getProvider().getName() + ", version is : " + ((X509Certificate)cert).getVersion() );
    }
}
