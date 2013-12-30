package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * This was created: 11/4/13 as 3:25 PM
 *
 * @author Victor Kazakov
 */
@XmlRootElement(name = "Dependency")
@XmlType(propOrder = {"dependentObject", "dependencies"})
public class DependencyMO {
    private Reference dependentObjectReference;
    private List<DependencyMO> dependencies;

    DependencyMO(){}

    @XmlElement(name = "Reference", required = true)
    public Reference getDependentObject() {
        return dependentObjectReference;
    }

    public void setDependentObject(Reference dependentObjectReference) {
        this.dependentObjectReference = dependentObjectReference;
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
