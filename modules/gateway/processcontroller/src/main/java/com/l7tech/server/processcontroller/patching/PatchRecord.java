package com.l7tech.server.processcontroller.patching;

import java.util.Collection;
import java.util.Collections;

/**
 * @author jbufu
 */
public class PatchRecord {

    // - PUBLIC

    public PatchRecord(long timestamp, String patchId, PatchServiceApi.Action action) {
        this(timestamp, patchId, action, null);
    }

    public PatchRecord(long timestamp, String patchId, PatchServiceApi.Action action, Collection<String> nodes) {
        this.timestamp = timestamp;
        this.patchId = patchId;
        this.action = action;
        if (nodes != null)
            this.nodes = Collections.unmodifiableCollection(nodes);
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

    public Collection<String> getNodes() {
        return nodes;
    }

    public void setNodes(Collection<String> nodes) {
        this.nodes = nodes == null ? nodes : Collections.unmodifiableCollection(nodes);
    }

    public String getLogMessage() {
        return logMessage;
    }

    public void setLogMessage(String logMessage) {
        this.logMessage = logMessage;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(timestamp).append(":").append(patchId).append(":").append(action).append(":");
        result.append(nodes == null ? "" : nodes.toString()).append(":");
        if (logMessage != null) result.append(logMessage);
        return result.toString();
    }

    // - PRIVATE

    private long timestamp;
    private String patchId;
    private PatchServiceApi.Action action;
    private Collection<String> nodes;
    private String logMessage;

}
