package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;
import java.util.Map;

/**
 * This was created: 11/4/13 as 3:20 PM
 *
 * @author Victor Kazakov
 */
@XmlRootElement(name = "DependencyList")
@XmlType(propOrder = {"options", "searchObjectItem", "dependencies", "missingDependencies"})
@XmlAccessorType(XmlAccessType.PROPERTY)
public class DependencyListMO  {

    protected DependencyListMO(){}

    private Map<String,Object> options;
    private DependencyMO searchObjectItem;
    private List<DependencyMO> dependencies;
    private List<DependencyMO> missingDependencies;

    @XmlElement(name = "Options")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    @XmlElement(name = "Reference")
    public DependencyMO getSearchObjectItem() {
        return searchObjectItem;
    }

    public void setSearchObjectItem(DependencyMO searchObjectItem) {
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

    @XmlElement(name = "MissingDependency")
    @XmlElementWrapper(name = "MissingDependencies")
    public List<DependencyMO> getMissingDependencies() {
        return missingDependencies;
    }

    public void setMissingDependencies(List<DependencyMO> missingDependencies) {
        this.missingDependencies = missingDependencies;
    }
}
