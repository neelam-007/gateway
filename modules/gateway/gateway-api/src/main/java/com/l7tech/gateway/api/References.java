package com.l7tech.gateway.api;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * This holds a list of entity references
 *
 * @author Victor Kazakov
 */
@XmlRootElement(name = "List")
@XmlType(name = "EntityReferencesType", propOrder = {"references"})
public class References {

    private List<Reference> references = null;

    References() {
    }

    References(List<Reference> references) {
        this.references = references;
    }

    @XmlElement(name = "Item")
    public List<Reference> getReferences() {
        return references;
    }

    public void setReferences(List<Reference> references) {
        this.references = references;
    }
}
