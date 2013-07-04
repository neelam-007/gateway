package com.l7tech.gateway.common.custom;

import com.l7tech.json.JSONFactory;
import com.l7tech.policy.assertion.ext.message.CustomJsonData;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test JsonDataToCustomConverter.
 */
public class JsonDataToCustomConverterTest {
    static private final String JSON_SOURCE = "{\n" +
            "\"input\": [\n" +
            "{ \"firstName\":\"John\" , \"lastName\":\"Doe\" }, \n" +
            "{ \"firstName\":\"Anna\" , \"lastName\":\"Smith\" }, \n" +
            "{ \"firstName\":\"Peter\" , \"lastName\":\"Jones\" }\n" +
            "]\n" +
            "}";

    @Before
    public void setUp() throws Exception {
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullSupport() {
        //noinspection ConstantConditions
        new JsonDataToCustomConverter(null);
        fail("This message should not have been displayed");
    }

    @Test
    public void test() throws Exception {
        final CustomJsonData jsonData = new JsonDataToCustomConverter(JSONFactory.getInstance().newJsonData(JSON_SOURCE));

        assertNotNull(jsonData);
        assertEquals(jsonData.getJsonData(), JSON_SOURCE);
        assertTrue(jsonData.getJsonObject() instanceof Map);

        Map jsonObjRoot = (Map)jsonData.getJsonObject();

        assertSame("jsonData root have only one element", jsonObjRoot.size(), 1);
        assertNotNull("jsonData input element is not null", jsonObjRoot.get("input"));
        assertTrue("jsonData input element is of List type", jsonObjRoot.get("input") instanceof List);
        List jsonObjInput = (List)jsonObjRoot.get("input");

        assertSame("jsonData input element have 3 child elements", jsonObjInput.size(), 3);
        assertTrue("jsonData input element child(0) is of type Map", jsonObjInput.get(0) instanceof Map);
        assertTrue("jsonData input element child(1) is of type Map", jsonObjInput.get(1) instanceof Map);
        assertTrue("jsonData input element child(2) is of type Map", jsonObjInput.get(2) instanceof Map);

        Map jsonObjInputChild0 = (Map)jsonObjInput.get(0);
        assertSame(jsonObjInputChild0.size(), 2);
        assertEquals(jsonObjInputChild0.get("firstName"), "John");
        assertEquals(jsonObjInputChild0.get("lastName"), "Doe");

        Map jsonObjInputChild1 = (Map)jsonObjInput.get(1);
        assertSame(jsonObjInputChild1.size(), 2);
        assertEquals(jsonObjInputChild1.get("firstName"), "Anna");
        assertEquals(jsonObjInputChild1.get("lastName"), "Smith");

        Map jsonObjInputChild2 = (Map)jsonObjInput.get(2);
        assertSame(jsonObjInputChild2.size(), 2);
        assertEquals(jsonObjInputChild2.get("firstName"), "Peter");
        assertEquals(jsonObjInputChild2.get("lastName"), "Jones");
    }
}
