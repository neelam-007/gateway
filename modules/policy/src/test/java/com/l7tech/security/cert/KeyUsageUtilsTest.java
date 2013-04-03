package com.l7tech.security.cert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.test.BugId;
import com.l7tech.util.HexUtils;
import org.junit.Test;

import static com.l7tech.common.TestDocuments.getWssInteropAliceCert;
import static com.l7tech.security.cert.KeyUsageUtils.isCertSslCapable;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * JUnit test for key usage
 */
public class KeyUsageUtilsTest {

    private static final String CA_CERT =
            "MIIDizCCAnOgAwIBAgIQWaCxRe3INcSU8VNJ4/HerDANBgkqhkiG9w0BAQUFADAyMQ4wDAYDVQQK\n" +
            "DAVPQVNJUzEgMB4GA1UEAwwXT0FTSVMgSW50ZXJvcCBUZXN0IFJvb3QwHhcNMDUwMzE5MDAwMDAw\n" +
            "WhcNMTkwMzE5MjM1OTU5WjAwMQ4wDAYDVQQKDAVPQVNJUzEeMBwGA1UEAwwVT0FTSVMgSW50ZXJv\n" +
            "cCBUZXN0IENBMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmR2GR3IduCfoZfvmwYpe\n" +
            "pKNZN6iaDcm4JmqqC3nN5NiuQ4ROq2YCRhG90QW8puhsO6XaRiRO6WQQpwdtm/tgseDAAdw0bMPW\n" +
            "rnjaFhgFlaEB0eK5fu9UiCPGkwurWNc8EQlk2r71uCwOx6BYGFsnSnBEfj64zoVri2olksXc2aos\n" +
            "6urhujP6zvixsCxfo8Jq2v1yLUZpDaiTp2GfyDMSZKROcBz4FnEIN7yKZDMYpHSx2SmcwmQnjeeA\n" +
            "x1EH876+PpycsbJwStt3lIYchk5vWqJSZzN7PElEgzLWv8QeWZ0Zb8wteQyWrG5wN2FCTcqF3W29\n" +
            "FBeZig6u5Y3mibwDYQIDAQABo4GeMIGbMBIGA1UdEwEB/wQIMAYBAf8CAQAwNQYDVR0fBC4wLDAq\n" +
            "oiiGJmh0dHA6Ly9pbnRlcm9wLmJidGVzdC5uZXQvY3JsL3Jvb3QuY3JsMA4GA1UdDwEB/wQEAwIB\n" +
            "BjAdBgNVHQ4EFgQUwJ0o/MHrNaEd1qqqoBwaTcJJDw8wHwYDVR0jBBgwFoAU3/6RlcdWSCY9wNw5\n" +
            "PcYJ90z6SOIwDQYJKoZIhvcNAQEFBQADggEBADvsOGOmhnxjwW2+2c17/W7o4BolmqqlVFppFyEB\n" +
            "4pUd+kqJ3XFiyVxweVwGdJfpUQLKP/KBzpqo4D11ttMaE2ioat0RUGylAl9PG/yalOH/vMgFq4Xk\n" +
            "hokoHPPD1tUbiuY8+pD+5jXR0NNj25yv7iSutZ7xA7bcMx+RQpDO9Mzhlk03SZt5FjsLrimLiEOt\n" +
            "kTkBt8Gw1wCu253+Bt5JHboBhgEa9hTmdQ3hYqO/q54Gymmd/NsNCxZDbUxVqu/XzBxZer6AQ4do\n" +
            "mv5fc9efCOk0k06aMmYjKXEYI5i9OqutWu442ZXJV6lnWKZ1akFi/sA4DNnYPrz825+hzOeesBI=";

    public static final String CRIT_EXT_CERT =
            "MIIC8TCCAlqgAwIBAgIBAjANBgkqhkiG9w0BAQUFADBsMQswCQYDVQQGEwJDQTELMAkGA1UECBMC\n" +
            "QkMxDjAMBgNVBAcTBVZDSVRZMQswCQYDVQQKEwJMNzEVMBMGA1UEAxMMTGF5ZXIgNyBURUNIMRww\n" +
            "GgYJKoZIhvcNAQkBFg1sN0BsN3RlY2guY29tMB4XDTEzMDIxMjE5NDMzNFoXDTEzMDMxNDE5NDMz\n" +
            "NFowazELMAkGA1UEBhMCQ0ExCzAJBgNVBAgTAkJDMQswCQYDVQQHEwJWQzELMAkGA1UEChMCTDcx\n" +
            "FzAVBgNVBAMTDnd3dy5sN3RlY2guY29tMRwwGgYJKoZIhvcNAQkBFg1sN0BsN3RlY2guY29tMIGf\n" +
            "MA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCptOq8RVgOwfGolgkB0jnrOh+RFPEQM0OZUOm2+A9y\n" +
            "X16ZswJUlqpQLK/Uw+dGCMR1Q2f+fbwL9vk32jxJMPTJrKTF2GayafeP0Mx5B36w4YsYd1xc0zTq\n" +
            "pl2i2v4ljwyaCDX+FXYbaeYLBKJwWFueJggj0tKI2/vfGxLJnDpn+wIDAQABo4GjMIGgMAwGA1Ud\n" +
            "EwEB/wQCMAAwHQYDVR0OBBYEFJJjewRKUEPfzJQjLj6qRK6vzjCPMB8GA1UdIwQYMBaAFL4QgMiJ\n" +
            "/tGzYldZFWUA7aVnSPi2MA4GA1UdDwEB/wQEAwIFoDAgBgNVHSUBAf8EFjAUBggrBgEFBQcDAgYI\n" +
            "KwYBBQUHAwEwHgYDVR0RBBcwFYIKbGF5ZXI3LmRldoIHZGV2NC5sNzANBgkqhkiG9w0BAQUFAAOB\n" +
            "gQAJVPIO09fCDXMm/WD8YxSzIKgkDCigi9yGhlx3VA71ggES4yTgbN6pgd5vuOcDS1yAtklsVWN7\n" +
            "/xKUON2hCvyLvTJYHKzZA72HHQh7xxfUOgyVX5/cBmx9zLgghpDkoDATdkl9uFyD8LaRrZKay/CQ\n" +
            "jdXHG8mwoIDewGygYNZ3MA==";

    @Test
    public void testKeyUsageRsa() throws Exception {
        assertTrue( "Usage permitted", isCertSslCapable( getWssInteropAliceCert() ) );
    }

    @Test
    public void testKeyUsageRsaCa() throws Exception {
        assertFalse( "Usage permitted", isCertSslCapable( CertUtils.decodeCert( HexUtils.decodeBase64( CA_CERT ) ) ) );
    }

    @Test
    @BugId("SSG-6504")
    public void testKeyUsageCritEku() throws Exception {
        assertTrue( "Usage permitted", isCertSslCapable( CertUtils.decodeCert( HexUtils.decodeBase64( CRIT_EXT_CERT ) ) ) );
    }
}
