package com.l7tech.server.util;

import com.l7tech.common.TestKeys;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
import com.l7tech.util.Functions;
import com.l7tech.util.HexUtils;
import com.l7tech.util.TextUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class ContextVariableUtilsTest {

    @BugNumber(11642)
    @Test
    public void testExpressionsSupported() throws Exception {
        final String expression = "${myDest}gov:icam:bae:v2:1:7000:0000";
        final Map<String, Object> serverVariables = new HashMap<String, Object>();
        serverVariables.put("mydest", "urn:idmanagement.");

        final TestAudit auditor = new TestAudit();
        final List<String> resolvedStrings = ContextVariableUtils.getAllResolvedStrings(expression,
                serverVariables,
                auditor,
                TextUtils.URI_STRING_SPLIT_PATTERN,
                new Functions.UnaryVoid<Object>() {
                    @Override
                    public void call(Object o) {
                        fail("Unexpected");
                    }
                });

        assertEquals(1, resolvedStrings.size());
        assertEquals("urn:idmanagement.gov:icam:bae:v2:1:7000:0000", resolvedStrings.get(0));
    }

    @Test
    public void testTextOnly() throws Exception {
        final String expression = "gov:icam:bae:v2:1:7000:0000";
        final Map<String, Object> serverVariables = new HashMap<String, Object>();

        final TestAudit auditor = new TestAudit();
        final List<String> resolvedStrings = ContextVariableUtils.getAllResolvedStrings(expression,
                serverVariables,
                auditor,
                TextUtils.URI_STRING_SPLIT_PATTERN,
                new Functions.UnaryVoid<Object>() {
                    @Override
                    public void call(Object o) {
                        fail("Unexpected");
                    }
                });

        assertEquals(1, resolvedStrings.size());
        assertEquals("gov:icam:bae:v2:1:7000:0000", resolvedStrings.get(0));
    }

    /**
     * Tests that expression, text values, variable references with space separated values and multi valued references
     * are processed as expected.
     */
    @Test
    public void testAllSupportedInputMethods() throws Exception {

        String expression = "${myDest}gov:icam:bae:v2:1:7000:0000 http://donal.com ${output} ${multivar}";

        final Map<String, Object> serverVariables = new HashMap<String, Object>();
        // variable with space separated values
        serverVariables.put("output", "http://one.com http://two.com http://three.com");
        // multi var reference
        serverVariables.put("multivar", new ArrayList<String>(Arrays.asList("http://input1.com", "http://input2.com", "http://input3.com")));
        // var with single value
        serverVariables.put("mydest", "urn:idmanagement.");


        final TestAudit auditor = new TestAudit();
        final List<String> resolvedStrings = ContextVariableUtils.getAllResolvedStrings(expression,
                serverVariables,
                auditor,
                TextUtils.URI_STRING_SPLIT_PATTERN,
                new Functions.UnaryVoid<Object>() {
                    @Override
                    public void call(Object o) {
                        fail("Unexpected");
                    }
                });

        for (String s : auditor) {
            System.out.println(s);
        }
        // urn:idmanagement.gov:icam:bae:v2:1:7000:0000, http://donal.com, http://one.com, http://two.com, http://three.com, http://input1.com, http://input2.com, http://input3.com

        assertEquals(8, resolvedStrings.size());
        assertEquals("urn:idmanagement.gov:icam:bae:v2:1:7000:0000", resolvedStrings.get(0));
        assertEquals("http://donal.com", resolvedStrings.get(1));
        assertEquals("http://one.com", resolvedStrings.get(2));
        assertEquals("http://two.com", resolvedStrings.get(3));
        assertEquals("http://three.com", resolvedStrings.get(4));
        assertEquals("http://input1.com", resolvedStrings.get(5));
        assertEquals("http://input2.com", resolvedStrings.get(6));
        assertEquals("http://input3.com", resolvedStrings.get(7));
    }

    @Test
    public void testEmptyStringsIgnored() throws Exception {
        String expression = "";
        List<String> stringList = ContextVariableUtils.getStringsFromList(new ArrayList<Object>(Arrays.asList(expression)),
                TextUtils.URI_STRING_SPLIT_PATTERN,
                new Functions.UnaryVoid<Object>() {
            @Override
            public void call(Object o) {
                fail("unexpected");
            }
        });

        assertEquals("No results expected", 0, stringList.size());
        stringList = ContextVariableUtils.getStringsFromList(new ArrayList<Object>(Arrays.asList(expression)), null, null);
        assertEquals("No results expected", 0, stringList.size());
    }

    @Test
    public void testObjectIgnored() throws Exception {
        String expression = "";
        final boolean[] found = new boolean[1];
        ContextVariableUtils.getStringsFromList(new ArrayList<Object>(Arrays.asList(expression, new Object())),
                null,
                new Functions.UnaryVoid<Object>() {
            @Override
            public void call(Object o) {
                found[0] = true;
            }
        });

        assertTrue("Callback should have been called", found[0]);
    }

    @Test( expected = ContextVariableUtils.NoBinaryRepresentationException.class )
    @BugId( "SSG-9126" )
    public void testConvertToBinary_null() throws Exception {
        ContextVariableUtils.convertContextVariableValueToByteArray( null, 0, null );
    }

    @Test( expected = ContextVariableUtils.NoBinaryRepresentationException.class )
    @BugId( "SSG-9126" )
    public void testConvertToBinary_unknownObjectClass() throws Exception {
        ContextVariableUtils.convertContextVariableValueToByteArray( new Object() {}, 0, null );
    }

    @Test
    @BugId( "SSG-9126" )
    public void testConvertToBinary_emptyString() throws Exception {
        assertTrue( Arrays.equals( new byte[0], ContextVariableUtils.convertContextVariableValueToByteArray( "", -1, null ) ) );
    }

    @Test
    @BugId( "SSG-9126" )
    public void testConvertToBinary_byteArray() throws Exception {
        byte[] binary = HexUtils.unHexDump( "00010488FFFEBEAA73658877A0002233445566EECCBBAA0043" );
        assertTrue( binary == ContextVariableUtils.convertContextVariableValueToByteArray( binary, 2, null ) );
    }

    @Test
    @BugId( "SSG-9126" )
    public void testConvertToBinary_defaultEncoding() throws Exception {
        byte[] binary = HexUtils.unHexDump( "00010488FFFEBEAA73658877A0002233445566EECCBBAA0043" );
        String str = new String( binary, Charsets.ISO8859 );
        assertTrue( Arrays.equals( binary, ContextVariableUtils.convertContextVariableValueToByteArray( str, 0, null ) ) );
    }

    @Test
    @BugId( "SSG-9126" )
    public void testConvertToBinary_UTF8() throws Exception {
        byte[] binary = HexUtils.unHexDump( "00010488FFFEBEAA73658877A0002233445566EECCBBAA0043" );
        // This binary message will be mangled by a UTF8 round trip
        String str = new String( binary, Charsets.UTF8 );
        assertTrue( Arrays.equals( str.getBytes( Charsets.UTF8 ), ContextVariableUtils.convertContextVariableValueToByteArray( str, -1, Charsets.UTF8 ) ) );
    }

    @Test
    @BugId( "SSG-9126" )
    public void testConvertToBinary_Message() throws Exception {
        byte[] binary = HexUtils.unHexDump( "00010488FFFEBEAA73658877A0002233445566EECCBBAA0043" );
        Message message = new Message( new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream( binary ) );
        assertTrue( Arrays.equals( binary, ContextVariableUtils.convertContextVariableValueToByteArray( message, -1, null ) ) );
    }

    @Test
    @BugId( "SSG-9126" )
    public void testConvertToBinary_MessageLimitOk() throws Exception {
        byte[] binary = HexUtils.unHexDump( "00010488FFFEBEAA73658877A0002233445566EECCBBAA0043" );
        Message message = new Message( new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream( binary ) );
        assertTrue( Arrays.equals( binary, ContextVariableUtils.convertContextVariableValueToByteArray( message, 25, null ) ) );
    }

    @Test( expected = IOException.class )
    @BugId( "SSG-9126" )
    public void testConvertToBinary_MessageLimitExceeded() throws Exception {
        byte[] binary = HexUtils.unHexDump( "00010488FFFEBEAA73658877A0002233445566EECCBBAA0043" );
        Message message = new Message( new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream( binary ) );
        assertTrue( Arrays.equals( binary, ContextVariableUtils.convertContextVariableValueToByteArray( message, 24, null ) ) );
    }

    @Test
    @BugId( "SSG-9126" )
    public void testConvertToBinary_MessagePart() throws Exception {
        byte[] binary = HexUtils.unHexDump( "00010488FFFEBEAA73658877A0002233445566EECCBBAA0043" );
        Message message = new Message( new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream( binary ) );
        assertTrue( Arrays.equals( binary, ContextVariableUtils.convertContextVariableValueToByteArray( message.getMimeKnob().getFirstPart(), -1, null ) ) );
    }

    @Test
    @BugId( "SSG-9126" )
    public void testConvertToBinary_MessagePartLimitOk() throws Exception {
        byte[] binary = HexUtils.unHexDump( "00010488FFFEBEAA73658877A0002233445566EECCBBAA0043" );
        Message message = new Message( new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream( binary ) );
        assertTrue( Arrays.equals( binary, ContextVariableUtils.convertContextVariableValueToByteArray( message.getMimeKnob().getFirstPart(), 25, null ) ) );
    }

    @Test( expected = IOException.class )
    @BugId( "SSG-9126" )
    public void testConvertToBinary_MessagePartLimitExceeded() throws Exception {
        byte[] binary = HexUtils.unHexDump( "00010488FFFEBEAA73658877A0002233445566EECCBBAA0043" );
        Message message = new Message( new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream( binary ) );
        assertTrue( Arrays.equals( binary, ContextVariableUtils.convertContextVariableValueToByteArray( message.getMimeKnob().getFirstPart(), 24, null ) ) );
    }


    @Test
    @BugId( "SSG-9126" )
    public void testConvertToBinary_X509Certificate() throws Exception {
        X509Certificate cert = TestKeys.getCert( TestKeys.RSA_2048_CERT_X509_B64 );
        assertTrue( Arrays.equals( cert.getEncoded(), ContextVariableUtils.convertContextVariableValueToByteArray( cert, 1, null ) ) );
    }
}
