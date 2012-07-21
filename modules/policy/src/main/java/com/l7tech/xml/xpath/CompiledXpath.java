/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml.xpath;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.xpath.XPathExpressionException;
import java.util.Map;

import static com.l7tech.xml.xpath.XpathVersion.UNSPECIFIED;
import static com.l7tech.xml.xpath.XpathVersion.XPATH_1_0;

/**
 * Represents an XPath expression that has been precompiled, and can be run against an ElementCursor.
 */
public abstract class CompiledXpath {
    /** A global, always-ready CompiledXpath that always instantly evaluates to Boolean.TRUE. */
    public static final CompiledXpath ALWAYS_TRUE = new Tautology();

    /** A global, always-ready CompiledXpath that always instantly evaluates to Boolean.FALSE. */
    public static final CompiledXpath ALWAYS_FALSE = new Contradiction();

    private final String expression;
    private final Map<String, String> nsmap;
    private final boolean mightUseVariables;
    private final boolean requiresTargetDocument;
    private final XpathVersion xpathVersion;

    /**
     * Initialize the CompiledXpath superclass.
     *
     * @param expression  the generic xpath expression, not optimized for any particular implementation.  Must not be null.
     * @param xpathVersion xpath version ("1.0", "2.0", or "3.0"), or null to assume "1.0".
     * @param nsmap       the namespace map, or null if no qualified names are used by expression.
     *
     */
    protected CompiledXpath(String expression, @NotNull XpathVersion xpathVersion, @Nullable Map<String, String> nsmap) {
        if (expression == null) throw new NullPointerException();
        if (UNSPECIFIED.equals(xpathVersion))
            xpathVersion = XPATH_1_0;
        this.xpathVersion = xpathVersion;
        this.expression = expression;
        this.nsmap = nsmap;
        boolean vars;
        try {
            vars = XpathUtil.usesXpathVariables(expression, xpathVersion);
        } catch (XPathExpressionException e) {
            vars = true;
        }
        this.mightUseVariables = vars;
        this.requiresTargetDocument = XpathUtil.usesTargetDocument(expression, xpathVersion);
    }

    /** @return the generic xpath expression string.  Never null. */
    protected String getExpression() {
        return expression;
    }

    /** @return the namespace map, or null if no qualified names are used in the expression. */
    protected Map<String, String> getNamespaceMap() {
        return nsmap;
    }

    /** @return true if this compiled xpath might use any XPath variables. */
    public boolean usesVariables() {
        return mightUseVariables;
    }

    /**
     * @return XPath version number, eg "1.0", "2.0" or "3.0".  Never null.
     */
    public XpathVersion getXpathVersion() {
        return xpathVersion;
    }

    /**
     * @return true if this compiled xpath might require a real target document.
     *         False if it does not refer to a target document and so should be safe to match against a dummy document.
     */
    public boolean requiresTargetDocument() {
        return requiresTargetDocument;
    }

    /** A utility expression that is always true. */
    private static final class Tautology extends CompiledXpath {
        protected Tautology() {
            super("0=0", UNSPECIFIED, null);
        }
    }

    /** A utility expression that is always false. */
    private static final class Contradiction extends CompiledXpath {
        protected Contradiction() {
            super("0=1", UNSPECIFIED, null);
        }
    }

    @Override
    public String toString() {
        return expression;
    }
}
