package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlElement;
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
@XmlRootElement(name = "DependencyResults")
@XmlType(propOrder = {"options", "searchObject", "dependencies"})
public class DependencyResultsMO {
    private Map<String,String> options;
    private DependentObjectMO searchObject;
    private List<DependencyMO> dependencies;

    DependencyResultsMO(){}

    @XmlElement(name = "options", required = true)
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    @XmlElement(name = "searchObject", required = true)
    public DependentObjectMO getSearchObject() {
        return searchObject;
    }

    public void setSearchObject(DependentObjectMO searchObject) {
        this.searchObject = searchObject;
    }

    @XmlElement(name = "dependencies")
    public List<DependencyMO> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<DependencyMO> dependencies) {
        this.dependencies = dependencies;
    }
}
