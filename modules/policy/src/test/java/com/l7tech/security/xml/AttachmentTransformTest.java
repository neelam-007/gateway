package com.l7tech.security.xml;

import com.l7tech.common.io.IOUtils;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeHeaders;
import com.l7tech.common.mime.MimeUtil;
import com.l7tech.util.HexUtils;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * JUnit tests for transform of SOAP attachments.
 *
 * @author Steve Jones
 */
public class AttachmentTransformTest extends TestCase {

    /**
     *
     */
    public static Test suite() {
        return new TestSuite(AttachmentTransformTest.class);
    }

    /**
     *
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * Test addition/canonicalization of default ContentType header
     */
    public void testDefaultHeaders() throws Exception {
        ByteArrayInputStream partIn = new ByteArrayInputStream((
                "asdf\n").getBytes());

        testCompleteTransform(partIn, "czwuRDy4/rTVpfX84xV2xqgUBqA=");
    }

    /**
     * Full test for MIME header and content canonicalization
     */
    public void testAllHeaders() throws Exception {
        ByteArrayInputStream partIn = new ByteArrayInputStream((
                "Content-Description: attachment\n" +
                "Content-Disposition: Attachment; Filename=empty.txt\n" +
                "Content-ID: <attachment-1>\n" +
                "Content-Location: empty.txt\n" +
                "Content-Type: text/plain\n" +
                "\n" +
                "asdf\n").getBytes());
        
        testCompleteTransform(partIn, "i5dYLz1sTDHFVojXtNAG7puBpTs=");
    }

    /**
     * Test for line wrapped MIME header and content canonicalization
     */
    public void testSpaceWrappedHeader() throws Exception {
        ByteArrayInputStream partIn = new ByteArrayInputStream((
                "Content-Description: attachment\n" +
                "Content-Disposition: Attachment\n" +
                " ; Filename=empty.txt\n" +
                "Content-ID: <attachment-1>\n" +
                "Content-Location: empty.txt\n" +
                "Content-Type: text/plain\n" +
                "\n" +
                "asdf\n").getBytes());

        testCompleteTransform(partIn, "i5dYLz1sTDHFVojXtNAG7puBpTs=");
    }

    /**
     * Test for line wrapped MIME header and content canonicalization
     */
    public void testTabWrappedHeader() throws Exception {
        ByteArrayInputStream partIn = new ByteArrayInputStream((
                "Content-Description: attachment\n" +
                "Content-Disposition: Attachment\n" +
                "\t; Filename=empty.txt\n" +
                "Content-ID: <attachment-1>\n" +
                "Content-Location: empty.txt\n" +
                "Content-Type: text/plain\n" +
                "\n" +
                "asdf\n").getBytes());

        testCompleteTransform(partIn, "i5dYLz1sTDHFVojXtNAG7puBpTs=");
    }

    /**
     * Test for header with quoted parameter value
     */
    public void testQuotedParameterHeader() throws Exception {
        ByteArrayInputStream partIn = new ByteArrayInputStream((
                "Content-Description: attachment\n" +
                "Content-Disposition: Attachment; Filename=\"empty.txt\"\n" +
                "Content-ID: <attachment-1>\n" +
                "Content-Location: empty.txt\n" +
                "Content-Type: text/plain\n" +
                "\n" +
                "asdf\n").getBytes());

        testCompleteTransform(partIn, "i5dYLz1sTDHFVojXtNAG7puBpTs=");
    }

    /**
     * Test for header with escaped quote in parameter value
     */
    public void testQuotedEscapedParameterHeader() throws Exception {
        ByteArrayInputStream partIn = new ByteArrayInputStream((
                "Content-Description: attachment\n" +
                "Content-Disposition: attachment; filename=\"e\\mp\\t\\\"y\\\\.txt\"\n" +
                "Content-ID: <attachment-1>\n" +
                "Content-Location: empty.txt\n" +
                "Content-Type: text/plain\n" +
                "Content-Transfer-Encoding: binary\n" +
                "\n" +
                "sdfasdf\n" +
                "sdfasdf\n" +
                "sdfasdf\n" +
                "sdfasdf\n" +
                "").getBytes());

        testCompleteTransform(partIn, "Pg9uXvWf+WzOuSpL0pSluNyS4nc=");
    }

    /**
     * Test for headers with parameters that must be reordered
     */
    public void testOrderedParameterHeader() throws Exception {
        ByteArrayInputStream partIn = new ByteArrayInputStream((
                "Content-Description: attachment\n" +
                "Content-Disposition: attachment; filename=empty.txt; modification-date=y; creation-date=b\n" +
                "Content-ID: <attachment-1>\n" +
                "Content-Location: empty.txt\n" +
                "Content-Type: text/plain\n" +
                "Content-Transfer-Encoding: binary\n" +
                "Content-Length: 32\n" +
                "\n" +
                "sdfasdf\n" +
                "sdfasdf\n" +
                "sdfasdf\n" +
                "sdfasdf\n").getBytes());

        testCompleteTransform(partIn, "m0NOP1cjqL9ehqW/sCrFQFbGoqk=");
    }

    /**
     * Test for line wrapped MIME header and content canonicalization
     */
    public void testCommentedHeader() throws Exception {
        ByteArrayInputStream partIn = new ByteArrayInputStream((
                "Content-Description: attachment\n" +
                "Content-Disposition: Attachment (this is an attachment); Filename=empty.txt\n" +
                "Content-ID: <attachment-1>\n" +
                "Content-Location: empty.txt\n" +
                "Content-Type: text/plain\n" +
                "\n" +
                "asdf\n").getBytes());

        testCompleteTransform(partIn, "i5dYLz1sTDHFVojXtNAG7puBpTs=");
    }

    /**
     * Test for header names / values with odd cases
     */
    public void testCaseInsentitiveHeaders() throws Exception {
        ByteArrayInputStream partIn = new ByteArrayInputStream((
                "content-description: attachment\n" +
                "content-disposition: attachment; Filename=empty.txt\n" +
                "content-ID: <attachment-1>\n" +
                "content-location: empty.txt\n" +
                "content-type: TEXT/PLAIN\n" +
                "\n" +
                "asdf\n").getBytes());

        testCompleteTransform(partIn, "i5dYLz1sTDHFVojXtNAG7puBpTs=");
    }

    /**
     * Test for RFC 2184 language headers
     */
    public void testRFC2184LanguageHeader() throws Exception {
        ByteArrayInputStream partIn = new ByteArrayInputStream((
                "content-description: attachment\n" +
                "content-disposition: attachment; Filename*=us-ascii'en-us'empty.txt\n" +
                "content-ID: <attachment-1>\n" +
                "content-location: empty.txt\n" +
                "content-type: TEXT/PLAIN\n" +
                "\n" +
                "asdf\n").getBytes());

        testCompleteTransform(partIn, "i5dYLz1sTDHFVojXtNAG7puBpTs=");
    }

    /**
     * Test for RFC 2184 parameter continuation header
     */
    public void testRFC2184ParamValueContinuationHeader() throws Exception {
        ByteArrayInputStream partIn = new ByteArrayInputStream((
                "content-description: attachment\n" +
                "content-disposition: attachment; Filename*2=.txt; Filename*1=empty\n" +
                "content-ID: <attachment-1>\n" +
                "content-location: empty.txt\n" +
                "content-type: TEXT/PLAIN\n" +
                "\n" +
                "asdf\n").getBytes());

        testCompleteTransform(partIn, "i5dYLz1sTDHFVojXtNAG7puBpTs=");
    }

    /**
     * Test for RFC 2184 parameter continuation + language header
     */
    public void testRFC2184AllHeader() throws Exception {
        ByteArrayInputStream partIn = new ByteArrayInputStream((
                "content-description: attachment\n" +
                "content-disposition: attachment; Filename*2=.tx; Filename*3*=%74; Filename*1*=us-ascii'en-us'empty\n" +
                "content-ID: <attachment-1>\n" +
                "content-location: empty.txt\n" +
                "content-type: TEXT/PLAIN\n" +
                "\n" +
                "asdf\n").getBytes());

        testCompleteTransform(partIn, "i5dYLz1sTDHFVojXtNAG7puBpTs=");
    }

    /**
     * Test for text canonicalization '\n' -> '\r\n'
     */
    public void testTextContent() throws Exception {
        ByteArrayInputStream partIn = new ByteArrayInputStream((
                "Content-Type: text/plain\n\n" +
                "sdfasdf\n" +
                "sdfasdf\n" +
                "sdfasdf\n" +
                "sdfasdf\n").getBytes());

        testContentOnlyTransform(partIn, "qIRXzE45CaVyX2FIa2FlFZR89dA=");
    }

    /**
     * Test for text canonicalization (already in correct format)
     */
    public void testNormalizedTextContent() throws Exception {
        ByteArrayInputStream partIn = new ByteArrayInputStream((
                "Content-Type: text/plain\r\n\r\n" +
                "sdfasdf\r\n" +
                "sdfasdf\r\n" +
                "sdfasdf\r\n" +
                "sdfasdf\r\n").getBytes());

        testContentOnlyTransform(partIn, "qIRXzE45CaVyX2FIa2FlFZR89dA=");
    }

    /**
     * Test for binary canonicalization (nothing to do)
     */
    public void testBinaryContent() throws Exception {
        ByteArrayInputStream partIn = new ByteArrayInputStream((
                "Content-Type: application/octet-stream\n\n" +
                "sdfasdf\n" +
                "sdfasdf\n" +
                "sdfasdf\n" +
                "sdfasdf\n").getBytes());

        testContentOnlyTransform(partIn, "6jRAHV1ldg8X1xAqWs0Lt1hjfX8=");
    }

    /**
     *
     */
    private void testContentOnlyTransform(InputStream partIn, String expectedDigest) throws Exception {
        testTransform(partIn, expectedDigest, new AttachmentContentTransform());
    }

    /**
     *
     */
    private void testCompleteTransform(InputStream partIn, String expectedDigest) throws Exception {
        testTransform(partIn, expectedDigest, new AttachmentCompleteTransform());
    }

    /**
     *
     */
    private void testTransform(InputStream partIn, String expectedDigest, AttachmentContentTransform transform) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

        //
        MimeHeaders headers = MimeUtil.parseHeaders(partIn);
        ContentTypeHeader cth = headers.getContentType();
        transform.processHeaders(headers, out);
        InputStream transformedInput = transform.processBody(cth, partIn);
        IOUtils.copyStream(transformedInput, out);

        System.out.println(new String(out.toByteArray()));

        byte[] digest = MessageDigest.getInstance("SHA-1").digest(out.toByteArray());

        assertEquals("Digest value", expectedDigest, HexUtils.encodeBase64(digest));
    }
}
