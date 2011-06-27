/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.util.ResourceUtils;

import javax.validation.constraints.NotNull;
import java.io.Closeable;

/**
 * A MessageFacet that holds a provided pre-existing knob.
 */
public class KnobHolderFacet extends MessageFacet {
    private final @NotNull Class[] knobClasses;
    private final @NotNull MessageKnob knob;

    public KnobHolderFacet(@NotNull Message message, @NotNull MessageFacet delegate, @NotNull Class knobClass, @NotNull MessageKnob knob) {
        super(message, delegate);
        if (knob == null || knobClass == null)
            throw new NullPointerException();
        if (!MessageKnob.class.isAssignableFrom(knobClass))
            throw new ClassCastException("knobClass must be derived from MessageKnob");
        this.knob = knob;
        this.knobClasses = new Class[] { knobClass };
    }

    public KnobHolderFacet(@NotNull Message message, @NotNull MessageFacet delegate, @NotNull MessageKnob knob, @NotNull Class... knobClasses) {
        super(message, delegate);
        if (knob == null || knobClasses == null)
            throw new NullPointerException();
        for (Class knobClass : knobClasses) {
            if (!MessageKnob.class.isAssignableFrom(knobClass))
                throw new ClassCastException("Every knobClass must be derived from MessageKnob");
        }
        this.knob = knob;
        this.knobClasses = knobClasses;
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
}
