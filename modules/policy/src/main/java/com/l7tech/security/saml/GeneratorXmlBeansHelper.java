/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 */
package com.l7tech.security.saml;

import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import java.util.List;
import java.util.logging.Logger;

/**
 * Helper methods for SAML Generators V1 and V2.
 *
 * @author darmstrong
 */
class GeneratorXmlBeansHelper {

    // PROTECTED

    static XmlObject createXmlObject(Object objectValue) {
        final XmlObject xmlObject = XmlObject.Factory.newInstance();

        final Node domNode = xmlObject.getDomNode();
        final Document ownerDoc = domNode.getOwnerDocument();

        if (objectValue == null) {
            //TODO [Donal] - determine after 'missing when' is implemented if this needs to be here.
            // adding for completeness - currently no null values should arrive here.
            // ExpandVariables should never return a null value for any of it's process methods.
            xmlObject.setNil();
            logger.warning("Null value for AttributeValue found.");
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
