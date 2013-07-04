package com.l7tech.policy.assertion.ext.message.knob;

/**
 * Base interface representing different knobs of a Message.
 */
public interface CustomMessageKnob {

    /**
     * Retrieve the knob name
     */
    String getKnobName();

    /**
     * Retrieve the knob description.
     */
    String getKnobDescription();
}
