package com.l7tech.policy.assertion.xml;

import com.l7tech.policy.assertion.Assertion;
import org.w3c.dom.*;
import org.apache.xml.serialize.XMLSerializer;
import org.apache.xml.serialize.OutputFormat;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringWriter;

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
            schema = elementToXml(schemaEl);
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
            potentiallists = wsdl.getDocumentElement().getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", WSDL_TYPES_ELNAME);
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
        switch (potentiallists.getLength()) {
            case 0:
                return null;
            case 1:
                schemael = (Element)potentiallists.item(0);
                break;
            default:
                return null;
        }
        return schemael;
    }

    /**
     * utility method that creates a document with an element of another document while maintaining the
     * namespace declarations of the parent elements.
     *
     * todo: move this to some xml util class
     */
    public static String elementToXml(Element schema) throws IOException, ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document schemadoc = dbf.newDocumentBuilder().newDocument();
        Element newRootNode = (Element)schemadoc.importNode(schema, true);
        schemadoc.appendChild(newRootNode);
        // remember all namespace declarations of parent elements
        Node node = schema.getParentNode();
        while (node != null) {
            if (node instanceof Element) {
                Element el = (Element)node;
                NamedNodeMap attrsmap = el.getAttributes();
                for (int i = 0; i < attrsmap.getLength(); i++) {
                    Attr attrnode = (Attr)attrsmap.item(i);
                    if (attrnode.getName().startsWith("xmlns:")) {
                        newRootNode.setAttribute(attrnode.getName(), attrnode.getValue());
                    }
                }

            }
            node = node.getParentNode();
        }
        // output to string
        final StringWriter sw = new StringWriter(512);
        XMLSerializer xmlSerializer = new XMLSerializer();
        xmlSerializer.setOutputCharStream(sw);
        OutputFormat of = new OutputFormat();
        of.setIndent(4);
        xmlSerializer.setOutputFormat(of);
        xmlSerializer.serialize(schemadoc);
        return sw.toString();
    }


    static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
    static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    static final String WSDL_TYPES_ELNAME = "types";
    static final String TOP_SCHEMA_ELNAME = "schema";

    private String schema;
}
