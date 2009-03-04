package com.l7tech.external.assertions.ldapquery;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.ArrayTypeMapping;
import com.l7tech.policy.wsp.BeanTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.util.Functions;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

import java.io.Serializable;
import java.util.*;

/**
 * Defines context variables being set based on values retrieved from an ldap connection. The values
 * are read for specific attribute names for a specific ldap entry whose discovery is based on an
 * ldap search filter expression.
 *
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 6, 2007<br/>
 */
public class LDAPQueryAssertion extends Assertion implements UsesVariables, SetsVariables, Serializable {
    private String searchFilter;
    private QueryAttributeMapping[] queryMappings = new QueryAttributeMapping[0];
    private long ldapProviderOid;
    private long cachePeriod = 10;
    private boolean enableCache = true;
    private boolean failIfNoResults = false;

    public LDAPQueryAssertion() {
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(SHORT_NAME, "LDAP Query Assertion");
        meta.put(LONG_NAME, "Retrieve attributes from an LDAP directory");

        meta.put(PALETTE_NODE_NAME, "LDAP Query");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "accessControl" });

        meta.put(POLICY_NODE_NAME, "Query LDAP");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, LDAPQueryAssertion>() {
            public String call(LDAPQueryAssertion assertion) {
                return "Query LDAP " + assertion.getSearchFilter();
            }
        });

        meta.put(POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(SERVER_ASSERTION_CLASSNAME, "com.l7tech.external.assertions.ldapquery.server.ServerLDAPQueryAssertion");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.ldapquery.console.LDAPQueryPropertiesDialog");

        meta.put(WSP_EXTERNAL_NAME, "LdapQueryAssertion");

        Collection<TypeMapping> othermappings = new ArrayList<TypeMapping>();
        othermappings.add(new ArrayTypeMapping(new QueryAttributeMapping[0], "queryAttributeMappings"));
        othermappings.add(new BeanTypeMapping(QueryAttributeMapping.class, "mapping"));
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(othermappings));

        meta.put(AssertionMetadata.WSP_COMPATIBILITY_MAPPINGS, new HashMap<String, TypeMapping>() {{
            put("LDAPQuery", new LDAPQueryCompatibilityAssertionMapping(new LDAPQueryAssertion(), "LDAPQuery"));
        }});

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        return meta;
    }

    public String getSearchFilter() {
        return searchFilter;
    }

    public void setSearchFilter(String searchFilter) {
        this.searchFilter = searchFilter;
    }

    public QueryAttributeMapping[] getQueryMappings() {
        return queryMappings;
    }

    public void setQueryMappings(QueryAttributeMapping[] queryMappings) {
        if (queryMappings == null) queryMappings = new QueryAttributeMapping[0];
        this.queryMappings = queryMappings;
    }

    public long getLdapProviderOid() {
        return ldapProviderOid;
    }

    public void setLdapProviderOid(long ldapProviderOid) {
        this.ldapProviderOid = ldapProviderOid;
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        // parse out of searchFilter
        return Syntax.getReferencedNames(searchFilter);
    }

    public VariableMetadata[] getVariablesSet() {
        ArrayList<VariableMetadata> output =  new ArrayList<VariableMetadata>();
        for (QueryAttributeMapping am : queryMappings) {
            output.add(new VariableMetadata(am.getMatchingContextVariableName(), false, true, am.getMatchingContextVariableName(), true));
        }
        return output.toArray(new VariableMetadata[output.size()]);
    }

    public String[] getAttrNames() {
        ArrayList<String> ret = new ArrayList<String>();
        for (QueryAttributeMapping mapping : queryMappings)
            ret.add(mapping.getAttributeName());
        return ret.toArray(new String[ret.size()]);
    }

    public long getCachePeriod() {
        return cachePeriod;
    }

    public void setCachePeriod(long cachePeriod) {
        this.cachePeriod = cachePeriod;
    }

    public boolean isEnableCache() {
        return enableCache;
    }

    public void setEnableCache(boolean enableCache) {
        this.enableCache = enableCache;
    }

    public boolean isFailIfNoResults() {
        return failIfNoResults;
    }

    public void setFailIfNoResults(boolean failIfNoResults) {
        this.failIfNoResults = failIfNoResults;
    }
}
