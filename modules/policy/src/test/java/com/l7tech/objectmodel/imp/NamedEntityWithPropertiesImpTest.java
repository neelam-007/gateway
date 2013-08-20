package com.l7tech.objectmodel.imp;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * This was created: 12/10/12 as 4:08 PM
 *
 * @author Victor Kazakov
 */
public class NamedEntityWithPropertiesImpTest {

    private static String myPropertyKey = "my.property.key";
    private static String key1 = "my.key.a";
    private static String value1 = "my.value.a";
    private static String key2 = "my.key.b";
    private static String value2 = "my.value.b";

    private static String xmlKey1Value1 = " <object class=\"java.util.HashMap\">\n" +
            "  <void method=\"put\">\n" +
            "   <string>my.key.a</string>\n" +
            "   <string>my.value.a</string>\n" +
            "  </void>\n" +
            " </object>";
    private static String xmlKey1and2Value1and2 = " <object class=\"java.util.HashMap\">\n" +
            "  <void method=\"put\">\n" +
            "   <string>my.key.b</string>\n" +
            "   <string>my.value.b</string>\n" +
            "  </void>\n" +
            "  <void method=\"put\">\n" +
            "   <string>my.key.a</string>\n" +
            "   <string>my.value.a</string>\n" +
            "  </void>\n" +
            " </object>";
    private static String xmlKey1and2Value1and1 = " <object class=\"java.util.HashMap\">\n" +
            "  <void method=\"put\">\n" +
            "   <string>my.key.b</string>\n" +
            "   <string>my.value.a</string>\n" +
            "  </void>\n" +
            "  <void method=\"put\">\n" +
            "   <string>my.key.a</string>\n" +
            "   <string>my.value.a</string>\n" +
            "  </void>\n" +
            " </object>";
    private static String xmlKey1and2Value1andnull = " <object class=\"java.util.HashMap\">\n" +
            "  <void method=\"put\">\n" +
            "   <string>my.key.b</string>\n" +
            "   <null/>\n" +
            "  </void>\n" +
            "  <void method=\"put\">\n" +
            "   <string>my.key.a</string>\n" +
            "   <string>my.value.a</string>\n" +
            "  </void>\n" +
            " </object>";

    private Field propertiesField;
    @Before
    public void setUp() throws Exception {
        propertiesField = NamedGoidEntityWithPropertiesImp.class.getDeclaredField("properties");
        propertiesField.setAccessible(true);
    }

    @Test
    public void testSettingRetrievingProperties() throws IllegalAccessException {
        MyNamedEntityWithProperties entity = new MyNamedEntityWithProperties();
        MyNamedEntityWithProperties entityCopy = new MyNamedEntityWithProperties();

        String xmlProperties = entity.getXmlProperties();
        Assert.assertNull("xmlProperties should be null if none are set.", xmlProperties);

        String property = entity.getProperty(myPropertyKey);
        Assert.assertNull(myPropertyKey + " should have a null value since it was not set yet.", property);

        entity.setProperty(key1, value1);
        xmlProperties = entity.getXmlProperties();
        Assert.assertNotNull("xmlProperties should be not be null there is one key set.", xmlProperties);
        Assert.assertTrue(xmlProperties.contains(xmlKey1Value1));
        Assert.assertEquals(value1, entity.getProperty(key1));
        entityCopy.setXmlProperties(entity.getXmlProperties());
        Assert.assertEquals(entity.getProperties(), entityCopy.getProperties());

        entity.setProperty(key2, value2);
        xmlProperties = entity.getXmlProperties();
        Assert.assertNotNull("xmlProperties should be not be null there is a key set.", xmlProperties);
        Assert.assertTrue(xmlProperties.contains(xmlKey1and2Value1and2));
        Assert.assertEquals(value1, entity.getProperty(key1));
        Assert.assertEquals(value2, entity.getProperty(key2));
        entityCopy.setXmlProperties(entity.getXmlProperties());
        Assert.assertEquals(entity.getProperties(), entityCopy.getProperties());

        entity.setProperty(key2, value1);
        xmlProperties = entity.getXmlProperties();
        Assert.assertNotNull("xmlProperties should be not be null there is a key set.", xmlProperties);
        Assert.assertTrue(xmlProperties.contains(xmlKey1and2Value1and1));
        Assert.assertEquals(value1, entity.getProperty(key1));
        Assert.assertEquals(value1, entity.getProperty(key2));
        entityCopy.setXmlProperties(entity.getXmlProperties());
        Assert.assertEquals(entity.getProperties(), entityCopy.getProperties());

        entity.setProperty(key2, null);
        xmlProperties = entity.getXmlProperties();
        Assert.assertNotNull("xmlProperties should be not be null there is a key set.", xmlProperties);
        Assert.assertTrue(xmlProperties.contains(xmlKey1and2Value1andnull));
        Assert.assertEquals(value1, entity.getProperty(key1));
        Assert.assertNull(entity.getProperty(key2));
        entityCopy.setXmlProperties(entity.getXmlProperties());
        Assert.assertEquals(entity.getProperties(), entityCopy.getProperties());
    }

    private class MyNamedEntityWithProperties extends NamedGoidEntityWithPropertiesImp {
        public HashMap<String, String> getProperties() throws IllegalAccessException {
            return (HashMap<String, String>) propertiesField.get(this);
        }
    }
}
