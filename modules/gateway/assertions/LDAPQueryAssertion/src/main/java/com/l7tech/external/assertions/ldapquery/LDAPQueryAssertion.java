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
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

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
public class LDAPQueryAssertion extends Assertion implements UsesEntities, UsesVariables, SetsVariables, Serializable {
    private String searchFilter;
    private QueryAttributeMapping[] queryMappings = new QueryAttributeMapping[0];
    private long ldapProviderOid;
    private long cachePeriod = 10;
    private boolean enableCache = true;
    private boolean failIfNoResults = false;

    public LDAPQueryAssertion() {
    }

    private final static String baseName = "Query LDAP";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<LDAPQueryAssertion>(){
        @Override
        public String getAssertionName( final LDAPQueryAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            return baseName + " " + assertion.getSearchFilter();

        }
    };

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Retrieve attributes from an LDAP directory server and store them in context variables.");
        meta.put(PROPERTIES_ACTION_NAME, "LDAP Query Properties");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "accessControl" });

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(SERVER_ASSERTION_CLASSNAME, "com.l7tech.external.assertions.ldapquery.server.ServerLDAPQueryAssertion");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.ldapquery.console.LDAPQueryPropertiesDialog");

        meta.put(WSP_EXTERNAL_NAME, "LDAPQuery");

        Collection<TypeMapping> othermappings = new ArrayList<TypeMapping>();
        othermappings.add(new ArrayTypeMapping(new QueryAttributeMapping[0], "queryAttributeMappings"));
        othermappings.add(new BeanTypeMapping(QueryAttributeMapping.class, "mapping"));
        othermappings.add(new ArrayTypeMapping(new Boolean[0], "bools")); /* only here for compat with pre-5.0 versions of the policy XML */
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(othermappings));

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

    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.ASSERTION)
    public EntityHeader[] getEntitiesUsed() {
        return new EntityHeader[] { new EntityHeader(Long.toString(ldapProviderOid), EntityType.ID_PROVIDER_CONFIG, null, null) };
    }

    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if( oldEntityHeader.getType().equals(EntityType.ID_PROVIDER_CONFIG) &&
            oldEntityHeader.getOid() == ldapProviderOid &&
            newEntityHeader.getType().equals(EntityType.ID_PROVIDER_CONFIG)) {
            ldapProviderOid = newEntityHeader.getOid();
        }
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

    /** @deprecated only for parsing pre-5.0 versions of the policy XML */
    public void setAttrNames(String[] attrNames) {
        queryMappings = LDAPQueryCompatibilityAssertionMapping.populateAttrNames(queryMappings, attrNames);
    }

    /** @deprecated only for parsing pre-5.0 versions of the policy XML */
    public void setMultivalued(Boolean[] multi) {
        queryMappings = LDAPQueryCompatibilityAssertionMapping.populateMultivalued(queryMappings, multi);
    }

    /** @deprecated only for parsing pre-5.0 versions of the policy XML */
    public void setVarNames(String[] varNames) {
        queryMappings = LDAPQueryCompatibilityAssertionMapping.populateVarNames(queryMappings, varNames);
    }
}
