package com.l7tech.server.ems.enterprise;

/**
 * Not thread safe (see JSONSupport).
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 26, 2008
 */
public class SsgClusterContent extends JSONSupport {
    private static final String ROOT = "Root Node";

    private String externalId;
    private String relatedId;  // Store the OID of a real published service / policy fragment if the entity is an alias.  relatedId = null if the entity is not an alias.
    private String parentId;
    private com.l7tech.objectmodel.EntityType entityType;
    private String name;
    private Integer version;
    private boolean isOperation;

    public SsgClusterContent(String externalId, String relatedId, String parentId, com.l7tech.objectmodel.EntityType entityType, String name, Integer version) {
        this.externalId = externalId;
        this.relatedId = relatedId;
        this.parentId = parentId;
        this.entityType = entityType;
        this.name = ROOT.equals(name) && (parentId==null||parentId.isEmpty()) ? "/" : name;
        this.version = version;
    }

    // Constructor for operations.
    public SsgClusterContent(String externalId, String parentId, String name) {
        this.externalId = externalId;
        this.parentId = parentId;
        this.name = ROOT.equals(name) && (parentId==null||parentId.isEmpty()) ? "/" : name;
        isOperation = true;
    }

    @Override
    protected void writeJson() {
        add(JSONConstants.ID, externalId);
        add(JSONConstants.RELATED_ID, relatedId);
        add(JSONConstants.PARENT_ID, parentId);
        add(JSONConstants.TYPE, findType(entityType, isOperation));
        add(JSONConstants.NAME, name);
        if (version != null) add(JSONConstants.VERSION, version.toString());
    }

    private String findType(com.l7tech.objectmodel.EntityType type, boolean isOperation) {
        if (isOperation) {
            return JSONConstants.EntityType.OPERATION;
        }

        if (com.l7tech.objectmodel.EntityType.FOLDER.equals(type)) {
            return JSONConstants.EntityType.SERVICE_FOLDER;
        } else if (com.l7tech.objectmodel.EntityType.SERVICE.equals(type)) {
            return JSONConstants.EntityType.PUBLISHED_SERVICE;
        } else if (com.l7tech.objectmodel.EntityType.SERVICE_ALIAS.equals(type)) {
            return JSONConstants.EntityType.PUBLISHED_SERVICE_ALIAS;
        } else if (com.l7tech.objectmodel.EntityType.POLICY.equals(type)) {
            return JSONConstants.EntityType.POLICY_FRAGMENT;
        } else if (com.l7tech.objectmodel.EntityType.POLICY_ALIAS.equals(type)) {
            return JSONConstants.EntityType.POLICY_FRAGMENT_ALIAS;
        } else {
            throw new IllegalArgumentException("Unsupported entity type ('" + entityType + "') in SSG Cluster Content.");
        }
    }
}
