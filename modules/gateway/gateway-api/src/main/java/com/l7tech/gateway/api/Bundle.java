package com.l7tech.gateway.api;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "Bundle")
@XmlType(name = "Bundle", propOrder = {"name", "references", "mappings", "dependencyGraph"})
public class Bundle {
    private List<Item> references;
    private List<Mapping> mappings;
    private DependencyListMO dependencies;
    private String name; // used to identify or describe this bundle

    Bundle(){}

    @XmlElementWrapper(name = "References")
    @XmlElement(name = "Item")
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

    @XmlElement(name = "DependencyGraph")
    public DependencyListMO getDependencyGraph() {
        return dependencies;
    }

    @XmlElement(name="name")
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setDependencyGraph(DependencyListMO dependencies) {
        this.dependencies = dependencies;
    }
}
