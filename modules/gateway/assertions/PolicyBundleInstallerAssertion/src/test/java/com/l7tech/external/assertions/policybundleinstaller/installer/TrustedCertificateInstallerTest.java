package com.l7tech.external.assertions.policybundleinstaller.installer;

import com.l7tech.common.io.XmlUtil;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test Trusted Certificate Installer.
 */
public class TrustedCertificateInstallerTest {
    public static final String CERTIFICATE_ENUM_DOC_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<enumeration>\n" +
            "    <l7:TrustedCertificate xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" id=\"13762563\" version=\"2\">\n" +
            "        <l7:Name>TestBundleCertificateInstallation1</l7:Name>\n" +
            "        <l7:CertificateData>\n" +
            "            <l7:IssuerName>CN=bug10862</l7:IssuerName>\n" +
            "            <l7:SerialNumber>230710867597964687</l7:SerialNumber>\n" +
            "            <l7:SubjectName>CN=bug10862</l7:SubjectName>\n" +
            "            <l7:Encoded>MIIDDjCCAfagAwIBAgIIAzOmT8z/tY8wDQYJKoZIhvcNAQEMBQAwEzERMA8GA1UEAxMIYnVnMTA4NjIwHhcNMTExMDIwMTcxNDI5WhcNMTYxMDE4MTcxNDI5WjATMREwDwYDVQQDEwhidWcxMDg2MjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJMlz2GdevXJPtBayOlhxISvH7vpfHR37k/zjBg21Wqy6H2Zj2M/HxxF0PZtI9rHNvUq+Rz0umor0G16APUmuCy26Z7fdwQcXdZW3WAOQxh4x3GUFHbVUuAN4ulngDujb5h3RqSlk9ECtuCe1e5aCQuA9IXOFYMkRVAO0YnLBuja+DTArb0ofGi+bBhe8mCR2jwHZWatAVkzsFVU4ehA4M8OhK6N5EwuF/6K3djeHSh8xSQ7jBkRJOkwViXqnAiJdDEei5H5LIobkV1as4iHPiJqAZCmIjbHSCUFqV5ydXOWULNyQmJqYtCTm8IYdZBiVCgHkOxw10BU8IFoFK01kwMCAwEAAaNmMGQwEgYDVR0TAQH/BAgwBgEB/wIBATAOBgNVHQ8BAf8EBAMCAQYwHQYDVR0OBBYEFNXbtoW6w9Da5Zionv/nFERvutWUMB8GA1UdIwQYMBaAFNXbtoW6w9Da5Zionv/nFERvutWUMA0GCSqGSIb3DQEBDAUAA4IBAQBTbVixlHuAPOngo6l1xS/CcgO9hLn3dGGnww6UBNVuODepiXLvbvmA8UhqCxqhn4LIo37z/pl29NXouautIxbLlT3YOFwPi9kipLkAeJeIVgQw1hcCsx+zjtw911opYZs/UmHS46pI9jdT9gGKzcuLLKW/R9EfyCGGSFMBrE4JOzHfLDBIJ5uAt4Qh83wVYxz7h12cnCNdf/Jsn1VTMI85Vx+FFiqVNXZKlmeAUU6uUyZR1bx6DE4mEkeZ2OMnGDmQJD3G0uJqDE4au4U+1pG8V88hqDNJelWl2gb/ZwGyuZPDNuUAtQQxtg8BmzdvJ1ihm5hNh+cAPNakRxIsXDIa</l7:Encoded>\n" +
            "        </l7:CertificateData>\n" +
            "        <l7:Properties>\n" +
            "            <l7:Property key=\"verifyHostname\">\n" +
            "                <l7:BooleanValue>false</l7:BooleanValue>\n" +
            "            </l7:Property>\n" +
            "            <l7:Property key=\"trustAnchor\">\n" +
            "                <l7:BooleanValue>false</l7:BooleanValue>\n" +
            "            </l7:Property>\n" +
            "            <l7:Property key=\"trustedForSigningServerCerts\">\n" +
            "                <l7:BooleanValue>false</l7:BooleanValue>\n" +
            "            </l7:Property>\n" +
            "            <l7:Property key=\"trustedForSsl\">\n" +
            "                <l7:BooleanValue>true</l7:BooleanValue>\n" +
            "            </l7:Property>\n" +
            "            <l7:Property key=\"trustedForSigningClientCerts\">\n" +
            "                <l7:BooleanValue>true</l7:BooleanValue>\n" +
            "            </l7:Property>\n" +
            "            <l7:Property key=\"revocationCheckingEnabled\">\n" +
            "                <l7:BooleanValue>true</l7:BooleanValue>\n" +
            "            </l7:Property>\n" +
            "            <l7:Property key=\"trustedAsSamlAttestingEntity\">\n" +
            "                <l7:BooleanValue>false</l7:BooleanValue>\n" +
            "            </l7:Property>\n" +
            "            <l7:Property key=\"trustedAsSamlIssuer\">\n" +
            "                <l7:BooleanValue>true</l7:BooleanValue>\n" +
            "            </l7:Property>\n" +
            "        </l7:Properties>\n" +
            "    </l7:TrustedCertificate>\n" +
            "    <l7:TrustedCertificate xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" id=\"13762566\" version=\"1\">\n" +
            "        <l7:Name>TestBundleCertificateInstallation2</l7:Name>\n" +
            "        <l7:CertificateData>\n" +
            "            <l7:IssuerName>CN=test8424</l7:IssuerName>\n" +
            "            <l7:SerialNumber>4276451065706719612</l7:SerialNumber>\n" +
            "            <l7:SubjectName>CN=test8424</l7:SubjectName>\n" +
            "            <l7:Encoded>MIIDDjCCAfagAwIBAgIIO1kBli7KKXwwDQYJKoZIhvcNAQEMBQAwEzERMA8GA1UEAxMIdGVzdDg0MjQwHhcNMTAwMzAxMjIwNTU1WhcNMTUwMjI4MjIwNTU1WjATMREwDwYDVQQDEwh0ZXN0ODQyNDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAINo6PySfoCCMica0ygwDi7g17V+0SV3pqz9yDlWtqgfeXr0N0bAFoaY/koTZzdgc0eri93h9aVENqIwAKEWMK9pSxE7R6NWolwItmnosXPe1RtYcUXnp/jsHAIFt8qHliY01LMZAnDS2Q3Md3/CW7ATnVwwddPH0dJK+FTG7+W78oMp4OP3Fm6QzVVelwnU4rIDNIA5fhUujhTvSYQMLVf27UKKWfGPBtmgyCVpWegv1jY7ekrq4NpkoImgMo2zOkcjfJQc4ORBdK3Vgpto/sBp10ZM/ds6NVAFWSRnfLzR3cW/+XDsrfRFlO2bq0BJ8JK0Pywc+3wLFCqTEzjN4+sCAwEAAaNmMGQwEgYDVR0TAQH/BAgwBgEB/wIBATAOBgNVHQ8BAf8EBAMCAQYwHQYDVR0OBBYEFA/kW1IGvWZVWDUjStB5m7BgUO5FMB8GA1UdIwQYMBaAFA/kW1IGvWZVWDUjStB5m7BgUO5FMA0GCSqGSIb3DQEBDAUAA4IBAQAl32+OJgJ849e29v04KIN9GnXDQ9LJkehXmivvA8pdQzWchRKAh7/BHYW/ZHmXahh517zvNDWvDyx8Qk6jpe65V1qVLmlcPU4zlpWhcWNNEjuB4OtR8sEYXv+5HKrC3KqgmUcyCoHaVm6p+uQ6ZVam45kR5OYnW6MmKsopV52FwRiPJC5xNTgBQXkqeBQ2EvgdL1E7xqS+Y3IuEuCkbw8jAYgjBRt0iMy3SYzS1QxaiZvcZSvtm+S8oZGGiCA/p+oURfjNnFBXSqC+qSR9WhyPRgOlPAXjsHo8CUOg00EJ3c2xIog4Q5HNZTCO+vFPrax7pwhEw8GFUlpYL5Iu6i03</l7:Encoded>\n" +
            "        </l7:CertificateData>\n" +
            "        <l7:Properties>\n" +
            "            <l7:Property key=\"verifyHostname\">\n" +
            "                <l7:BooleanValue>false</l7:BooleanValue>\n" +
            "            </l7:Property>\n" +
            "            <l7:Property key=\"trustAnchor\">\n" +
            "                <l7:BooleanValue>false</l7:BooleanValue>\n" +
            "            </l7:Property>\n" +
            "            <l7:Property key=\"trustedForSigningServerCerts\">\n" +
            "                <l7:BooleanValue>true</l7:BooleanValue>\n" +
            "            </l7:Property>\n" +
            "            <l7:Property key=\"trustedForSsl\">\n" +
            "                <l7:BooleanValue>true</l7:BooleanValue>\n" +
            "            </l7:Property>\n" +
            "            <l7:Property key=\"trustedForSigningClientCerts\">\n" +
            "                <l7:BooleanValue>true</l7:BooleanValue>\n" +
            "            </l7:Property>\n" +
            "            <l7:Property key=\"revocationCheckingEnabled\">\n" +
            "                <l7:BooleanValue>true</l7:BooleanValue>\n" +
            "            </l7:Property>\n" +
            "            <l7:Property key=\"trustedAsSamlAttestingEntity\">\n" +
            "                <l7:BooleanValue>true</l7:BooleanValue>\n" +
            "            </l7:Property>\n" +
            "            <l7:Property key=\"trustedAsSamlIssuer\">\n" +
            "                <l7:BooleanValue>true</l7:BooleanValue>\n" +
            "            </l7:Property>\n" +
            "        </l7:Properties>\n" +
            "    </l7:TrustedCertificate>\n" +
            "</enumeration>";

    @Test
    public void testFindCertificateSerialNumbersAndNamesFromEnumeration() {
        final Document certificateEnumDoc = XmlUtil.stringAsDocument(CERTIFICATE_ENUM_DOC_XML);
        final Map<Element, Element> certificatesMap = TrustedCertificateInstaller.findCertificateSerialNumbersAndNamesFromEnumeration(certificateEnumDoc);
        assertEquals(2, certificatesMap.size());

        final Collection<Element> nameElmts = certificatesMap.values();
        for (Element nameElmt: nameElmts) {
            String certName = XmlUtil.getTextValue(nameElmt);
            assertTrue(certName.equals("TestBundleCertificateInstallation1") || certName.equals("TestBundleCertificateInstallation2"));
        }

        final Collection<Element> serialNumElmts = certificatesMap.keySet();
        for (Element serialNumElmt: serialNumElmts) {
            String certSerialNum = XmlUtil.getTextValue(serialNumElmt);
            assertTrue(certSerialNum.equals("230710867597964687") || certSerialNum.equals("4276451065706719612"));
        }
    }
}
