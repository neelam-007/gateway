package com.l7tech.server.processcontroller.patching;

/**
 * @author jbufu
 */
public class PatchRecord {

    // - PUBLIC

    public PatchRecord(long timestamp, String patchId, PatchServiceApi.Action action) {
        this.timestamp = timestamp;
        this.patchId = patchId;
        this.action = action;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPatchId() {
        return patchId;
    }

    public void setPatchId(String patchId) {
        this.patchId = patchId;
    }

    public PatchServiceApi.Action getAction() {
        return action;
    }

    public void setAction(PatchServiceApi.Action action) {
        this.action = action;
    }

    public String getLogMessage() {
        return logMessage;
    }

    public void setLogMessage(String logMessage) {
        this.logMessage = logMessage;
    }

    // - PRIVATE

    private long timestamp;
    private String patchId;
    private PatchServiceApi.Action action;
    private String logMessage;

}
