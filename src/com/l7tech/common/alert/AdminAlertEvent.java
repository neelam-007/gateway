/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.alert;

import java.io.Serializable;

/**
 * Configuration for a alert that reports on administrative creations, updates and deletions
 * to a particular object or class of objects.
 */
public class AdminAlertEvent extends AlertEvent implements Serializable {
    /**
     * If true, this alert will fire when objects of the specified {@link #entityClassname} are created, regardless of {@link #entityId}.
     */
    private boolean watchingCreates;

    /**
     * If true, this alert will fire when objects of the specified {@link #entityClassname} are deleted, or a particular instance if {@link #entityId} is set.
     */
    private boolean watchingDeletes;

    /**
     * If true, this alert will fire when objects of the specified {@link #entityClassname} are updated, or a particular instance if {@link #entityId} is set.
     */
    private boolean watchingUpdates;

    /** Name of the entity class this alert will watch. */
    private String entityClassname;

    /** Identifier of the object this alert will watch, or null if all instances of {@link #entityClassname} are interesting. */
    private Long entityId;

    public String getEntityClassname() {
        return entityClassname;
    }

    public void setEntityClassname(String entityClassname) {
        this.entityClassname = entityClassname;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public boolean isWatchingDeletes() {
        return watchingDeletes;
    }

    public void setWatchingDeletes(boolean watchingDeletes) {
        this.watchingDeletes = watchingDeletes;
    }

    public boolean isWatchingUpdates() {
        return watchingUpdates;
    }

    public void setWatchingUpdates(boolean watchingUpdates) {
        this.watchingUpdates = watchingUpdates;
    }

    public boolean isWatchingCreates() {
        return watchingCreates;
    }

    public void setWatchingCreates(boolean watchingCreates) {
        this.watchingCreates = watchingCreates;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("<");
        sb.append(this.getClass().getName());
        if (entityClassname != null)
            sb.append("class=\"").append(entityClassname).append("\" ");
        if (entityId != null)
            sb.append("id=\"").append(entityId).append("\" ");
        sb.append("create=\"").append(watchingCreates).append("\" ");
        sb.append("update=\"").append(watchingUpdates).append("\" ");
        sb.append("delete=\"").append(watchingDeletes).append("\" ");
        sb.append("/>");
        return sb.toString();
    }
}
