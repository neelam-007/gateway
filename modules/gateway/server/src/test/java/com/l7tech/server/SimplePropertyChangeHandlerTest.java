package com.l7tech.server;

import com.l7tech.util.DateTimeConfigUtils;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.util.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.l7tech.util.Functions.map;
import static org.junit.Assert.*;

@SuppressWarnings({"JavaDoc"})
public class SimplePropertyChangeHandlerTest {

    @Before
    public void setUp() throws Exception {
        properties = new HashMap<String,String>();
        final Config mockConfig = new MockConfig(properties);
        handler = new SimplePropertyChangeHandler();
        testAudit = new TestAudit();
        final AuditFactory factory = testAudit.factory();
        dateParser = new DateTimeConfigUtils();
        ApplicationContexts.inject(handler, CollectionUtils.<String, Object>mapBuilder()
                .put("serverConfig", mockConfig)
                .put("auditFactory", factory)
                .put("dateParser", dateParser)
                .unmodifiableMap()
        );

        handler.afterPropertiesSet();

        clearContentTypes();
    }

    @After
    public void tearDown() throws Exception {
        clearContentTypes();
    }

    @Test
    public void testTextualContentTypes_PropertyNotSet() throws Exception {
        final ContentTypeHeader[] types = handler.getConfiguredContentTypes();
        Assert.assertNotNull(types);
        Assert.assertTrue("List should be empty", types.length == 0);
    }

    @Test
    public void testTextualContentTypes_PropertySet() throws Exception {
        //empty string
        properties.put(ServerConfigParams.PARAM_OTHER_TEXTUAL_CONTENT_TYPES, "");
        ContentTypeHeader[] types = handler.getConfiguredContentTypes();
        Assert.assertNotNull(types);
        Assert.assertTrue("List should be empty", types.length == 0);

        // valid values
        properties.put(ServerConfigParams.PARAM_OTHER_TEXTUAL_CONTENT_TYPES, "text/plain1; charset=utf-8  ");
        types = handler.getConfiguredContentTypes();
        assertFalse(testAudit.iterator().hasNext());
        Assert.assertNotNull(types);
        Assert.assertFalse("List should not be empty", types.length == 0);
        Assert.assertEquals(types[0].getType(), "text");
        Assert.assertEquals(types[0].getSubtype(), "plain1");
        Assert.assertEquals(types[0].getEncoding(), Charsets.UTF8);

        // multiple values
        properties.put(ServerConfigParams.PARAM_OTHER_TEXTUAL_CONTENT_TYPES, "text1/plain1; charset=utf-8\ntext2/plain2; charset=utf-8");
        types = handler.getConfiguredContentTypes();
        assertFalse(testAudit.iterator().hasNext());
        Assert.assertNotNull(types);
        Assert.assertEquals(2, types.length);

        Assert.assertEquals(types[0].getType(), "text1");
        Assert.assertEquals(types[0].getSubtype(), "plain1");
        Assert.assertEquals(types[0].getEncoding(), Charsets.UTF8);

        Assert.assertEquals(types[1].getType(), "text2");
        Assert.assertEquals(types[1].getSubtype(), "plain2");
        Assert.assertEquals(types[1].getEncoding(), Charsets.UTF8);

        properties.put(ServerConfigParams.PARAM_OTHER_TEXTUAL_CONTENT_TYPES, "text1/plain1; charset=utf-8\ntext1/plain1; charset=utf-8\rtext1/plain1; charset=utf-8\ftext1/plain1; charset=utf-8\n\ntext1/plain1; charset=utf-8\r\rtext1/plain1; charset=utf-8\f\ftext1/plain1; charset=utf-8");
        types = handler.getConfiguredContentTypes();
        assertFalse(testAudit.iterator().hasNext());
        Assert.assertNotNull(types);
        Assert.assertEquals(7, types.length);

        for (ContentTypeHeader type : types) {
            Assert.assertEquals(type.getType(), "text1");
            Assert.assertEquals(type.getSubtype(), "plain1");
            Assert.assertEquals(type.getEncoding(), Charsets.UTF8);
        }
    }

    @Test
    public void testTextualContentTypes_InvalidValue() throws Exception {
        // invalid values
        properties.put(ServerConfigParams.PARAM_OTHER_TEXTUAL_CONTENT_TYPES, "text/plain1; charset=utf-8; charset=utf-8 ");
        handler.getConfiguredContentTypes();
        assertTrue(testAudit.isAuditPresent(SystemMessages.INVALID_CONTENT_TYPE));
    }
    /**
     * Test that ContentTypeHeader's set method is called in response to the applicable property change event.
     */
    @Test
    public void testTextualContentTypes_Integration() throws Exception {
        // multiple values
        properties.put(ServerConfigParams.PARAM_OTHER_TEXTUAL_CONTENT_TYPES, "text1/plain1; charset=utf-8\ntext2/plain2; charset=utf-8");
        handler.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_OTHER_TEXTUAL_CONTENT_TYPES, "", ""));

        final List<ContentTypeHeader> contentTypes = ContentTypeHeader.getConfigurableTextualContentTypes();
        assertFalse(testAudit.iterator().hasNext());
        assertEquals("Incorrect number of types found", 2, contentTypes.size());
        assertEquals("Incorrect type found", ContentTypeHeader.create("text1/plain1; charset=utf-8").getType(), contentTypes.get(0).getType());
        assertEquals("Incorrect type found", ContentTypeHeader.create("text1/plain1; charset=utf-8").getSubtype(), contentTypes.get(0).getSubtype());
        assertEquals("Incorrect type found", ContentTypeHeader.create("text1/plain1; charset=utf-8").getEncoding(), contentTypes.get(0).getEncoding());
        assertEquals("Incorrect type found", ContentTypeHeader.create("text2/plain2; charset=utf-8").getType(), contentTypes.get(1).getType());
        assertEquals("Incorrect type found", ContentTypeHeader.create("text2/plain2; charset=utf-8").getSubtype(), contentTypes.get(1).getSubtype());
        assertEquals("Incorrect type found", ContentTypeHeader.create("text2/plain2; charset=utf-8").getEncoding(), contentTypes.get(1).getEncoding());
    }

    @Test
    public void testConfiguredDateFormats_PropertiesNotSet() throws Exception {
        final List<String> formats = handler.getCustomDateFormatsStrings();
        assertNotNull(formats);
        assertTrue(formats.isEmpty());
        assertFalse(testAudit.iterator().hasNext());
    }

    @Test
    public void testConfiguredDateFormats() throws Exception {
        //empty string
        properties.put(ServerConfigParams.PARAM_DATE_TIME_CUSTOM_FORMATS, "");
        List<String> formats = handler.getCustomDateFormatsStrings();
        assertFalse(testAudit.iterator().hasNext());
        assertNotNull(formats);
        assertTrue("List should be empty", formats.isEmpty());

        // single valid values
        properties.put(ServerConfigParams.PARAM_DATE_TIME_CUSTOM_FORMATS, "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        formats = handler.getCustomDateFormatsStrings();
        assertFalse(testAudit.iterator().hasNext());
        assertNotNull(formats);
        assertFalse("List should not be empty", formats.isEmpty());
        assertEquals(1, formats.size());
        assertEquals(formats.get(0), "yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        // test with leading and trailing delimiters and spaces
        properties.put(ServerConfigParams.PARAM_DATE_TIME_CUSTOM_FORMATS, " ;  yyyy-MM-dd'T'HH:mm:ss.SSSZ  ;  ");
        formats = handler.getCustomDateFormatsStrings();
        assertFalse(testAudit.iterator().hasNext());
        assertNotNull(formats);
        assertFalse("List should not be empty", formats.isEmpty());
        assertEquals(1, formats.size());
        assertEquals(formats.get(0), "yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        // valid multiple values
        properties.put(ServerConfigParams.PARAM_DATE_TIME_CUSTOM_FORMATS, "yyyy-MM-dd'T'HH:mm:ss.SSSZ;yyyy-MM-dd hh:mm:ss aaa;yyyy-MM-dd HH:mm:ss.SSS;");
        formats = handler.getCustomDateFormatsStrings();
        assertFalse(testAudit.iterator().hasNext());
        assertNotNull(formats);
        assertFalse("List should not be empty", formats.isEmpty());
        assertEquals(3, formats.size());
        assertEquals(formats.get(0), "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        assertEquals(formats.get(1), "yyyy-MM-dd hh:mm:ss aaa");
        assertEquals(formats.get(2), "yyyy-MM-dd HH:mm:ss.SSS");

        // valid multiple values with leading and trailing white space and some invalid values
        properties.put(ServerConfigParams.PARAM_DATE_TIME_CUSTOM_FORMATS, "  yyyy-MM-dd'T'HH:mm:ss.SSSZ  ;  yyyy-MM-dd hh:mm:ss aaa;yyyy-MM-dd HH:mm:ss.SSS; ;   ;   ; TT ;");
        formats = handler.getCustomDateFormatsStrings();
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertTrue(testAudit.isAuditPresent(SystemMessages.INVALID_CUSTOM_DATE_FORMAT));
        assertTrue(testAudit.isAuditPresentContaining("TT"));
        assertNotNull(formats);
        assertFalse("List should not be empty", formats.isEmpty());
        assertEquals(3, formats.size());
        assertEquals(formats.get(0), "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        assertEquals(formats.get(1), "yyyy-MM-dd hh:mm:ss aaa");
        assertEquals(formats.get(2), "yyyy-MM-dd HH:mm:ss.SSS");
    }

    /**
     * Test {@link ServerConfigParams#PARAM_DATE_TIME_AUTO_FORMATS} cluster property parsing via
     *  {@link com.l7tech.server.SimplePropertyChangeHandler#getCustomDateFormatsStrings()}
     */
    @Test
    public void testAutoDateFormats() throws Exception {

        final Functions.Unary<String, Pair<String, Pattern>> formatExtractorFunction = new Functions.Unary<String, Pair<String, Pattern>>() {
            @Override
            public String call(Pair<String, Pattern> o) {
                return o.left;
            }
        };

        // test empty auto format
        properties.put(ServerConfigParams.PARAM_DATE_TIME_AUTO_FORMATS, "");
        List<String> formats = map(handler.getAutoDateFormatsStrings(), formatExtractorFunction);
        assertFalse(testAudit.iterator().hasNext());
        assertNotNull(formats);
        assertTrue("List should be empty", formats.isEmpty());
        assertFalse("No audits should have been created.", testAudit.iterator().hasNext());

        // invalid input
        properties.put(ServerConfigParams.PARAM_DATE_TIME_AUTO_FORMATS, "T ^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$");
        formats = map(handler.getAutoDateFormatsStrings(), formatExtractorFunction);

        assertNotNull(formats);
        assertTrue("List should be empty", formats.isEmpty());
        assertTrue("Audit should have been generated", testAudit.isAuditPresent(SystemMessages.INVALID_AUTO_DATE_FORMAT));
        assertTrue(testAudit.isAuditPresentContaining("'T'"));
        testAudit.reset();

        properties.put(ServerConfigParams.PARAM_DATE_TIME_AUTO_FORMATS, " ^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$");
        formats = map(handler.getAutoDateFormatsStrings(), formatExtractorFunction);
        assertNotNull(formats);
        assertTrue("List should be empty", formats.isEmpty());
        assertFalse("No audits should have been created.", testAudit.iterator().hasNext());

        properties.put(ServerConfigParams.PARAM_DATE_TIME_AUTO_FORMATS, "yyyy-MM-dd'T'hh:mm:ss.SSSX ^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$");
        formats = map(handler.getAutoDateFormatsStrings(), formatExtractorFunction);
        assertNotNull(formats);
        assertFalse("List should not be empty", formats.isEmpty());
        assertEquals(1, formats.size());
        assertEquals("yyyy-MM-dd'T'hh:mm:ss.SSSX", formats.get(0));
        assertFalse("No audits should have been created.", testAudit.iterator().hasNext());

        final String value = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX ^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$" +
                "  \nyyyy-MM-dd'T'HH:mm:ss.SSXXX ^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{2}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$" +
                "  \nyyyy-MM-dd'T'HH:mm:ss.SXXX ^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{1}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$" +
                "  \nyyyy-MM-dd'T'HH:mm:ssXXX ^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$" +
                "  \nyyyy-MM-dd'T'HH:mmXXX ^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$" +
                "  \nyyyy-MM-dd ^\\d{4}-\\d{2}-\\d{2}$ " +
                "  \nyyyy-MM ^\\d{4}-\\d{2}$ " +
                "  \nyyyy ^\\d{4}$ " +
                "  \nEEE, dd MMM yyyy HH:mm:ss z ^[a-zA-Z]{3},\\s\\d{2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{2}:\\d{2}:\\d{2}\\s(?:[a-zA-Z]{3}|(?:\\+|-)\\d{4})$" +
                "  \nEEE, dd MMM yy HH:mm:ss Z ^[a-zA-Z]{3},\\s\\d{2}\\s[a-zA-Z]{3}\\s\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\s(?:[a-zA-Z]{3}|(?:\\+|-)\\d{4})$ " +
                "  \nEEE, dd-MMM-yy HH:mm:ss z ^(?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday),\\s\\d{2}-[a-zA-Z]{3}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\s(?:[a-zA-Z]{3}|(?:\\+|-)\\d{4})$" +
                "  \nEEE MMM dd HH:mm:ss yyyy ^[a-zA-Z]{3}\\s[a-zA-Z]{3}\\s(\\d{2}|\\s\\d)\\s\\d{2}:\\d{2}:\\d{2}\\s\\d{4}$";
        
        System.out.println(value);
        properties.put(ServerConfigParams.PARAM_DATE_TIME_AUTO_FORMATS, value);

        formats = map(handler.getAutoDateFormatsStrings(), formatExtractorFunction);
        assertFalse("No audits should have been created.", testAudit.iterator().hasNext());
        assertNotNull(formats);
        assertFalse("List should not be empty", formats.isEmpty());
//        for (String format : formats) {
//            System.out.println(format);
//        }

        assertEquals(12, formats.size());

        String[] expectedFormats = {"yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ss.SSXXX", "yyyy-MM-dd'T'HH:mm:ss.SXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mmXXX","yyyy-MM-dd","yyyy-MM","yyyy", "EEE, dd MMM yyyy HH:mm:ss z",
                "EEE, dd MMM yy HH:mm:ss Z", "EEE, dd-MMM-yy HH:mm:ss z", "EEE MMM dd HH:mm:ss yyyy"};

        for (int i = 0, expectedFormatsLength = expectedFormats.length; i < expectedFormatsLength; i++) {
            String expectedFormat = expectedFormats[i];
            final String actualFormat = formats.get(i);
            assertEquals(expectedFormat, actualFormat);
        }

        properties.put(ServerConfigParams.PARAM_DATE_TIME_AUTO_FORMATS, "yyyy-MM-dd'T'hh:mm:ss.SSSX ^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$ " +
                "yyyy-MM-dd'T'hh:mm:ss.SSX ^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{2}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$ " +
                "yyyy-MM-dd'T'hh:mm:ss.SX ^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{1}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$" +
                " yyyy-MM-dd'T'hh:mm:ssX ^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$ " +
                "yyyy-MM-dd'T'hh:mmX ^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$ " +
                "yyyy-MM-dd $ yyyy ^ " + //first format's pattern is missing apart from end $ and second format's pattern only contains ^
                "EEE, dd MMM yyyy HH:mm:ss z ^[a-zA-Z]{3},\\s\\d{2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{2}:\\d{2}:\\d{2}\\s(?:[a-zA-Z]{3}|(?:\\+|-)\\d{4})$ " +
                "EEE, dd MMM yy HH:mm:ss Z ^[a-zA-Z]{3},\\s\\d{2}\\s[a-zA-Z]{3}\\s\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\s(?:[a-zA-Z]{3}|(?:\\+|-)\\d{4})$ " +
                "EEE, dd-MMM-yy HH:mm:ss z ^(?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday),\\s\\d{2}-[a-zA-Z]{3}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\s(?:[a-zA-Z]{3}|(?:\\+|-)\\d{4})$ " +
                "EEE MMM dd HH:mm:ss yyyy ^[a-zA-Z]{3}\\s[a-zA-Z]{3}\\s(\\d{2}|\\s\\d)\\s\\d{2}:\\d{2}:\\d{2}\\s\\d{4}$");

        formats = map(handler.getAutoDateFormatsStrings(), formatExtractorFunction);
        assertNotNull(formats);
        assertFalse("List should not be empty", formats.isEmpty());
//        for (String format : formats) {
//            System.out.println(format);
//        }

        assertEquals(8, formats.size());

        expectedFormats = new String[]{"yyyy-MM-dd'T'hh:mm:ss.SSSX", "yyyy-MM-dd'T'hh:mm:ss.SSX", "yyyy-MM-dd'T'hh:mm:ss.SX",
                "yyyy-MM-dd'T'hh:mm:ssX", "yyyy-MM-dd'T'hh:mmX",
                "EEE, dd MMM yy HH:mm:ss Z", "EEE, dd-MMM-yy HH:mm:ss z", "EEE MMM dd HH:mm:ss yyyy"};

        for (int i = 0, expectedFormatsLength = expectedFormats.length; i < expectedFormatsLength; i++) {
            String expectedFormat = expectedFormats[i];
            final String actualFormat = formats.get(i);
            assertEquals(expectedFormat, actualFormat);
        }

//        for (String s : testAudit) {
//            System.out.println(s);
//        }

        assertTrue("Audit should be found", testAudit.isAuditPresentContaining("yyyy-MM-dd"));
        assertTrue("Audit should be found", testAudit.isAuditPresentContaining("yyyy ^ EEE, dd MMM yyyy HH:mm:ss z ^[a-zA-Z]{3},\\s\\d{2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{2}:\\d{2}:\\d{2}\\s(?:[a-zA-Z]{3}|(?:\\+|-)\\d{4})"));
    }

    /**
     * Tests that date formats are set correctly in DateTimeConfigUtils bean after a property change for each cluster properties.
     *
     */
    @Test
    public void testDateFormats_Integration() throws Exception {
        properties.put(ServerConfigParams.PARAM_DATE_TIME_CUSTOM_FORMATS, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z';yyyy-MM-dd hh:mm:ss aaa;yyyy-MM-dd HH:mm:ss.SSS");

        handler.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_DATE_TIME_AUTO_FORMATS, "", ""));
        handler.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_DATE_TIME_CUSTOM_FORMATS, "", ""));

        final List<String> configuredFormats = dateParser.getConfiguredDateFormats();
        assertEquals("Incorrect number of date formats registered", 3, configuredFormats.size());

        final String[] expectedFormats = {"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd hh:mm:ss aaa", "yyyy-MM-dd HH:mm:ss.SSS"};

        assertEquals("Incorrect number of formats resolved from cluster properties", expectedFormats.length, configuredFormats.size());
        for (int i = 0, configuredDateFormatsStringsSize = configuredFormats.size(); i < configuredDateFormatsStringsSize; i++) {
            String formatsString = configuredFormats.get(i);
            final String expectedFormat = expectedFormats[i];
            assertEquals("Unexpected format found for index '" + i +"'", expectedFormat, formatsString);
        }
    }

    // - PRIVATE
    private SimplePropertyChangeHandler handler;
    private Map<String,String> properties;
    public TestAudit testAudit;
    private DateTimeConfigUtils dateParser;

    private void clearContentTypes() {
        ContentTypeHeader.setConfigurableTextualContentTypes(new ContentTypeHeader[]{});
    }
}
