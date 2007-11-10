package com.l7tech.common.security.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import com.l7tech.common.mime.MimeHeaders;
import com.l7tech.common.mime.MimeUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.util.HexUtils;

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
        transform.processBody(cth, partIn, out);

        byte[] digest = MessageDigest.getInstance("SHA-1").digest(out.toByteArray());

        assertEquals("Digest value", expectedDigest, HexUtils.encodeBase64(digest));
    }
}
