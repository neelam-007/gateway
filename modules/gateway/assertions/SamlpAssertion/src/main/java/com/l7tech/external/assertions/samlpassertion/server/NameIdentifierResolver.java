package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.external.assertions.samlpassertion.SamlProtocolAssertion;
import org.jetbrains.annotations.Nullable;

/**
 * @author: vchan
 */
public abstract class NameIdentifierResolver <SamlProtocolAssertionType extends SamlProtocolAssertion>{

    protected final SamlProtocolAssertionType assertion;
    protected String nameValue;
    protected String nameFormat; // SAML 2.0 only
    protected String nameQualifier; // SAML 2.0 only

    protected NameIdentifierResolver(final SamlProtocolAssertionType assertion) throws SamlpAssertionException {
        this.assertion = assertion;
        parse();
    }

    public String getNameValue() {
        return nameValue;
    }

    @Nullable
    public String getNameFormat() {
        return nameFormat;
    }

    public String getNameQualifier() {
        return nameQualifier;
    }

    protected abstract void parse() throws SamlpAssertionException;
}
