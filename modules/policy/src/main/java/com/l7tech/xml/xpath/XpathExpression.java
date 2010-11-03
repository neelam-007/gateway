package com.l7tech.xml.xpath;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.NamespaceMigratable;

import java.io.Serializable;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class <code>XpathExpression</code> contains the XPath expression
 * and the namespace array.
 */
public class XpathExpression extends CompilableXpath implements Serializable, NamespaceMigratable {

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

    @Override
    public void migrateNamespaces(Map<String, String> nsUriSourceToDest) {
        Map<String, String> origNsMap = getNamespaces();
        Map<String, String> newNsMap = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : origNsMap.entrySet()) {
            String origUri = entry.getValue();
            String newUri = nsUriSourceToDest.get(origUri);
            newNsMap.put(entry.getKey(), newUri != null ? newUri : origUri);
        }
        setNamespaces(newNsMap);
    }


    @Override
    public Set<String> getNamespaceUrisUsed() {
        return getNamespaceUrisUsed(true);
    }

    /**
     * Finds all namespace URIs from the namespace map that are visibly used within the actual expression.
     *
     * @param lookForQnameLiterals if true, we will take note of anything that looks like it might be a qname literal
     *                             that appears within a string literal, and consider the corresponding namespace URI
     *                             as visibly used.
     * @return the set of namespace URIs from this expressions namespace map that are visibly used by the expression.
     *         Never null, but may be empty.
     *         The returned set belongs to the caller and may be freely modified.
     */
    public Set<String> getNamespaceUrisUsed(final boolean lookForQnameLiterals) {
        String expr = getExpression();
        if (expr == null)
            return Collections.emptySet();

        try {
            Set<String> usedPrefixes = XpathUtil.getNamespacePrefixesUsedByXpath(expr, lookForQnameLiterals);
            Map<String, String> nsmap = new HashMap<String, String>(namespaces);
            nsmap.keySet().retainAll(usedPrefixes);
            return new HashSet<String>(nsmap.values());
        } catch (ParseException e) {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Unable to parse XPath expression to determine namespaces used: " + ExceptionUtils.getMessage(e), e);
            return Collections.emptySet();
        }
    }

    //- PRIVATE

    private String expression;
    private Map<String, String> namespaces = new LinkedHashMap<String, String>();
    private static final Logger logger = Logger.getLogger(XpathExpression.class.getName());
}
