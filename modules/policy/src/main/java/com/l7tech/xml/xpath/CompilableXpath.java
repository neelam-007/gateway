/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml.xpath;

import com.l7tech.xml.InvalidXpathException;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Superclass for classes that are capable of producing CompiledXpath instances.
 */
public abstract class CompilableXpath {
    /**
     * Get the generic version of this xpath expression, not optimized for any particular implementation.
     *
     * @return the xpath expression, which may be simple or may use any XPath 1.0 features.  Never null.
     */
    public abstract String getExpression();

    /**
     * Get the namespace map used by this xpath expression.
     *
     * @return the namespace map, or null if the expression does not contain any qualified names.
     */
    public abstract Map<String,String> getNamespaces();

    /**
     * Get the Xpath language version to use (eg, "1.0", "2.0" or "3.0").
     *
     * @return the language version.  Never null.
     */
    @NotNull
    public XpathVersion getXpathVersion() {
        return XpathVersion.XPATH_1_0;
    }

    /**
     * Get the version of this expression that is specially for use with Jaxen.  By default this is the same
     * as {@link #getExpression()}.  Subclasses can override this to provide a version of the expression that
     * is tweaked to work well with {@link org.jaxen.dom.DOMXPath}, and which may differ from the result of calling
     * {@link #getExpression}.
     *
     * @return the expression to use with Jaxen.  Never null.
     */
    public String getExpressionForJaxen() {
        return getExpression();
    }


    /**
     * Return a CompiledXpath that will work with Jaxen.
     *
     * @return a compiled xpath.  Never null.
     * @throws InvalidXpathException if this CompilableXpath turned out not to be quite so compilable after all
     */
    public CompiledXpath compile() throws InvalidXpathException {
        return new DomCompiledXpath(this);
    }
}
