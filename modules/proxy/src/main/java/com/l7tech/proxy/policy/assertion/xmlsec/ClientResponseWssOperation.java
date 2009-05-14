package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.DomCompiledXpath;

/**
 * Code shared among response signature and response encryption checking assertions.
 */
public abstract class ClientResponseWssOperation<AT extends XpathBasedAssertion> extends ClientAssertion {
    protected final AT data;
    private final DomCompiledXpath compiledXpath;
    private final InvalidXpathException compileFailure;

    protected ClientResponseWssOperation(AT data) {
        if (data == null)
            throw new IllegalArgumentException("security elements is null");
        this.data = data;
        DomCompiledXpath xp;
        InvalidXpathException fail;
        try {
            xp = new DomCompiledXpath(data.getXpathExpression());
            fail = null;
        } catch (InvalidXpathException e) {
            xp = null;
            fail = e;
        }
        this.compiledXpath = xp;
        this.compileFailure = fail;
    }

    protected DomCompiledXpath getCompiledXpath() throws PolicyAssertionException {
        if (compileFailure != null)
            throw new PolicyAssertionException(data, compileFailure);
        if (compiledXpath == null)
            throw new PolicyAssertionException(data, "No CompiledXpath"); // can't happen
        return compiledXpath;
    }
}
