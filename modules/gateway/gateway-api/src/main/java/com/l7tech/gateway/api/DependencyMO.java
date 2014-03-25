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
@XmlType(propOrder = {"name", "id", "type", "dependencies"})
public class DependencyMO {
    private String name;
    private String id;
    private String type;
    private List<DependencyMO> dependencyIds;

    DependencyMO(){}

    @XmlElement(name = "Name", required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(name = "Id", required = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlElement(name = "Type", required = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @XmlElement(name = "Dependency")
    @XmlElementWrapper(name = "Dependencies")
    public List<DependencyMO> getDependencies() {
        return dependencyIds;
    }

    public void setDependencies(List<DependencyMO> dependencyIds) {
        this.dependencyIds = dependencyIds;
    }
}
