/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.event.wsman;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEvent;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class WSManagementRequestEvent extends ApplicationEvent {

    public WSManagementRequestEvent(Object source) {
        super(source);
    }

    /**
     * Get any processing exception caused by processing this message.
     * @return any exception which was thrown
     */
    @Nullable
    public Exception getProcessingException() {
        return processingException;
    }

    /**
     * Set an exception caused during processing of this event.
     * @param processingException event thrown
     */
    public void setProcessingException(@Nullable Exception processingException) {
        this.processingException = processingException;
    }

    /**
     * True when some module processed this event.
     * @return
     */
    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    /**
     * Get the version of the policy bundle this event supports.
     * @return
     */
    @NotNull
    public String getBundleVersionNs() {
        return bundleVersionNs;
    }

    public void setBundleVersionNs(String bundleVersionNs) {
        this.bundleVersionNs = bundleVersionNs;
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled.set(cancelled);
    }

    // - PRIVATE

    private final static String GATEWAY_MGMT_APRIL_10 = "http://ns.l7tech.com/2010/04/gateway-management";
    @Nullable
    private Exception processingException;
    private boolean processed = false;
    private String bundleVersionNs = GATEWAY_MGMT_APRIL_10;
    private AtomicBoolean cancelled = new AtomicBoolean();

}
