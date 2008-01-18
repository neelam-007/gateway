package com.l7tech.common.http;

import junit.framework.TestCase;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Arrays;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

import com.l7tech.common.mime.ContentTypeHeader;

/**
 * Tests the code that guesses the character encoding of XML files that are retrieved through
 * HTTP.
 */
public class GenericHttpClientXmlDecodingTest  extends TestCase {
    private static class MockHttpResponse extends GenericHttpResponse {
        private ByteArrayInputStream inputStream = null;
        private ContentTypeHeader contentTypeHeader = null;

        public MockHttpResponse(byte[] bytes, ContentTypeHeader contentTypeHeader) {
            inputStream = new ByteArrayInputStream(bytes);
            this.contentTypeHeader = contentTypeHeader;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public int getStatus() {
            return 200;
        }

        public HttpHeaders getHeaders() {
            return null;
        }

        public void close() {
        }

        public Long getContentLength() {
            return new Long(inputStream.available());
        }

        public ContentTypeHeader getContentType() {
            return contentTypeHeader;
        }
    }

    private static final String XML_MESSAGE = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                                              "<test_document>\n" +
                                              "    <test_element myattribute=\"value\">Text</test_element>\n" +
                                              "</test_document>";
    private static final String ENCODING_PATTERN = "^<\\?xml.*encoding=\".*\"\\?>";

    private static String getMessageWithEncoding(String encoding) {
        return XML_MESSAGE.replaceFirst(ENCODING_PATTERN, "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>");
    }

    public void testUTF32BEWithBOM() throws Exception {
        String message = getMessageWithEncoding("UTF-32BE");
        byte[] messageBytes = message.getBytes("X-UTF-32BE-BOM");

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("UTF-32BE", guessedEncodingResult.encoding);

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true));
    }

    public void testUTF32BEWithoutBOM() throws Exception {
        String message = getMessageWithEncoding("UTF-32BE");
        byte[] bytes = message.getBytes("X-UTF-32BE-BOM");
        byte[] messageBytes = Arrays.copyOfRange(bytes, 4, bytes.length);

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("UTF-32BE", guessedEncodingResult.encoding);

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true));
    }

    public void testUTF32LEWithBOM() throws Exception {
        String message = getMessageWithEncoding("UTF-32LE");
        byte[] messageBytes = message.getBytes("X-UTF-32LE-BOM");

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("UTF-32LE", guessedEncodingResult.encoding);

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true));
    }

    public void testUTF16LEWithBOM() throws Exception {
        String message = getMessageWithEncoding("UTF-16LE");
        byte[] messageBytes = message.getBytes("X-UTF-16LE-BOM");

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("X-UTF-16LE-BOM", guessedEncodingResult.encoding);

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true));
    }

    public void testUTF16BEWithBOM() throws Exception {
        String message = getMessageWithEncoding("UTF-16BE");
        byte[] bytes = message.getBytes("UTF-16BE");
        byte[] messageBytes = new byte[bytes.length + 2];
        messageBytes[0] = (byte)0xfe;
        messageBytes[1] = (byte)0xff;
        System.arraycopy(bytes, 0, messageBytes, 2, bytes.length);

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("UTF-16BE", guessedEncodingResult.encoding);

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true));
    }

    public void testUTF8WithBOM() throws Exception {
        String message = getMessageWithEncoding("UTF-8");
        byte[] bytes = message.getBytes("UTF-8");
        byte[] messageBytes = new byte[bytes.length + 3];
        messageBytes[0] = (byte)0xef;
        messageBytes[1] = (byte)0xbb;
        messageBytes[2] = (byte)0xbf;
        System.arraycopy(bytes, 0, messageBytes, 3, bytes.length);

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("UTF-8", guessedEncodingResult.encoding);

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true));
    }

    public void testUTF32LEWithoutBOM() throws Exception {
        String message = getMessageWithEncoding("UTF-32LE");
        byte[] bytes = message.getBytes("X-UTF-32LE-BOM");
        byte[] messageBytes = Arrays.copyOfRange(bytes, 4, bytes.length);

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("UTF-32LE", guessedEncodingResult.encoding);

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true));
    }

    public void testUTF16LEWithoutBOM() throws Exception {
        String message = getMessageWithEncoding("UTF-16LE");
        byte[] bytes = message.getBytes("X-UTF-16LE-BOM");
        byte[] messageBytes = Arrays.copyOfRange(bytes, 2, bytes.length);

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("UTF-16LE", guessedEncodingResult.encoding);

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true));
    }

    public void testUTF16BEWithoutBOM() throws Exception {
        String message = getMessageWithEncoding("UTF-16BE");
        byte[] messageBytes = message.getBytes("UTF-16BE");

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("UTF-16BE", guessedEncodingResult.encoding);

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true));
    }

    public void testUTF8WithoutBOM() throws Exception {
        String message = getMessageWithEncoding("UTF-8");
        byte[] messageBytes = message.getBytes("UTF-8");

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("UTF-8", guessedEncodingResult.encoding);

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true));
    }

    public void testCp1047() throws Exception {
        String message = getMessageWithEncoding("Cp1047");
        byte[] messageBytes = message.getBytes("Cp1047");

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("Cp1047", guessedEncodingResult.encoding);

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true));
    }

    public void testISO_8859_1() throws Exception {
        String message = getMessageWithEncoding("ISO-8859-1");
        byte[] messageBytes = message.getBytes("ISO-8859-1");

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("ISO-8859-1", guessedEncodingResult.encoding);

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true));
    }
}
