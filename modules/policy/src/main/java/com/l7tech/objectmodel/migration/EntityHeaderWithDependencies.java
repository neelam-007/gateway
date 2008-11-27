/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel.migration;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityHeaderRef;

import javax.xml.bind.annotation.*;
import java.util.Collections;
import java.util.Set;

/** @author alex */
@XmlRootElement
@XmlType(propOrder={"header", "dependencies"})
public class EntityHeaderWithDependencies {
    private EntityHeader header;

    private Set<EntityHeaderRef> dependencies;

    protected EntityHeaderWithDependencies() { }

    public EntityHeaderWithDependencies(EntityHeader header, Set<EntityHeaderRef> dependencies) {
        this.header = header;
        this.dependencies = dependencies;
    }

    public EntityHeaderWithDependencies(EntityHeader header) {
        this.header = header;
        this.dependencies = Collections.emptySet();
    }

    @XmlElementRef
    public EntityHeader getHeader() {
        return header;
    }

    @XmlElementWrapper(name="dependencies")
    @XmlElementRef
    public Set<EntityHeaderRef> getDependencies() {
        return dependencies;
    }

    protected void setHeader(EntityHeader header) {
        this.header = header;
    }

    protected void setDependencies(Set<EntityHeaderRef> dependencies) {
        this.dependencies = dependencies;
    }
}
