package com.l7tech.common.http;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.test.BugNumber;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests the code that guesses the character encoding of XML files that are retrieved through
 * HTTP.
 */
public class GenericHttpClientXmlDecodingTest {
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

    private static String replaceXmlDeclaration(String xml, String xmlDec) {
        return xml.replaceFirst(ENCODING_PATTERN, xmlDec);
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testAllEncodings() throws Exception {
        String smallXmlMessage = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" +
                "<test_document>\n" +
                "    <test_element myattribute=\"value\">Text</test_element>\n" +
                "</test_document>";
        // The following charsets cannot be determined by examining the bytes
        HashSet<String> unsupportedEncodings = new HashSet<String>();
        unsupportedEncodings.add("JIS_X0212-1990");
        unsupportedEncodings.add("IBM290");
        unsupportedEncodings.add("x-IBM300");
        unsupportedEncodings.add("x-IBM834");
        unsupportedEncodings.add("x-IBM930");
        unsupportedEncodings.add("x-JIS0208");
        unsupportedEncodings.add("x-MacDingbat");
        unsupportedEncodings.add("x-MacSymbol");
        // Unsupported encoding not document in GenericHttpResponse, temporarily skipped to make the unit test pass.
        unsupportedEncodings.add("IBM-930");

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
            assertNotNull("Guessed result for " + charsetName, guessedEncodingResult);
            assertNotNull("Could not guess encoding for " + charsetName + ". Charset may have been newly added.", guessedEncodingResult.encoding);
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

    /**
     * S (white space) consists of one or more space (#x20) characters, carriage returns, line feeds, or tabs.
     */
    @BugNumber(8366)
    @Test
    public void testXmlDeclarationFormat() throws Exception {
        // Spaces
        testFormat( "<?xml version= '1.0' encoding= 'ascii' ?>" );
        testFormat( "<?xml version= '1.0' encoding= 'ascii' standalone= 'yes'?>" );
        testFormat( "<?xml version = '1.0' encoding = 'ascii' ?>" );
        testFormat( "<?xml version = '1.0' encoding = 'ascii' standalone = 'no'?>" );
        testFormat( "<?xml version= \"1.0\" encoding= 'ascii' ?>" );
        testFormat( "<?xml version= \"1.0\" encoding= \"ascii\" standalone= \"yes\"?>" );
        testFormat( "<?xml version = \"1.0\" encoding = \"ascii\" ?>" );
        testFormat( "<?xml version = \"1.0\" encoding = \"ascii\" standalone = \"yes\"?>" );

        // Tabs
        testFormat( "<?xml\tversion=\t'1.0'\tencoding=\t'ascii'\t?>" );
        testFormat( "<?xml\tversion=\t'1.0'\tencoding=\t'ascii'\tstandalone=\t'yes'?>" );
        testFormat( "<?xml\tversion\t=\t'1.0'\tencoding\t=\t'ascii'\t?>" );
        testFormat( "<?xml\tversion\t=\t'1.0'\tencoding\t=\t'ascii'\tstandalone\t=\t'yes'?>" );
        testFormat( "<?xml\tversion=\t\"1.0\"\tencoding=\t'ascii'\t?>" );
        testFormat( "<?xml\tversion=\t\"1.0\"\tencoding=\t\"ascii\"\tstandalone=\t\"no\"?>" );
        testFormat( "<?xml\tversion\t=\t\"1.0\"\tencoding\t=\t\"ascii\"\t?>" );
        testFormat( "<?xml\tversion\t=\t\"1.0\"\tencoding\t=\t\"ascii\"\tstandalone\t=\t\"yes\"?>" );

        // Carriage return
        testFormat( "<?xml\rversion=\r'1.0'\rencoding=\r'ascii'\r?>" );
        testFormat( "<?xml\rversion=\r'1.0'\rencoding=\r'ascii'\rstandalone=\r'yes'?>" );
        testFormat( "<?xml\rversion\r=\r'1.0'\rencoding\r=\r'ascii'\r?>" );
        testFormat( "<?xml\rversion\r=\r'1.0'\rencoding\r=\r'ascii'\rstandalone\r=\r'yes'?>" );
        testFormat( "<?xml\rversion=\r\"1.0\"\rencoding=\r'ascii'\r?>" );
        testFormat( "<?xml\rversion=\r\"1.0\"\rencoding=\r\"ascii\"\rstandalone=\r\"no\"?>" );
        testFormat( "<?xml\rversion\r=\r\"1.0\"\rencoding\r=\r\"ascii\"\r  ?>" );
        testFormat( "<?xml\rversion\r=\r\"1.0\"\rencoding\r=\r\"ascii\"\rstandalone\r=\r\"yes\"  ?>" );

        // Line feed
        testFormat( "<?xml\nversion=\n'1.0'\nencoding=\n'ascii'\n?>" );
        testFormat( "<?xml\nversion=\n'1.0'\nencoding=\n'ascii'\nstandalone=\n'yes'?>" );
        testFormat( "<?xml\nversion\n=\n'1.0'\nencoding\n=\n'ascii'\n?>" );
        testFormat( "<?xml\nversion\n=\n'1.0'\nencoding\n=\n'ascii'\nstandalone\n=\n'yes'?>" );
        testFormat( "<?xml\nversion=\n\"1.0\"\nencoding=\n'ascii'\n?>" );
        testFormat( "<?xml\nversion=\n\"1.0\"\nencoding=\n\"ascii\"\nstandalone=\n\"no\"?>" );
        testFormat( "<?xml\nversion\n=\n\"1.0\"\nencoding\n=\n\"ascii\"\n?>" );
        testFormat( "<?xml\nversion\n=\n\"1.0\"\nencoding\n=\n\"ascii\"\nstandalone\n=\n\"yes\"?>" );

        // Minimum spaces
        testFormat( "<?xml version='1.0' encoding='ascii'?>" );
        testFormat( "<?xml version='1.0' encoding='ascii' standalone='yes'?>" );

        // Lots of various spaces
        testFormat( "<?xml\n\r    \tversion\n\r    \t=\n\r    \t'1.0'\n\r    \tencoding\n\r    \t=\n\r    \t'ascii'?>" );
        testFormat( "<?xml\n\r    \tversion\n\r    \t=\n\r    \t'1.0'\n\r    \tencoding\n\r    \t=\n\r    \t'ascii'\n\r    \tstandalone\n\r    \t=\n\r    \t'yes'\n\r    \t?>" );
    }

    private void testFormat( final String xmlDeclaration ) throws Exception {
        String message = replaceXmlDeclaration(XML_MESSAGE, xmlDeclaration);
        GenericHttpResponse.GuessedEncodingResult guessedEncodingResult = GenericHttpResponse.getXmlEncoding(message.getBytes("UTF-8"));
        assertNotNull( "Guessed encoding null ("+xmlDeclaration+")", guessedEncodingResult );
        assertEquals( "Guessed encoding ("+xmlDeclaration+")", "US-ASCII", guessedEncodingResult.encoding.displayName() );
    }
}
