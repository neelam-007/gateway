package com.l7tech.security.cert;

import static com.l7tech.common.TestDocuments.getWssInteropAliceCert;
import com.l7tech.common.io.CertUtils;
import static com.l7tech.security.cert.KeyUsageUtils.isCertSslCapable;
import com.l7tech.util.HexUtils;
import static org.junit.Assert.*;
import org.junit.Test;

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

    @Test
    public void testKeyUsageRsa() throws Exception {
        assertTrue( "Usage permitted", isCertSslCapable( getWssInteropAliceCert() ) );
    }

    @Test
    public void testKeyUsageRsaCa() throws Exception {
        assertFalse( "Usage permitted", isCertSslCapable( CertUtils.decodeCert( HexUtils.decodeBase64( CA_CERT ) ) ) );
    }
}
