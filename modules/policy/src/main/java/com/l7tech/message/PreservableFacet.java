package com.l7tech.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface implemented by message facets that need to be preserved when a message is re-initialized.
 * <p/>
 * This is for transport-specific features like HttpServletResponseFacet, where the binding to an underlying
 * HTTP servlet response must be maintained after reinitialization.
 */
public abstract class PreservableFacet extends MessageFacet {
    protected PreservableFacet(@NotNull Message message, @Nullable MessageFacet delegate) {
        super(message, delegate);
    }

    /**
     * Reattach this facet (or, possibly, a new instance with the same contents as this facet) to the specified Message as the new root facet.
     *
     * @param message message to which this facet (or, possibly, a new instance with the same contents as this facet) should be reattached.
     * @param delegate the current root facet. It will become the next facet in the list, with this facet (or its new clone) to become the new root facet.
     * @return this facet (or, possibly, a new instance with the same contents) to serve as the new root facet.
     */
    public abstract MessageFacet reattach(Message message, MessageFacet delegate);
}
