package com.l7tech.server.event;

import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEvent;

/**
 * Event used to request the appropriate handler to install a policy bundle.
 */
public class InstallPolicyBundleEvent extends ApplicationEvent {

    public InstallPolicyBundleEvent(final Object source,
                                    final BundleResolver bundleResolver,
                                    final PolicyBundleInstallerContext context) {
        super(source);
        this.bundleResolver = bundleResolver;
        this.context = context;
    }

    /**
     * The context may be updated by the processor of this event. Any changes must be seen by the producer of this event.
     *
     * @return context
     */
    @NotNull
    public PolicyBundleInstallerContext getContext() {
        return context;
    }

    @NotNull
    public BundleResolver getBundleResolver() {
        return bundleResolver;
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

    // - PRIVATE

    @NotNull
    final BundleResolver bundleResolver;
    @NotNull
    final PolicyBundleInstallerContext context;
    @Nullable
    private Exception processingException;
    private boolean processed = false;

}
