package com.l7tech.policy.assertion.xml;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * Contains the xml schema for which requests and/or responses need to be validated against.
 * At runtime, the element being validated is always the child of the soap body element.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 4, 2004<br/>
 * $Id$<br/>
 *
 */
public class SchemaValidation extends Assertion {

    /**
     * the actual schema used for validation
     * @return a string containing the xml document
     */
    public String getSchema() {
        return schema;
    }

    /**
     * @param schema a string containing the actual xml schema (not a url)
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * extracts the schema from the wsdl's &lt;types&gt; element and set the schema property with it
     * @param wsdl an actual xml Document contianing the wsdl for the service
     * @throws IllegalArgumentException thrown when the wsdl passed did not refer to a schema
     */
    public void assignSchemaFromWsdl(Document wsdl) throws IllegalArgumentException, IOException {
        Element schemaEl = extractSchemaElementFromWsdl(wsdl);
        if (schemaEl == null) throw new IllegalArgumentException("this wsdl document does not contain a" +
                                                                 "schema to validate against");
        try {
            schema = XmlUtil.elementToXml(schemaEl);
        } catch (ParserConfigurationException e) {
            throw new IOException("could not serialize the schema in the wsdl " + e.getMessage());
        }
    }

    private Element extractSchemaElementFromWsdl(Document wsdl)  {
        if (wsdl == null) return null;
        NodeList potentiallists = wsdl.getDocumentElement().getElementsByTagName(WSDL_TYPES_ELNAME);
        Element typesel = null;
        switch (potentiallists.getLength()) {
            case 1:
                typesel = (Element)potentiallists.item(0);
                break;
            default:
                break;
        }
        if (typesel == null) {
            potentiallists = wsdl.getDocumentElement().getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/",
                                                                              WSDL_TYPES_ELNAME);
            typesel = null;
            switch (potentiallists.getLength()) {
                case 1:
                    typesel = (Element)potentiallists.item(0);
                    break;
                default:
                    break;
            }
        }

        if (typesel == null) {
            return null;
        }
        potentiallists = typesel.getElementsByTagNameNS(W3C_XML_SCHEMA, TOP_SCHEMA_ELNAME);
        Element schemael = null;
        // todo, some wsdls may contain more than one schema in one types element
        // those schemas cannot be combines since a schema can have only one targetNamespace
        // therefore, the proper way to suppor this would be to redesign this assertion so
        // that it can contain more than one schema to validate against.
        // see bugzilla #915
        if (potentiallists.getLength() > 0) {
            schemael = (Element)potentiallists.item(0);
        }
        return schemael;
    }

    public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
    public static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    public static final String WSDL_TYPES_ELNAME = "types";
    public static final String TOP_SCHEMA_ELNAME = "schema";

    private String schema;
}
