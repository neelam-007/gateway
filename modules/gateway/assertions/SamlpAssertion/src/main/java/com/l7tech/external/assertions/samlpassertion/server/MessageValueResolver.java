package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.external.assertions.samlpassertion.SamlProtocolAssertion;

/**
 * @author: vchan
 */
public abstract class MessageValueResolver<T> {

    protected final SamlProtocolAssertion assertion;
    protected T value;

    protected MessageValueResolver(final SamlProtocolAssertion assertion) {
        this.assertion = assertion;

        parse();
    }

    public T getValue() {
        return value;
    }

    protected abstract void parse();
}