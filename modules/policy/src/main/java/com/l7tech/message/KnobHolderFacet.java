/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.util.ResourceUtils;

import java.io.Closeable;
import java.io.IOException;

/**
 * A MessageFacet that holds a provided pre-existing knob.
 */
public class KnobHolderFacet extends MessageFacet {
    private final Class knobClass;
    private final MessageKnob knob;

    public KnobHolderFacet(Message message, MessageFacet delegate, Class knobClass, MessageKnob knob) {
        super(message, delegate);
        if (knob == null || knobClass == null)
            throw new NullPointerException();
        if (!MessageKnob.class.isAssignableFrom(knobClass))
            throw new ClassCastException("knobClass must be derived from MessageKnob");
        this.knob = knob;
        this.knobClass = knobClass;
    }

    public MessageKnob getKnob(Class c) {
        if (c == knobClass)
            return knob;
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
