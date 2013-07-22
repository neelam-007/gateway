package com.l7tech.server.custom.knob;

import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.ext.message.knob.CustomHttpHeadersKnob;

import org.jetbrains.annotations.NotNull;

/**
 * Sample HttpHeader knob implementation.
 */
public class CustomHttpHeadersKnobImpl extends CustomMessageKnobBase implements CustomHttpHeadersKnob {

    private final Message message;

    public CustomHttpHeadersKnobImpl(@NotNull final Message message) {
        this("HttpHeaders", "HttpHeaders custom message knob", message);
    }

    public CustomHttpHeadersKnobImpl(@NotNull final String name,
                                     @NotNull final String description,
                                     @NotNull final Message message) {
        super(name, description);
        this.message = message;
    }

    @Override
    public String[] getHeaderNames() {
        final HttpServletRequestKnob requestKnob = message.getKnob(HttpServletRequestKnob.class);
        return requestKnob == null ? new String[0] : requestKnob.getHeaderNames();
    }

    @Override
    public String[] getHeaderValues(final String name) {
        final HttpServletRequestKnob requestKnob = message.getKnob(HttpServletRequestKnob.class);
        return requestKnob == null ? new String[0] : requestKnob.getHeaderValues(name);
    }
}
