package com.l7tech.policy.exporter;

import org.w3c.dom.Element;

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
    public abstract void serializeToRefElement(Element referencesParentElement);

    /**
     * Parse references from an exported 
     * @param serializedReferences a EXPORTED_REFERENCES_ELNAME element
     * @return
     */
    public static ExternalReference[] parseReferences(Element serializedReferences) {
        // todo
        return null;
    }
}
