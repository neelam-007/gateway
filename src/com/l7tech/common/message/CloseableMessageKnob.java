package com.l7tech.common.message;

/**
 * $Id$
 */
public interface CloseableMessageKnob extends MessageKnob {
    /** Free any resources used by this Knob.  Never throws. */
    void close();
}
