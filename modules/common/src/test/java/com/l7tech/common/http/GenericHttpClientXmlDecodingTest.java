package com.l7tech.common.http;

import com.l7tech.common.mime.ContentTypeHeader;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Tests the code that guesses the character encoding of XML files that are retrieved through
 * HTTP.
 */
public class GenericHttpClientXmlDecodingTest  extends TestCase {
    private final int maxResponseSize = 1024 * 1024 * 10;

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

    // Use a large message (> 1024 bytes), since no more than 1024 bytes are examined
    private static final String XML_MESSAGE;
    static {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
        sb.append("<test_document>\n");
        for(int i = 0;i < 1024;i++) {
            sb.append("    <test_element myattribute=\"value\">Text</test_element>\n");
        }
        sb.append("</test_document>");

        XML_MESSAGE = sb.toString();
    }

    private static final String ENCODING_PATTERN = "^<\\?xml.*encoding=\".*\"\\?>";

    private static String getMessageWithEncoding(String encoding) {
        return XML_MESSAGE.replaceFirst(ENCODING_PATTERN, "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>");
    }

    private static String replaceEncoding(String xml, String encoding) {
        return xml.replaceFirst(ENCODING_PATTERN, "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>");
    }

    public void testUTF32BEWithBOM() throws Exception {
        String message = getMessageWithEncoding("UTF-32BE");
        byte[] messageBytes = message.getBytes("X-UTF-32BE-BOM");

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("UTF-32BE", guessedEncodingResult.encoding.name());

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true, maxResponseSize));
    }

    public void testUTF32BEWithoutBOM() throws Exception {
        String message = getMessageWithEncoding("UTF-32BE");
        byte[] bytes = message.getBytes("X-UTF-32BE-BOM");
        byte[] messageBytes = Arrays.copyOfRange(bytes, 4, bytes.length);

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("UTF-32BE", guessedEncodingResult.encoding.name());

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true, maxResponseSize));
    }

    public void testUTF32LEWithBOM() throws Exception {
        String message = getMessageWithEncoding("UTF-32LE");
        byte[] messageBytes = message.getBytes("X-UTF-32LE-BOM");

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("UTF-32LE", guessedEncodingResult.encoding.name());

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true, maxResponseSize));
    }

    public void testUTF16LEWithBOM() throws Exception {
        String message = getMessageWithEncoding("UTF-16LE");
        byte[] messageBytes = message.getBytes("X-UTF-16LE-BOM");

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals(Charset.forName("X-UTF-16LE-BOM").name(), guessedEncodingResult.encoding.name());

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true, maxResponseSize));
    }

    public void testUTF16BEWithBOM() throws Exception {
        String message = getMessageWithEncoding("UTF-16BE");
        byte[] bytes = message.getBytes("UTF-16BE");
        byte[] messageBytes = new byte[bytes.length + 2];
        messageBytes[0] = (byte)0xfe;
        messageBytes[1] = (byte)0xff;
        System.arraycopy(bytes, 0, messageBytes, 2, bytes.length);

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("UTF-16BE", guessedEncodingResult.encoding.name());

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true, maxResponseSize));
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
        assertEquals("UTF-8", guessedEncodingResult.encoding.name());

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true, maxResponseSize));
    }

    public void testUTF32LEWithoutBOM() throws Exception {
        String message = getMessageWithEncoding("UTF-32LE");
        byte[] bytes = message.getBytes("X-UTF-32LE-BOM");
        byte[] messageBytes = Arrays.copyOfRange(bytes, 4, bytes.length);

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("UTF-32LE", guessedEncodingResult.encoding.name());

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true, maxResponseSize));
    }

    public void testUTF16LEWithoutBOM() throws Exception {
        String message = getMessageWithEncoding("UTF-16LE");
        byte[] bytes = message.getBytes("X-UTF-16LE-BOM");
        byte[] messageBytes = Arrays.copyOfRange(bytes, 2, bytes.length);

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("UTF-16LE", guessedEncodingResult.encoding.name());

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true, maxResponseSize));
    }

    public void testUTF16BEWithoutBOM() throws Exception {
        String message = getMessageWithEncoding("UTF-16BE");
        byte[] messageBytes = message.getBytes("UTF-16BE");

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("UTF-16BE", guessedEncodingResult.encoding.name());

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true, maxResponseSize));
    }

    public void testUTF8WithoutBOM() throws Exception {
        String message = getMessageWithEncoding("UTF-8");
        byte[] messageBytes = message.getBytes("UTF-8");

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("UTF-8", guessedEncodingResult.encoding.name());

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true, maxResponseSize));
    }

    public void testCp1047() throws Exception {
        String message = getMessageWithEncoding("Cp1047");
        byte[] messageBytes = message.getBytes("Cp1047");

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals(Charset.forName("Cp1047").name(), guessedEncodingResult.encoding.name());

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true, maxResponseSize));
    }

    public void testISO_8859_1() throws Exception {
        String message = getMessageWithEncoding("ISO-8859-1");
        byte[] messageBytes = message.getBytes("ISO-8859-1");

        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(messageBytes);
        assertEquals("ISO-8859-1", guessedEncodingResult.encoding.name());

        String decodedMessage = new String(messageBytes, guessedEncodingResult.bytesToSkip, messageBytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
        assertEquals(message, decodedMessage);

        ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
        MockHttpResponse response = new MockHttpResponse(messageBytes, contentTypeHeader);
        assertEquals(message, response.getAsString(true, maxResponseSize));
    }

    public void testAllEncodings() throws Exception {
        String smallXmlMessage = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" +
                "<test_document>\n" +
                "    <test_element myattribute=\"value\">Text</test_element>\n" +
                "</test_document>";
        // The following charsets cannot be determined by examining the bytes
        HashSet<String> unsupportedEncodings = new HashSet<String>();
        unsupportedEncodings.add("JIS_X0212-1990");
        unsupportedEncodings.add("x-IBM834");
        unsupportedEncodings.add("x-IBM930");
        unsupportedEncodings.add("x-JIS0208");
        unsupportedEncodings.add("x-MacDingbat");
        unsupportedEncodings.add("x-MacSymbol");

        for(String charsetName : Charset.availableCharsets().keySet()) {
            if(unsupportedEncodings.contains(charsetName)) {
                continue;
            }

            // Try the large message
            String msg = getMessageWithEncoding(charsetName);
            byte[] bytes;
            try {
                bytes = msg.getBytes(charsetName);
            } catch(UnsupportedOperationException e) {
                continue; // Cannot test this charset
            }
            GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(bytes);
            String decodedMessage = new String(bytes, guessedEncodingResult.bytesToSkip, bytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
            assertEquals("Big message (charset = " + charsetName + ")", msg, decodedMessage);

            // Try the small message
            msg = replaceEncoding(smallXmlMessage, charsetName);
            bytes = msg.getBytes(charsetName);
            guessedEncodingResult = GenericHttpResponse.getXmlEncoding(bytes);
            decodedMessage = new String(bytes, guessedEncodingResult.bytesToSkip, bytes.length - guessedEncodingResult.bytesToSkip, guessedEncodingResult.encoding);
            assertEquals("Small message (charset = " + charsetName + ")", msg, decodedMessage);
        }
    }
}
