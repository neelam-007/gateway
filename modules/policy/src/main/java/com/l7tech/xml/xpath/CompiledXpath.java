/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml.xpath;

import java.util.Map;

/**
 * Represents an XPath expression that has been precompiled, and can be run against an ElementCursor.
 */
public abstract class CompiledXpath {
    /** A global, always-ready CompiledXpath that always instantly evaluates to Boolean.TRUE. */
    public static final CompiledXpath ALWAYS_TRUE = new Tautology();

    /** A global, always-ready CompiledXpath that always instantly evaluates to Boolean.FALSE. */
    public static final CompiledXpath ALWAYS_FALSE = new Contradiction();

    private final String expression;
    private final Map nsmap;

    /**
     * Initialize the CompiledXpath superclass.
     *
     * @param expression  the generic xpath expression, not optimized for any particular implementation.  Must not be null.
     * @param nsmap       the namespace map, or null if no qualified names are used by expression.
     */
    protected CompiledXpath(String expression, Map nsmap) {
        if (expression == null) throw new NullPointerException();
        this.expression = expression;
        this.nsmap = nsmap;
    }

    /** @return the generic xpath expression string.  Never null. */
    protected String getExpression() {
        return expression;
    }

    /** @return the namespace map, or null if no qualified names are used in the expression. */
    protected Map getNamespaceMap() {
        return nsmap;
    }

    /** A utility expression that is always true. */
    private static final class Tautology extends CompiledXpath {
        protected Tautology() {
            super("0=0", null);
        }
    }

    /** A utility expression that is always false. */
    private static final class Contradiction extends CompiledXpath {
        protected Contradiction() {
            super("0=1", null);
        }
    }
}
