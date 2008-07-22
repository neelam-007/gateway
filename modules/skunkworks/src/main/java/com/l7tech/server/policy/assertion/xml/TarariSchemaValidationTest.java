/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.xml;

import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.tarari.GlobalTarariContextImpl;
import com.tarari.xml.rax.schema.SchemaLoader;
import com.tarari.xml.rax.schema.SchemaResolver;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;

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
public class TarariSchemaValidationTest extends TestCase {
    private static final Logger logger = Logger.getLogger(TarariSchemaValidationTest.class.getName());
    private final String REUTERS_SCHEMA_URL = "http://locutus/reuters/schemas1/ReutersResearchAPI.xsd";
    private final String REUTERS_REQUEST_URL = "http://locutus/reuters/request1.xml";
    private ApplicationContext testApplicationContext;

    public TarariSchemaValidationTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TarariSchemaValidationTest.class);
    }

    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(TarariSchemaValidationTest.suite());
        System.out.println("Test complete: " + TarariSchemaValidationTest.class);
    }

    protected void setUp() throws Exception {
        System.setProperty("com.l7tech.common.xml.tarari.enable", "true");
        GlobalTarariContextImpl context = (GlobalTarariContextImpl) TarariLoader.getGlobalContext();
        if (context != null) {
            context.compileAllXpaths();
        }
        testApplicationContext = null;//ApplicationContexts.getTestApplicationContext();
    }

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
