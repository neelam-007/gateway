package com.l7tech.external.assertions.websocket.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This is singleton class used to read Xsd schema
 * User: nilic
 * Date: 7/11/12
 * Time: 11:04 AM
 * To change this template use File | Settings | File Templates.
 */
public class ReadWebSocketXsdSchema {

    private static ReadWebSocketXsdSchema instance = null;
    private static final String xmlSchema = "<xs:schema attributeFormDefault=\"unqualified\" elementFormDefault=\"qualified\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <xs:element name=\"request\">\n" +
            "        <xs:complexType>\n" +
            "            <xs:sequence>\n" +
            "                <xs:element name=\"websocket\">\n" +
            "                    <xs:complexType>\n" +
            "                        <xs:sequence>\n" +
            "                            <xs:element type=\"xs:string\" name=\"id\" minOccurs=\"1\"/>\n" +
            "                            <xs:element type=\"xs:string\" name=\"clientId\" minOccurs=\"1\"/>\n" +
            "                            <xs:element type=\"xs:string\" name=\"type\" minOccurs=\"1\"/>\n" +
            "                            <xs:element type=\"xs:string\" name=\"origin\" minOccurs=\"1\"/>\n" +
            "                            <xs:element type=\"xs:string\" name=\"protocol\" minOccurs=\"1\"/>\n" +
            "                            <xs:element type=\"xs:string\" name=\"offset\" minOccurs=\"1\"/>\n" +
            "                            <xs:element type=\"xs:string\" name=\"length\" minOccurs=\"1\"/>\n" +
            "                            <xs:element type=\"xs:string\" name=\"data\" minOccurs=\"1\"/>\n" +
            "                        </xs:sequence>\n" +
            "                    </xs:complexType>\n" +
            "                </xs:element>\n" +
            "            </xs:sequence>\n" +
            "        </xs:complexType>\n" +
            "    </xs:element>\n" +
            "</xs:schema>";


    private ReadWebSocketXsdSchema (){}

    public static synchronized ReadWebSocketXsdSchema getInstance(){
        if (instance == null)
            instance = new ReadWebSocketXsdSchema();
        return instance;
    }

    public InputStream getInputStreamXMLSchema() throws IOException {

        return new ByteArrayInputStream(xmlSchema.getBytes());
    }
}
