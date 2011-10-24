/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 */
package com.l7tech.security.saml;

import com.l7tech.message.Message;
import com.l7tech.util.ExceptionUtils;
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
        } else if (objToProcess instanceof Message) {
            Message msg = (Message) objToProcess;
            try {
                // If the Message is XML add it as toString.
                final Element documentElement = msg.getXmlKnob().getDocumentReadOnly().getDocumentElement();
                final Node importedNode = ownerDoc.importNode(documentElement, true);
                parentNode.appendChild(importedNode);
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw new RuntimeException(e);
                }
                //todo [Donal] - perhaps clients should have already converted any Messages to Elements? Remove this burden from here.
                logger.warning("Could not include Message variable as XML content. Including as toString instead. Exception info: " + ExceptionUtils.getMessage(e));
                // this is equivalent to referencing ${messageVar} and forgetting to leave out the '.mainpart' suffix.
                final Text textNode = ownerDoc.createTextNode(msg.toString());
                parentNode.appendChild(textNode);
            }
        } else {
            String s = objToProcess.toString();
            final Text textNode = ownerDoc.createTextNode(s);
            parentNode.appendChild(textNode);
        }
    }
}
