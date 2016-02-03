package com.l7tech.external.assertions.apiportalintegration.server.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author chean22, 1/26/2016
 */
public class PortalSyncPostbackJson {
    private String incrementStatus;
    private long incrementStart;
    private long incrementEnd;
    private String entityType;
    private String bulkSync;
    private String errorMessage;
    //keys: "id", "msg"
    private List<Map<String, String>> entityErrors = new ArrayList<>();

    public PortalSyncPostbackJson() {
    }

    public String getIncrementStatus() {
        return incrementStatus;
    }

    public void setIncrementStatus(String incrementStatus) {
        this.incrementStatus = incrementStatus;
    }

    public long getIncrementStart() {
        return incrementStart;
    }

    public void setIncrementStart(long incrementStart) {
        this.incrementStart = incrementStart;
    }

    public long getIncrementEnd() {
        return incrementEnd;
    }

    public void setIncrementEnd(long incrementEnd) {
        this.incrementEnd = incrementEnd;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getBulkSync() {
        return bulkSync;
    }

    public void setBulkSync(String bulkSync) {
        this.bulkSync = bulkSync;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<Map<String, String>> getEntityErrors() {
        return entityErrors;
    }

    public void setEntityErrors(List<Map<String, String>> entityErrors) {
        this.entityErrors = entityErrors;
    }
}
