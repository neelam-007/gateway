/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.util;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.security.MockGenericHttpClient;

import org.junit.Test;
import org.junit.Assert;

import java.net.URL;
import java.io.IOException;
import java.security.cert.X509Certificate;

/**
 * Test certificate downloading.  The SSG must be running on sybok:8080, and must contain
 * a user "testuser" with the password "testpassword".
 */
public class CertificateDownloaderTest {

    public static void main( String[] args ) throws Exception {
        String user = "mike";
        String pass = "asdfasdf";

        CertificateDownloader cd = new CertificateDownloader(new SimpleHttpClient(new UrlConnectionHttpClient()),
                                                             new URL("http://fish:8080"),
                                                             user,
                                                             pass.toCharArray());
        X509Certificate cert = cd.downloadCertificate();
        System.out.println("Certificate DN       : " + (cert == null ? "<NULL>" : cert.getSubjectDN()));
        System.out.println("Certificate is valid?: " + (cert == null ? "<NULL>" :  cd.isValidCert()));
        System.out.println("Password checked?    : " + (cert == null ? "<NULL>" :  cd.isUncheckablePassword()));
    }

    @Test
    public void testUserUnknown() throws Exception {
        CertificateDownloader cd = new CertificateDownloader(getClient( unknownUserBody, unknownUserCheck ),
                                                             new URL("http://fish:8080"),
                                                             "tesasdfasdftuser",
                                                             "testpagergassword".toCharArray());
        Assert.assertNotNull(cd.downloadCertificate());
        Assert.assertFalse(cd.isValidCert());
        Assert.assertTrue(cd.isUncheckablePassword());
  }

    @Test
    public void testSuccess() throws Exception {
        CertificateDownloader cd = new CertificateDownloader(getClient( successBody, successCheck ),
                                                             new URL("http://fish:8080"),
                                                             "mike",
                                                             "asdfasdf".toCharArray()){
            protected String getNonce() {
                return "3151956493247494442";
            }
        };
        Assert.assertNotNull(cd.downloadCertificate());
        Assert.assertTrue(cd.isValidCert());
        Assert.assertFalse(cd.isUncheckablePassword());
    }

    @Test
    public void testFailureCheck() throws Exception {
        CertificateDownloader cd = new CertificateDownloader(getClient( failureCertCheckInvalidBody, failureCertCheckInvalidCheck ),
                                                             new URL("http://fish:8080"),
                                                             "mike",
                                                             "asdfasdf".toCharArray());
        Assert.assertNotNull(cd.downloadCertificate());
        Assert.assertFalse(cd.isValidCert());
        Assert.assertFalse(cd.isUncheckablePassword());
    }

    @Test
    public void testFailureCert() throws Exception {
        CertificateDownloader cd = new CertificateDownloader(getClient( failureWrongCertBody, failureWrongCertCheck ),
                                                             new URL("http://fish:8080"),
                                                             "mike",
                                                             "asdfasdf".toCharArray());
        Assert.assertNotNull(cd.downloadCertificate());
        Assert.assertFalse(cd.isValidCert());
        Assert.assertFalse(cd.isUncheckablePassword());
    }

    private SimpleHttpClient getClient( final String body,
                                         final String checkValue ) throws IOException {
        byte [] data = body.getBytes( "UTF-8" );
        /*
          int status
        , HttpHeaders headers
        , ContentTypeHeader contentTypeHeader
        , Long contentLength
        , byte[] body
         */
        return new SimpleHttpClient( new MockGenericHttpClient(
                200,
                checkValue == null ?
                    new GenericHttpHeaders(new HttpHeader[0] ) :
                    new GenericHttpHeaders( new HttpHeader[]{
                        new GenericHttpHeader( "L7-Cert-Check--2", checkValue )
                    } ),
                ContentTypeHeader.parseValue("application/x-x509-server-cert"),
                (long) data.length,
                data ) );
    }

    private static final String failureWrongCertCheck = "8ec080bb85e5eb8a5111ae1d3a046038; L7SSGDigestRealm";
    private static final String failureWrongCertBody =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIDDDCCAfSgAwIBAgIQM6YEf7FVYx/tZyEXgVComTANBgkqhkiG9w0BAQUFADAw\n" +
            "MQ4wDAYDVQQKDAVPQVNJUzEeMBwGA1UEAwwVT0FTSVMgSW50ZXJvcCBUZXN0IENB\n" +
            "MB4XDTA1MDMxOTAwMDAwMFoXDTE4MDMxOTIzNTk1OVowQjEOMAwGA1UECgwFT0FT\n" +
            "SVMxIDAeBgNVBAsMF09BU0lTIEludGVyb3AgVGVzdCBDZXJ0MQ4wDAYDVQQDDAVB\n" +
            "bGljZTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAoqi99By1VYo0aHrkKCNT\n" +
            "4DkIgPL/SgahbeKdGhrbu3K2XG7arfD9tqIBIKMfrX4Gp90NJa85AV1yiNsEyvq+\n" +
            "mUnMpNcKnLXLOjkTmMCqDYbbkehJlXPnaWLzve+mW0pJdPxtf3rbD4PS/cBQIvtp\n" +
            "jmrDAU8VsZKT8DN5Kyz+EZsCAwEAAaOBkzCBkDAJBgNVHRMEAjAAMDMGA1UdHwQs\n" +
            "MCowKKImhiRodHRwOi8vaW50ZXJvcC5iYnRlc3QubmV0L2NybC9jYS5jcmwwDgYD\n" +
            "VR0PAQH/BAQDAgSwMB0GA1UdDgQWBBQK4l0TUHZ1QV3V2QtlLNDm+PoxiDAfBgNV\n" +
            "HSMEGDAWgBTAnSj8wes1oR3WqqqgHBpNwkkPDzANBgkqhkiG9w0BAQUFAAOCAQEA\n" +
            "BTqpOpvW+6yrLXyUlP2xJbEkohXHI5OWwKWleOb9hlkhWntUalfcFOJAgUyH30TT\n" +
            "pHldzx1+vK2LPzhoUFKYHE1IyQvokBN2JjFO64BQukCKnZhldLRPxGhfkTdxQgdf\n" +
            "5rCK/wh3xVsZCNTfuMNmlAM6lOAg8QduDah3WFZpEA0s2nwQaCNQTNMjJC8tav1C\n" +
            "Br6+E5FAmwPXP7pJxn9Fw9OXRyqbRA4v2y7YpbGkG2GI9UvOHw6SGvf4FRSthMMO\n" +
            "35YbpikGsLix3vAsXWWi4rwfVOYzQK0OFPNi9RMCUdSH06m9uLWckiCxjos0FQOD\n" +
            "ZE9l4ATGy9s9hNVwryOJTw==\n" +
            "-----END CERTIFICATE-----";

    private static final String failureCertCheckInvalidCheck = "8ec080bb85e5eb8a5111ae1d3e046038; L7SSGDigestRealm";
    private static final String failureCertCheckInvalidBody =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIICFjCCAX+gAwIBAgIIF5tFbwyB34AwDQYJKoZIhvcNAQEFBQAwHzEdMBsGA1UEAwwUcm9vdC5m\r\n" +
            "aXNoLmw3dGVjaC5jb20wHhcNMDgwNjAzMTkzODAzWhcNMTAwNjAzMTk0ODAzWjAaMRgwFgYDVQQD\r\n" +
            "DA9maXNoLmw3dGVjaC5jb20wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAK/2LY7wvbm+8Ijv\r\n" +
            "Sq7YTWYf602H+zZVXMPyPqV1bMjYLWj7OnpguLoOnYw5XWXZMhenaw1VLrgVUnGKtiMmk4NNBBpe\r\n" +
            "Axi9gmHyX1rFL0sVJRcnwxI5Gnmb2d9p5sHHBOM/PJesPfEtZ8Phs+dd0yZZwpQmYgunkQ+KMjL3\r\n" +
            "PPoRAgMBAAGjYDBeMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgXgMB0GA1UdDgQWBBTY3J+o\r\n" +
            "gUMTM3Qrlpc4l6Zq3Z+qRzAfBgNVHSMEGDAWgBTQxJhFth4Mt/1md9omK9U6zUPYxTANBgkqhkiG\r\n" +
            "9w0BAQUFAAOBgQBRNcxFUIJD6/WdmmGam9o4+cPN2jaXS964JltkOs0LGU1hf1xqMa7yi18usVHq\r\n" +
            "lanTFQdeWV6exJ+QHjdz1woME6wCsV9VHujpyzOZSSFusU4KaYQNZVprKET7UC75v29Sh85wPr20\r\n" +
            "QirpPd1YWRTSM7BNa9g7xpNLAFHF0HJJwg==\n" +
            "-----END CERTIFICATE-----";

    private static final String unknownUserCheck = null;
    private static final String unknownUserBody =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIICFjCCAX+gAwIBAgIIF5tFbwyB34AwDQYJKoZIhvcNAQEFBQAwHzEdMBsGA1UEAwwUcm9vdC5m\r\n" +
            "aXNoLmw3dGVjaC5jb20wHhcNMDgwNjAzMTkzODAzWhcNMTAwNjAzMTk0ODAzWjAaMRgwFgYDVQQD\r\n" +
            "DA9maXNoLmw3dGVjaC5jb20wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAK/2LY7wvbm+8Ijv\r\n" +
            "Sq7YTWYf602H+zZVXMPyPqV1bMjYLWj7OnpguLoOnYw5XWXZMhenaw1VLrgVUnGKtiMmk4NNBBpe\r\n" +
            "Axi9gmHyX1rFL0sVJRcnwxI5Gnmb2d9p5sHHBOM/PJesPfEtZ8Phs+dd0yZZwpQmYgunkQ+KMjL3\r\n" +
            "PPoRAgMBAAGjYDBeMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgXgMB0GA1UdDgQWBBTY3J+o\r\n" +
            "gUMTM3Qrlpc4l6Zq3Z+qRzAfBgNVHSMEGDAWgBTQxJhFth4Mt/1md9omK9U6zUPYxTANBgkqhkiG\r\n" +
            "9w0BAQUFAAOBgQBRNcxFUIJD6/WdmmGam9o4+cPN2jaXS964JltkOs0LGU1hf1xqMa7yi18usVHq\r\n" +
            "lanTFQdeWV6exJ+QHjdz1woME6wCsV9VHujpyzOZSSFusU4KaYQNZVprKET7UC75v29Sh85wPr20\r\n" +
            "QirpPd1YWRTSM7BNa9g7xpNLAFHF0HJJwg==\n" +
            "-----END CERTIFICATE-----" ;

    private static final String successCheck = "d6cbc06de2280d1481aea1fce2dfd208; L7SSGDigestRealm";
    private static final String successBody =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIICFjCCAX+gAwIBAgIIF5tFbwyB34AwDQYJKoZIhvcNAQEFBQAwHzEdMBsGA1UEAwwUcm9vdC5m\r\n" +
            "aXNoLmw3dGVjaC5jb20wHhcNMDgwNjAzMTkzODAzWhcNMTAwNjAzMTk0ODAzWjAaMRgwFgYDVQQD\r\n" +
            "DA9maXNoLmw3dGVjaC5jb20wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAK/2LY7wvbm+8Ijv\r\n" +
            "Sq7YTWYf602H+zZVXMPyPqV1bMjYLWj7OnpguLoOnYw5XWXZMhenaw1VLrgVUnGKtiMmk4NNBBpe\r\n" +
            "Axi9gmHyX1rFL0sVJRcnwxI5Gnmb2d9p5sHHBOM/PJesPfEtZ8Phs+dd0yZZwpQmYgunkQ+KMjL3\r\n" +
            "PPoRAgMBAAGjYDBeMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgXgMB0GA1UdDgQWBBTY3J+o\r\n" +
            "gUMTM3Qrlpc4l6Zq3Z+qRzAfBgNVHSMEGDAWgBTQxJhFth4Mt/1md9omK9U6zUPYxTANBgkqhkiG\r\n" +
            "9w0BAQUFAAOBgQBRNcxFUIJD6/WdmmGam9o4+cPN2jaXS964JltkOs0LGU1hf1xqMa7yi18usVHq\r\n" +
            "lanTFQdeWV6exJ+QHjdz1woME6wCsV9VHujpyzOZSSFusU4KaYQNZVprKET7UC75v29Sh85wPr20\r\n" +
            "QirpPd1YWRTSM7BNa9g7xpNLAFHF0HJJwg==\n" +
            "-----END CERTIFICATE-----\n";

}
