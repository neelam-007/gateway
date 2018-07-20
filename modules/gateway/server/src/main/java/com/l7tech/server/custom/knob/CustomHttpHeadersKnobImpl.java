package com.l7tech.server.custom.knob;

import com.l7tech.message.HeadersKnob;
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
        return message.getHeadersKnob().getHeaderNames(HeadersKnob.HEADER_TYPE_HTTP);
    }

    @Override
    public String[] getHeaderValues(final String name) {
        return message.getHeadersKnob().getHeaderValues(name, HeadersKnob.HEADER_TYPE_HTTP);
    }
}
