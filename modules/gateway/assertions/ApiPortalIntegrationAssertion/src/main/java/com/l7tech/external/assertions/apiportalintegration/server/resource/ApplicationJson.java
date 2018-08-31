package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.google.common.base.Strings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author chean22, 1/22/2016
 */
public class ApplicationJson {
    private long incrementStart;
    private String entityType;
    private String bulkSync;
    private boolean isApiPlanEnabled = false;
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

    public String getBulkSync() {
        return bulkSync;
    }

    public void setBulkSync(String bulkSync) {
        this.bulkSync = bulkSync;
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

    public boolean isApiPlanEnabled() {
        return isApiPlanEnabled;
    }

    public void setApiPlanEnabled(boolean apiPlanEnabled) {
        isApiPlanEnabled = apiPlanEnabled;
    }

    /**
     * Validate JSON
     */
    public void validate() throws IOException {
        if (Strings.isNullOrEmpty(getEntityType())) {
            throw new IOException("Invalid JSON input, missing entity type field");
        }
        if (Strings.isNullOrEmpty(getBulkSync())) {
            throw new IOException("Invalid JSON input, missing bulk sync field");
        }
        if (this.getNewOrUpdatedEntities() == null) {
            throw new IOException("Invalid JSON input, missing new or updated entities field");
        }

        // deletedIds field is optional.  When bulkSync is true or no deleted IDs are available, deletedIds will not exist.
    }
}
