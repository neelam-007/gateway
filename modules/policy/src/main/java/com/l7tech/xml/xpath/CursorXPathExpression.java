package com.l7tech.xml.xpath;

import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.XpathExpression;
import org.xml.sax.InputSource;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * CursorXPathExpression implementation of XPathExpression.
 *
 * <p>Thin wrapper around CompiledXpath.</p>
 *
 * @author $Author$
 * @version $Revision$
 * @see XpathExpression XpathExpression
 * @see com.l7tech.xml.xpath.CompiledXpath CompiledXpath
 */
class CursorXPathExpression implements XPathExpression {

    //- PUBLIC

    public Object evaluate(Object item, QName returnType) throws XPathExpressionException {
        if(returnType==null) throw new NullPointerException("returnType must not be null");
        throwIfInvalidReturnType(returnType);

        //TODO Get a cursor from a Document facade?
        if(item instanceof ElementCursor) {
            ElementCursor ec = (ElementCursor) item;
            XpathResult result = ec.getXpathResult(compiledXpath);
            Object resultObj = null;

            if(returnType.equals(XPathConstants.STRING)) {
                resultObj = toString(result);
            }
            else if(returnType.equals(XPathConstants.BOOLEAN)) {
                resultObj = toBoolean(result);
            }
            else if(returnType.equals(XPathConstants.NUMBER)) {
                resultObj = toDouble(result);
            }
            else { // either NODE or NODESET
                throw new XPathExpressionException("Unsupported returnType '"+returnType+"'");
            }

            return resultObj;
        }
        else {
            throw new XPathExpressionException("item must be an ElementCursor!");
        }
    }

    public String evaluate(Object item) throws XPathExpressionException {
        return (String) evaluate(item, XPathConstants.STRING);
    }

    public Object evaluate(InputSource source, QName returnType) throws XPathExpressionException {
        if(source==null) throw new NullPointerException("source must not be null");
        if(returnType==null) throw new NullPointerException("returnType must not be null");
        throwIfInvalidReturnType(returnType);
        //TODO implement?
        throw new XPathExpressionException("Not implemented");
    }

    public String evaluate(InputSource source) throws XPathExpressionException {
        if(source==null) throw new NullPointerException("source must not be null");
        //TODO implement?
        throw new XPathExpressionException("Not implemented");
    }

    public String toString() {
        return "CursorXPathExpression()[expression='"+compiledXpath.getExpression()+"']";
    }

    //- PACKAGE

    CursorXPathExpression(String expression,
                          NamespaceContext namespaceContext,
                          XPathVariableResolver variableResolver,
                          XPathFunctionResolver functionResolver) throws XPathExpressionException {
        this.namespaceContext = namespaceContext;
        this.variableResolver = variableResolver;
        this.functionResolver = functionResolver;

        try {
            XpathExpression xpe = new XpathExpression(expression);
            xpe.setNamespaces(toMap(expression, namespaceContext));
            this.compiledXpath = xpe.compile();
        }
        catch(InvalidXpathException xpe) {
            throw (XPathExpressionException) new XPathExpressionException(xpe.getMessage()).initCause(xpe);
        }
    }

    //- PRIVATE

    private final CompiledXpath compiledXpath;
    private final NamespaceContext namespaceContext;
    private final XPathVariableResolver variableResolver;
    private final XPathFunctionResolver functionResolver;

    private void throwIfInvalidReturnType(QName returnType) {
        if(!returnType.equals(XPathConstants.BOOLEAN)
        && !returnType.equals(XPathConstants.NODE)
        && !returnType.equals(XPathConstants.NODESET)
        && !returnType.equals(XPathConstants.NUMBER)
        && !returnType.equals(XPathConstants.STRING)) {
            throw new IllegalArgumentException("Invalid returnType '"+returnType+"'");
        }
    }

    private String toString(XpathResult xpathResult) throws XPathExpressionException {
        String result = null;

        switch(xpathResult.getType()) {
            case XpathResult.TYPE_BOOLEAN:
                result = Boolean.toString(xpathResult.getBoolean());
                break;
            case XpathResult.TYPE_NUMBER:
                result = Double.toString(xpathResult.getNumber());
                break;
            case XpathResult.TYPE_STRING:
                result = xpathResult.getString();
                break;
            default:
                throw new XPathExpressionException("Cannot convert result type ("+xpathResult.getType()+") to string.");
        }

        return result;
    }

    private Boolean toBoolean(XpathResult xpathResult) throws XPathExpressionException {
        Boolean result = null;

        switch(xpathResult.getType()) {
            case XpathResult.TYPE_BOOLEAN:
                result = Boolean.valueOf(xpathResult.getBoolean());
                break;
            case XpathResult.TYPE_NUMBER:
                Double dbl = Double.valueOf(xpathResult.getNumber());
                if(!dbl.isNaN() && dbl.doubleValue()!=0) {
                    result = Boolean.TRUE;
                }
                else {
                    result = Boolean.FALSE;
                }
                break;
            case XpathResult.TYPE_STRING:
                String str = xpathResult.getString();
                if(str!=null && str.length()>0) {
                    result = Boolean.TRUE;
                }
                else {
                    result = Boolean.FALSE;
                }
                break;
            default:
                throw new XPathExpressionException("Cannot convert result type ("+xpathResult.getType()+") to string.");
        }

        return result;
    }

    private Double toDouble(XpathResult xpathResult) throws XPathExpressionException {
        Double result = null;

        switch(xpathResult.getType()) {
            case XpathResult.TYPE_BOOLEAN:
                result = Double.valueOf(xpathResult.getBoolean() ? 1 : 0);
                break;
            case XpathResult.TYPE_NUMBER:
                result = Double.valueOf(xpathResult.getNumber());
                break;
            case XpathResult.TYPE_STRING:
                String str = xpathResult.getString();
                if(str==null) {
                    result = Double.valueOf(Double.NaN);
                }
                else {
                    try {
                        result = Double.valueOf(str.trim());
                    }
                    catch(NumberFormatException nfe) {
                        result = Double.valueOf(Double.NaN);
                    }
                }
                break;
            default:
                throw new XPathExpressionException("Cannot convert result type ("+xpathResult.getType()+") to string.");
        }

        return result;
    }

    private Map toMap(String expression, NamespaceContext namespaceContext) {
        Map nsMap = Collections.EMPTY_MAP;

        if(namespaceContext != null) {
            StringTokenizer potentialPrefixTokenizer = new StringTokenizer(expression, "/:@( ");
            if(potentialPrefixTokenizer.countTokens()>0) {
                nsMap = new HashMap();
                while(potentialPrefixTokenizer.hasMoreTokens()) {
                    String possiblePrefix = potentialPrefixTokenizer.nextToken();
                    String namespace = namespaceContext.getNamespaceURI(possiblePrefix);
                    if(namespace!=null && namespace.length()>0) {
                        nsMap.put(possiblePrefix, namespace);
                    }
                }
            }
        }

        return nsMap;
    }
}
