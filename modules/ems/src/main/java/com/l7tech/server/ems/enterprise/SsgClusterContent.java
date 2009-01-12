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
    private String relatedId;  // Store the OID of a real published service / policy fragment if the entity is an alias.  relatedId = null if the entity is not an alias.
    private String parentId;
    private EntityType entityType;
    private String name;
    private Integer version;

    public SsgClusterContent(String id, String relatedId, String parentId, EntityType entityType, String name, Integer version) {
        this.id = id;
        this.relatedId = relatedId;
        this.parentId = parentId;
        this.entityType = entityType;
        this.name = ROOT.equals(name) && (parentId==null||parentId.isEmpty()) ? "/" : name;
        this.version = version;
    }

    @Override
    protected void writeJson() {
        add(JSONConstants.ID, id);
        add(JSONConstants.RELATED_ID, relatedId);
        add(JSONConstants.PARENT_ID, parentId);
        add(JSONConstants.TYPE, findType(entityType));
        add(JSONConstants.NAME, name);
        if (version != null) add(JSONConstants.VERSION, version.toString());
    }

    private String findType(EntityType type) {
        if (EntityType.SERVICE_OPERATION.equals(type)) {
            return JSONConstants.Entity.OPERATION;
        } else if (EntityType.FOLDER.equals(type)) {
            return JSONConstants.Entity.SERVICE_FOLDER;
        } else if (EntityType.SERVICE.equals(type)) {
            return JSONConstants.Entity.PUBLISHED_SERVICE;
        } else if (EntityType.SERVICE_ALIAS.equals(type)) {
            return JSONConstants.Entity.PUBLISHED_SERVICE_ALIAS;
        } else if (EntityType.POLICY.equals(type)) {
            return JSONConstants.Entity.POLICY_FRAGMENT;
        } else if (EntityType.POLICY_ALIAS.equals(type)) {
            return JSONConstants.Entity.POLICY_FRAGMENT_ALIAS;
        } else {
            throw new IllegalArgumentException("Unsupported entity type ('" + entityType + "') in SSG Cluster Content.");
        }
    }
}
