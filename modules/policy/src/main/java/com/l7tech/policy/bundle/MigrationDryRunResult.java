package com.l7tech.policy.bundle;

import java.io.Serializable;

/**
 * A data transfer class holding a migration error mapping in a usable format for the UI
 *  (e.g. as much as possible let view handle only display logic).
 */
public class MigrationDryRunResult implements Serializable {
    private String errorTypeStr, entityTypeStr, srcId, errorMessage, name, policyResourceXml;

    public MigrationDryRunResult(String errorTypeStr, String entityTypeStr, String srcId, String errorMessage, String name, String policyResourceXml) {
        this.errorTypeStr = errorTypeStr;
        this.entityTypeStr = entityTypeStr;
        this.srcId = srcId;
        this.errorMessage = errorMessage;
        this.name = name;
        this.policyResourceXml = policyResourceXml;
    }

    @Override
    public String toString() {
        return "errorTypeStr=" + errorTypeStr + ", entityTypeStr=" + entityTypeStr + ", id=" + srcId +  ", errorMessage=" + errorMessage + ", name=" + name;
    }

    public String getErrorTypeStr() {
        return errorTypeStr;
    }

    public void setErrorTypeStr(String errorTypeStr) {
        this.errorTypeStr = errorTypeStr;
    }

    public String getEntityTypeStr() {
        return entityTypeStr;
    }

    public String getSrcId() {
        return srcId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getName() {
        return name;
    }

    public String getPolicyResourceXml() {
        return policyResourceXml;
    }
}