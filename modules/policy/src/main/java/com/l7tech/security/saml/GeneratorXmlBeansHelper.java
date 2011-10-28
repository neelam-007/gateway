/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 */
package com.l7tech.security.saml;

import org.apache.xmlbeans.XmlObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import java.util.List;
import java.util.logging.Logger;

import static com.l7tech.security.saml.Attribute.NullBehavior.NO_ATTRIBUTE_VALUE;

/**
 * Helper methods for SAML Generators V1 and V2.
 *
 * @author darmstrong
 */
class GeneratorXmlBeansHelper {

    // PROTECTED

    /**
     * Create an XmlObject to represent an AttributeValue elements content.
     *
     * //TODO Bug 11200 - clients of this method may need to call more than once, once for each AttributeValue element.
     *
     * @param objectValue value to add as contents of an AttributeValue. May be null.
     * @param nullBehavior Behavior of the AttributeValue contents when null.
     * @return The XmlObject to add to an AttributeValue element. May be null when no AttributeValue should be added.
     */
    @Nullable
    static XmlObject createXmlObjectForAttributeValueContents(@Nullable final Object objectValue,
                                                              @NotNull final Attribute.NullBehavior nullBehavior) {
        // determine if there is anything to do - this is a convenience for callers
        if (objectValue == null && nullBehavior == NO_ATTRIBUTE_VALUE) {
            return null;
        }

        final XmlObject xmlObject = XmlObject.Factory.newInstance();

        final Node domNode = xmlObject.getDomNode();
        final Document ownerDoc = domNode.getOwnerDocument();

        if (objectValue == null) {
            xmlObject.setNil();
        } else if (objectValue instanceof List) {
            List<Object> listObjs = (List<Object>) objectValue;
            for (Object obj : listObjs) {
                addObjectToAttributeValueElm(ownerDoc, domNode, obj);
            }
        } else {
            addObjectToAttributeValueElm(ownerDoc, domNode, objectValue);
        }

        return xmlObject;
    }

    // PRIVATE

    private static final Logger logger = Logger.getLogger(GeneratorXmlBeansHelper.class.getName());

    private static void addObjectToAttributeValueElm(final Document ownerDoc, final Node parentNode, final Object objToProcess) {
        if (objToProcess instanceof Element) {
            Element elm = (Element) objToProcess;
            final Node importedNode = ownerDoc.importNode(elm, true);
            parentNode.appendChild(importedNode);
        } else {
            String s = objToProcess.toString();
            final Text textNode = ownerDoc.createTextNode(s);
            parentNode.appendChild(textNode);
        }
    }
}
