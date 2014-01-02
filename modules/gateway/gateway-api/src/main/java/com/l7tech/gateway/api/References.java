package com.l7tech.gateway.api;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * This holds a list of entity references
 *
 * @author Victor Kazakov
 */
@XmlRootElement(name = "references")
@XmlType(name = "EntityReferencesType", propOrder = {"links", "references"})
public class References {

    private List<Reference> references = null;
    private List<Link> links;

    References() {
    }

    References(List<Reference> references) {
        this.references = references;
    }

    @XmlElement(name = "reference")
    public List<Reference> getReferences() {
        return references;
    }

    public void setReferences(List<Reference> references) {
        this.references = references;
    }

    @XmlElement(name = "link")
    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }
}
