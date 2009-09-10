package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.proxy.policy.assertion.ClientAssertionWithMetaSupport;
import com.l7tech.xml.xpath.DeferredFailureDomCompiledXpathHolder;
import com.l7tech.xml.xpath.DomCompiledXpath;
import org.jaxen.JaxenException;

/**
 * Code shared among client XpathBasedAssertions that need a single DomCompiledXpath instance.
 */
public abstract class ClientDomXpathBasedAssertion<AT extends XpathBasedAssertion> extends ClientAssertionWithMetaSupport {
    protected final AT data;
    private final DeferredFailureDomCompiledXpathHolder compiledXpath;

    protected ClientDomXpathBasedAssertion(AT data) {
        super(data);
        if (data == null)
            throw new IllegalArgumentException("Assertion bean is null");
        this.data = data;
        this.compiledXpath = new DeferredFailureDomCompiledXpathHolder(data.getXpathExpression());
    }

    protected DomCompiledXpath getCompiledXpath() throws JaxenException {
        return compiledXpath.getCompiledXpath();
    }
}
