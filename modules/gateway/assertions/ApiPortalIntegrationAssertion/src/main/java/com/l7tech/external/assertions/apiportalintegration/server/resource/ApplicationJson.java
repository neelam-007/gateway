package com.l7tech.external.assertions.apiportalintegration.server.resource;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chean22, 1/22/2016
 */
public class ApplicationJson {
    private long incrementStart;
    private String entityType;
    private List<String> deletedIds = new ArrayList<>();
    private List<ApplicationEntity> newOrUpdatedEntities = new ArrayList<>();

    public ApplicationJson() {
    }

    public long getIncrementStart() {
        return incrementStart;
    }

    public void setIncrementStart(long incrementStart) {
        this.incrementStart = incrementStart;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public List<String> getDeletedIds() {
        return deletedIds;
    }

    public void setDeletedIds(List<String> deletedIds) {
        this.deletedIds = deletedIds;
    }

    public List<ApplicationEntity> getNewOrUpdatedEntities() {
        return newOrUpdatedEntities;
    }

    public void setNewOrUpdatedEntities(List<ApplicationEntity> newOrUpdatedEntities) {
        this.newOrUpdatedEntities = newOrUpdatedEntities;
    }
}
