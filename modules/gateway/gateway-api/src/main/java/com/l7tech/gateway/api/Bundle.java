package com.l7tech.gateway.api;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "Bundle")
@XmlType(name = "Bundle", propOrder = {"references", "mappings"})
public class Bundle {
    private List<Item> references;
    private List<Mapping> mappings;

    Bundle(){}

    @XmlElementWrapper(name = "References")
    @XmlElement(name = "Item", required = true)
    public List<Item> getReferences() {
        return references;
    }

    public void setReferences(List<Item> references) {
        this.references = references;
    }

    @XmlElementWrapper(name = "Mappings")
    @XmlElement(name = "Mapping")
    public List<Mapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<Mapping> mappings) {
        this.mappings = mappings;
    }
}
