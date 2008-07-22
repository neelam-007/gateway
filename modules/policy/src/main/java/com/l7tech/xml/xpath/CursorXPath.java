package com.l7tech.xml.xpath;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathVariableResolver;
import javax.xml.xpath.XPathFunctionResolver;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

import org.xml.sax.InputSource;

/**
 * CursorXPath implementation of XPath.
 *
 * <p>Thin wrapper around CompiledXpath.</p>
 *
 * @author $Author$
 * @version $Revision$
 * @see XpathExpression XpathExpression
 * @see com.l7tech.xml.xpath.CompiledXpath CompiledXpath
 */
class CursorXPath implements XPath {

    //- PUBLIC

    public XPathExpression compile(String expression) throws XPathExpressionException {
        if(expression==null) throw new NullPointerException("expression must not be null");
        return new CursorXPathExpression(expression, namespaceContext, variableResolver, functionResolver);
    }

    public Object evaluate(String expression, Object item, QName returnType) throws XPathExpressionException {
        if(expression==null) throw new NullPointerException("expression must not be null");
        if(returnType==null) throw new NullPointerException("returnType must not be null");
        return compile(expression).evaluate(item, returnType);
    }

    public String evaluate(String expression, Object item) throws XPathExpressionException {
        if(expression==null) throw new NullPointerException("expression must not be null");
        return compile(expression).evaluate(item);
    }

    public Object evaluate(String expression, InputSource source, QName returnType) throws XPathExpressionException {
        if(expression==null) throw new NullPointerException("expression must not be null");
        if(returnType==null) throw new NullPointerException("returnType must not be null");
        return compile(expression).evaluate(source, returnType);
    }

    public String evaluate(String expression, InputSource source) throws XPathExpressionException {
        if(expression==null) throw new NullPointerException("expression must not be null");
        return compile(expression).evaluate(source);
    }

    public void reset() {
        this.variableResolver = this.origVariableResolver;
        this.functionResolver = this.origFunctionResolver;
    }

    public NamespaceContext getNamespaceContext() {
        return namespaceContext;
    }

    public void setNamespaceContext(NamespaceContext nsContext) {
        if(nsContext==null) throw new NullPointerException("nsContext must not be null");
        namespaceContext = nsContext;
    }

    public XPathVariableResolver getXPathVariableResolver() {
        return variableResolver;
    }

    /**
     * Not currently used
     */
    public void setXPathVariableResolver(XPathVariableResolver resolver) {
        if(resolver==null) throw new NullPointerException("resolver must not be null");
        variableResolver = resolver;
    }

    public XPathFunctionResolver getXPathFunctionResolver() {
        return functionResolver;
    }

    /**
     * Not currently used
     */
    public void setXPathFunctionResolver(XPathFunctionResolver resolver) {
        if(resolver==null) throw new NullPointerException("resolver must not be null");
        functionResolver = resolver;
    }

    //- PACKAGE

    CursorXPath(XPathVariableResolver variableResolver, XPathFunctionResolver functionResolver) {
        this.origVariableResolver = this.variableResolver = variableResolver;
        this.origFunctionResolver = this.functionResolver = functionResolver;
    }

    //- PRIVATE

    private NamespaceContext namespaceContext;
    private XPathVariableResolver variableResolver;
    private XPathFunctionResolver functionResolver;

    private final XPathVariableResolver origVariableResolver;
    private final XPathFunctionResolver origFunctionResolver;
}
