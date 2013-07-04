package com.l7tech.policy.assertion.ext.message.knob;

/**
 * A simple Http headers extractor knob.
 */
public interface CustomHttpHeadersKnob extends CustomMessageKnob {
    /**
     * @return an array of the names of all the headers in the message, or an empty array if there are none. Never null.
     */
    String[] getHeaderNames();

    /**
     * @return an array of the values for the specified header in the message, or an empty array if there are none. Never null.
     */
    String[] getHeaderValues(String name);
}
