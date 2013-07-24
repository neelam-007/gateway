package com.l7tech.policy.assertion.ext.message.knob;

/**
 * An interface providing access to specific aspects of a particular message including (but not limited to)
 * specific details about the transport, protocol, format, or standard.
 * <p/>
 * For example, the CustomHttpHeadersKnob provides access to HTTP headers within a message.
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
