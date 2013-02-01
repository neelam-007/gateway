package com.l7tech.message;

import com.l7tech.util.ResourceUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;

/**
 * A MessageFacet that holds a provided pre-existing knob.
 */
class KnobHolderFacet extends PreservableFacet {
    private final @NotNull Class[] knobClasses;
    private final @NotNull MessageKnob knob;
    private final boolean preservable;

    KnobHolderFacet(@NotNull Message message, @Nullable MessageFacet delegate, @NotNull MessageKnob knob, boolean preserveOnReinit, @NotNull Class... knobClasses) {
        super(message, delegate);
        for (Class knobClass : knobClasses) {
            if (!MessageKnob.class.isAssignableFrom(knobClass))
                throw new ClassCastException("Every knobClass must be derived from MessageKnob");
        }
        this.knob = knob;
        this.knobClasses = knobClasses;
        this.preservable = preserveOnReinit;
    }

    public MessageKnob getKnob(Class c) {
        for (Class knobClass : knobClasses) {
            if (c == knobClass)
                return knob;
        }
        return super.getKnob(c);
    }

    public void close() {
        try {
            if (knob instanceof java.io.Closeable) {
                ResourceUtils.closeQuietly((Closeable)knob);
            }
        } finally {
            super.close();
        }
    }

    @Override
    boolean isPreservable() {
        return preservable;
    }

    @Override
    MessageFacet reattach(Message message, MessageFacet delegate) {
        return new KnobHolderFacet(message, delegate, knob, preservable, knobClasses);
    }
}
