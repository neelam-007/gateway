package com.l7tech.common.xml;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Class <code>XpathExpression</code> contains the XPath expression
 * and the namespace array.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class XpathExpression implements Serializable {
    private String expression;
    private Map namespaces = new HashMap();

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
     * @param namespaces the namespaces map, may be null, it will result
     *                   in internal namespace map have zero elements.
     */
    public XpathExpression(String expression, Map namespaces) {
        this.expression = expression;
        if (namespaces != null) {
            this.namespaces.putAll(namespaces);
        }
    }

    /**
     * @return the XPath expression
     */
    public String getExpression() {
        return expression;
    }

    /**
     * @return the namespace map, never <b>null</b>, note that the
     *         this is the reference, not safe copy
     */
    public Map getNamespaces() {
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
    public void setNamespaces(Map namespaces) {
        if (namespaces != null) {
            this.namespaces.clear();
            this.namespaces.putAll(namespaces);
        }
    }
}
