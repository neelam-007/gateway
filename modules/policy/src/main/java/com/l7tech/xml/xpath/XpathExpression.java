package com.l7tech.xml.xpath;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class <code>XpathExpression</code> contains the XPath expression
 * and the namespace array.
 */
public class XpathExpression extends CompilableXpath implements Serializable {

    //- PUBLIC

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
     * Convenience constructor for an XPath expression that up to one unique namespace prefix.  
     *
     * @param expression  an expression that may use up to one unique namespace prefix, or null.
     * @param customPrefix   custom namespace prefix to declare, or null.
     * @param customNsUri    custom namespace URI to declare, or null.
     */
    public XpathExpression(String expression, String customPrefix, String customNsUri) {
        this(expression, makeMap(customPrefix, customNsUri));
    }

    /**
     * Construct the XPath expression with optional namespaces
     *
     * @param expression the XPath expression
     * @param namespaces the namespaces map, may be null, it will result
     *                   in internal namespace map have zero elements.
     */
    public XpathExpression(String expression, Map<String, String> namespaces) {
        this.expression = expression;
        if (namespaces != null) {
            this.namespaces.putAll(namespaces);
        }
    }

    /**
     * @return the XPath expression
     */
    @Override
    public String getExpression() {
        return expression;
    }

    /**
     * @return the namespace map, never <b>null</b>, note that the
     *         this is the reference, not safe copy
     */
    @Override
    public Map<String,String> getNamespaces() {
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
     * @param namespaces namespace map of prefix to namespace URI
     */
    public void setNamespaces(Map<String,String> namespaces) {
        if (namespaces != null) {
            this.namespaces.clear();
            this.namespaces.putAll(namespaces);
        }
    }

    /** @noinspection RedundantIfStatement*/
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof XpathExpression)) return false;

        final XpathExpression xpathExpression = (XpathExpression)o;

        if (expression != null ? !expression.equals(xpathExpression.expression) : xpathExpression.expression != null) return false;
        if (!namespaces.equals(xpathExpression.namespaces)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (expression != null ? expression.hashCode() : 0);
        result = 29 * result + namespaces.hashCode();
        return result;
    }

    /**
     * Utility method to create a namespace Map for a single namespace.
     *
     * @param customPrefix  prefix, or null to return a null map.
     * @param customNsUri   namespace URI, or null to return a null map.
     * @return a new Map, or null if one of the arguments was null.
     */
    public static Map<String, String> makeMap(String customPrefix, String customNsUri) {
        if (customNsUri == null || customPrefix == null)
            return null;
        Map<String, String> ret = new HashMap<String,String>();
        ret.put(customPrefix, customNsUri);
        return ret;
    }

    //- PRIVATE

    private String expression;
    private Map<String, String> namespaces = new LinkedHashMap<String, String>();
}
