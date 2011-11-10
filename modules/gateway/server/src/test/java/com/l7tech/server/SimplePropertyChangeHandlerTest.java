package com.l7tech.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.util.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class SimplePropertyChangeHandlerTest {

    @Before
    public void setUp() throws Exception {
        properties = new HashMap<String, String>();
        final Config mockConfig = new MockConfig(properties);
        handler = new SimplePropertyChangeHandler();
        ApplicationContexts.inject(handler, CollectionUtils.<String, Object>mapBuilder()
                .put("serverConfig", mockConfig)
                .put("auditFactory", new TestAudit().factory())
                .unmodifiableMap()
        );

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
        properties.put(ServerConfigParams.PARAM_OTHER_TEXTUAL_CONTENT_TYPES, "text/plain1; charset=utf-8");
        types = handler.getConfiguredContentTypes();
        Assert.assertNotNull(types);
        Assert.assertFalse("List should not be empty", types.length == 0);
        Assert.assertEquals(types[0].getType(), "text");
        Assert.assertEquals(types[0].getSubtype(), "plain1");
        Assert.assertEquals(types[0].getEncoding(), Charsets.UTF8);

        // multiple values
        properties.put(ServerConfigParams.PARAM_OTHER_TEXTUAL_CONTENT_TYPES, "text1/plain1; charset=utf-8\ntext2/plain2; charset=utf-8");
        types = handler.getConfiguredContentTypes();
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
        Assert.assertNotNull(types);
        Assert.assertEquals(7, types.length);

        for (ContentTypeHeader type : types) {
            Assert.assertEquals(type.getType(), "text1");
            Assert.assertEquals(type.getSubtype(), "plain1");
            Assert.assertEquals(type.getEncoding(), Charsets.UTF8);
        }
    }

    // - PRIVATE
    private SimplePropertyChangeHandler handler;
    private Map<String,String> properties;

    private void clearContentTypes() {
        ContentTypeHeader.setConfigurableTextualContentTypes(new ContentTypeHeader[]{});
    }
}
