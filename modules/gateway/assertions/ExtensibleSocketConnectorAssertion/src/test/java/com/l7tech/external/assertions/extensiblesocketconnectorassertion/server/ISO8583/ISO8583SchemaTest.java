package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 19/02/13
 * Time: 12:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class ISO8583SchemaTest {

    @Test
    public void testDefaultSchemaWithDefaultTestFile() throws Exception {
        String testFile = "DeFaUlT";  //should accept 'default', do not match case.
        ISO8583Schema schema = new ISO8583Schema();
        schema.initialize(testFile);

        ISO8583DataElement[] dataElements = schema.getSchema();

        assertNotNull(dataElements);
        assertEquals(192, dataElements.length);
        for (int i = 0; i < 128; i++) {
            assertNotNull(dataElements[i]);
        }
        for (int i = 128; i < dataElements.length; i++) {
            assertNull(dataElements[i]);
        }
    }

    @Test
    public void testDefaultSchemaWithEmptyTestFile() throws Exception {
        String testFile = "";
        ISO8583Schema schema = new ISO8583Schema();
        schema.initialize(testFile);

        ISO8583DataElement[] dataElements = schema.getSchema();

        assertNotNull(dataElements);
        assertEquals(192, dataElements.length);
        for (int i = 0; i < 128; i++) {
            assertNotNull(dataElements[i]);
        }
        for (int i = 128; i < dataElements.length; i++) {
            assertNull(dataElements[i]);
        }
    }

    @Test
    public void testDefaultSchemaWithNullTestFile() throws Exception {
        String testFile = null;
        ISO8583Schema schema = new ISO8583Schema();
        schema.initialize(testFile);

        ISO8583DataElement[] dataElements = schema.getSchema();

        assertNotNull(dataElements);
        assertEquals(192, dataElements.length);
        for (int i = 0; i < 128; i++) {
            assertNotNull(dataElements[i]);
        }
        for (int i = 128; i < dataElements.length; i++) {
            assertNull(dataElements[i]);
        }
    }

    @Test
    public void testDefaultSchemaWithFileName() throws Exception {
        String testFile = getClass().getResource("schemaTest.properties").getPath();
        ISO8583Schema schema = new ISO8583Schema();
        schema.initialize(testFile);

        ISO8583DataElement[] dataElements = schema.getSchema();

        assertNotNull(dataElements);
        assertEquals(192, dataElements.length);

        for (int i = 0; i < 123; i++) {
            assertNotNull(dataElements[i]);
        }
        for (int i = 123; i < dataElements.length; i++) {
            assertNull(dataElements[i]);
        }
    }
}
