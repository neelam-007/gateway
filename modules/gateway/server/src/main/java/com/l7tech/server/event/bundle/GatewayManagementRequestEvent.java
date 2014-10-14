package com.l7tech.server.event.bundle;

import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEvent;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class GatewayManagementRequestEvent extends ApplicationEvent {

    public GatewayManagementRequestEvent(Object source) {
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
     * @return boolean
     */
    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    @Nullable
    public String getReasonNotProcessed() {
        return reasonNotProcessed;
    }

    public void setReasonNotProcessed(@Nullable String reasonNotProcessed) {
        this.reasonNotProcessed = reasonNotProcessed;
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled.set(cancelled);
    }

    // - PRIVATE

    @Nullable
    private Exception processingException;
    private boolean processed = false;
    @Nullable
    private String reasonNotProcessed;
    private AtomicBoolean cancelled = new AtomicBoolean();

}
