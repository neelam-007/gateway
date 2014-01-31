package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;
import java.util.Map;

@XmlTransient
public abstract class DependencyResults<D> {
    private Map<String,Object> options;
    private Item searchObjectItem;
    private List<D> dependencies;

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
    public List<D> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<D> dependencies) {
        this.dependencies = dependencies;
    }
}
