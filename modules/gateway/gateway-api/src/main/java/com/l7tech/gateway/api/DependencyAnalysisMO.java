package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;
import java.util.Map;

/**
 * This was created: 11/4/13 as 3:20 PM
 *
 * @author Victor Kazakov
 */
@XmlRootElement(name = "DependencyAnalysis")
@XmlType(propOrder = {"options", "searchObjectReference", "dependencies"})
public class DependencyAnalysisMO {
    private Map<String,String> options;
    private Reference searchObjectReference;
    private List<DependencyMO> dependencies;

    DependencyAnalysisMO(){}

    @XmlElement(name = "Options")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    @XmlElement(name = "Reference", required = true)
    public Reference getSearchObjectReference() {
        return searchObjectReference;
    }

    public void setSearchObjectReference(Reference searchObjectReference) {
        this.searchObjectReference = searchObjectReference;
    }

    @XmlElement(name = "Dependency")
    @XmlElementWrapper(name = "Dependencies")
    public List<DependencyMO> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<DependencyMO> dependencies) {
        this.dependencies = dependencies;
    }
}
