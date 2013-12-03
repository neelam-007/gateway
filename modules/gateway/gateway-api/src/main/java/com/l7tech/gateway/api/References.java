package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * This holds a list of entity references
 *
 * @author Victor Kazakov
 */
@XmlRootElement(name = "references")
@XmlAccessorType(XmlAccessType.FIELD)
public class References {

    @XmlElement(name = "reference")
    private List<Reference> references = null;

    References() {
    }

    References(List<Reference> references) {
        this.references = references;
    }

    public List<Reference> getReferences() {
        return references;
    }

    public void setReferences(List<Reference> references) {
        this.references = references;
    }
}
