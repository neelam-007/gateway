/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.xml;

import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.tarari.GlobalTarariContextImpl;
import com.l7tech.server.ApplicationContexts;
import com.tarari.xml.rax.schema.SchemaLoader;
import com.tarari.xml.rax.schema.SchemaResolver;
import org.springframework.context.ApplicationContext;
import org.junit.Ignore;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Tests for schema validation code.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 3, 2004<br/>
 * $Id$<br/>
 *
 */
@Ignore("Developer only tests")
public class TarariSchemaValidationTest {
    private final String REUTERS_SCHEMA_URL = "http://locutus/reuters/schemas1/ReutersResearchAPI.xsd";

    @Before
    public void setUp() throws Exception {
        System.setProperty("com.l7tech.common.xml.tarari.enable", "true");
        GlobalTarariContextImpl context = (GlobalTarariContextImpl) TarariLoader.getGlobalContext();
        if (context != null) {
            context.compileAllXpaths();
        }
    }

    @Test
    public void testFuckingTarari() throws Exception {
        SchemaResolver resolver = new SchemaResolver() {
            public byte[] resolveSchema(String string, String string1, String string2) {
                throw new RuntimeException("Screw you!");
            }
        };

        SchemaLoader.setSchemaResolver(resolver);
        SchemaLoader.unloadAllSchemas();

        InputStream is = new URL(REUTERS_SCHEMA_URL).openStream();
        SchemaLoader.loadSchema(is, REUTERS_SCHEMA_URL);
    }
}
