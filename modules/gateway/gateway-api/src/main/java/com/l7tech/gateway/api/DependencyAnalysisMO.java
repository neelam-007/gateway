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
@XmlType(propOrder = {"options", "searchObjectItem", "dependencies"})
public class DependencyAnalysisMO {
    private Map<String,Object> options;
    private Item searchObjectItem;
    private List<DependencyMO> dependencies;

    DependencyAnalysisMO(){}

    @XmlElement(name = "Options")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    @XmlElement(name = "Reference", required = true)
    public Item getSearchObjectItem() {
        return searchObjectItem;
    }

    public void setSearchObjectItem(Item searchObjectItem) {
        this.searchObjectItem = searchObjectItem;
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
