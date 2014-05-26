package com.l7tech.external.assertions.jsondocumentstructure.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class JsonDocumentStructureTestHelper {
    protected static final String SINGLE_OBJECT_DOCUMENT =
            "{\n" +
            "    \"firstName\": \"John\",\n" +
            "    \"lastName\": \"Smith\",\n" +
            "    \"isAlive\": true,\n" +
            "    \"age\": 25,\n" +
            "    \"height_cm\": 167.64,\n" +
            "    \"address\": {\n" +
            "        \"streetAddress\": \"21 2nd Street\",\n" +
            "        \"city\": \"New York\",\n" +
            "        \"state\": \"NY\",\n" +
            "        \"postalCode\": \"10021-3100\"\n" +
            "    },\n" +
            "    \"phoneNumbers\": [\n" +
            "       { \"type\": \"home\", \"number\": \"212 555-1234\" },\n" +
            "       { \"type\": \"mobile\", \"number\": \"646 555-1234\" },\n" +
            "       { \"type\": \"fax\",  \"number\": \"212 555-4567\" },\n" +
            "       { \"type\": \"work\",  \"number\": \"646 555-4567\" },\n" +
            "       { \"type\": \"pager\",  \"number\": \"212 555-8901\" }\n" +
            "    ]\n" +
            "}";

    protected static final String NESTED_ARRAYS_DOCUMENT =
            "[\n" +
            "    \"John\", \"Smith\", [\n" +
            "        \"21 2nd Street\", \"New York\", \"NY\", \"10021-3100\"\n" +
            "    ],\n" +
            "    [\n" +
            "       { \"type\": \"home\", \"number\": \"212 555-1234\" },\n" +
            "       { \"type\": \"mobile\", \"number\": \"646 555-1234\" },\n" +
            "       { \"type\": \"fax\",  \"number\": \"212 555-4567\" },\n" +
            "       { \"type\": \"work\",  \"number\": \"646 555-4567\" },\n" +
            "       { \"type\": \"pager\",  \"number\": \"212 555-8901\" }\n" +
            "    ]\n" +
            "]";

    // missing comma to separate object entries
    protected static final String POORLY_FORMED_DOCUMENT =
            "{\n" +
            "    \"firstName\": \"John\"\n" +
            "    \"lastName\": \"Smith\"\n" +
            "}";

    // single string value token
    protected static final String SINGLE_TYPED_VALUE_DOCUMENT = "\"single typed value - string\"";

    // a document starting with a typed value may have no other tokens - in this case the comma and number tokens
    protected static final String POORLY_FORMED_SINGLE_TYPED_VALUE_DOCUMENT = "\"single typed value - string\", 20.6";

    protected static InputStream getInputStream(String testContent) throws IOException {
        return new ByteArrayInputStream(testContent.getBytes());
    }
}
