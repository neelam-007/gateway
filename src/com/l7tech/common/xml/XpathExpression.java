package com.l7tech.common.xml;

import org.apache.xml.utils.NameSpace;

import java.io.Serializable;

/**
 * Class <code>XpathExpression</code> contains the XPath expression
 * and the namespace array.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class XpathExpression implements Serializable {
    private String expression;
    private NameSpace[] namespaces = new NameSpace[]{};

    /**
     * default constructor for XML serialization support
     */
    public XpathExpression() {}

    /**
     * Construct the XPath expression with no namespaces
     *
     * @param expression the XPath expression
     */
    public XpathExpression(String expression) {
        this(expression, null);
    }

    /**
     * Construct the XPath expression with optional namespaces
     *
     * @param expression the XPath expression
     * @param namespaces the namespaces array, may be null, it will result
     *                   in internal namespace array have zero elements.
     */
    public XpathExpression(String expression, NameSpace[] namespaces) {
        this.expression = expression;
        if (namespaces != null) {
            this.namespaces = namespaces;
        }
    }

    /**
     * @return the XPath expression
     */
    public String getExpression() {
        return expression;
    }

    /**
     * @return the namespace array, never <b>null</b>
     */
    public NameSpace[] getNamespaces() {
        return namespaces;
    }

    /**
     * Set the XPath expression
     *
     * @param expression the XPath expression
     */
    public void setExpression(String expression) {
        this.expression = expression;
    }

    /**
     * set the namespaces array
     *
     * @param namespaces
     */
    public void setNamespaces(NameSpace[] namespaces) {
        if (namespaces != null) {
            this.namespaces = namespaces;
        }
    }
}
