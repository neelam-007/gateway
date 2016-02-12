package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.google.common.base.Strings;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author chean22, 1/26/2016
 */
public class PortalSyncPostbackJson {

    public final static String SYNC_STATUS_OK = "ok";
    public final static String SYNC_STATUS_ERROR = "error";
    public final static String SYNC_STATUS_PARTIAL = "partial";
    public final static String ERROR_ENTITY_ID_LABEL = "id";
    public final static String ERROR_ENTITY_MSG_LABEL = "msg";

    private String incrementStatus;
    private long incrementStart;
    private long incrementEnd;
    private String entityType;
    private String bulkSync;
    private String errorMessage;
    //keys: "id", "msg"
    private List<Map<String, String>> entityErrors = new ArrayList<>();
    private String syncLog;

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

    public String getSyncLog() {
        return syncLog;
    }

    public void setSyncLog(String syncLog) {
        this.syncLog = syncLog;
    }


    /**
     * validate the postback message.
     */
    public void validate() throws IOException{
        if (this.getIncrementStatus().equalsIgnoreCase(SYNC_STATUS_PARTIAL) && CollectionUtils.isEmpty(getEntityErrors())) {
            throw new IOException("When the incremental status is '" + SYNC_STATUS_PARTIAL + "', the message must contain a list of entity sync errors");
        } else if (getIncrementStatus().equalsIgnoreCase(SYNC_STATUS_ERROR) && StringUtils.isEmpty(getErrorMessage())) {
            throw new IOException("When the incremental status is '" + SYNC_STATUS_ERROR + "', the message must contain an error message");
        }
        if (Strings.isNullOrEmpty(getEntityType())) {
            throw new IOException("Invalid JSON input, missing entity type field");
        }
        if (Strings.isNullOrEmpty(getBulkSync())) {
            throw new IOException("Invalid JSON input, missing bulk sync field");
        }
        if (Strings.isNullOrEmpty(getSyncLog())) {
            throw new IOException("Invalid JSON input, missing sync log field");
        }
    }

}
