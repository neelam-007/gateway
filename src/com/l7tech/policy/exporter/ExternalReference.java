package com.l7tech.policy.exporter;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.util.XmlUtil;

import java.util.Collection;
import java.util.ArrayList;

/**
 * An external reference used by an exported policy.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 16, 2004<br/>
 * $Id$<br/>
 */
public abstract class ExternalReference {
    /**
     * Adds a child element to the passed references element that contains the xml
     * form of this reference object. Used by the policy exporter when serializing
     * references to xml format.
     * @param referencesParentElement
     */
    public abstract void serializeToRefElement(Element referencesParentElement);

    /**
     * Checks whether or not an external reference can be mapped on this local
     * system without administrator interaction.
     */
    public abstract boolean verifyReference();

    /**
     * Parse references from an exported policy's exp:References element.
     * @param refElements an ExporterConstants.EXPORTED_REFERENCES_ELNAME element
     * @return
     */
    public static ExternalReference[] parseReferences(Element refElements) throws InvalidDocumentFormatException {
        // Verify that the passed element is what is expected
        if (!refElements.getLocalName().equals(ExporterConstants.EXPORTED_REFERENCES_ELNAME)) {
            throw new InvalidDocumentFormatException("The passed element must be " +
                                                     ExporterConstants.EXPORTED_REFERENCES_ELNAME);
        }
        if (!refElements.getNamespaceURI().equals(ExporterConstants.EXPORTED_POL_NS)) {
            throw new InvalidDocumentFormatException("The passed element must have namespace " +
                                                     ExporterConstants.EXPORTED_POL_NS);
        }
        // Go through child elements and process them one by one
        Collection references = new ArrayList();
        NodeList children = refElements.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element refEl = (Element)child;
                // Get the type of reference
                String refType = refEl.getAttribute(ExporterConstants.REF_TYPE_ATTRNAME);
                if (refType.equals(IdProviderReference.class.getName())) {
                    references.add(IdProviderReference.parseFromElement(refEl));
                } else if (refType.equals(JMSEndpointReference.class.getName())) {
                    references.add(JMSEndpointReference.parseFromElement(refEl));
                } else if (refType.equals(CustomAssertionReference.class.getName())) {
                    references.add(CustomAssertionReference.parseFromElement(refEl));
                }
            }
        }
        return (ExternalReference[])references.toArray(new ExternalReference[0]);
    }

    static String getParamFromEl(Element parent, String param) {
        NodeList nodeList = parent.getElementsByTagName(param);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element)nodeList.item(i);
            String val = XmlUtil.getTextValue(node);
            if (val != null && val.length() > 0) return val;
        }
        return null;
    }
}
