package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.external.assertions.samlpassertion.SamlProtocolAssertion;

/**
 * @author: vchan
 */
public abstract class NameIdentifierResolver {

    protected final SamlProtocolAssertion assertion;
    protected String nameValue;
    protected String nameFormat;

    protected NameIdentifierResolver(final SamlProtocolAssertion assertion) {
        this.assertion = assertion;

        parse();
    }

    public String getNameValue() {
        return nameValue;
    }

    public String getNameFormat() {
        return nameFormat;
    }

    protected abstract void parse();
}
