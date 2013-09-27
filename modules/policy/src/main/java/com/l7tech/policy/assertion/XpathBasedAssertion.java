package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.VariableUseSupport.VariablesUsedSupport;
import com.l7tech.policy.assertion.annotation.RequiresXML;
import com.l7tech.xml.NamespaceMigratable;
import com.l7tech.xml.soap.SoapVersion;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathUtil;
import org.jetbrains.annotations.Nullable;

import javax.xml.soap.SOAPConstants;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * Base class for XML security assertions whose primary configurable feature is an Xpath expression.
 */
@RequiresXML()
public abstract class XpathBasedAssertion extends Assertion implements UsesVariables, NamespaceMigratable {
    protected XpathExpression xpathExpression;

    protected XpathBasedAssertion() {
    }

    public XpathExpression getXpathExpression() {
        return xpathExpression;
    }

    public void setXpathExpression(@Nullable XpathExpression xpathExpression) {
        this.xpathExpression = xpathExpression;
    }
    
    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public final String[] getVariablesUsed() {
        return doGetVariablesUsed().asArray();
    }

    @Override
    public String toString() {
        XpathExpression x = getXpathExpression();
        StringBuffer sb = new StringBuffer(super.toString());
        if (x != null && x.getExpression() != null)
            sb.append(" pattern=" + x.getExpression());
        if (x != null && x.getNamespaces() != null)
            sb.append(" namespacesmap=" + x.getNamespaces());
        return sb.toString();
    }

    /** Shortcut to get xpath pattern.  Name doesn't use get, to hide it from policy serializer. */
    public String pattern() {
        if (getXpathExpression() != null)
            return getXpathExpression().getExpression();
        return null;
    }

    /** Shortcut to get namespace map.  Name doesn't use get, to hide it from policy serializer. */
    public Map<String, String> namespaceMap() {
        if (getXpathExpression() != null)
            return getXpathExpression().getNamespaces();
        return null;
    }

    /**
     * Create a default XpathExpression value for this assertion for either a non-SOAP or SOAP policy,
     * using the specified SOAP version (if for a SOAP policy).
     * <p/>
     * This method just uses "/" for non-SOAP policies, or "/s:Envelope/s:Body" for SOAP policies (with the "s:"
     * prefix defined as the SOAP 1.2 namespace URI if the SoapVersion is 1.2, and the SOAP 1.1 namespace otherwise).
     *
     * @param soapPolicy true if it is known that the value will be used only by a SOAP policy; otherwise false.
     * @param soapVersion the SOAP version to use if a SOAP-based expression is created.  May be UNSPECIFIED or null.
     * @return a new XpathExpression instance suitable for the specified situation.  Never null.
     */
    public XpathExpression createDefaultXpathExpression(boolean soapPolicy, SoapVersion soapVersion) {
        String nsuri = SoapVersion.SOAP_1_2.equals(soapVersion) ? SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE : SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE;
        return new XpathExpression("/s:Envelope/s:Body", "s", nsuri);
    }

    /**
     * @return the previous default xpath value for many XpathBasedAssertions, for deserialization purposes.
     *         This value should not be used for any other purpose; users of xpath based assertions should
     *         override it with a contextually correct value instead (at the very least ensuring the correct SOAP version is used).
     */
    public static XpathExpression compatOrigDefaultXpathValue() {
        return new XpathExpression("/soapenv:Envelope/soapenv:Body", "soapenv", SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE);
    }

    @Override
    public void migrateNamespaces(Map<String, String> nsUriSourceToDest) {
        XpathExpression xpath = getXpathExpression();
        if (xpath != null)
            xpath.migrateNamespaces(nsUriSourceToDest);
    }

    @Override
    public Set<String> findNamespaceUrisUsed() {
        XpathExpression xpath = getXpathExpression();
        return xpath == null ? Collections.<String>emptySet() : xpath.findNamespaceUrisUsed();
    }

    public boolean permitsFullyDynamicExpression() {
        return false;
    }

    public static boolean isFullyDynamicXpath(String expression) {
        return getFullyDynamicXpathVariableName(expression) != null;
    }

    private static final Pattern SINGLE_VAR_PATTERN = Pattern.compile("^\\$\\{([a-zA-Z_][a-zA-Z0-9_\\-\\.]*)\\}$");

    public static String getFullyDynamicXpathVariableName(String expression) {
        if (expression == null)
            return null;
        Matcher matcher = SINGLE_VAR_PATTERN.matcher(expression);
        if (!matcher.matches())
            return null;
        return matcher.group(1);
    }

    protected VariablesUsed doGetVariablesUsed() {
        VariablesUsed used = new VariablesUsed();
        if (xpathExpression != null) {
            final String expr = xpathExpression.getExpression();
            if ( expr != null ) {
                used.addVariables( XpathUtil.getUnprefixedVariablesUsedInXpath(expr, xpathExpression.getXpathVersion()) );
            }
        }
        return used;
    }

    protected final class VariablesUsed extends VariablesUsedSupport<VariablesUsed> {
        private VariablesUsed() {
        }

        @Override
        protected VariablesUsed get() {
            return this;
        }
    }
}
