package com.l7tech.server.ems.enterprise;

import org.mortbay.util.ajax.JSON;

import java.util.Map;

import com.l7tech.objectmodel.EntityType;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 26, 2008
 */
public class SsgClusterContent implements JSON.Convertible {
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
    public void toJSON(JSON.Output output) {
        output.add(JSONConstants.ID, id);
        output.add(JSONConstants.PARENT_ID, parentId);
        output.add(JSONConstants.TYPE, findType(entityType));
        output.add(JSONConstants.NAME, name);
        if (version != null) output.add(JSONConstants.VERSION, version.toString());
    }

    @Override
    public void fromJSON(Map map) {
        throw new UnsupportedOperationException("Mapping from JSON not supported.");
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
