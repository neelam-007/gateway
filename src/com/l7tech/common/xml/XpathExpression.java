package com.l7tech.common.xml;

import com.l7tech.common.util.SoapUtil;

import javax.xml.rpc.NamespaceConstants;
import javax.xml.soap.SOAPConstants;
import java.io.Serializable;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Class <code>XpathExpression</code> contains the XPath expression
 * and the namespace array.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class XpathExpression implements Serializable {
    private String expression;
    private Map namespaces = new LinkedHashMap();

    /**
     * default constructor for XML serialization support
     */
    public XpathExpression() {}

    /**
     * @return a standard xpath value that points to the soap body (standard for many xpath based assertions)
     */
    public static XpathExpression soapBodyXpathValue() {
        XpathExpression xpath = new XpathExpression();
        xpath.setExpression(SoapUtil.SOAP_BODY_XPATH);
        Map nss = new LinkedHashMap();
        nss.put(NamespaceConstants.NSPREFIX_SOAP_ENVELOPE, SOAPConstants.URI_NS_SOAP_ENVELOPE);
        xpath.setNamespaces(nss);
        return xpath;
    }

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

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof XpathExpression)) return false;

        final XpathExpression xpathExpression = (XpathExpression)o;

        if (expression != null ? !expression.equals(xpathExpression.expression) : xpathExpression.expression != null) return false;
        if (!namespaces.equals(xpathExpression.namespaces)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (expression != null ? expression.hashCode() : 0);
        result = 29 * result + namespaces.hashCode();
        return result;
    }
}
