package com.l7tech.common.mime;

import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.io.IOExceptionThrowingInputStream;
import com.l7tech.common.io.NullOutputStream;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import com.l7tech.util.*;

import static com.l7tech.util.CollectionUtils.list;

import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Test for MimeBody class.
 */
public class MimeBodyTest {
    private static Logger log = Logger.getLogger(MimeBodyTest.class.getName());

    private static long FIRST_PART_MAX_BYTE = 0L;

    @Test
    public void testEmptySinglePartMessage() throws Exception {
        MimeBody mm = new MimeBody(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new EmptyInputStream(),FIRST_PART_MAX_BYTE);
        assertEquals( -1L, mm.getFirstPart().getContentLength()); // size of part not yet known
        long len = mm.getEntireMessageBodyLength(); // force entire body to be read
        assertEquals( 0L, len);
        assertEquals( 0L, mm.getFirstPart().getContentLength()); // size now known
    }

    @Test
    public void testEmptyMultiPartMessage() throws Exception {
        try{
            MimeBody mm = makeMessage("",MESS_CONTENT_TYPE,FIRST_PART_MAX_BYTE);
            mm.getEntireMessageBodyLength(); // force entire body to be read
        }catch (IOException e){
            // exception expected
            assertEquals("Message MIME type is multipart/related, but no initial boundary found", e.getMessage());
            return ;
        }
        fail("Did not receive expected IOException");
    }

    @Test
    public void testEmptyPart() throws Exception {
        System.setProperty("com.l7tech.common.mime.allowLaxEmptyMultipart", "true");
        ConfigFactory.clearCachedConfig();
        MimeBody.loadLaxEmptyMutipart();

        try{
            MimeBody mm = makeMessage("----=Part_-763936460.407197826076299",MESS_CONTENT_TYPE,FIRST_PART_MAX_BYTE);
        } catch (IOException e){
            // exception expected
            assertEquals("Multipart stream ended before a terminating boundary was encountered", e.getMessage());
            return ;

        } finally {
            // full clean up of internal state of MimeBody
            System.clearProperty("com.l7tech.common.mime.allowLaxEmptyMultipart");
            ConfigFactory.clearCachedConfig();
            MimeBody.loadLaxEmptyMutipart();
        }
        fail("Did not receive expected IOException");

    }

    @Test
    public void testEmptyMultiPartMessageLaxParsing() throws Exception {

        System.setProperty("com.l7tech.common.mime.allowLaxEmptyMultipart", "true");
        ConfigFactory.clearCachedConfig();
        MimeBody.loadLaxEmptyMutipart();

        try{
            MimeBody mm = makeMessage("",MESS_CONTENT_TYPE,FIRST_PART_MAX_BYTE);
            assertEquals( -1L, mm.getFirstPart().getContentLength()); // size of part not yet known
            long len = mm.getEntireMessageBodyLength(); // force entire body to be read
            assertEquals( 0L, len);
            assertEquals( 0L, mm.getFirstPart().getContentLength()); // size now known
            assertEquals( "multipart", mm.getFirstPart().getContentType().getType());
            assertEquals( "related", mm.getFirstPart().getContentType().getSubtype());
            assertEquals( 1L, mm.getNumPartsKnown()); // no other parts

        } finally {
            // full clean up of internal state of MimeBody
            System.clearProperty("com.l7tech.common.mime.allowLaxEmptyMultipart");
            ConfigFactory.clearCachedConfig();
            MimeBody.loadLaxEmptyMutipart();
        }
    }



    /**
     * We strictly match the start parameter to the Content-ID by default. This can be turned off but only via a system
     * property. This test ensures that the default case is still valid e.g. do not match
     */
    @Test
    public void testStartDoesNotMatchIfNoSurroundingBrackets() throws Exception {
        // Content-ID here has surrounding brackets
        String mess = "PREAMBLE GARBAGE\r\nBLAH BLAH BLAH\r\n------=Part_-763936460.407197826076299\r\n" +
                    "Content-Transfer-Encoding: 8bit\r\n" +
                    "Content-Type: text/xml; charset=utf-8\r\n" +
                    "Content-ID: <-76394136.15558>\r\n" +
                    "\r\n" +
                    SOAP +
                    "\r\n" +
                    "------=Part_-763936460.407197826076299\r\n" +
                    "Content-Transfer-Encoding: 8bit\r\n" +
                    "Content-Type: application/octet-stream\r\n" +
                    "Content-ID: -76392836.15558\r\n" +
                    "\r\n" +
                     RUBY +
                    "\r\n" +
                    "------=Part_-763936460.407197826076299--\r\n";

        String boundary = "----=Part_-763936460.407197826076299";
        // Start has no surrounding brackets
        String contentType = "multipart/related; type=\"text/xml\"; boundary=\"" + boundary + "\"; start=\"-76394136.15558\"";
        // this should throw an IOException due to the mis match
        try {
            makeMessage(mess, contentType,FIRST_PART_MAX_BYTE);
            fail("Method should have thrown due to start not matching Content-ID");
        } catch (Exception e) {
            assertTrue(e instanceof IOException);
            assertEquals("Multipart content type has a \"start\" parameter, but it doesn't match the cid of the first MIME part.", e.getMessage());
        }
    }

    /**
     * If start does not match Content-ID simply because Content-ID is surrounded by &lt; and &gt;, then allow them to
     * match in a lax mode where start will match Content-ID if the surrounding brackets required on Content-ID are ignored.
     * (see rfc 822 and rfc2045) and start defined in rfc 2387 has no such rule.
     *
     */
    @BugId("SSG-6388")
    @Test
    public void testSupportStartNoMatchContentIdBecauseOfBrackets() throws Exception {

        // Content-ID here has surrounding brackets
        String mess = "PREAMBLE GARBAGE\r\nBLAH BLAH BLAH\r\n------=Part_-763936460.407197826076299\r\n" +
                    "Content-Transfer-Encoding: 8bit\r\n" +
                    "Content-Type: text/xml; charset=utf-8\r\n" +
                    "Content-ID: <-76394136.15558>\r\n" +
                    "\r\n" +
                    SOAP +
                    "\r\n" +
                    "------=Part_-763936460.407197826076299\r\n" +
                    "Content-Transfer-Encoding: 8bit\r\n" +
                    "Content-Type: application/octet-stream\r\n" +
                    "Content-ID: -76392836.15558\r\n" +
                    "\r\n" +
                     RUBY +
                    "\r\n" +
                    "------=Part_-763936460.407197826076299--\r\n";

        String boundary = "----=Part_-763936460.407197826076299";

        try {
            // MimeBody may already be loaded so we need to reset it internally to allow new value to be picked up.
            System.setProperty("com.l7tech.common.mime.allowLaxStartParamMatch", "true");
            ConfigFactory.clearCachedConfig();
            MimeBody.loadLaxStartParam();
            {
                // Start has no surrounding brackets
                String contentType = "multipart/related; type=\"text/xml\"; boundary=\"" + boundary + "\"; start=\"-76394136.15558\"";
                MimeBody mm = makeMessage(mess, contentType,FIRST_PART_MAX_BYTE);
                assertNotNull(mm);
            }

            {
                // Now if start IS surrounded by brackets, then we must continue to support that
                String contentType = "multipart/related; type=\"text/xml\"; boundary=\"" + boundary + "\"; start=\"<-76394136.15558>\"";
                MimeBody mm = makeMessage(mess, contentType,FIRST_PART_MAX_BYTE);
                assertNotNull(mm);
            }
        } finally {
            // full clean up of internal state of MimeBody
            System.clearProperty("com.l7tech.common.mime.allowLaxStartParamMatch");
            ConfigFactory.clearCachedConfig();
            MimeBody.loadLaxStartParam();
        }

    }

    /**
     * Comparing start to Content-ID should never allow stripping of angle brackets from the start parameter.
     *
     */
    @BugId("SSG-6388")
    @Test(expected = IOException.class)
    public void testNeverModifyStartParameterForComparison() throws Exception {

        // Content-ID here has surrounding brackets
        String mess = "PREAMBLE GARBAGE\r\nBLAH BLAH BLAH\r\n------=Part_-763936460.407197826076299\r\n" +
                    "Content-Transfer-Encoding: 8bit\r\n" +
                    "Content-Type: text/xml; charset=utf-8\r\n" +
                    "Content-ID: <-76394136.15558>\r\n" +
                    "\r\n" +
                    SOAP +
                    "\r\n" +
                    "------=Part_-763936460.407197826076299\r\n" +
                    "Content-Transfer-Encoding: 8bit\r\n" +
                    "Content-Type: application/octet-stream\r\n" +
                    "Content-ID: -76392836.15558\r\n" +
                    "\r\n" +
                     RUBY +
                    "\r\n" +
                    "------=Part_-763936460.407197826076299--\r\n";

        String boundary = "----=Part_-763936460.407197826076299";

        // start has two surround angle brackets
        String contentType = "multipart/related; type=\"text/xml\"; boundary=\"" + boundary + "\"; start=\"<<-76394136.15558>>\"";
        makeMessage(mess, contentType,FIRST_PART_MAX_BYTE);
    }

    @Test
    public void testSimple() throws Exception {
        MimeBody mm = makeMessage(MESS, MESS_CONTENT_TYPE,FIRST_PART_MAX_BYTE);

        PartInfo rubyPart = mm.getPart(1);
        InputStream rubyStream = rubyPart.getInputStream(true);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        IOUtils.copyStream(rubyStream, bo);
        assertTrue(Arrays.equals(RUBY.getBytes(), bo.toByteArray()));
        log.info("Ruby part retrieved " + bo.toByteArray().length + " bytes: \n" + new String(bo.toByteArray()));

        PartInfo soapPart = mm.getPart(0);
        InputStream soapStream = soapPart.getInputStream(true);
        bo = new ByteArrayOutputStream();
        IOUtils.copyStream(soapStream, bo);
        assertTrue(Arrays.equals(SOAP.getBytes(), bo.toByteArray()));
        log.info("Soap part retrieved " + bo.toByteArray().length + " bytes: \n" + new String(bo.toByteArray()));

        // We read them out of order, so this part will have been stashed
        soapStream = soapPart.getInputStream(false);

        try {
            rubyStream = rubyPart.getInputStream(false);
            fail("Ruby part was read destructively, and is last part, but was stashed anyway");
        } catch (NoSuchPartException e) {
            log.info("The Ruby part was consumed successfully: " + e.getMessage());
        }

        try {
            mm.getPart(2);
            fail("Failed to get expected exception");
        } catch (NoSuchPartException e) {
            log.info("Got proper exception on trying to get a nonexistent MIME part: " + e.getMessage());
        }
    }

    @Test
    public void testReadWriteIdentityEncoded() throws Exception {
        MimeBody mm = makeMessage(MESS, MESS_CONTENT_TYPE,FIRST_PART_MAX_BYTE);

        // read parts
        PartInfo rubyPart = mm.getPart(1);
        byte[] rubyData = IOUtils.slurpStream( rubyPart.getInputStream(false) );
        log.info("Ruby part retrieved " + rubyData.length + " bytes: \n[" + new String(rubyData)+ "]");
        assertArrayEquals("Ruby part equality", RUBY.getBytes(), rubyData);

        PartInfo soapPart = mm.getPart(0);
        byte[] soapData = IOUtils.slurpStream( soapPart.getInputStream(false) );
        log.info("Soap part retrieved " + soapData.length + " bytes: \n" + new String(soapData));
        assertArrayEquals("SOAP part equality", SOAP.getBytes(), soapData);

        // update parts
        rubyPart.setBodyBytes( SOAP.getBytes() );
        soapPart.setBodyBytes( RUBY.getBytes() );

        rubyPart.getHeaders().add( MimeHeader.parseValue("X-a", "v") );
        byte[] body = IOUtils.slurpStream( mm.getEntireMessageBodyAsInputStream( false ) );
        String bodyString = new String(body);
        log.info( "Message body : " + bodyString );

        assertTrue("Body contains raw SOAP", bodyString.contains( "version=\"1.0\"" ));
        assertTrue("Body contains raw Ruby", bodyString.contains( "require 'soap/rpc/driver'" ));
        assertTrue("Ruby comes before SOAP" , bodyString.indexOf( "version=\"1.0\"" ) > bodyString.indexOf( "require 'soap/rpc/driver'" ));
    }

    @Test
    public void testReadWriteContentTransferEncoded() throws Exception {
        MimeBody mm = makeMessage(MESS_ENC, MESS_CONTENT_TYPE,FIRST_PART_MAX_BYTE);

        // read parts
        PartInfo rubyPart = mm.getPart(1);
        byte[] rubyData = IOUtils.slurpStream( rubyPart.getInputStream(false) );
        rubyData = new String(rubyData).replaceAll("\r","").getBytes(); // quoted printable will replace \n with \r\n
        log.info("Ruby part retrieved " + rubyData.length + " bytes: \n[" + new String(rubyData)+ "]");
        assertArrayEquals("Ruby part equality", RUBY.getBytes(), rubyData);

        PartInfo soapPart = mm.getPart(0);
        byte[] soapData = IOUtils.slurpStream( soapPart.getInputStream(false) );
        log.info("Soap part retrieved " + soapData.length + " bytes: \n" + new String(soapData));
        assertArrayEquals("SOAP part equality", SOAP.getBytes(), soapData);

        // update parts after switching the encoding
        rubyPart.getHeaders().replace( MimeHeader.parseValue( MimeUtil.CONTENT_TRANSFER_ENCODING, MimeUtil.TRANSFER_ENCODING_BASE64 ));
        rubyPart.setBodyBytes( RUBY.getBytes() );
        soapPart.getHeaders().replace( MimeHeader.parseValue( MimeUtil.CONTENT_TRANSFER_ENCODING, MimeUtil.TRANSFER_ENCODING_QUOTED_PRINTABLE ));
        soapPart.setBodyBytes( SOAP.getBytes() );
        
        byte[] body = IOUtils.slurpStream( mm.getEntireMessageBodyAsInputStream( false ) );
        String bodyString = new String(body);
        log.info( "Message body : " + bodyString );

        assertTrue("Body contains escaped SOAP", bodyString.contains( "version=3D\"1.0\"" ));
        assertTrue("Body contains escaped Ruby", bodyString.contains( "cmVxdWlyZSAnc2" ));
    }

    @Test
    public void testSimpleWithNoPreamble() throws Exception {
        MimeBody mm = makeMessage(MESS2, MESS2_CONTENT_TYPE,FIRST_PART_MAX_BYTE);

        PartInfo rubyPart = mm.getPart(1);
        InputStream rubyStream = rubyPart.getInputStream(true);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        IOUtils.copyStream(rubyStream, bo);
        log.info("Ruby part retrieved " + bo.toByteArray().length + " bytes: \n" + new String(bo.toByteArray()));

        PartInfo soapPart = mm.getPart(0);
        InputStream soapStream = soapPart.getInputStream(true);
        bo = new ByteArrayOutputStream();
        IOUtils.copyStream(soapStream, bo);
        log.info("Soap part retrieved " + bo.toByteArray().length + " bytes: \n" + new String(bo.toByteArray()));

        // We read them out of order, so this part will have been stashed
        soapStream = soapPart.getInputStream(false);

        try {
            rubyStream = rubyPart.getInputStream(false);
            fail("Ruby part was read destructively, and is last part, but was stashed anyway");
        } catch (NoSuchPartException e) {
            log.info("The Ruby part was consumed successfully: " + e.getMessage());
        }

        try {
            mm.getPart(2);
            fail("Failed to get expected exception");
        } catch (NoSuchPartException e) {
            log.info("Got proper exception on trying to get a nonexistent MIME part: " + e.getMessage());
        }
    }

    private MimeBody makeMessage(String message, String contentTypeValue, long maxSize) throws IOException, NoSuchPartException {
        InputStream mess = new ByteArrayInputStream(message.getBytes());
        ContentTypeHeader mr = ContentTypeHeader.parseValue(contentTypeValue);
        StashManager sm = new ByteArrayStashManager();
        return new MimeBody(sm, mr, mess,maxSize);
    }

    @Test
    public void testSinglePart() throws Exception {
        final String body = "<foo/>";
        MimeBody mm = makeMessage(body, "text/xml",FIRST_PART_MAX_BYTE);
        PartInfo p = mm.getPart(0);
        InputStream in = p.getInputStream(true);
        final byte[] bodyStream = IOUtils.slurpStream(in);
        assertTrue(Arrays.equals(bodyStream, body.getBytes()));

    }

    @Test
    public void testStreamAllWithNoPreamble() throws Exception {
        MimeBody mm = makeMessage(MESS2, MESS2_CONTENT_TYPE,FIRST_PART_MAX_BYTE);

        InputStream bodyStream = mm.getEntireMessageBodyAsInputStream(true);
        byte[] body = IOUtils.slurpStream(bodyStream);
        final String bodyStr = new String(body);
        int bodyStart = bodyStr.indexOf("<?xml ");

        assertEquals(MESS2.substring(MESS2.indexOf("<?xml ")), bodyStr.substring(bodyStart));
    }

    @Test
    public void testStreamAllWithPreamble() throws Exception {
        MimeBody mm = makeMessage(MESS, MESS_CONTENT_TYPE,FIRST_PART_MAX_BYTE);

        InputStream bodyStream = mm.getEntireMessageBodyAsInputStream(true);
        byte[] body = IOUtils.slurpStream(bodyStream);
        final String bodyStr = new String(body);
        int bodyStart = bodyStr.indexOf("<?xml ");

        assertEquals(MESS.substring(MESS.indexOf("<?xml ")), bodyStr.substring(bodyStart));

        try {
            long len = mm.getEntireMessageBodyLength();
            fail("Failed to get expected exception trying to compute body length with part bodies missing (got len=" + len + ")");
        } catch (NoSuchPartException e) {
            log.info("Got expected exception: " + e.getMessage());
        }
    }

    @Test
    public void testStreamAllConsumedRubyPart() throws Exception {
        MimeBody mm = makeMessage(MESS2, MESS2_CONTENT_TYPE,FIRST_PART_MAX_BYTE);

        // Destroy body of ruby part
        IOUtils.slurpStream(mm.getPart(1).getInputStream(true));

        try {
            mm.getEntireMessageBodyAsInputStream(true);
            fail("Failed to get expected exception when trying to stream all after destructively reading 1 part");
        } catch (NoSuchPartException e) {
            log.info("Got expected exception: " + e.getMessage());
        }
    }

    @Test
    public void testStreamAllWithAllStashed() throws Exception {
        MimeBody mm = makeMessage(MESS, MESS_CONTENT_TYPE,FIRST_PART_MAX_BYTE);

        mm.getPart(1).getInputStream(false);

        InputStream bodyStream = mm.getEntireMessageBodyAsInputStream(true);
        byte[] body = IOUtils.slurpStream(bodyStream);
        assertEquals( (long) body.length, mm.getEntireMessageBodyLength());

        // strip added content-length for the comparison
        String bodyStr = new String(body).replaceAll("Content-Length:\\s*\\d+\r\n", "");

        // Skip over the HTTP headers for the comparison
        int bodyStart = bodyStr.indexOf("<?xml ");

        assertEquals(MESS.substring(MESS.indexOf("<?xml ")), bodyStr.substring(bodyStart));
    }

    @Test
    public void testStreamAllWithFirstPartStashed() throws Exception {
        MimeBody mm = makeMessage(MESS, MESS_CONTENT_TYPE,FIRST_PART_MAX_BYTE);

        mm.getPart(0).getInputStream(false);

        InputStream bodyStream = mm.getEntireMessageBodyAsInputStream(true);
        byte[] body = IOUtils.slurpStream(bodyStream);
        final String bodyStr = new String(body);
        int bodyStart = bodyStr.indexOf("<?xml ");

        // TODO less sensitive comparision that will not give false negative here (due to reordered headers)
        assertEquals(MESS.substring(MESS.indexOf("<?xml ")), bodyStr.substring(bodyStart));
    }

    @Test
    public void testLookupsByCid() throws Exception {
        MimeBody mm = makeMessage(MESS, MESS_CONTENT_TYPE,FIRST_PART_MAX_BYTE);

        PartInfo rubyPart = mm.getPartByContentId(MESS_RUBYCID);
        InputStream rubyStream = rubyPart.getInputStream(true);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        IOUtils.copyStream(rubyStream, bo);
        assertTrue(Arrays.equals(RUBY.getBytes(), bo.toByteArray()));
        log.info("Ruby part retrieved " + bo.toByteArray().length + " bytes: \n" + new String(bo.toByteArray()));

        PartInfo soapPart = mm.getPartByContentId(MESS_SOAPCID);
        InputStream soapStream = soapPart.getInputStream(true);
        bo = new ByteArrayOutputStream();
        IOUtils.copyStream(soapStream, bo);
        assertTrue(Arrays.equals(SOAP.getBytes(), bo.toByteArray()));
        log.info("Soap part retrieved " + bo.toByteArray().length + " bytes: \n" + new String(bo.toByteArray()));

        // We read them out of order, so this part will have been stashed
        soapStream = soapPart.getInputStream(false);

        try {
            rubyStream = rubyPart.getInputStream(false);
            fail("Ruby part was read destructively, and is last part, but was stashed anyway");
        } catch (NoSuchPartException e) {
            log.info("The Ruby part was consumed successfully: " + e.getMessage());
        }

        try {
            mm.getPart(2);
            fail("Failed to get expected exception");
        } catch (NoSuchPartException e) {
            log.info("Got proper exception on trying to get a nonexistent MIME part: " + e.getMessage());
        }
    }

    @Test
    public void testByteArrayCtorNullByteArray() {
        try {
            new MimeBody(null, ContentTypeHeader.XML_DEFAULT,FIRST_PART_MAX_BYTE);
            fail("Did not get a fast-failure exception passing null byte array to MimeBody(byte[], ctype)");
        } catch (NullPointerException e) {
            // This is acceptable
        } catch (IllegalArgumentException e) {
            // So is this
        } catch (NoSuchPartException e) {
            throw new RuntimeException("Wrong exception", e);
        } catch (IOException e) {
            throw new RuntimeException("Wrong exception", e);
        }
    }

    @Test
    public void testIterator() throws Exception {
        MimeBody mm = makeMessage(MESS, MESS_CONTENT_TYPE,FIRST_PART_MAX_BYTE);

        List parts = new ArrayList();
        for (PartIterator i = mm.iterator(); i.hasNext(); ) {
            PartInfo partInfo = i.next();
            parts.add(partInfo);
            log.info("Saw part: " + partInfo.getContentId(true));
            // Force body to be stashed
            partInfo.getInputStream(false).close();
        }

        assertEquals( 2L, (long) parts.size() );
        assertEquals(MESS_SOAPCID, ((PartInfo)parts.get(0)).getContentId(true));
        assertEquals(MESS_RUBYCID, ((PartInfo)parts.get(1)).getContentId(true));
    }

    @Test
    public void testIterator2() throws Exception {
        SwaTestcaseFactory stfu = new SwaTestcaseFactory(23, 888, 29L );
        byte[] testMsg = stfu.makeTestMessage();
        InputStream mess = new ByteArrayInputStream(testMsg);
        MimeBody mm = new MimeBody(new ByteArrayStashManager(),
                                                   ContentTypeHeader.parseValue("multipart/mixed; boundary=\"" +
                                                                                new String(stfu.getBoundary()) + "\""),
                                                   mess,FIRST_PART_MAX_BYTE);

        List parts = new ArrayList();
        for (PartIterator i = mm.iterator(); i.hasNext(); ) {
            PartInfo partInfo = i.next();
            if (partInfo.getPosition() == 0)
                continue;
            parts.add(partInfo);
            log.info("Saw part: " + partInfo.getContentId(true));
            partInfo.getPosition();
            // Force body to be stashed
            //partInfo.getInputStream(false).close();
        }

        assertEquals( 22L, (long) parts.size() );
    }

    /**
     * Test case for bug 3470.
     *
     * There was an off by one error in the part iteration logic that could
     * cause the last part to be missed when iterating.
     */
    @Test
    public void testIterationWithoutConsumption() throws Exception {
        SwaTestcaseFactory stfu = new SwaTestcaseFactory(4, 1024*50, 33L );
        byte[] testMsg = stfu.makeTestMessage();
        InputStream mess = new ByteArrayInputStream(testMsg);

        MimeBody mm = new MimeBody(new ByteArrayStashManager(),
                                                   ContentTypeHeader.parseValue("multipart/mixed; boundary=\"" +
                                                                                new String(stfu.getBoundary()) + "\""),
                                                   mess,FIRST_PART_MAX_BYTE);

        List parts = new ArrayList();
        for (PartIterator i = mm.iterator(); i.hasNext(); ) {
            PartInfo partInfo = i.next();
            parts.add(partInfo);
        }

        assertEquals( 4L, (long) parts.size() );
    }

    @Test
    public void testContentLengthThatLies() throws Exception {
        final ContentTypeHeader ct = ContentTypeHeader.parseValue("multipart/related; boundary=blah");
        final String mess = "--blah\r\nContent-Length: 10\r\n\r\n\r\n--blah\r\n\r\n--blah--";
        try {
            // Test fail on getActualContentLength
            MimeBody mm = new MimeBody(mess.getBytes(), ct,FIRST_PART_MAX_BYTE);
            long len = mm.getPart(0).getActualContentLength();
            fail("Failed to throw expected exception on Content-Length: header that lies (got len=" + len + ")");
        } catch (IOException e) {
            // ok
            log.info("Correct exception thrown on Content-Length: header discovered to be lying: " + e.getMessage());
        }

        try {
            // Test fail during iteration
            int num = 0;
            MimeBody mm = new MimeBody(mess.getBytes(), ct,FIRST_PART_MAX_BYTE);
            for (PartIterator i = mm.iterator(); i.hasNext(); ) {
                PartInfo partInfo = i.next();
                partInfo.getInputStream(false).close();
                num++;
            }
            fail("Failed to throw expected exception on Content-Length: header that lies (saw " + num + " parts)");
        } catch (IOException e) {
            // ok
            log.info("Correct exception thrown on Content-Length: header discovered to be lying: " + e.getMessage());
        }
    }

    @Test
    public void testFailureToStartAtPartZero() throws Exception {
        try {
            makeMessage(MESS3, MESS3_CONTENT_TYPE,FIRST_PART_MAX_BYTE);
            fail("Failed to detect multipart content type start parameter that points somewhere other than the first part");
        } catch (IOException e) {
            // Ok
        }
    }

    @Test
    public void testBug2180() throws Exception {
        makeMessage(BUG_2180, MESS_BUG_2180_CTYPE,FIRST_PART_MAX_BYTE);
    }

    @Test
    public void testStreamValidatedParts() throws Exception {
        MimeBody mm = makeMessage(MESS2, MESS2_CONTENT_TYPE,FIRST_PART_MAX_BYTE);
        mm.setEntireMessageBodyAsInputStreamIsValidatedOnly();
        mm.readAndStashEntireMessage();
        assertTrue(mm.getNumPartsKnown()!=1); // Not a valid test if the message only has one part to start with

        // Serialize
        byte[] output = IOUtils.slurpStream(mm.getEntireMessageBodyAsInputStream(false));

        // Check only one part
        MimeBody rebuilt = new MimeBody(output, ContentTypeHeader.parseValue(MESS2_CONTENT_TYPE),FIRST_PART_MAX_BYTE);
        rebuilt.readAndStashEntireMessage();

        assertEquals( 1L, (long) rebuilt.getNumPartsKnown() );
    }
    
    @Test
    public void testGetBytesIfAlreadyAvailable() throws Exception {
        SwaTestcaseFactory stfu = new SwaTestcaseFactory(1, 50000, 29L );
        InputStream in = new ByteArrayInputStream(stfu.makeTestMessage());

        final HybridStashManager stashManager = new HybridStashManager(2038, new File("."), "testGBIAA_1");
        try {
            MimeBody mm = new MimeBody(
                    stashManager,
                    ContentTypeHeader.parseValue("multipart/mixed; boundary=\"" +
                            new String(stfu.getBoundary()) + "\""),
                    in,FIRST_PART_MAX_BYTE);

            byte[] firstPart = IOUtils.slurpStream(mm.getPart(0).getInputStream(false));
            assertEquals( (long) firstPart.length, 50000L );

            // Fetch with high limit should succeed
            byte[] got = mm.getFirstPart().getBytesIfAvailableOrSmallerThan(99999);
            assertTrue(Arrays.equals(got, firstPart));

            // Fetch with low limit should return null
            got = mm.getFirstPart().getBytesIfAvailableOrSmallerThan(1024);
            assertEquals(got, null);
        } finally {
            stashManager.close();
        }
    }

    @BugNumber(9107)
    @Test
    public void testIOExceptionDuringInitialStash_checkingLength() throws Exception {
        @SuppressWarnings({"ThrowableInstanceNeverThrown"})
        MimeBody m = new MimeBody(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new IOExceptionThrowingInputStream(new IOException("Problem?")),FIRST_PART_MAX_BYTE);

        try {
            m.getEntireMessageBodyLength();
            fail("Expected exception not thrown");
        } catch (IOException e) {
            // Ok
        }

        try {
            m.getEntireMessageBodyLength();
            fail("Expected exception not thrown");
        } catch (IOException e) {
            assertTrue(ExceptionUtils.getMessage(e).contains("Problem?"));
        }
    }

    @BugNumber(9107)
    @Test
    public void testIOExceptionDuringInitialStash_reading() throws Exception {
        @SuppressWarnings({"ThrowableInstanceNeverThrown"})
        MimeBody m = new MimeBody(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new IOExceptionThrowingInputStream(new IOException("Problem?")),FIRST_PART_MAX_BYTE);

        InputStream s = null;
        try {
            IOUtils.copyStream(s = m.getEntireMessageBodyAsInputStream(false), new NullOutputStream());
            fail("Expected exception not thrown");
        } catch (IOException e) {
            // Ok
        } finally {
            ResourceUtils.closeQuietly(s);
        }

        try {
            IOUtils.copyStream(s = m.getEntireMessageBodyAsInputStream(false), new NullOutputStream());
            fail("Expected exception not thrown");
        } catch (IOException e) {
            assertTrue(ExceptionUtils.getMessage(e).contains("Problem?"));
        } finally {
            ResourceUtils.closeQuietly(s);
        }
    }

    @BugNumber(9505)
    @Test
    public void testSizeLimitSinglePartNonXml() throws Exception {
        final String body = "non-xml blah blah blah blah blah";
        final String ctype = "application/x-whatever";
        for ( final Boolean destroyAsRead : list( true, false ) ) {
            doTestSizeLimit("Should enforce and fail limit", body, ctype, 12L, true, destroyAsRead);
            doTestSizeLimit("Should not fail limit; body too small", body, ctype, 64L, false, destroyAsRead);
            doTestSizeLimit("Should not fail limit; no limit", body, ctype, 0L, false, destroyAsRead);
        }
    }

    @BugNumber(9505)
    @Test
    public void testSizeLimitSinglePartXml() throws Exception {
        final String body = SOAP;
        final String ctype = "text/xml";
        for ( final Boolean destroyAsRead : list( true, false ) ) {
            doTestSizeLimit("Should enforce and fail limit", body, ctype, 256L, true, destroyAsRead);
            doTestSizeLimit("Should not fail limit; body too small", body, ctype, 4096L, false, destroyAsRead);
            doTestSizeLimit("Should not fail limit; no limit", body, ctype, 0L, false, destroyAsRead);
        }
    }

    @BugNumber(9505)
    @Test
    public void testSizeLimitMultiPartNonXml() throws Exception {
        final String body = MESS_NONXML;
        final String ctype = MESS_NONXML_CONTENT_TYPE;
        for ( final Boolean destroyAsRead : list( true, false ) ) {
            doTestSizeLimit("Should enforce and fail limit", body, ctype, 400L, true, destroyAsRead);
            doTestSizeLimit("Should not fail limit; body too small", body, ctype, 8192L, false, destroyAsRead);
            doTestSizeLimit("Should not fail limit; no limit", body, ctype, 0L, false, destroyAsRead);
        }
    }

    @BugNumber(9505)
    @Test
    public void testSizeLimitMultiPartXml() throws Exception {
        final String body = MESS;
        final String ctype = MESS_CONTENT_TYPE;
        for ( final Boolean destroyAsRead : list( true, false ) ) {
            doTestSizeLimit("Should enforce and fail limit", body, ctype, 400L, true, destroyAsRead);
            doTestSizeLimit("Should not fail limit; body too small", body, ctype, destroyAsRead ? 514L : 2000L, false, destroyAsRead); //Size is 450 +  64 to ensure bounary can be read
            doTestSizeLimit("Should not fail limit; no limit", body, ctype, 0L, false, destroyAsRead);
        }
    }

    private void doTestSizeLimit(String msg, String body, String ctype, long limit, boolean expectSizeFailure, boolean destroyAsRead) throws IOException, NoSuchPartException {
        MimeBody m = null;
        InputStream s = null;
        try {
            m = makeMessage(body, ctype, limit);
            PartInfo part0 = m.getPart( 0 );
            s = part0.getInputStream( destroyAsRead );
            while ( s.read(new byte[64]) >= 0 ) {
                // read some more ...
            }
            if (expectSizeFailure)
                fail(msg);
            try {
                for ( int i=1; i<1000; i++ ) {
                    PartInfo part = m.getPart( i );
                    ResourceUtils.closeQuietly( s );
                    IOUtils.copyStream( s = part.getInputStream( destroyAsRead ), new NullOutputStream() );
                }
            } catch ( NoSuchPartException e ) {
                // no more parts to check
            } catch (ByteLimitInputStream.DataSizeLimitExceededException e) {
                if (!expectSizeFailure)
                    fail(msg + " (after reading first part)");
            }
        } catch (ByteLimitInputStream.DataSizeLimitExceededException e) {
            if (!expectSizeFailure)
                fail(msg);
        } finally {
            ResourceUtils.closeQuietly(s);
            ResourceUtils.closeQuietly(m);
        }
    }

    private static String encode( final String text, final String encoding ) {
        try {
            return new String( IOUtils.slurpStream( MimeUtil.getEncodingInputStream( text.getBytes(), encoding )));
        } catch ( IOException e ) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @BugNumber(10464)
    public void testContentLengthHeadersAdded() throws Exception {
        final String body = BUG_10464_MESS;
        final String ctype = BUG_10464_CTYPE;
        MimeBody mb = new MimeBody(body.getBytes(Charsets.UTF8), ContentTypeHeader.create(ctype), FIRST_PART_MAX_BYTE);
        byte[] bytes = IOUtils.slurpStream(mb.getEntireMessageBodyAsInputStream(false));
        String result = new String(bytes, Charsets.UTF8);
        assertFalse("To avoid breaking signatures, Content-Length headers shall not be added to parts that did not originally have them",
                result.toLowerCase().contains("content-length:"));
    }

    public final String MESS_SOAPCID = "-76394136.15558";
    public final String MESS_RUBYCID = "-76392836.15558";
    public static final String MESS_BOUNDARY = "----=Part_-763936460.407197826076299";
    public static final String MESS_CONTENT_TYPE = "multipart/related; type=\"text/xml\"; boundary=\"" +
            MESS_BOUNDARY+ "\"; start=\"-76394136.15558\"";
    public static final String MESS_PAYLOAD_NS = "urn:EchoAttachmentsService";
    public static final String SOAP = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
            "<env:Envelope xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "    xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "  <env:Body>\n" +
            "    <n1:echoOne xmlns:n1=\"" + MESS_PAYLOAD_NS + "\"\n" +
            "        env:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "      <file href=\"cid:-76392836.15558\"></file>\n" +
            "    </n1:echoOne>\n" +
            "  </env:Body>\n" +
            "</env:Envelope>\n";

    public static final String RUBY = "require 'soap/rpc/driver'\n" +
            "require 'soap/attachment'\n" +
            "\n" +
            "attachment = ARGV.shift || __FILE__\n" +
            "\n" +
            "#server = 'http://localhost:7700/'\n" +
            "server = 'http://data.l7tech.com:80/ssg/soap'\n" +
            "\n" +
            "driver = SOAP::RPC::Driver.new(server, 'urn:EchoAttachmentsService')\n" +
            "driver.wiredump_dev = STDERR\n" +
            "driver.add_method('echoOne', 'file')\n" +
            "\n" +
            "File.open(attachment)  do |fin|\n" +
            "  File.open('attachment.out', 'w') do |fout|\n" +
            "\tfout << driver.echoOne(SOAP::Attachment.new(fin))\n" +
            "  end      \n" +
            "end\n" +
            "\n" +
            "\n";

    public static final String MESS = "PREAMBLE GARBAGE\r\nBLAH BLAH BLAH\r\n------=Part_-763936460.407197826076299\r\n" +
            "Content-Transfer-Encoding: 8bit\r\n" +
            "Content-Type: text/xml; charset=utf-8\r\n" +
            "Content-ID: -76394136.15558\r\n" +
            "\r\n" +
            SOAP +
            "\r\n" +
            "------=Part_-763936460.407197826076299\r\n" +
            "Content-Transfer-Encoding: 8bit\r\n" +
            "Content-Type: application/octet-stream\r\n" +
            "Content-ID: <-76392836.15558>\r\n" +
            "\r\n" +
             RUBY +
            "\r\n" +
            "------=Part_-763936460.407197826076299--\r\n";

    public static final String MESS_NONXML_CONTENT_TYPE = "multipart/related; type=\"application/x-rubything\"; boundary=\"" +
            MESS_BOUNDARY+ "\"; start=\"-76394136.15558\"";
    public static final String MESS_NONXML = "------=Part_-763936460.407197826076299\r\n" +
            "Content-Transfer-Encoding: 8bit\r\n" +
            "Content-Type: application/x-rubything; charset=utf-8\r\n" +
            "Content-ID: -76394136.15558\r\n" +
            "\r\n" +
            RUBY +
            "\r\n" +
            "------=Part_-763936460.407197826076299\r\n" +
            "Content-Transfer-Encoding: 8bit\r\n" +
            "Content-Type: application/octet-stream\r\n" +
            "Content-ID: <-76392836.15558>\r\n" +
            "\r\n" +
             RUBY +
            "\r\n" +
            "------=Part_-763936460.407197826076299--\r\n";

    public static final String MESS_ENC = "PREAMBLE GARBAGE\r\nBLAH BLAH BLAH\r\n------=Part_-763936460.407197826076299\r\n" +
            "Content-Transfer-Encoding: BAsE64\r\n" +
            "Content-Type: text/xml; charset=utf-8\r\n" +
            "Content-ID: -76394136.15558\r\n" +
            "\r\n" +
            encode(SOAP, "base64") +
            "\r\n" +
            "------=Part_-763936460.407197826076299\r\n" +
            "cOntent-transfEr-encOding: qUoTeD-prinTaBLE\r\n" +
            "Content-Type: aPplication/octeT-stream\r\n" +
            "Content-ID: <-76392836.15558>\r\n" +
            "\r\n" +
            encode(RUBY, "quoted-printable") +
            "\r\n" +
            "------=Part_-763936460.407197826076299--\r\n";

    public static final String MESS2_BOUNDARY = "----=Part_-763936460.00306951464153826";
    public static final String MESS2_CONTENT_TYPE = "multipart/related; type=\"text/xml\"; boundary=\"" +
            MESS2_BOUNDARY + "\"; start=\"-76394136.13454\"";
    public static final String MESS2 = "------=Part_-763936460.00306951464153826\r\n" +
            "Content-Transfer-Encoding: 8bit\r\n" +
            "Content-Type: text/xml; charset=utf-8\r\n" +
            "Content-ID: -76394136.13454\r\n" +
            "\r\n" +
            "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
            "<env:Envelope xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "    xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "  <env:Body>\n" +
            "    <n1:echoOne xmlns:n1=\"urn:EchoAttachmentsService\"\n" +
            "        env:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "      <file href=\"cid:-76392836.13454\"></file>\n" +
            "    </n1:echoOne>\n" +
            "  </env:Body>\n" +
            "</env:Envelope>\n" +
            "\r\n" +
            "------=Part_-763936460.00306951464153826\r\n" +
            "Content-Transfer-Encoding: 8bit\r\n" +
            "Content-Type: application/octet-stream\r\n" +
            "Content-ID: <-76392836.13454>\r\n" +
            "\r\n" +
            "require 'soap/rpc/driver'\n" +
            "require 'soap/attachment'\n" +
            "\n" +
            "attachment = ARGV.shift || __FILE__\n" +
            "\n" +
            "#server = 'http://localhost:7700/'\n" +
            "server = 'http://data.l7tech.com:80/'\n" +
            "\n" +
            "driver = SOAP::RPC::Driver.new(server, 'urn:EchoAttachmentsService')\n" +
            "driver.wiredump_dev = STDERR\n" +
            "driver.add_method('echoOne', 'file')\n" +
            "\n" +
            "File.open(attachment)  do |fin|\n" +
            "  File.open('attachment.out', 'w') do |fout|\n" +
            ".fout << driver.echoOne(SOAP::Attachment.new(fin))\n" +
            "  end      \n" +
            "end\n" +
            "\n" +
            "\n" +
            "\r\n" +
            "------=Part_-763936460.00306951464153826--\r\n";

    // A test message with a start parameter pointing at the second attachment
    public static final String MESS3_BOUNDARY = "----=Part_-763936460.00306951464153826";
    public static final String MESS3_CONTENT_TYPE = "multipart/related; type=\"text/xml\"; boundary=\"" +
            MESS2_BOUNDARY + "\"; start=\"-76392836.13454\"";
    public static final String MESS3 = "------=Part_-763936460.00306951464153826\r\n" +
            "Content-Transfer-Encoding: 8bit\r\n" +
            "Content-Type: text/xml; charset=utf-8\r\n" +
            "Content-ID: -76394136.13454\r\n" +
            "\r\n" +
            "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
            "<env:Envelope xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "    xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "  <env:Body>\n" +
            "    <n1:echoOne xmlns:n1=\"urn:EchoAttachmentsService\"\n" +
            "        env:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "      <file href=\"cid:-76392836.13454\"></file>\n" +
            "    </n1:echoOne>\n" +
            "  </env:Body>\n" +
            "</env:Envelope>\n" +
            "\r\n" +
            "------=Part_-763936460.00306951464153826\r\n" +
            "Content-Transfer-Encoding: 8bit\r\n" +
            "Content-Type: application/octet-stream\r\n" +
            "Content-ID: <-76392836.13454>\r\n" +
            "\r\n" +
            "require 'soap/rpc/driver'\n" +
            "require 'soap/attachment'\n" +
            "\n" +
            "attachment = ARGV.shift || __FILE__\n" +
            "\n" +
            "#server = 'http://localhost:7700/'\n" +
            "server = 'http://data.l7tech.com:80/'\n" +
            "\n" +
            "driver = SOAP::RPC::Driver.new(server, 'urn:EchoAttachmentsService')\n" +
            "driver.wiredump_dev = STDERR\n" +
            "driver.add_method('echoOne', 'file')\n" +
            "\n" +
            "File.open(attachment)  do |fin|\n" +
            "  File.open('attachment.out', 'w') do |fout|\n" +
            ".fout << driver.echoOne(SOAP::Attachment.new(fin))\n" +
            "  end      \n" +
            "end\n" +
            "\n" +
            "\n" +
            "\r\n" +
            "------=Part_-763936460.00306951464153826--\r\n";


    private static final String MESS_BUG_2180_CTYPE = "multipart/related; type=\"text/xml\"; start=\"<40080056B6C225289B8E845639E05547>\"; \tboundary=\"----=_Part_4_20457766.1136482180671\"";
    public static final String BUG_2180 =
            "------=_Part_4_20457766.1136482180671\r\n" +
            "Content-Type: text/xml; charset=UTF-8\r\n" +
            "Content-Transfer-Encoding: binary\r\n" +
            "Content-Id: <40080056B6C225289B8E845639E05547>\r\n" +
            "\r\n" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            " <soapenv:Body>\n" +
            "  <ns1:echoDir soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:ns1=\"urn:EchoAttachmentsService\">\n" +
            "   <source xsi:type=\"soapenc:Array\" soapenc:arrayType=\"ns1:DataHandler[1]\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "    <item href=\"cid:5DE0F2F64B5AFDB73D4C8FF242FABEE6\"/>\n" +
            "   </source>\n" +
            "  </ns1:echoDir>\n" +
            " </soapenv:Body>\n" +
            "</soapenv:Envelope>\n" +
            "------=_Part_4_20457766.1136482180671\r\n" +
            "Content-Type: text/plain\r\n" +
            "Content-Transfer-Encoding: binary\r\n" +
            "Content-Id: <5DE0F2F64B5AFDB73D4C8FF242FABEE6>\r\n" +
            "\r\n" +
            "abc123\n" +
            "------=_Part_4_20457766.1136482180671--";

    private static final String BUG_10464_CTYPE = "multipart/related; type=\"text/xml\"; start=\"<rootpart@soapui.org>\"; boundary=\"----=_Part_38_1469256273.1305075794627\"";
    private static final String BUG_10464_MESS =
            "------=_Part_38_1469256273.1305075794627\r\n" +
                    "Content-Type: text/xml; charset=UTF-8\r\n" +
                    "Content-Transfer-Encoding: 8bit\r\n" +
                    "Content-ID: <rootpart@soapui.org>\r\n" +
                    "\r\n" +
                    "<wheatley/>\r\n" +
                    "------=_Part_38_1469256273.1305075794627\r\n" +
                    "Content-Type: text/plain; charset=us-ascii\r\n" +
                    "Content-Transfer-Encoding: 7bit\r\n" +
                    "\r\n" +
                    "glados\r\n" +
                    "------=_Part_38_1469256273.1305075794627--";
}
