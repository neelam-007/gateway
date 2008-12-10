package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.EntityType;

/**
 * Not thread safe (see JSONSupport).
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 26, 2008
 */
public class SsgClusterContent extends JSONSupport {
    private static final String ROOT = "Root Node";

    private String id;
    private String parentId;
    private EntityType entityType;
    private String name;
    private Integer version;

    public SsgClusterContent(String id, String parentId, EntityType entityType, String name, Integer version) {
        this.id = id;
        this.parentId = parentId;
        this.entityType = entityType;
        this.name = ROOT.equals(name) && (parentId==null||parentId.isEmpty()) ? "/" : name;
        this.version = version;
    }

    @Override
    protected void writeJson() {
        add(JSONConstants.ID, id);
        add(JSONConstants.PARENT_ID, parentId);
        add(JSONConstants.TYPE, findType(entityType));
        add(JSONConstants.NAME, name);
        if (version != null) add(JSONConstants.VERSION, version.toString());
    }

    private String findType(EntityType type) {
        if (type == null) {
            return JSONConstants.Entity.OPERATION;
        } else if (EntityType.FOLDER.equals(type)) {
            return JSONConstants.Entity.SERVICE_FOLDER;
        } else if (EntityType.SERVICE.equals(type)) {
            return JSONConstants.Entity.PUBLISHED_SERVICE;
        } else if (EntityType.POLICY.equals(type)) {
            return JSONConstants.Entity.POLICY_FRAGMENT;
        } else {
            throw new IllegalArgumentException("Unsupported entity type ('" + entityType + "') in SSG Cluster Content.");
        }
    }
}
