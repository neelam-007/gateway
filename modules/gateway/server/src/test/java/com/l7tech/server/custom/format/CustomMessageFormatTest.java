package com.l7tech.server.custom.format;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.custom.ContentTypeHeaderToCustomConverter;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.ext.message.CustomJsonData;
import com.l7tech.policy.assertion.ext.message.CustomMessage;
import com.l7tech.policy.assertion.ext.message.CustomMessageAccessException;
import com.l7tech.policy.assertion.ext.message.InvalidDataException;
import com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat;
import com.l7tech.policy.assertion.ext.message.format.CustomMessageFormatFactory;
import com.l7tech.policy.assertion.ext.message.format.NoSuchMessageFormatException;
import com.l7tech.server.custom.CustomMessageImpl;
import com.l7tech.util.IOUtils;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import scala.actors.threadpool.Arrays;

import static com.l7tech.server.policy.custom.CustomAssertionsSampleContents.*;
import static org.junit.Assert.*;

/**
 * Test CustomMessageFormat
 */
public class CustomMessageFormatTest {

    private CustomMessageFormatRegistry formatRegistry;
    private CustomMessageFormatFactory formatFactory;

    @Before
    public void setUp() throws Exception {
        //noinspection serial
        formatRegistry = new CustomMessageFormatRegistry(new HashMap<Class, CustomMessageFormat>(){{
            put(Document.class,
                    new CustomMessageXmlFormat(CustomMessageFormatFactory.XML_FORMAT,
                            CustomMessageFormatFactoryImpl.XML_FORMAT_DESC
                    )
            );
            put(CustomJsonData.class,
                    new CustomMessageJsonFormat(CustomMessageFormatFactory.JSON_FORMAT,
                            CustomMessageFormatFactoryImpl.JSON_FORMAT_DESC
                    )
            );
            put(InputStream.class,
                    new CustomMessageInputStreamFormat(CustomMessageFormatFactory.INPUT_STREAM_FORMAT,
                            CustomMessageFormatFactoryImpl.INPUT_STREAM_FORMAT_DESC
                    )
            );
        }});
        formatFactory = formatRegistry.getMessageFormatFactory();
    }

    private void printKnownFormats() throws Exception {
        System.out.println("-------------------------------------------------------------------");
        for (CustomMessageFormat format: formatFactory.getKnownFormats()) {
            System.out.println("Format; class = " + format.getRepresentationClass().getSimpleName() + "; name = " + format.getFormatName() + "; desc = " + format.getFormatDescription() + ";");
        }
        System.out.println("-------------------------------------------------------------------");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testKnownFormatsExtraction() throws Exception {
        // extract using name
        CustomMessageFormat docFormat = formatFactory.getFormatByName(CustomMessageFormatFactory.XML_FORMAT);
        assertNotNull(docFormat);
        assertTrue(docFormat instanceof CustomMessageXmlFormat);
        assertEquals(docFormat.getFormatName(), CustomMessageFormatFactory.XML_FORMAT);
        CustomMessageFormat jsonFormat = formatFactory.getFormatByName(CustomMessageFormatFactory.JSON_FORMAT);
        assertNotNull(jsonFormat);
        assertTrue(jsonFormat instanceof CustomMessageJsonFormat);
        assertEquals(jsonFormat.getFormatName(), CustomMessageFormatFactory.JSON_FORMAT);
        CustomMessageFormat iStreamFormat = formatFactory.getFormatByName(CustomMessageFormatFactory.INPUT_STREAM_FORMAT);
        assertNotNull(iStreamFormat);
        assertTrue(iStreamFormat instanceof CustomMessageInputStreamFormat);
        assertEquals(iStreamFormat.getFormatName(), CustomMessageFormatFactory.INPUT_STREAM_FORMAT);

        // extract using representation class
        docFormat = formatFactory.getFormatForRepresentationClass(Document.class);
        assertNotNull(docFormat);
        assertTrue(docFormat instanceof CustomMessageXmlFormat);
        assertEquals(docFormat.getFormatName(), CustomMessageFormatFactory.XML_FORMAT);
        jsonFormat = formatFactory.getFormatForRepresentationClass(CustomJsonData.class);
        assertNotNull(jsonFormat);
        assertTrue(jsonFormat instanceof CustomMessageJsonFormat);
        assertEquals(jsonFormat.getFormatName(), CustomMessageFormatFactory.JSON_FORMAT);
        iStreamFormat = formatFactory.getFormatForRepresentationClass(InputStream.class);
        assertNotNull(iStreamFormat);
        assertTrue(iStreamFormat instanceof CustomMessageInputStreamFormat);
        assertEquals(iStreamFormat.getFormatName(), CustomMessageFormatFactory.INPUT_STREAM_FORMAT);

        // extract using convenient functions
        docFormat = formatFactory.getXmlFormat();
        assertNotNull(docFormat);
        assertTrue(docFormat instanceof CustomMessageXmlFormat);
        assertEquals(docFormat.getFormatName(), CustomMessageFormatFactory.XML_FORMAT);
        jsonFormat = formatFactory.getJsonFormat();
        assertNotNull(jsonFormat);
        assertTrue(jsonFormat instanceof CustomMessageJsonFormat);
        assertEquals(jsonFormat.getFormatName(), CustomMessageFormatFactory.JSON_FORMAT);
        iStreamFormat = formatFactory.getStreamFormat();
        assertNotNull(iStreamFormat);
        assertTrue(iStreamFormat instanceof CustomMessageInputStreamFormat);
        assertEquals(iStreamFormat.getFormatName(), CustomMessageFormatFactory.INPUT_STREAM_FORMAT);

        printKnownFormats();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateFormats() throws Exception {
        int initialSize;
        assertTrue((initialSize = formatFactory.getKnownFormats().size()) >= 3);

        // remove a format
        CustomMessageFormat<CustomJsonData> jsonFormat = (CustomMessageFormat<CustomJsonData>)formatRegistry.remove(CustomJsonData.class);
        assertNotNull(jsonFormat);
        assertTrue(formatFactory.getKnownFormats().size() < initialSize);
        try {
            formatFactory.getFormatForRepresentationClass(CustomJsonData.class);
            fail("formatFactory.getFormatForRepresentationClass should throw");
        } catch (NoSuchMessageFormatException ignore) { }
        try {
            formatFactory.getFormatByName(CustomMessageFormatFactory.JSON_FORMAT);
            fail("formatFactory.getFormatByName should throw");
        } catch (NoSuchMessageFormatException ignore) { }

        //register a new format String
        CustomMessageFormat newStringFormat = new CustomMessageFormat<String>() {
            @Override
            public Class<String> getRepresentationClass() { return String.class; }
            @Override
            public String getFormatName() { return "Text"; }
            @Override
            public String getFormatDescription() { return "New Text Format"; }
            @Override
            public String extract(CustomMessage message) throws CustomMessageAccessException { return null; }
            @Override
            public void overwrite(CustomMessage message, String contents) throws CustomMessageAccessException { }
            @Override
            public <K> String createBody(K content) throws CustomMessageAccessException { return null; }
        };
        formatRegistry.register(String.class, newStringFormat);
        assertNotNull(formatFactory.getFormatForRepresentationClass(String.class));
        assertNotNull(formatFactory.getFormatByName("TEXT")); // case insensitive
        assertTrue(formatFactory.getKnownFormats().size() == initialSize);

        printKnownFormats();
    }

    @Test
    public void testXmlCreateBody() throws Exception {
        // get XML message format
        final CustomMessageFormat<Document> xmlFormat = formatFactory.getXmlFormat();
        assertNotNull(xmlFormat);

        // create from String (valid content)
        Document testDoc = xmlFormat.createBody(XML_CONTENT);
        assertNotNull(testDoc);
        assertTrue(testDoc.isEqualNode(XmlUtil.stringToDocument(XML_CONTENT)));
        // create from String (invalid content)
        try {
            xmlFormat.createBody(XML_CONTENT_INVALID);
            fail("xmlFormat.createBody should throw for invalid XML");
        } catch (CustomMessageAccessException ignore) { }
        try {
            xmlFormat.createBody(JSON_CONTENT);
            fail("xmlFormat.createBody should throw for JSON");
        } catch (CustomMessageAccessException ignore) { }

        // create from InputStream (valid content)
        InputStream inputStream = formatFactory.getStreamFormat().createBody(XML_CONTENT);
        testDoc = xmlFormat.createBody(inputStream);
        assertNotNull(testDoc);
        assertTrue(testDoc.isEqualNode(XmlUtil.stringToDocument(XML_CONTENT)));
        // create from InputStream (invalid content)
        inputStream = formatFactory.getStreamFormat().createBody(XML_CONTENT_INVALID);
        try {
            xmlFormat.createBody(inputStream);
            fail("xmlFormat.createBody should throw for invalid XML");
        } catch (CustomMessageAccessException ignore) { }
        inputStream = formatFactory.getStreamFormat().createBody(JSON_CONTENT);
        try {
            xmlFormat.createBody(inputStream);
            fail("xmlFormat.createBody should throw for JSON");
        } catch (CustomMessageAccessException ignore) { }

        // unsupported format e.g. byte[]
        try {
            xmlFormat.createBody("test".getBytes());
            fail("xmlFormat.createBody should throw for unsupported type");
        } catch (CustomMessageAccessException ignore) { }

        // doesn't allow wrong type cast
        try {
            //noinspection UnusedDeclaration
            CustomJsonData tmpJsonData = (CustomJsonData)xmlFormat.createBody(XML_CONTENT);
            fail("xmlFormat.createBody should return only Document object");
        } catch (ClassCastException ignore) { }
        try {
            //noinspection UnusedDeclaration
            InputStream tmpInputStreamData = (InputStream)xmlFormat.createBody(XML_CONTENT);
            fail("xmlFormat.createBody should return only Document object");
        } catch (ClassCastException ignore) { }

        // test null content
        try {
            xmlFormat.createBody(null);
            fail("xmlFormat.createBody should throw for null content");
        } catch (CustomMessageAccessException ignore) { }
    }

    @Test
    public void testJsonCreateBody() throws Exception {
        // get JSON message format
        final CustomMessageFormat<CustomJsonData> jsonFormat = formatFactory.getJsonFormat();
        assertNotNull(jsonFormat);

        // create from String (valid content)
        CustomJsonData testJson = jsonFormat.createBody(JSON_CONTENT);
        assertNotNull(testJson);
        assertEquals(testJson.getJsonData(), JSON_CONTENT);
        assertNotNull(testJson.getJsonObject()); // should not throw
        // create from String (invalid content)
        testJson = jsonFormat.createBody(JSON_CONTENT_INVALID); // doesn't throw
        try {
            testJson.getJsonObject();
            fail("testJson.getJsonObject should throw for invalid JSON");
        } catch (InvalidDataException ignore) { }
        testJson = jsonFormat.createBody(XML_CONTENT);  // doesn't throw
        try {
            testJson.getJsonObject();
            fail("testJson.getJsonObject should throw for invalid JSON");
        } catch (InvalidDataException ignore) { }

        // create from InputStream (valid content)
        InputStream inputStream = formatFactory.getStreamFormat().createBody(JSON_CONTENT);
        testJson = jsonFormat.createBody(inputStream);
        assertNotNull(testJson);
        assertEquals(testJson.getJsonData(), JSON_CONTENT);
        assertNotNull(testJson.getJsonObject()); // should not throw
        // create from InputStream (invalid content)
        inputStream = formatFactory.getStreamFormat().createBody(JSON_CONTENT_INVALID);
        testJson = jsonFormat.createBody(inputStream); // doesn't throw
        try {
            testJson.getJsonObject();
            fail("testJson.getJsonObject should throw for invalid JSON");
        } catch (InvalidDataException ignore) { }
        inputStream = formatFactory.getStreamFormat().createBody(XML_CONTENT);
        testJson = jsonFormat.createBody(inputStream); // doesn't throw
        try {
            testJson.getJsonObject();
            fail("testJson.getJsonObject should throw for invalid JSON");
        } catch (InvalidDataException ignore) { }

        // unsupported format e.g. byte[]
        try {
            jsonFormat.createBody("test".getBytes());
            fail("jsonFormat.createBody should throw for unsupported type");
        } catch (CustomMessageAccessException ignore) { }

        // doesn't allow wrong type cast
        try {
            //noinspection UnusedDeclaration
            Document tmpJsonData = (Document)jsonFormat.createBody(JSON_CONTENT);
            fail("jsonFormat.createBody should return only CustomJsonData object");
        } catch (ClassCastException ignore) { }
        try {
            //noinspection UnusedDeclaration
            InputStream tmpInputStreamData = (InputStream)jsonFormat.createBody(XML_CONTENT);
            fail("jsonFormat.createBody should return only CustomJsonData object");
        } catch (ClassCastException ignore) { }

        // test null content
        try {
            jsonFormat.createBody(null);
            fail("jsonFormat.createBody should throw for null content");
        } catch (CustomMessageAccessException ignore) { }
    }

    @Test
    public void testInputStreamCreateBody() throws Exception {
        // get InputStream message format
        final CustomMessageFormat<InputStream> iStreamFormat = formatFactory.getStreamFormat();
        assertNotNull(iStreamFormat);

        // lets use a JSON payload
        InputStream testInputStream = iStreamFormat.createBody(JSON_CONTENT);
        String value = new String(IOUtils.slurpStream(testInputStream));
        assertEquals(value, JSON_CONTENT);

        InputStream inputStream = formatFactory.getStreamFormat().createBody(JSON_CONTENT);
        testInputStream = iStreamFormat.createBody(inputStream);
        assertSame("Expected the same reference to be returned", inputStream, testInputStream);

        // unsupported format e.g. byte[]
        try {
            iStreamFormat.createBody("test".getBytes());
            fail("iStreamFormat.createBody should throw for unsupported type");
        } catch (CustomMessageAccessException ignore) { }

        // doesn't allow wrong type cast
        try {
            //noinspection UnusedDeclaration
            Document tmpJsonData = (Document)iStreamFormat.createBody(XML_CONTENT);
            fail("iStreamFormat.createBody should return only CustomJsonData object");
        } catch (ClassCastException ignore) { }
        try {
            //noinspection UnusedDeclaration
            CustomJsonData tmpInputStreamData = (CustomJsonData)iStreamFormat.createBody(JSON_CONTENT);
            fail("iStreamFormat.createBody should return only CustomJsonData object");
        } catch (ClassCastException ignore) { }

        // test null content
        try {
            iStreamFormat.createBody(null);
            fail("jsonFormat.createBody should throw for null content");
        } catch (CustomMessageAccessException ignore) { }
    }

    /**
     * Helper function for creating custom message
     */
    private <T> CustomMessage createMessage(@Nullable ContentTypeHeader contentType, @Nullable T content) throws Exception {
        Message message = null;
        if (content instanceof Document) {
            message = new Message((Document)content);
        } else if (content instanceof byte[]) {
            message = new Message();
            message.initialize(contentType, (byte[])content);
        } else if (content instanceof String) {
            message = new Message();
            message.initialize(contentType, ((String)content).getBytes());
        } else if (content == null) {
            message = new Message(); // uninitialized
        }
        assertNotNull(message);
        return new CustomMessageImpl(message);
    }

    /**
     * Convenient function for creating uninitialized message.
     */
    private CustomMessage createMessage() throws Exception {
        return createMessage(null, null);
    }

    @Test
    public void testXmlExtract() throws Exception {
        // get XML message format
        final CustomMessageFormat<Document> xmlFormat = formatFactory.getXmlFormat();
        assertNotNull(xmlFormat);

        // valid XML
        CustomMessage xmlMessage = createMessage(ContentTypeHeader.XML_DEFAULT, XML_CONTENT);
        Document docXML = xmlMessage.extract(xmlFormat);
        assertNotNull(docXML);
        assertTrue(docXML.isEqualNode(XmlUtil.stringToDocument(XML_CONTENT)));
        // try convenient function
        docXML = xmlMessage.getDocument();
        assertNotNull(docXML);
        assertTrue(docXML.isEqualNode(XmlUtil.stringToDocument(XML_CONTENT)));
        // throw for invalid casts
        try {
            //noinspection UnusedDeclaration
            CustomJsonData tmpJson = (CustomJsonData)xmlMessage.extract(xmlFormat);
            fail("xmlMessage.extract(xmlFormat) should return only Document object");
        } catch (ClassCastException ignore) { }
        try {
            //noinspection UnusedDeclaration
            InputStream tmpInputStream = (InputStream)xmlMessage.extract(xmlFormat);
            fail("xmlMessage.extract(xmlFormat) should return only Document object");
        } catch (ClassCastException ignore) { }

        // invalid XML
        xmlMessage = createMessage(ContentTypeHeader.XML_DEFAULT, XML_CONTENT_INVALID);
        try {
            //noinspection UnusedAssignment
            docXML = xmlMessage.extract(xmlFormat);
            fail("xmlMessage.extract(xmlFormat) should throw for invalid XML");
        } catch (CustomMessageAccessException ignore) { }
        // try convenient function
        docXML = xmlMessage.getDocument();
        assertNull(docXML); // expected null

        // invalid XML (i.e. JSON) shouldn't throw
        xmlMessage = createMessage(ContentTypeHeader.APPLICATION_JSON, JSON_CONTENT);
        docXML = xmlMessage.extract(xmlFormat);
        assertNull(docXML); // should return null
        // try convenient function
        docXML = xmlMessage.getDocument();
        assertNull(docXML); // expected null

        // multipart first part XML
        xmlMessage = createMessage(ContentTypeHeader.parseValue(MULTIPART_FIRST_PART_XML_CONTENT_TYPE), MULTIPART_FIRST_PART_XML_CONTENT);
        docXML = xmlMessage.extract(xmlFormat);
        assertNotNull(docXML);
        assertTrue(docXML.isEqualNode(XmlUtil.stringToDocument(MULTIPART_XML_PART_CONTENT)));
        // try convenient function
        docXML = xmlMessage.getDocument();
        assertNotNull(docXML);
        assertTrue(docXML.isEqualNode(XmlUtil.stringToDocument(MULTIPART_XML_PART_CONTENT)));

        // multipart first part empty XML
        xmlMessage = createMessage(ContentTypeHeader.parseValue(MULTIPART_FIRST_PART_XML_CONTENT_TYPE), MULTIPART_FIRST_PART_XML_CONTENT_EMPTY);
        try {
            //noinspection UnusedAssignment
            docXML = xmlMessage.extract(xmlFormat);
            fail("xmlMessage.extract(xmlFormat) should throw for empty XML first part of multipart data");
        } catch (CustomMessageAccessException ignore) { }
        // try convenient function
        docXML = xmlMessage.getDocument();
        assertNull(docXML); // expected null

        // multipart first part invalid XML (JSON)
        xmlMessage = createMessage(ContentTypeHeader.parseValue(MULTIPART_FIRST_PART_XML_CONTENT_TYPE), MULTIPART_FIRST_PART_XML_CONTENT_INVALID);
        try {
            //noinspection UnusedAssignment
            docXML = xmlMessage.extract(xmlFormat);
            fail("xmlMessage.extract(xmlFormat) should throw for invalid XML first part of multipart data");
        } catch (CustomMessageAccessException ignore) { }
        // try convenient function
        docXML = xmlMessage.getDocument();
        assertNull(docXML); // expected null

        // multipart first part non XML
        xmlMessage = createMessage(ContentTypeHeader.parseValue(MULTIPART_FIRST_PART_APP_OCTET_CONTENT_TYPE), MULTIPART_FIRST_PART_APP_OCTET_CONTENT);
        docXML = xmlMessage.extract(xmlFormat);
        assertNull(docXML); // should return null
        // try convenient function
        docXML = xmlMessage.getDocument();
        assertNull(docXML); // expected null

        // uninitialized message
        xmlMessage = createMessage();
        docXML = xmlMessage.extract(xmlFormat);
        assertNull(docXML); // should return null
        // try convenient function
        docXML = xmlMessage.getDocument();
        assertNull(docXML); // expected null

        // test null
        try {
            xmlMessage.extract(null);
            fail("xmlMessage.extract should throw for null content");
        } catch (CustomMessageAccessException ignore) { }
        try {
            xmlFormat.extract(null);
            fail("xmlFormat.extract should throw for null content");
        } catch (CustomMessageAccessException ignore) { }
    }

    @Test
    public void testJsonExtract() throws Exception {
        // get JSON message format
        final CustomMessageFormat<CustomJsonData> jsonFormat = formatFactory.getJsonFormat();
        assertNotNull(jsonFormat);

        // valid JSON
        CustomMessage jsonMessage = createMessage(ContentTypeHeader.APPLICATION_JSON, JSON_CONTENT);
        CustomJsonData jsonData = jsonMessage.extract(jsonFormat);
        assertNotNull(jsonData);
        assertEquals(jsonData.getJsonData(), JSON_CONTENT);
        assertNotNull(jsonData.getJsonObject());
        // try convenient function
        jsonData = jsonMessage.getJsonData();
        assertNotNull(jsonData);
        assertEquals(jsonData.getJsonData(), JSON_CONTENT);
        assertNotNull(jsonData.getJsonObject());
        // throw for invalid casts
        try {
            //noinspection UnusedDeclaration
            Document tmpXML = (Document)jsonMessage.extract(jsonFormat);
            fail("jsonMessage.extract(jsonFormat) should return only CustomJsonData object");
        } catch (ClassCastException ignore) { }
        try {
            //noinspection UnusedDeclaration
            InputStream tmpInputStream = (InputStream)jsonMessage.extract(jsonFormat);
            fail("jsonMessage.extract(jsonFormat) should return only CustomJsonData object");
        } catch (ClassCastException ignore) { }

        // invalid JSON
        jsonMessage = createMessage(ContentTypeHeader.APPLICATION_JSON, JSON_CONTENT_INVALID);
        jsonData = jsonMessage.extract(jsonFormat);
        assertNotNull(jsonData);
        try {
            jsonData.getJsonObject();
            fail("jsonData.getJsonObject should throw for invalid JSON");
        } catch (InvalidDataException ignore) { }
        // try convenient function
        jsonData = jsonMessage.getJsonData();
        assertNotNull(jsonData);
        try {
            jsonData.getJsonObject();
            fail("jsonData.getJsonObject should throw for invalid JSON");
        } catch (InvalidDataException ignore) { }

        // invalid JSON (XML) shouldn't throw
        jsonMessage = createMessage(ContentTypeHeader.XML_DEFAULT, XML_CONTENT);
        jsonData = jsonMessage.extract(jsonFormat);
        assertNull(jsonData); // should return null
        // try convenient function
        jsonData = jsonMessage.getJsonData();
        assertNull(jsonData);

        // multipart first part JSON
        jsonMessage = createMessage(ContentTypeHeader.parseValue(MULTIPART_FIRST_PART_JSON_CONTENT_TYPE), MULTIPART_FIRST_PART_JSON_CONTENT);
        jsonData = jsonMessage.extract(jsonFormat);
        assertNotNull(jsonData);
        assertEquals(jsonData.getJsonData(), MULTIPART_JSON_PART_CONTENT);
        assertNotNull(jsonData.getJsonObject());
        // try convenient function
        jsonData = jsonMessage.getJsonData();
        assertNotNull(jsonData);
        assertEquals(jsonData.getJsonData(), MULTIPART_JSON_PART_CONTENT);
        assertNotNull(jsonData.getJsonObject());

        // multipart first part empty JSON
        jsonMessage = createMessage(ContentTypeHeader.parseValue(MULTIPART_FIRST_PART_JSON_CONTENT_TYPE), MULTIPART_FIRST_PART_JSON_CONTENT_EMPTY);
        jsonData = jsonMessage.extract(jsonFormat);
        assertNotNull(jsonData);
        assertEquals(jsonData.getJsonData(), "");
        try {
            jsonData.getJsonObject();
            fail("jsonData.getJsonObject should throw for empty first part JSON of multipart data.");
        } catch (InvalidDataException ignore) { }
        // try convenient function
        jsonData = jsonMessage.getJsonData();
        assertNotNull(jsonData);
        assertEquals(jsonData.getJsonData(), "");
        try {
            jsonData.getJsonObject();
            fail("jsonData.getJsonObject should throw for empty first part JSON of multipart data.");
        } catch (InvalidDataException ignore) { }

        // multipart first part invalid JSON (XML)
        jsonMessage = createMessage(ContentTypeHeader.parseValue(MULTIPART_FIRST_PART_JSON_CONTENT_TYPE), MULTIPART_FIRST_PART_JSON_CONTENT_INVALID);
        jsonData = jsonMessage.extract(jsonFormat);
        assertNotNull(jsonData);
        assertEquals(jsonData.getJsonData(), MULTIPART_XML_PART_CONTENT);
        try {
            jsonData.getJsonObject();
            fail("jsonData.getJsonObject should throw for invalid first part JSON of multipart data.");
        } catch (InvalidDataException ignore) { }
        // try convenient function
        jsonData = jsonMessage.getJsonData();
        assertNotNull(jsonData);
        assertEquals(jsonData.getJsonData(), MULTIPART_XML_PART_CONTENT);
        try {
            jsonData.getJsonObject();
            fail("jsonData.getJsonObject should throw for invalid first part JSON of multipart data.");
        } catch (InvalidDataException ignore) { }

        // multipart first part non JSON
        jsonMessage = createMessage(ContentTypeHeader.parseValue(MULTIPART_FIRST_PART_APP_OCTET_CONTENT_TYPE), MULTIPART_FIRST_PART_APP_OCTET_CONTENT);
        jsonData = jsonMessage.extract(jsonFormat);
        assertNull(jsonData); // should return null
        // try convenient function
        jsonData = jsonMessage.getJsonData();
        assertNull(jsonData);

        // uninitialized message
        jsonMessage = createMessage();
        jsonData = jsonMessage.extract(jsonFormat);
        assertNull(jsonData); // should return null
        // try convenient function
        jsonData = jsonMessage.getJsonData();
        assertNull(jsonData);

        // test null
        try {
            jsonMessage.extract(null);
            fail("jsonMessage.extract should throw for null content");
        } catch (CustomMessageAccessException ignore) { }
        try {
            jsonFormat.extract(null);
            fail("jsonFormat.extract should throw for null content");
        } catch (CustomMessageAccessException ignore) { }
    }

    @Test
    public void testInputStreamExtract() throws Exception {
        // get InputStream message format
        final CustomMessageFormat<InputStream> iStreamFormat = formatFactory.getStreamFormat();
        assertNotNull(iStreamFormat);

        // some content bytes
        CustomMessage iStreamMessage = createMessage(ContentTypeHeader.OCTET_STREAM_DEFAULT, INPUT_STREAM_CONTENT_BYTES);
        InputStream inputStream = iStreamMessage.extract(iStreamFormat);
        assertNotNull(inputStream);
        assertTrue(Arrays.equals(IOUtils.slurpStream(inputStream), INPUT_STREAM_CONTENT_BYTES));
        // try convenient function
        inputStream = iStreamMessage.getInputStream();
        assertNotNull(inputStream);
        assertTrue(Arrays.equals(IOUtils.slurpStream(inputStream), INPUT_STREAM_CONTENT_BYTES));
        // throw for invalid casts
        try {
            //noinspection UnusedDeclaration
            Document tmpXML = (Document)iStreamMessage.extract(iStreamFormat);
            fail("iStreamMessage.extract(iStreamFormat) should return only InputStream object");
        } catch (ClassCastException ignore) { }
        try {
            //noinspection UnusedDeclaration
            CustomJsonData tmpInputStream = (CustomJsonData)iStreamMessage.extract(iStreamFormat);
            fail("iStreamMessage.extract(iStreamFormat) should return only InputStream object");
        } catch (ClassCastException ignore) { }

        // uninitialized message
        iStreamMessage = createMessage();
        inputStream = iStreamMessage.extract(iStreamFormat);
        assertNull(inputStream); // should return null
        // try convenient function
        inputStream = iStreamMessage.getInputStream();
        assertNull(inputStream); // should return null

        // test null
        try {
            iStreamFormat.extract(null);
            fail("iStreamFormat.extract should throw for null content");
        } catch (CustomMessageAccessException ignore) { }
        try {
            iStreamMessage.extract(null);
            fail("iStreamMessage.extract should throw for null content");
        } catch (CustomMessageAccessException ignore) { }
    }

    @Test
    public void testXmlOverwrite() throws Exception {
        // initial message value is XML  
        CustomMessage message = createMessage(ContentTypeHeader.XML_DEFAULT, XML_CONTENT);
        assertTrue(message.getDocument().isEqualNode(XmlUtil.stringToDocument(XML_CONTENT)));
        assertTrue(message.getContentType().matches("text", "xml"));
        // set to JSON
        message.overwrite(formatFactory.getJsonFormat(), formatFactory.getJsonFormat().createBody(JSON_CONTENT));
        assertEquals(message.getJsonData().getJsonData(), JSON_CONTENT);
        assertNotNull(message.getJsonData().getJsonObject());
        assertTrue(message.getContentType().matches("application", "json")); // overwrite will set the content-type accordingly
        assertNull(message.getDocument());
        // reset content type
        message.setContentType(new ContentTypeHeaderToCustomConverter(ContentTypeHeader.SOAP_1_2_DEFAULT));
        assertTrue(message.getContentType().matches("application", "soap+xml"));
        assertEquals(message.getJsonData().getJsonData(), JSON_CONTENT);
        assertNotNull(message.getJsonData().getJsonObject());

        // initial message value is XML
        message = createMessage(ContentTypeHeader.XML_DEFAULT, XML_CONTENT);
        assertTrue(message.getDocument().isEqualNode(XmlUtil.stringToDocument(XML_CONTENT)));
        // set it to binary
        message.overwrite(formatFactory.getStreamFormat(), formatFactory.getStreamFormat().createBody(INPUT_STREAM_CONTENT_STRING));
        assertTrue(Arrays.equals(IOUtils.slurpStream(message.getInputStream()), INPUT_STREAM_CONTENT_BYTES));
        assertTrue(message.getContentType().matches("text", "xml")); // should be unchanged
        assertNull(message.getDocument());
        message.setContentType(new ContentTypeHeaderToCustomConverter(ContentTypeHeader.OCTET_STREAM_DEFAULT));
        assertTrue(message.getContentType().matches("application", "octet-stream"));

        // initial message value is XML
        message = createMessage(ContentTypeHeader.XML_DEFAULT, XML_CONTENT);
        assertTrue(message.getDocument().isEqualNode(XmlUtil.stringToDocument(XML_CONTENT)));
        // set it to SOAP (XML)
        message.overwrite(formatFactory.getStreamFormat(), formatFactory.getStreamFormat().createBody(SOAP_CONTENT));
        assertTrue(message.getDocument().isEqualNode(XmlUtil.stringToDocument(SOAP_CONTENT)));
        assertTrue(message.getContentType().matches("text", "xml")); // should be unchanged
        assertNull(message.getJsonData());
        message.setContentType(new ContentTypeHeaderToCustomConverter(ContentTypeHeader.SOAP_1_2_DEFAULT));
        assertTrue(message.getContentType().matches("application", "soap+xml"));

        // test null
        message = createMessage(ContentTypeHeader.XML_DEFAULT, XML_CONTENT);
        assertTrue(message.getDocument().isEqualNode(XmlUtil.stringToDocument(XML_CONTENT)));
        try {
            message.overwrite(null, formatFactory.getStreamFormat().createBody(INPUT_STREAM_CONTENT_STRING));
            fail("message.overwrite should throw for null format");
        } catch (CustomMessageAccessException ignore) { }
        try {
            formatFactory.getXmlFormat().overwrite(null, XmlUtil.stringToDocument(XML_CONTENT));
            fail("xmlFormat.overwrite should throw for null message");
        } catch (CustomMessageAccessException ignore) { }
        try {
            message.overwrite(formatFactory.getXmlFormat(), null);
            fail("message.overwrite should throw for null content i.e. Document");
        } catch (IllegalArgumentException ignore) { }
    }

    @Test
    public void testJsonOverwrite() throws Exception {
        // initial message value is JSON
        CustomMessage message = createMessage(ContentTypeHeader.APPLICATION_JSON, JSON_CONTENT);
        assertEquals(message.getJsonData().getJsonData(), JSON_CONTENT);
        assertNotNull(message.getJsonData().getJsonObject());
        assertTrue(message.getContentType().matches("application", "json"));
        // set to XML
        try {
            // TODO: For some reason the core Message component is throwing RuntimeException with cause SAXException
            // when we are setting the new XML content, if the message content was previously set with other then XML content
            message.overwrite(formatFactory.getXmlFormat(), formatFactory.getXmlFormat().createBody(XML_CONTENT));
            fail("expected behaviour is to fail if content was previously set to other then XML.");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof SAXException);
        }
        assertNull(message.getDocument());
        assertEquals(message.getJsonData().getJsonData(), JSON_CONTENT);
        assertNotNull(message.getJsonData().getJsonObject());
        assertTrue(message.getContentType().matches("application", "json"));

        // set the content to XML using InputStream, before setting the content-type to text/xml
        // initial message value is JSON
        message = createMessage(ContentTypeHeader.APPLICATION_JSON, JSON_CONTENT);
        assertEquals(message.getJsonData().getJsonData(), JSON_CONTENT);
        assertNotNull(message.getJsonData().getJsonObject());
        assertTrue(message.getContentType().matches("application", "json"));
        // set to XML (using InputStream)
        message.overwrite(formatFactory.getStreamFormat(), formatFactory.getStreamFormat().createBody(XML_CONTENT));
        assertTrue(message.getContentType().matches("application", "json"));
        assertNull(message.getDocument());
        assertNotNull(message.getJsonData()); // actually since the content type is not changed, it still have Json data
        assertEquals(message.getJsonData().getJsonData(), XML_CONTENT);
        try {
            message.getJsonData().getJsonObject();
            fail("jsonData.getJsonObject should throw since content is XML.");
        } catch (InvalidDataException ignore) { }
        message.setContentType(new ContentTypeHeaderToCustomConverter(ContentTypeHeader.XML_DEFAULT));
        assertTrue(message.getContentType().matches("text", "xml"));
        assertNull(message.getDocument());

        // set the content to XML using InputStream, after setting the content-type to text/xml
        // initial message value is JSON
        message = createMessage(ContentTypeHeader.APPLICATION_JSON, JSON_CONTENT);
        assertEquals(message.getJsonData().getJsonData(), JSON_CONTENT);
        assertNotNull(message.getJsonData().getJsonObject());
        assertTrue(message.getContentType().matches("application", "json"));
        // set to XML (using InputStream)
        message.setContentType(new ContentTypeHeaderToCustomConverter(ContentTypeHeader.XML_DEFAULT));
        assertTrue(message.getContentType().matches("text", "xml"));
        message.overwrite(formatFactory.getStreamFormat(), formatFactory.getStreamFormat().createBody(XML_CONTENT));
        assertTrue(message.getContentType().matches("text", "xml"));
        assertTrue(message.getDocument().isEqualNode(XmlUtil.stringToDocument(XML_CONTENT)));
        assertNull(message.getJsonData());

        // initial message value is JSON
        message = createMessage(ContentTypeHeader.APPLICATION_JSON, JSON_CONTENT);
        assertEquals(message.getJsonData().getJsonData(), JSON_CONTENT);
        assertNotNull(message.getJsonData().getJsonObject());
        assertTrue(message.getContentType().matches("application", "json"));
        // set to SOAP
        message.setContentType(new ContentTypeHeaderToCustomConverter(ContentTypeHeader.SOAP_1_2_DEFAULT));
        assertTrue(message.getContentType().matches("application", "soap+xml"));
        message.overwrite(formatFactory.getStreamFormat(), formatFactory.getStreamFormat().createBody(SOAP_CONTENT));
        assertTrue(message.getDocument().isEqualNode(XmlUtil.stringToDocument(SOAP_CONTENT)));
        assertNull(message.getJsonData());

        // initial message value is JSON
        message = createMessage(ContentTypeHeader.APPLICATION_JSON, JSON_CONTENT);
        assertEquals(message.getJsonData().getJsonData(), JSON_CONTENT);
        assertNotNull(message.getJsonData().getJsonObject());
        assertTrue(message.getContentType().matches("application", "json"));
        // set it to binary
        message.overwrite(formatFactory.getStreamFormat(), formatFactory.getStreamFormat().createBody(INPUT_STREAM_CONTENT_STRING));
        assertTrue(Arrays.equals(IOUtils.slurpStream(message.getInputStream()), INPUT_STREAM_CONTENT_BYTES));
        assertTrue(message.getContentType().matches("application", "json")); // should be unchanged
        assertNull(message.getDocument());
        assertNotNull(message.getJsonData());
        assertEquals(message.getJsonData().getJsonData(), INPUT_STREAM_CONTENT_STRING);
        try {
            message.getJsonData().getJsonObject();
            fail("jsonData.getJsonObject should throw since content is not JSON.");
        } catch (InvalidDataException ignore) { }

        // initial message value is JSON
        message = createMessage(ContentTypeHeader.APPLICATION_JSON, JSON_CONTENT);
        assertEquals(message.getJsonData().getJsonData(), JSON_CONTENT);
        assertNotNull(message.getJsonData().getJsonObject());
        assertTrue(message.getContentType().matches("application", "json"));
        // set it to another JSON
        message.overwrite(formatFactory.getJsonFormat(), formatFactory.getJsonFormat().createBody(JSON_SECOND_CONTENT));
        assertTrue(message.getContentType().matches("application", "json")); // should be unchanged
        assertEquals(message.getJsonData().getJsonData(), JSON_SECOND_CONTENT);
        assertNotNull(message.getJsonData().getJsonObject());
        assertNull(message.getDocument());

        // initial message value is JSON
        message = createMessage(ContentTypeHeader.APPLICATION_JSON, JSON_CONTENT);
        assertEquals(message.getJsonData().getJsonData(), JSON_CONTENT);
        assertNotNull(message.getJsonData().getJsonObject());
        assertTrue(message.getContentType().matches("application", "json"));
        // set it to invalid JSON
        message.overwrite(formatFactory.getJsonFormat(), formatFactory.getJsonFormat().createBody(JSON_CONTENT_INVALID));
        assertTrue(message.getContentType().matches("application", "json")); // should be unchanged
        assertEquals(message.getJsonData().getJsonData(), JSON_CONTENT_INVALID);
        try {
            message.getJsonData().getJsonObject();
            fail("jsonData.getJsonObject should throw since content is not JSON.");
        } catch (InvalidDataException ignore) { }
        assertNull(message.getDocument());

        // test null
        message = createMessage(ContentTypeHeader.APPLICATION_JSON, JSON_CONTENT);
        assertEquals(message.getJsonData().getJsonData(), JSON_CONTENT);
        assertNotNull(message.getJsonData().getJsonObject());
        assertTrue(message.getContentType().matches("application", "json"));
        try {
            message.overwrite(null, formatFactory.getStreamFormat().createBody(INPUT_STREAM_CONTENT_STRING));
            fail("message.overwrite should throw for null format");
        } catch (CustomMessageAccessException ignore) { }
        try {
            formatFactory.getJsonFormat().overwrite(null, formatFactory.getJsonFormat().createBody(JSON_CONTENT));
            fail("jsonFormat.overwrite should throw for null message");
        } catch (CustomMessageAccessException ignore) { }
        message.overwrite(formatFactory.getJsonFormat(), null);
        assertEquals(message.getJsonData().getJsonData(), "");
        try {
            message.getJsonData().getJsonObject();
            fail("jsonData.getJsonObject should throw since content is empty.");
        } catch (InvalidDataException ignore) { }
    }

    @Test
    public void testInputStreamOverwrite() throws Exception {
        // initial message value is InputStream
        CustomMessage message = createMessage(ContentTypeHeader.OCTET_STREAM_DEFAULT, INPUT_STREAM_CONTENT_BYTES);
        assertTrue(Arrays.equals(IOUtils.slurpStream(message.getInputStream()), INPUT_STREAM_CONTENT_BYTES));
        assertTrue(message.getContentType().matches("application", "octet-stream"));
        // set to XML
        try {
            // TODO: For some reason the core Message component is throwing RuntimeException with cause SAXException
            // when we are setting the new XML content, if the message content was previously set with other then XML content
            message.overwrite(formatFactory.getXmlFormat(), formatFactory.getXmlFormat().createBody(XML_CONTENT));
            fail("expected behaviour is to fail if content was previously set to other then XML.");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof SAXException);
        }
        assertTrue(message.getContentType().matches("application", "octet-stream"));
        assertNull(message.getDocument());
        assertNull(message.getJsonData());
        try {
            message.getInputStream();
        } catch (CustomMessageAccessException ignore) { }

        // initial message value is InputStream
        message = createMessage(ContentTypeHeader.OCTET_STREAM_DEFAULT, INPUT_STREAM_CONTENT_BYTES);
        assertTrue(Arrays.equals(IOUtils.slurpStream(message.getInputStream()), INPUT_STREAM_CONTENT_BYTES));
        assertTrue(message.getContentType().matches("application", "octet-stream"));
        // set to XML
        message.setContentType(new ContentTypeHeaderToCustomConverter(ContentTypeHeader.XML_DEFAULT));
        message.overwrite(formatFactory.getStreamFormat(), formatFactory.getStreamFormat().createBody(XML_CONTENT));
        assertTrue(message.getContentType().matches("text", "xml"));
        assertTrue(message.getDocument().isEqualNode(XmlUtil.stringToDocument(XML_CONTENT)));
        assertNull(message.getJsonData());
        assertTrue(Arrays.equals(IOUtils.slurpStream(message.getInputStream()), XML_CONTENT.getBytes()));

        // initial message value is InputStream
        message = createMessage(ContentTypeHeader.OCTET_STREAM_DEFAULT, INPUT_STREAM_CONTENT_BYTES);
        assertTrue(Arrays.equals(IOUtils.slurpStream(message.getInputStream()), INPUT_STREAM_CONTENT_BYTES));
        assertTrue(message.getContentType().matches("application", "octet-stream"));
        // set to JSON
        message.overwrite(formatFactory.getJsonFormat(), formatFactory.getJsonFormat().createBody(JSON_CONTENT));
        assertTrue(message.getContentType().matches("application", "json"));
        assertEquals(message.getJsonData().getJsonData(), JSON_CONTENT);
        assertNotNull(message.getJsonData().getJsonObject());
        assertNull(message.getDocument());
        assertTrue(Arrays.equals(IOUtils.slurpStream(message.getInputStream()), JSON_CONTENT.getBytes()));

        // test null
        message = createMessage(ContentTypeHeader.OCTET_STREAM_DEFAULT, INPUT_STREAM_CONTENT_BYTES);
        assertTrue(Arrays.equals(IOUtils.slurpStream(message.getInputStream()), INPUT_STREAM_CONTENT_BYTES));
        assertTrue(message.getContentType().matches("application", "octet-stream"));
        try {
            message.overwrite(null, formatFactory.getStreamFormat().createBody(INPUT_STREAM_CONTENT_STRING));
            fail("message.overwrite should throw for null format");
        } catch (CustomMessageAccessException ignore) { }
        try {
            formatFactory.getStreamFormat().overwrite(null, formatFactory.getStreamFormat().createBody(INPUT_STREAM_CONTENT_STRING));
            fail("iStreamFormat.overwrite should throw for null message");
        } catch (CustomMessageAccessException ignore) { }
        try {
            message.overwrite(formatFactory.getStreamFormat(), null);
            fail("message.overwrite should throw for null content i.e. InputStream");
        } catch (IllegalArgumentException ignore) { }
    }
}
