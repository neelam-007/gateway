package com.l7tech.common.message;

import com.l7tech.common.xml.tarari.TarariMessageContext;

/**
 * Makes a {@link TarariMessageContext} available to assertions.  (The TarariMessageContext will need to be downcasted to
 * {@link com.l7tech.common.xml.tarari.TarariMessageContextImpl} by the caller, since we can't statically link any
 * Tarari classes from any class that needs to run without a Tarari card).
 * <p>
 * Note that this knob will not be present in any message before {@link Message#isSoap} has been called on it, and even
 * then only on systems with Tarari hardware installed.
 */
public class TarariKnob implements CloseableMessageKnob {
    private TarariMessageContext context;

    public TarariKnob(TarariMessageContext context) {
        this.context = context;
    }

    /** @return the TarariMessageContext, or null if one is not available. */
    public TarariMessageContext getContext() {
        return context;
    }

    public void close() {
        if (context != null) {
            context.close();
            context = null;
        }
    }

    /**
     * Invalidate any TarariKnob attached to the specified Message.
     *
     * @param m the Message whose TarariKnob, if any, should be closed.
     */
    static void invalidate(Message m) {
        TarariKnob knob = (TarariKnob)m.getKnob(TarariKnob.class);
        if (knob != null)
            knob.close();
    }
}
