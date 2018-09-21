package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.EntityHeader;

import java.io.Serializable;

/**
 * Transfer object for Resolved Entity Header information
 */
public class ResolvedEntityHeader implements Serializable {
    private EntityHeader entityHeader;
    private String name;
    private String path;
    private static final long serialVersionUID = -1752153501322477805L;

    public EntityHeader getEntityHeader() {
        return entityHeader;
    }

    public void setEntityHeader(final EntityHeader entityHeader) {
        this.entityHeader = entityHeader;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }
}
