package com.l7tech.gateway.api;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "Bundle")
@XmlType(name = "Bundle", propOrder = {"managedObjects"})
public class Bundle {
    private List<Resource> managedObjects;

    Bundle() {
    }

    @XmlElementWrapper(name = "Resources", required = false)
    @XmlElement(name = "Resource", required = false)
    public List<Resource> getManagedObjects() {
        return managedObjects;
    }

    public void setManagedObjects(List<Resource> managedObjects) {
        this.managedObjects = managedObjects;
    }
}
