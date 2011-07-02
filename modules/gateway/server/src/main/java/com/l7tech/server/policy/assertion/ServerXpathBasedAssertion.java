package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.CompiledXpath;
import com.l7tech.policy.assertion.XpathBasedAssertion;

/**
 * Abstract superclass for server assertions whose operation centers around running a single xpath against
 * a message.
 */
public abstract class ServerXpathBasedAssertion<AT extends XpathBasedAssertion> extends AbstractServerAssertion<AT> {
    private final String xpath;
    private final CompiledXpath compiledXpath;

    public ServerXpathBasedAssertion(AT assertion) {
        super(assertion);
        CompiledXpath compiledXpath;
        try {
            compiledXpath = assertion.getXpathExpression().compile();
        } catch (InvalidXpathException e) {
            logAndAudit(AssertionMessages.XPATH_PATTERN_INVALID, null, ExceptionUtils.getDebugException(e));
            // Invalid expression -- disable processing
            compiledXpath = null;
        }
        this.xpath = assertion.pattern();
        this.compiledXpath = compiledXpath;
    }

    /** @return the compiled xpath, or null if it was invalid and could not be compiled and so checkRequest should always fail. */
    protected CompiledXpath getCompiledXpath() {
        return compiledXpath;
    }

    /** @return the xpath, or null. */
    protected String getXpath() {
        return xpath;
    }
}
