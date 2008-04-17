package com.l7tech.external.assertions.ldapquery;

import com.l7tech.policy.assertion.*;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.ArrayTypeMapping;
import com.l7tech.common.util.Functions;

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
    private QueryAttributeMapping[] queryMappings;
    private long ldapProviderOid;

    public LDAPQueryAssertion() {
    }

    public AssertionMetadata meta() {
        clearCachedMetadata(getClass().getName());
        
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

        meta.put(WSP_EXTERNAL_NAME, "LDAPQuery");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Comparison" rather than "set:modularAssertions"
        // meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        Collection<TypeMapping> othermappings = new ArrayList<TypeMapping>();
        //othermappings.add(new ArrayTypeMapping(new QueryAttributeMapping[0], "qwe79qw87"));
        //othermappings.add(new BeanTypeMapping(QueryAttributeMapping.class, "df654dsf65"));
        othermappings.add(new ArrayTypeMapping(new Boolean[0], "bools"));
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(othermappings));

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        return meta;
    }

    public String getSearchFilter() {
        return searchFilter;
    }

    public void setSearchFilter(String searchFilter) {
        this.searchFilter = searchFilter;
    }

    public QueryAttributeMapping[] currentQueryMappings() {
        if (queryMappings == null) return new QueryAttributeMapping[0];
        return queryMappings;
    }

    public void reassignQueryMappings(QueryAttributeMapping[] queryMappings) {
        this.queryMappings = queryMappings;
    }

    public long getLdapProviderOid() {
        return ldapProviderOid;
    }

    public void setLdapProviderOid(long ldapProviderOid) {
        this.ldapProviderOid = ldapProviderOid;
    }

    public String[] getVariablesUsed() {
        // parse out of searchFilter
        return Syntax.getReferencedNames(searchFilter);
    }

    public VariableMetadata[] getVariablesSet() {
        ArrayList<VariableMetadata> output =  new ArrayList<VariableMetadata>();
        for (QueryAttributeMapping am : currentQueryMappings()) {
            output.add(new VariableMetadata(am.getMatchingContextVariableName(), false, true, am.getMatchingContextVariableName(), true));
        }
        return output.toArray(new VariableMetadata[output.size()]);
    }

    // wsp annoyance work around below
    private void resetMappingsSize(int newSize) {
        queryMappings = new QueryAttributeMapping[newSize];
        for (int i = 0; i < queryMappings.length; i++) queryMappings[i] = new QueryAttributeMapping("foo", "bar");
    }

    public String[] getAttrNames() {
        QueryAttributeMapping[] tmp = currentQueryMappings();
        String[] output = new String[tmp.length];
        for (int i = 0; i < tmp.length; i++) {
            output[i] = tmp[i].getAttributeName();
        }
        return output;
    }

    public void setAttrNames(String[] attrNames) {
        if (attrNames == null) {
            queryMappings = null;
        } else {
            QueryAttributeMapping[] current = currentQueryMappings();
            if (current.length != attrNames.length) {
                resetMappingsSize(attrNames.length);
            }
            for (int i = 0; i < queryMappings.length; i++) {
                queryMappings[i].setAttributeName(attrNames[i]);
            }
        }
    }

    public String[] getVarNames() {
        QueryAttributeMapping[] tmp = currentQueryMappings();
        String[] output = new String[tmp.length];
        for (int i = 0; i < tmp.length; i++) {
            output[i] = tmp[i].getMatchingContextVariableName();
        }
        return output;
    }

    public void setVarNames(String[] varNames) {
        if (varNames == null) {
            queryMappings = null;
        } else {
            QueryAttributeMapping[] current = currentQueryMappings();
            if (current.length != varNames.length) {
                resetMappingsSize(varNames.length);
            }
            for (int i = 0; i < queryMappings.length; i++) {
                queryMappings[i].setMatchingContextVariableName(varNames[i]);
            }
        }
    }

    public Boolean[] getMultivalued() {
        QueryAttributeMapping[] tmp = currentQueryMappings();
        Boolean[] output = new Boolean[tmp.length];
        for (int i = 0; i < tmp.length; i++) {
            output[i] = tmp[i].isMultivalued();
        }
        return output;
    }

    public void setMultivalued(Boolean[] multivalued) {
        if (multivalued == null) {
            queryMappings = null;
        } else {
            QueryAttributeMapping[] current = currentQueryMappings();
            if (current.length != multivalued.length) {
                resetMappingsSize(multivalued.length);
            }
            for (int i = 0; i < queryMappings.length; i++) {
                queryMappings[i].setMultivalued(multivalued[i]);
            }
        }
    }
    // end of wsp annoyance work around
}
