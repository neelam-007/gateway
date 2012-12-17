package com.l7tech.objectmodel.encass;

import com.l7tech.common.TestKeys;
import com.l7tech.message.Message;
import com.l7tech.policy.variable.DataType;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import sun.security.x509.X509CertImpl;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.*;

import static org.junit.Assert.*;

public class EncapsulatedAssertionStringEncodingTest {
    private static final Calendar CALENDAR = new GregorianCalendar(2012, Calendar.DECEMBER, 1, 1, 1, 1);
    private X509Certificate cert;

    @Before
    public void setup() {
        CALENDAR.setTimeZone(TimeZone.getTimeZone("UTC"));
        CALENDAR.set(Calendar.MILLISECOND, 0);
        cert = TestKeys.getCertAndKey("RSA_1024").left;
    }

    @Test
    public void encodeBinaryToString() {
        assertEquals("dGVzdA==", EncapsulatedAssertionStringEncoding.encodeToString(DataType.BINARY, "test".getBytes()));
    }

    @Test
    public void encodeBinaryToStringInvalid() {
        assertNull(EncapsulatedAssertionStringEncoding.encodeToString(DataType.BINARY, "notbinary"));
        assertNull(EncapsulatedAssertionStringEncoding.encodeToString(DataType.BINARY, null));
    }

    @Test
    public void encodeDateToString() {
        assertEquals("2012-12-01T01:01:01.000Z", EncapsulatedAssertionStringEncoding.encodeToString(DataType.DATE_TIME, CALENDAR.getTime()));
    }

    @Test
    public void encodeDateToStringInvalid() {
        assertNull(EncapsulatedAssertionStringEncoding.encodeToString(DataType.DATE_TIME, "invalid"));
        assertNull(EncapsulatedAssertionStringEncoding.encodeToString(DataType.DATE_TIME, null));
    }

    @Test
    public void encodeCertificateToString() {
        final String encoded = EncapsulatedAssertionStringEncoding.encodeToString(DataType.CERTIFICATE, cert);
        assertTrue(StringUtils.deleteWhitespace(encoded).contains(TestKeys.RSA_1024_CERT_X509_B64));
    }

    @Test
    public void encodeCertificateToStringInvalid() {
        assertNull(EncapsulatedAssertionStringEncoding.encodeToString(DataType.CERTIFICATE, "not a cert"));
        assertNull(EncapsulatedAssertionStringEncoding.encodeToString(DataType.CERTIFICATE, new X509CertImpl()));
        assertNull(EncapsulatedAssertionStringEncoding.encodeToString(DataType.CERTIFICATE, null));
    }

    /**
     * If DataType is not supported, should just delegate to toString().
     */
    @Test
    public void encodeToStringUnsupportedDataType() {
        final Message msg = new Message();
        assertEquals(msg.toString(), EncapsulatedAssertionStringEncoding.encodeToString(DataType.MESSAGE, msg));
        assertNull(EncapsulatedAssertionStringEncoding.encodeToString(DataType.MESSAGE, null));
    }

    @Test
    public void decodeDateFromString() {
        assertEquals(CALENDAR.getTime(), (Date) EncapsulatedAssertionStringEncoding.decodeFromString(DataType.DATE_TIME, "2012-12-01T01:01:01.000Z"));
    }

    @Test
    public void decodeDateFromStringInvalid() {
        assertNull(EncapsulatedAssertionStringEncoding.decodeFromString(DataType.DATE_TIME, "invalid"));
        assertNull(EncapsulatedAssertionStringEncoding.decodeFromString(DataType.DATE_TIME, null));
    }

    @Test
    public void decodeCertificateFromString() {
        final X509Certificate cert = (X509Certificate)EncapsulatedAssertionStringEncoding.decodeFromString(DataType.CERTIFICATE, TestKeys.RSA_1024_CERT_X509_B64);
        assertEquals(new BigInteger("15005762314007893580"), cert.getSerialNumber());
    }

    @Test
    public void decodeCertificateFromStringInvalid() {
        assertNull(EncapsulatedAssertionStringEncoding.decodeFromString(DataType.CERTIFICATE, "invalid"));
        assertNull(EncapsulatedAssertionStringEncoding.decodeFromString(DataType.CERTIFICATE, null));
    }

    @Test
    public void decodeBinaryFromString() {
        assertTrue(Arrays.equals("test".getBytes(), (byte[])EncapsulatedAssertionStringEncoding.decodeFromString(DataType.BINARY, "dGVzdA==")));
    }

    @Test
    public void decodeBinaryFromStringNull() {
        assertNull(EncapsulatedAssertionStringEncoding.decodeFromString(DataType.BINARY, null));
    }

    @Test
    public void decodeBooleanFromString() {
        assertTrue((Boolean)EncapsulatedAssertionStringEncoding.decodeFromString(DataType.BOOLEAN, "true"));
        assertTrue((Boolean)EncapsulatedAssertionStringEncoding.decodeFromString(DataType.BOOLEAN, "TRUE"));
        assertFalse((Boolean)EncapsulatedAssertionStringEncoding.decodeFromString(DataType.BOOLEAN, "false"));
        assertFalse((Boolean)EncapsulatedAssertionStringEncoding.decodeFromString(DataType.BOOLEAN, "FALSE"));
    }

    @Test
    public void decodeBooleanFromStringInvalid() {
        assertFalse((Boolean)EncapsulatedAssertionStringEncoding.decodeFromString(DataType.BOOLEAN, "invalid"));
        assertFalse((Boolean)EncapsulatedAssertionStringEncoding.decodeFromString(DataType.BOOLEAN, null));
    }

    @Test
    public void decodeFromStringUnsupportedDataType() {
        assertEquals("foo", EncapsulatedAssertionStringEncoding.decodeFromString(DataType.MESSAGE, "foo"));
        assertNull(EncapsulatedAssertionStringEncoding.decodeFromString(DataType.MESSAGE, null));
    }
}
