package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.CompiledXpath;
import com.l7tech.xml.xpath.FastXpath;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathVersion;

import java.util.Map;

/**
 * Abstract superclass for server assertions whose operation centers around running a single xpath against
 * a message.
 */
public abstract class ServerXpathBasedAssertion<AT extends XpathBasedAssertion> extends AbstractServerAssertion<AT> {
    private final String xpath;
    private final CompiledXpath compiledXpath;
    private final String[] dynamicVars;

    public ServerXpathBasedAssertion(AT assertion) {
        super(assertion);

        this.xpath = assertion.pattern();

        this.dynamicVars = XpathBasedAssertion.isFullyDynamicXpath(xpath)
            ? Syntax.getReferencedNames(xpath)
            : null;

        CompiledXpath compiledXpath;
        try {
            compiledXpath = dynamicVars != null ? null : assertion.getXpathExpression().compile();
        } catch (InvalidXpathException e) {
            logAndAudit(AssertionMessages.XPATH_PATTERN_INVALID, null, ExceptionUtils.getDebugException(e));
            // Invalid expression -- disable processing
            compiledXpath = null;
        }
        this.compiledXpath = compiledXpath;
    }

    /**
     * @param context the PEC, in case this is a fully-dynamic xpath based on a context variable that must be compiled every time.
     * @return the compiled xpath, or null if it was invalid and could not be compiled and so checkRequest should always fail.
     */
    protected CompiledXpath getCompiledXpath(PolicyEnforcementContext context) {
        if (compiledXpath != null)
            return compiledXpath;

        if (dynamicVars == null)
            return null;

        return compileDynamicXpath(context);
    }

    private CompiledXpath compileDynamicXpath(PolicyEnforcementContext context) {
        Map<String, ?> varMap = context.getVariableMap(dynamicVars, getAudit());
        String expression = ExpandVariables.process(xpath, varMap, getAudit());

        if (expression.length() < 1) {
            logAndAudit(AssertionMessages.XPATH_DYNAMIC_PATTERN_INVALID);
            return null;
        }

        final XpathExpression axp = assertion.getXpathExpression();
        XpathVersion xpathVersion = axp == null ? XpathVersion.UNSPECIFIED : axp.getXpathVersion();
        Map<String, String> nsmap = axp == null ? null : axp.getNamespaces();
        XpathExpression xpathExpression = new XpathExpression(expression, xpathVersion, nsmap) {
            @Override
            public FastXpath toTarariNormalForm() {
                // Disable FastXpath for fully-dynamic expressions so we don't thrash the FPGA with constant rebuilds
                return null;
            }
        };

        CompiledXpath compiledXpath;
        try {
            compiledXpath = xpathExpression.compile();
        } catch (InvalidXpathException e) {
            logAndAudit(AssertionMessages.XPATH_DYNAMIC_PATTERN_INVALID, null, ExceptionUtils.getDebugException(e));
            // Invalid expression -- disable processing
            compiledXpath = null;
        }
        return compiledXpath;
    }

    protected boolean compiledXpathReferencesTargetDocument() {
        return compiledXpath == null || compiledXpath.requiresTargetDocument();
    }

    protected boolean compiledXpathUsesVariables() {
        return compiledXpath == null || compiledXpath.usesVariables();
    }

    /** @return the xpath, or null. */
    protected String getXpath() {
        return xpath;
    }
}
