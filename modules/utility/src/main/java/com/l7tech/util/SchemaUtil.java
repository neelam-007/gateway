package com.l7tech.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * XML Schema utility methods.
 *
 * @author Steve Jones
 */
public class SchemaUtil {

    //- PUBLIC

    /**
     * Check if the given document is an XML Schema.
     *
     * @param document The document to check
     * @return true if the document is a schema.
     */
    public static boolean isSchema(final Document document) {
        boolean schema = false;

        if (document != null) {
            schema = isSchema(document.getDocumentElement());
        }

        return schema;
    }

    /**
     * Check if the given element is an XML Schema (document element).
     *
     * @param element The document to check (must be a "schema" element)
     * @return true if the element is a schema.
     */
    public static boolean isSchema(final Element element) {
        boolean schema = false;

        if (element != null) {
            schema = isSchema(new QName(element.getNamespaceURI(), element.getLocalName()));
        }

        return schema;
    }

    /**
     * Check if the given QName is an XML Schema (document element).
     *
     * @param name The qualified name to check (must be a "schema")
     * @return true if the qname is for a schema.
     */
    public static boolean isSchema(final QName name) {
        boolean schema = false;

        if (name != null) {
            schema = XMLSCHEMA_ELEMENTS.contains(name);
        }

        return schema;
    }

    //- PRIVATE

    private static final Collection<QName> XMLSCHEMA_ELEMENTS = Collections.unmodifiableList(Arrays.asList(
        new QName("http://www.w3.org/1999/XMLSchema", "schema"),
        new QName("http://www.w3.org/2000/10/XMLSchema", "schema"),
        new QName("http://www.w3.org/2001/XMLSchema", "schema")
    ));
}
