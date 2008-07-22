/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml.xpath;

import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.tarari.GlobalTarariContext;

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
    public abstract Map getNamespaces();

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
     * Get the version of this expression that is specially for use with Tarari XPath 1.0.  By default this
     * is the same as {@link #getExpression()}.  Subclasses can override this to provide a version of the
     * expression that is tweaked to work well with ... and
     * which may differ from the result of calling {@link #getExpression()}.
     *
     * @return the expression to use with Tarari XPath 1.0.  Never null.
     */
    public String getExpressionForTarari() {
        return getExpression();
    }

    /**
     * Get the version of this expression that is specially for use with Tarari fastxpath.  By default this
     * is the same as {@link #getExpression()}.  Subclasses can override this to provide a version of the
     * expression that is tweaked to work well with {@link #toTarariNormalForm()} and which may differ from the result
     * of calling {@link #getExpression()}.
     *
     * @return the expression to use with Tarari fastxpath.  Need not already be in Tarari normal form.
     *         Never null.
     */
    public String getExpressionForTarariFastxpath() {
        return getExpression();
    }

    /**
     * Convert the expression to Tarari Normal Form.  By default this method attempts to convert the result of
     * {@link #getExpressionForTarariFastxpath()} into Tarari Normal Form, and returns null if the converter
     * is unavailable or if the conversion fails.
     * <p/>
     * Subclasses can provide their own implementation that produces a Tarari Normal Form version of this
     * expression in some other way.
     * <p/>
     * In Tarari normal form, qualified names are replaced
     * by predicates, and there are no nested predicates or other features beyond a very basic set (the
     * following is from the Tarari XPathCompiler Javadoc):
     * <p/>
     * The XPath engine handles a proper subset of XPath expressions.
     * <p>These axes are supported:
     * <ul>
     * <li>child
     * <li>descendent-or-self from the root only (such as //bar, but not /foo//bar).
     * </ul>
     * <p>These functions, without arguments, are supported:
     * <ul>
     * <li>local-name()
     * <li>namespace-uri()
     * <li>text()
     * <li>position()
     * <li>first().
     * </ul>
     * <p>These axes and logical operators are supported in the predicate:
     * attribute, such as <pre>/*[attribute::*], [@pfx:name], [@name="value"]</pre>
     * <ul>
     * <li>not
     * <li>and
     * <li>or
     * <li>=
     * <li>!= for position only, such as [position() != 1] along with the abbreviated syntax, with the exception of the abbreviation for the parent axis(..).
     * </ul>
     * <p>See RAX Programming Interface Guide for Java for set of examples.
     *
     * @return the current expression expressed in Tarari Normal Form, or null if the converter is not available or
     *         if this expression can't be expressed in Tarari Normal Form.
     */
    public FastXpath toTarariNormalForm() {
        GlobalTarariContext gtc = TarariLoader.getGlobalContext();
        if (gtc == null) return null;
        return gtc.toTarariNormalForm(getExpressionForTarariFastxpath(), getNamespaces());
    }

    /**
     * Return a CompiledXpath that will work with Jaxen, and will also work with Tarari if the hardware is available
     * in the current process.  The compiled XPath will be registered globally to run in parallel if the
     * current XPath expression can be expressed in Tarari normal form.
     *
     * @return a compiled xpath.  Never null.
     * @throws InvalidXpathException if this CompilableXpath turned out not to be quite so compilable after all
     */
    public CompiledXpath compile() throws InvalidXpathException {
        CompiledXpath result = null;
        GlobalTarariContext gtc = TarariLoader.getGlobalContext();
        if (gtc != null)
            result = gtc.compileXpath(this);
        if (result == null)
            result = new DomCompiledXpath(this);
        return result;
    }
}
