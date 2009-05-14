package com.l7tech.xml.xpath;

import org.jaxen.VariableContext;
import org.jaxen.UnresolvableException;

/**
 * A Jaxen VariableContext backed by a {@link com.l7tech.xml.xpath.XpathVariableFinder} instance;
 * either provided in advance, or looked up at query time using the thread-local {@link com.l7tech.xml.xpath.XpathVariableContext}.
 */
public class XpathVariableFinderVariableContext implements VariableContext {
    private final XpathVariableFinder variableFinder;

    /**
     * Create a context that will pull variable values from the specified XpathVariableFinder,
     * or from the then-current thread-local XpathVariableContext if finder is null.
     *
     * @param finder a specific finder to use, or null to use whatever the current thread-local finder is
     *               when a variable value is queried for.
     */
    public XpathVariableFinderVariableContext(XpathVariableFinder finder) {
        this.variableFinder = finder;
    }

    public Object getVariableValue(String ns, String prefix, String localName) throws UnresolvableException {
        if (ns != null && ns.length() > 0)
            throw new UnresolvableException("Unsupported XPath variable namespace URI: " + ns);
        if (prefix != null && prefix.length() > 0)
            throw new UnresolvableException("Unsupported XPath variable prefix: " + ns);
        XpathVariableFinder finder = variableFinder == null ? XpathVariableContext.getCurrentVariableFinder() : variableFinder;
        if (finder == null)
            throw new UnresolvableException("No XPath variable context is available to resolve XPath variable: " + localName);
        try {
            return finder.getVariableValue(ns, localName);
        } catch (NoSuchXpathVariableException e) {
            throw (UnresolvableException)new UnresolvableException("No such XPath variable: " + localName).initCause(e);
        }
    }
}
