/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.Auditor;
import com.l7tech.common.xml.InvalidXpathException;
import com.l7tech.common.xml.xpath.CompiledXpath;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;

/**
 * Abstract superclass for server assertions whose operation centers around running a single xpath against
 * a message.
 */
public abstract class ServerXpathBasedAssertion extends AbstractServerAssertion implements ServerAssertion {
    protected final Auditor auditor;
    private final String xpath;
    private final CompiledXpath compiledXpath;

    public ServerXpathBasedAssertion(XpathBasedAssertion assertion, ApplicationContext springContext) {
        super(assertion);
        auditor = new Auditor(this, springContext, Logger.getLogger(getClass().getName()));
        CompiledXpath compiledXpath;
        try {
            compiledXpath = assertion.getXpathExpression().compile();
        } catch (InvalidXpathException e) {
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
