/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.policy.exporter;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.DomUtils;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;

import java.util.Collection;
import java.util.ArrayList;

/**
 * An external reference used by an exported policy.
 */
public abstract class ExternalReference {
    /**
     * Adds a child element to the passed references element that contains the xml
     * form of this reference object. Used by the policy exporter when serializing
     * references to xml format.
     * @param referencesParentElement
     */
    abstract void serializeToRefElement(Element referencesParentElement);

    /**
     * Checks whether or not an external reference can be mapped on this local
     * system without administrator interaction.
     */
    abstract boolean verifyReference() throws InvalidPolicyStreamException;

    /**
     * Once an exported policy is loaded with it's references and the references are
     * verified, this method will apply the necessary changes to the assertion. If
     * the assertion type passed does not relate to the reference, it will be left
     * untouched.
     * Returns false if the assertion should be deleted from the tree.
     * @param assertionToLocalize will be fixed once this method returns.
     */
    abstract boolean localizeAssertion(Assertion assertionToLocalize);

    /**
     * Parse references from an exported policy's exp:References element.
     * @param refElements an ExporterConstants.EXPORTED_REFERENCES_ELNAME element
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
        Collection<ExternalReference> references = new ArrayList<ExternalReference>();
        NodeList children = refElements.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element refEl = (Element)child;
                // Get the type of reference
                String refType = refEl.getAttribute(ExporterConstants.REF_TYPE_ATTRNAME);
                if (refType.equals(FederatedIdProviderReference.class.getName())) {
                    references.add(FederatedIdProviderReference.parseFromElement(refEl));
                } else if (refType.equals(IdProviderReference.class.getName())) {
                    references.add(IdProviderReference.parseFromElement(refEl));
                } else if (refType.equals(JMSEndpointReference.class.getName())) {
                    references.add(JMSEndpointReference.parseFromElement(refEl));
                } else if (refType.equals(CustomAssertionReference.class.getName())) {
                    references.add(CustomAssertionReference.parseFromElement(refEl));
                } else if (refType.equals(ExternalSchemaReference.class.getName())) {
                    references.add(ExternalSchemaReference.parseFromElement(refEl));
                } else if (refType.equals(IncludedPolicyReference.class.getName())) {
                    references.add(IncludedPolicyReference.parseFromElement(refEl));
                } else if (refType.equals(TrustedCertReference.class.getName())) {
                    references.add(TrustedCertReference.parseFromElement(refEl));
                } else if (refType.equals(PrivateKeyReference.class.getName())) {
                    references.add(PrivateKeyReference.parseFromElement(refEl));
                } else if (refType.equals(JdbcConnectionReference.class.getName())) {
                    references.add(JdbcConnectionReference.parseFromElement(refEl));
                }
            }
        }
        return references.toArray(new ExternalReference[0]);
    }

    static String getParamFromEl(Element parent, String param) {
        NodeList nodeList = parent.getElementsByTagName(param);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element)nodeList.item(i);
            String val = DomUtils.getTextValue(node);
            if (val != null && val.length() > 0) return val;
        }
        return null;
    }
}
