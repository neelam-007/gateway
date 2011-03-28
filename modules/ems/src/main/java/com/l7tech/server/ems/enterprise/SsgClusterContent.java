package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.EntityType;

import java.io.Serializable;

/**
 * Not thread safe (see JSONSupport).
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 26, 2008
 */
public class SsgClusterContent extends JSONSupport implements Comparable<SsgClusterContent>, Serializable {
    private static final String ROOT = "Root Node";

    private final String externalId;
    private final String relatedId;  // Store the OID of a real published service / policy fragment if the entity is an alias.  relatedId = null if the entity is not an alias.
    private final String parentId;
    private final com.l7tech.objectmodel.EntityType entityType;
    private final String name;
    private final Integer version;
    private final boolean isOperation;

    public SsgClusterContent(String externalId, String relatedId, String parentId, com.l7tech.objectmodel.EntityType entityType, String name, Integer version) {
        this.externalId = externalId;
        this.relatedId = relatedId;
        this.parentId = parentId;
        this.entityType = entityType;
        this.name = ROOT.equals(name) && (parentId==null||parentId.isEmpty()) ? "/" : name;
        this.version = version;
        this.isOperation = false;
    }

    // Constructor for operations.
    public SsgClusterContent(String externalId, String parentId, String name) {
        this.externalId = externalId;
        this.relatedId = null;
        this.parentId = parentId;
        this.entityType = null;
        this.name = ROOT.equals(name) && (parentId==null||parentId.isEmpty()) ? "/" : name;
        this.version = null;
        isOperation = true;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getRelatedId() {
        return relatedId;
    }

    public String getParentId() {
        return parentId;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public String getName() {
        return name;
    }

    public boolean isOperation() {
        return isOperation;
    }

    public Integer getVersion() {
        return version;
    }

    public static String getJsonType( final EntityType type ) {
        String jsonType = null;

        if (EntityType.FOLDER.equals(type)) {
            jsonType = JSONConstants.EntityType.SERVICE_FOLDER;
        } else if (EntityType.SERVICE.equals(type)) {
            jsonType = JSONConstants.EntityType.PUBLISHED_SERVICE;
        } else if (EntityType.SERVICE_ALIAS.equals(type)) {
            jsonType = JSONConstants.EntityType.PUBLISHED_SERVICE_ALIAS;
        } else if (EntityType.POLICY.equals(type)) {
            jsonType = JSONConstants.EntityType.POLICY_FRAGMENT;
        } else if (EntityType.POLICY_ALIAS.equals(type)) {
            jsonType = JSONConstants.EntityType.POLICY_FRAGMENT_ALIAS;
        }

        return jsonType;
    }

    @Override
    protected void writeJson() {
        add(JSONConstants.ID, externalId);
        add(JSONConstants.RELATED_ID, relatedId);
        add(JSONConstants.PARENT_ID, parentId);
        add(JSONConstants.TYPE, findType(entityType, isOperation));
        add(JSONConstants.NAME, name);
        if (version != null) add(JSONConstants.VERSION, version.toString());
        add(JSONConstants.RBAC_CUD, true);
    }

    private String findType(com.l7tech.objectmodel.EntityType type, boolean isOperation) {
        if (isOperation) {
            return JSONConstants.EntityType.OPERATION;
        }

        final String jsonType = getJsonType( type );
        if ( jsonType == null ) {
            throw new IllegalArgumentException("Unsupported entity type ('" + entityType + "') in Gateway Cluster Content.");
        }
        return jsonType;
    }

    @Override
    public int compareTo(SsgClusterContent o) {
        return name.compareToIgnoreCase( o.name );
    }
}
