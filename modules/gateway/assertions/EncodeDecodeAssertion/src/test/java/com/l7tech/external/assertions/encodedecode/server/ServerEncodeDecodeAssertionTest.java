package com.l7tech.external.assertions.encodedecode.server;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.encodedecode.EncodeDecodeAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.DataType;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.util.Functions;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import org.junit.Test;

import java.security.cert.X509Certificate;

/**
 * Test the EncodeDecodeAssertion.
 */
public class ServerEncodeDecodeAssertionTest {

    @Test
    public void testBase64() throws Exception {
        roundTripTest( EncodeDecodeAssertion.TransformType.BASE64_ENCODE,
                       EncodeDecodeAssertion.TransformType.BASE64_DECODE,
                       false );

        roundTripTest( EncodeDecodeAssertion.TransformType.BASE64_ENCODE,
                       EncodeDecodeAssertion.TransformType.BASE64_DECODE,
                       true );
    }

    @Test
    public void testBase64Strict() throws Exception {
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE64_DECODE, AssertionStatus.NONE, true, 0, "L 3 R l e H\tQvI\rHdpd\nGg    gc3BhY2VzIGFuZCA8Ij5z" );
        oneWayTest( EncodeDecodeAssertion.TransformType.BASE64_DECODE, AssertionStatus.FALSIFIED, true, 0, "L3 [ RleHQvIHdpdGggc3BhY2VzIGFuZCA8Ij5z" );
    }

    @Test
    public void testBase16() throws Exception {
        roundTripTest( EncodeDecodeAssertion.TransformType.HEX_ENCODE,
                       EncodeDecodeAssertion.TransformType.HEX_DECODE,
                       false );

        roundTripTest( EncodeDecodeAssertion.TransformType.HEX_ENCODE,
                       EncodeDecodeAssertion.TransformType.HEX_DECODE,
                       true );
    }

    @Test
    public void testBase16Strict() throws Exception {
        oneWayTest( EncodeDecodeAssertion.TransformType.HEX_DECODE, AssertionStatus.NONE, true, 0, "2F:74 6578742F\r207\n76 \t 974682073706163657320616E64203C223E73" );
        oneWayTest( EncodeDecodeAssertion.TransformType.HEX_DECODE, AssertionStatus.FALSIFIED, true, 0, "2F746578r742F20776974682073706163657320616E64203C223E73" );
    }

    @Test
    public void testURL() throws Exception {
        roundTripTest( EncodeDecodeAssertion.TransformType.URL_ENCODE,
                       EncodeDecodeAssertion.TransformType.URL_DECODE,
                       false );

        roundTripTest( EncodeDecodeAssertion.TransformType.URL_ENCODE,
                       EncodeDecodeAssertion.TransformType.URL_DECODE,
                       true );
    }

    @Test
    public void testMessageInputOutput() throws Exception {
        // Test message input
        final Message message = new Message();
        message.initialize( ContentTypeHeader.TEXT_DEFAULT, "/text/ with spaces and <\">s".getBytes( ContentTypeHeader.TEXT_DEFAULT.getEncoding() ));
        String result1 = (String) oneWayTest( EncodeDecodeAssertion.TransformType.BASE64_ENCODE, AssertionStatus.NONE, true, 0, message );
        assertEquals( "B64 message encoding", "L3RleHQvIHdpdGggc3BhY2VzIGFuZCA8Ij5z", result1 );

        // Test message output
        Message result2 = (Message) oneWayTest( AssertionStatus.NONE, "L3RleHQvIHdpdGggc3BhY2VzIGFuZCA8Ij5z", new Functions.UnaryVoid<EncodeDecodeAssertion>(){
            @Override
            public void call( final EncodeDecodeAssertion assertion ) {
                assertion.setTransformType( EncodeDecodeAssertion.TransformType.BASE64_DECODE );
                assertion.setTargetDataType( DataType.MESSAGE );
                assertion.setTargetContentType( ContentTypeHeader.TEXT_DEFAULT.getFullValue() );
            }
        } );
        assertEquals( "Message text", "/text/ with spaces and <\">s", new String(IOUtils.slurpStream( result2.getMimeKnob().getEntireMessageBodyAsInputStream()), ContentTypeHeader.TEXT_DEFAULT.getEncoding()) );
    }

    @Test
    public void testCertificateInputOutput() throws Exception {
        // Test certificate input
        String certB64 = "MIICazCCAdSgAwIBAgIEPb7rVDANBgkqhkiG9w0BAQQFADB6MRMwEQYDVQQDDApKb2huIFNtaXRoMR8wHQYDVQQLDBZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMQwwCgYDVQQKDANJQk0xEjAQBgNVBAcMCUN1cGVydGlubzETMBEGA1UECAwKQ2FsaWZvcm5pYTELMAkGA1UEBhMCVVMwHhcNMDUwMTAxMDAwMDAwWhcNMjUxMjMxMjM1OTU5WjB6MRMwEQYDVQQDDApKb2huIFNtaXRoMR8wHQYDVQQLDBZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMQwwCgYDVQQKDANJQk0xEjAQBgNVBAcMCUN1cGVydGlubzETMBEGA1UECAwKQ2FsaWZvcm5pYTELMAkGA1UEBhMCVVMwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAK2K2KzkU42+/bfpfDUIo68oA5DQ1iW9F38UrC5/5PVcIVp0cyu28eGr/5n8OVyfZhBg4Kn1q5L5aQFwvQBSskk9RvBkgHYLIFkmOdLv6N1vftEphBSw1E2WB0hyhkzxu8JmV0FJ+dq3jEM/JA4kHsTEOsyYj20/Q1j0Y3Sel+fDAgMBAAEwDQYJKoZIhvcNAQEEBQADgYEAiA+65PCTbLfkB7OLz5OEQUwySoK16nTY3cXKGrq1rWdHAYmr+FfVF+1ePicihDMVqfzZHeHMlNAvjVRliwP4HuU58OMz3Jn+8iJ0exKH9EKgfFZ7csX7cyXtZfvaMTxlAca04muonxJS0FFqxSFgJNScQELaA6R82wse0hksr7o=";
        final X509Certificate certificate = CertUtils.decodeCert( HexUtils.decodeBase64( certB64 ) );
        String result1 = (String) oneWayTest( EncodeDecodeAssertion.TransformType.BASE64_ENCODE, AssertionStatus.NONE, true, 0, certificate );
        assertEquals( "B64 certificate encoding", certB64, result1 );

        // Test certificate output
        X509Certificate certificate2 = (X509Certificate) oneWayTest( AssertionStatus.NONE, certB64, new Functions.UnaryVoid<EncodeDecodeAssertion>(){
            @Override
            public void call( final EncodeDecodeAssertion assertion ) {
                assertion.setTransformType( EncodeDecodeAssertion.TransformType.BASE64_DECODE );
                assertion.setTargetDataType( DataType.CERTIFICATE );
            }
        } );
        assertEquals( "Certificate B64", certB64, HexUtils.encodeBase64(certificate2.getEncoded(), true));        
    }

    @Test
    public void testOutputFormatting() throws Exception {
        final String sourceText = "test source text with special characters and some non-latin ones test source text with special characters and some non-latin ones test source text with special characters and some non-latin ones test source text with special characters and some non-latin ones \u03d0~`!@#$%^&*()_-+=}]{[|\"':;?/>.<,";
        final String out1 = (String) oneWayTest( EncodeDecodeAssertion.TransformType.BASE64_ENCODE, AssertionStatus.NONE, true, 1, sourceText );
        for ( int i=1; i<out1.length(); i=i+2 ) {
            assertTrue( "b64 line break at " + i, out1.charAt( i ) == '\n' );
        }

        final String out2 = (String) oneWayTest( EncodeDecodeAssertion.TransformType.HEX_ENCODE, AssertionStatus.NONE, true, 64, sourceText );
        for ( int i=64; i<out2.length(); i=i+65 ) {
            assertTrue( "hex line break at " + i, out2.charAt( i ) == '\n' );
        }

        final String out3 = (String) oneWayTest( EncodeDecodeAssertion.TransformType.URL_ENCODE, AssertionStatus.NONE, true, 76, sourceText );
        for ( int i=76; i<out3.length(); i=i+77 ) {
            assertTrue( "url line break at " + i, out3.charAt( i ) == '\n' );
        }
    }

    @Test
    public void testMessageOutput() throws Exception {
        final String sourceText = "test source text with special characters and some non-latin ones test source text with special characters and some non-latin ones test source text with special characters and some non-latin ones test source text with special characters and some non-latin ones \u03d0~`!@#$%^&*()_-+=}]{[|\"':;?/>.<,";
        final Message outputEnc = (Message) oneWayTest(AssertionStatus.NONE, sourceText, new Functions.UnaryVoid<EncodeDecodeAssertion>(){
            @Override
            public void call( final EncodeDecodeAssertion assertion ) {
                assertion.setTransformType( EncodeDecodeAssertion.TransformType.BASE64_ENCODE );
                assertion.setTargetDataType( DataType.MESSAGE );
            }
        } );
        assertNotNull( "Message output encode", outputEnc );
        
        final String sourceB64 = new String( IOUtils.slurpStream( outputEnc.getMimeKnob().getEntireMessageBodyAsInputStream() ));
        final Message outputDec = (Message) oneWayTest(AssertionStatus.NONE, sourceB64, new Functions.UnaryVoid<EncodeDecodeAssertion>(){
            @Override
            public void call( final EncodeDecodeAssertion assertion ) {
                assertion.setTransformType( EncodeDecodeAssertion.TransformType.BASE64_DECODE );
                assertion.setTargetDataType( DataType.MESSAGE );
            }
        } );
        assertNotNull( "Message output decode", outputDec );
    }

    private void roundTripTest( final EncodeDecodeAssertion.TransformType encode,
                                final EncodeDecodeAssertion.TransformType decode,
                                final boolean strict ) throws Exception {
        final EncodeDecodeAssertion assertion = new EncodeDecodeAssertion();
        assertion.setSourceVariableName( "source" );
        assertion.setTargetVariableName( "output1" );
        assertion.setTargetDataType( DataType.STRING );
        assertion.setTransformType( encode );

        final String sourceText = "test source text with special characters and some non-latin ones \u03d0~`!@#$%^&*()_-+=}]{[|\"':;?/>.<,";
        final PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext( null, null );
        pec.setVariable( "source", sourceText );

        final ServerEncodeDecodeAssertion serverEncodeDecodeAssertion = new ServerEncodeDecodeAssertion( assertion );
        final AssertionStatus status = serverEncodeDecodeAssertion.checkRequest( pec );

        assertEquals( "Encode status", AssertionStatus.NONE, status );
        assertFalse( "Output not encoded", sourceText.equals( pec.getVariable( "output1" )));

        assertion.setStrict( strict );
        assertion.setSourceVariableName( "output1" );
        assertion.setTargetVariableName( "output2" );
        assertion.setTransformType( decode );
        final ServerEncodeDecodeAssertion serverEncodeDecodeAssertion2 = new ServerEncodeDecodeAssertion( assertion );
        final AssertionStatus status2 = serverEncodeDecodeAssertion2.checkRequest( pec );

        assertEquals( "Decode status", AssertionStatus.NONE, status2 );
        assertEquals( "Round trip text mismatch", sourceText, pec.getVariable( "output2" ) );
    }

    private Object oneWayTest( final EncodeDecodeAssertion.TransformType encode,
                               final AssertionStatus expectedStatus,
                               final boolean strict,
                               final int lineBreaks,
                               final Object data ) throws Exception {
        return oneWayTest( expectedStatus, data, new Functions.UnaryVoid<EncodeDecodeAssertion>(){
            @Override
            public void call( final EncodeDecodeAssertion assertion ) {
                assertion.setTransformType( encode );
                assertion.setStrict( strict );
                assertion.setLineBreakInterval( lineBreaks );
            }
        } );
    }

    private Object oneWayTest( final AssertionStatus expectedStatus,
                               final Object data,
                               final Functions.UnaryVoid<EncodeDecodeAssertion> configCallback ) throws Exception {
        final EncodeDecodeAssertion assertion = new EncodeDecodeAssertion();
        assertion.setSourceVariableName( "source" );
        assertion.setTargetVariableName( "target" );
        assertion.setTargetDataType( DataType.STRING );
        if ( configCallback!=null ) configCallback.call( assertion );

        final PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext( null, null );
        pec.setVariable( "source", data );

        final ServerEncodeDecodeAssertion serverEncodeDecodeAssertion = new ServerEncodeDecodeAssertion( assertion );
        AssertionStatus status;
        try {
            status = serverEncodeDecodeAssertion.checkRequest( pec );
        } catch ( AssertionStatusException e ) {
            status = e.getAssertionStatus();
        }
        assertEquals( "Decode status", expectedStatus, status );

        return status==AssertionStatus.NONE ? pec.getVariable( "target" ) : null;
    }
}
