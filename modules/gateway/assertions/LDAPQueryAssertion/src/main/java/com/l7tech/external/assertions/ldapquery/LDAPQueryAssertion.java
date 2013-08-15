package com.l7tech.external.assertions.ldapquery;

import com.l7tech.objectmodel.Goid;
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
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Functions;
import com.l7tech.util.GoidUpgradeMapper;

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
    private static final String META_INITIALIZED = LDAPQueryAssertion.class.getName() + ".metadataInitialized";
    private final static String baseName = "Query LDAP";
    private String searchFilter;
    private boolean searchFilterInjectionProtected = false;
    private QueryAttributeMapping[] queryMappings = new QueryAttributeMapping[0];
    private Goid ldapProviderOid;
    private int cacheSize = 0; // 0 for unlimited
    private long cachePeriod = 10; // minutes
    private boolean enableCache = true;
    private boolean failIfNoResults = false;
    private boolean failIfTooManyResults = false;
    private boolean allowMultipleResults = false;
    private int maximumResults = 0;

    public LDAPQueryAssertion() {
    }

    /**
     * Create a new LDAPQueryAssertion with default properties.
     */
    public static LDAPQueryAssertion newInstance() {
        LDAPQueryAssertion assertion = new LDAPQueryAssertion();
        assertion.setSearchFilterInjectionProtected( true );
        assertion.setCacheSize( 100 );
        return assertion;
    }

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<LDAPQueryAssertion>(){
        @Override
        public String getAssertionName( final LDAPQueryAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            return baseName + " " + assertion.getSearchFilter();

        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

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
        meta.put(ASSERTION_FACTORY, new Functions.Unary<LDAPQueryAssertion, LDAPQueryAssertion>(){
            @Override
            public LDAPQueryAssertion call( final LDAPQueryAssertion responseWssTimestamp ) {
                return newInstance();
            }
        } );

        Collection<TypeMapping> othermappings = new ArrayList<TypeMapping>();
        othermappings.add(new ArrayTypeMapping(new QueryAttributeMapping[0], "queryAttributeMappings"));
        othermappings.add(new BeanTypeMapping(QueryAttributeMapping.class, "mapping"));
        othermappings.add(new ArrayTypeMapping(new Boolean[0], "bools")); /* only here for compat with pre-5.0 versions of the policy XML */
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(othermappings));

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public String getSearchFilter() {
        return searchFilter;
    }

    public void setSearchFilter(String searchFilter) {
        this.searchFilter = searchFilter;
    }

    public boolean isSearchFilterInjectionProtected() {
        return searchFilterInjectionProtected;
    }

    public void setSearchFilterInjectionProtected( final boolean searchFilterInjectionProtected ) {
        this.searchFilterInjectionProtected = searchFilterInjectionProtected;
    }

    public QueryAttributeMapping[] getQueryMappings() {
        return queryMappings;
    }

    public void setQueryMappings(QueryAttributeMapping[] queryMappings) {
        if (queryMappings == null) queryMappings = new QueryAttributeMapping[0];
        this.queryMappings = queryMappings;
    }

    public Goid getLdapProviderOid() {
        return ldapProviderOid;
    }

    public void setLdapProviderOid(Goid ldapProviderOid) {
        this.ldapProviderOid = ldapProviderOid;
    }

    // For backward compat while parsing pre-GOID policies.  Not needed for new assertions.
    @Deprecated
    public void setLdapProviderOid( long ldapProviderOid ) {
        this.ldapProviderOid =
                GoidUpgradeMapper.mapOid(EntityType.ID_PROVIDER_CONFIG, ldapProviderOid);
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.ASSERTION)
    public EntityHeader[] getEntitiesUsed() {
        return new EntityHeader[] { new EntityHeader(ldapProviderOid, EntityType.ID_PROVIDER_CONFIG, null, null) };
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if( oldEntityHeader.getType().equals(EntityType.ID_PROVIDER_CONFIG) &&
            oldEntityHeader.getGoid().equals(ldapProviderOid) &&
            newEntityHeader.getType().equals(EntityType.ID_PROVIDER_CONFIG)) {
            ldapProviderOid = newEntityHeader.getGoid();
        }
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        // parse out of searchFilter
        return Syntax.getReferencedNames(searchFilter);
    }

    @Override
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

    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize( final int cacheSize ) {
        this.cacheSize = cacheSize;
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

    public boolean isFailIfTooManyResults() {
        return failIfTooManyResults;
    }

    public void setFailIfTooManyResults( final boolean failIfTooManyResults ) {
        this.failIfTooManyResults = failIfTooManyResults;
    }

    public boolean isAllowMultipleResults() {
        return allowMultipleResults;
    }

    public void setAllowMultipleResults( final boolean allowMultipleResults ) {
        this.allowMultipleResults = allowMultipleResults;
    }

    public int getMaximumResults() {
        return maximumResults;
    }

    public void setMaximumResults( final int maximumResults ) {
        this.maximumResults = maximumResults;
    }

    /** @deprecated only for parsing pre-5.0 versions of the policy XML */
    @Deprecated
    public void setAttrNames(String[] attrNames) {
        queryMappings = LDAPQueryCompatibilityAssertionMapping.populateAttrNames(queryMappings, attrNames);
    }

    /** @deprecated only for parsing pre-5.0 versions of the policy XML */
    @Deprecated
    public void setMultivalued(Boolean[] multi) {
        queryMappings = LDAPQueryCompatibilityAssertionMapping.populateMultivalued(queryMappings, multi);
    }

    /** @deprecated only for parsing pre-5.0 versions of the policy XML */
    @Deprecated
    public void setVarNames(String[] varNames) {
        queryMappings = LDAPQueryCompatibilityAssertionMapping.populateVarNames(queryMappings, varNames);
    }

    @Override
    public LDAPQueryAssertion clone() {
        LDAPQueryAssertion cloned = (LDAPQueryAssertion) super.clone();
        cloned.queryMappings = cloned.queryMappings.clone();
        for ( int i=0; i<cloned.queryMappings.length; i++  ) {
            cloned.queryMappings[i] = cloned.queryMappings[i].clone();
        }
        return cloned;
    }
}
