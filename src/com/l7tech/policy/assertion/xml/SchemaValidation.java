package com.l7tech.policy.assertion.xml;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
     * Return whether the schema validation has been configured for message/operation
     * arguments (true) or for the whole content of the soap:body (false). The arguments
     * are considered to be the child elements of the operation element, and the operation
     * node is the  first element under the soap:body.
     * This is used for example when configuring the schema validation from wsdl in cases
     * where the schema in  wsdl/types element describes only the arguments (rpc/lit) and
     * not the whole content of the soap:body.
     *
     * @return true if schema applies to arguments only, false otherwise
     */
    public boolean isApplyToArguments() {
        return applyToArguments;
    }

    /**
     * Set whether the schema validation applies to arguments o
     * @param applyToArguments set true to apply to arguments, false to apply to whole body.
     *                         The default is false.
     * @see #isApplyToArguments()
     */
    public void setApplyToArguments(boolean applyToArguments) {
        this.applyToArguments = applyToArguments;
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
        schema = XmlUtil.elementToXml(schemaEl);
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
    private boolean applyToArguments;
}
