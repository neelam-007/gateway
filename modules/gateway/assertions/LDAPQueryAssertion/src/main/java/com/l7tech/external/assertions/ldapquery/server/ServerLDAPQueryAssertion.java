package com.l7tech.external.assertions.ldapquery.server;

import com.l7tech.external.assertions.ldapquery.LDAPQueryAssertion;
import com.l7tech.external.assertions.ldapquery.QueryAttributeMapping;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.ldap.LdapIdentityProvider;
import com.l7tech.server.identity.ldap.LdapUtils;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.util.ManagedTimerTask;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.TimeUnit;
import org.apache.commons.collections.map.LRUMap;
import org.springframework.context.ApplicationContext;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side impl of LDAPQueryAssertion
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 6, 2007<br/>
 */
public class ServerLDAPQueryAssertion extends AbstractServerAssertion<LDAPQueryAssertion> {

    //- PUBLIC

    @SuppressWarnings({ "unchecked" })
    public ServerLDAPQueryAssertion( final LDAPQueryAssertion assertion,
                                     final ApplicationContext applicationContext ) {
        super(assertion);
        auditor = new Auditor(this, applicationContext, logger);
        identityProviderFactory = applicationContext.getBean("identityProviderFactory", IdentityProviderFactory.class);
        varsUsed = assertion.getVariablesUsed();
        cachedAttributeValues = Collections.synchronizedMap( assertion.getCacheSize() > 0 ? new LRUMap( assertion.getCacheSize() ) : new HashMap());
        Timer timer = applicationContext.getBean( "managedBackgroundTimer", Timer.class );
        timer.schedule( cacheCleanupTask, 5393L, cacheCleanupInterval );
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext pec ) throws IOException, PolicyAssertionException {
        // reconstruct filter expression
        final Map<String,Object> variableMap = pec.getVariableMap(varsUsed, auditor);
        final String filterExpression = !assertion.isSearchFilterInjectionProtected() ?
            ExpandVariables.process(assertion.getSearchFilter(), variableMap, auditor) :
            ExpandVariables.process(assertion.getSearchFilter(), variableMap, auditor, new Functions.Unary<String,String>(){
                @Override
                public String call( final String replacement ) {
                    return LdapUtils.filterEscape( replacement );
                }
            });

        CacheEntry cacheEntry = null;
        if (assertion.isEnableCache()) {
            cacheEntry = (CacheEntry) cachedAttributeValues.get(filterExpression);
        }

        if (cacheEntry == null || (System.currentTimeMillis() - cacheEntry.timestamp) > TimeUnit.MINUTES.toMillis(assertion.getCachePeriod())) {
            try {
                cacheEntry = createNewCacheEntry(filterExpression, assertion.getAttrNames());
            } catch (FindException e) {
                logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                return AssertionStatus.SERVER_ERROR;
            }
            
            if (assertion.isEnableCache()) {
                cachedAttributeValues.put(filterExpression, cacheEntry);
            }
        } else {
            if ( logger.isLoggable( Level.FINE )) {
                logger.log( Level.FINE, "Using cached result for filter ''{0}''.", filterExpression );
            }
        }

        if (assertion.isFailIfNoResults() && cacheEntry.cachedAttributes.size() < 1) {
            return AssertionStatus.FAILED;
        }

        pushToPec(pec, cacheEntry);

        return AssertionStatus.NONE;
    }

    @Override
    public void close() {
        if ( logger.isLoggable( Level.FINE )) {
            logger.log( Level.FINE, "Cancelling cache cleanup task." );
        }
        cacheCleanupTask.cancel();
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerLDAPQueryAssertion.class.getName());
    private static final long cacheCleanupInterval = SyspropUtil.getLong( "com.l7tech.external.assertions.ldapquery.cacheCleanupInterval", 321123L ); // around every 5 minutes

    private final IdentityProviderFactory identityProviderFactory;
    private final Auditor auditor;
    private final String[] varsUsed;
    // key: resolved search filter value value: cached entry
    private final Map<? super String,? super CacheEntry> cachedAttributeValues;
    private final TimerTask cacheCleanupTask = new ManagedTimerTask(){
        @Override
        protected void doRun() {
            cacheCleanup();
        }
    };

    private IdentityProvider getIdProvider() throws FindException {
        // get identity provider
        IdentityProvider output = identityProviderFactory.getProvider(assertion.getLdapProviderOid());
        if (output == null) {
            throw new FindException("The ldap identity provider attached to this LDAP Query assertion cannot be found. Perhaps" +
                    " it has been deleted since the assertion was created. " + assertion.getLdapProviderOid());
        }
        return output;
    }

    private void pushToPec( final PolicyEnforcementContext pec,
                            final CacheEntry cacheEntry) {
        for (final Map.Entry<String,AttributeValue> entry : cacheEntry.cachedAttributes.entrySet()) {
            pec.setVariable(entry.getKey(), entry.getValue().getStringOrStrings());
        }
    }

    private CacheEntry createNewCacheEntry( final String filter,
                                            final String[] attributeNames ) throws FindException {
        final CacheEntry cacheEntry = new CacheEntry();
        final IdentityProvider provider = getIdProvider();
        if (provider instanceof LdapIdentityProvider) {
            final LdapIdentityProvider identityProvider = (LdapIdentityProvider) provider;
            DirContext dirContext = null;
            NamingEnumeration answer = null;
            try {
                dirContext = identityProvider.getBrowseContext();
                final SearchControls sc = new SearchControls();
                sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
                sc.setReturningAttributes( attributeNames );
                sc.setCountLimit( 2 );
                answer = dirContext.search(LdapUtils.name(identityProvider.getConfig().getSearchBase()), filter, sc);

                if (answer.hasMore()) {
                    final SearchResult sr = (SearchResult) answer.next();
                    if ( answer.hasMore() ) {
                        logger.warning("Search filter returned more than one ldap entry: " + filter);
                    }
                    logger.info("Reading LDAP attributes for " + sr.getNameInNamespace());
                    for (final QueryAttributeMapping attributeMapping : assertion.getQueryMappings()) {
                        final Attribute valuesWereLookingFor = sr.getAttributes().get(attributeMapping.getAttributeName());
                        if (valuesWereLookingFor != null && valuesWereLookingFor.size() > 0) {
                            if (attributeMapping.isMultivalued()) {
                                if (attributeMapping.isJoinMultivalued()) {
                                    final StringBuilder builder = new StringBuilder();
                                    for (int i = 0; i < valuesWereLookingFor.size(); i++) {
                                        if (i > 0) {
                                            builder.append(", ");
                                        }
                                        builder.append(valuesWereLookingFor.get(i).toString());
                                    }
                                    logger.fine("Set " + attributeMapping.getMatchingContextVariableName() + " to " + builder.toString());
                                    cacheEntry.cachedAttributes.put(attributeMapping.getMatchingContextVariableName(), new AttributeValue(builder.toString()));
                                } else {
                                    List<String> valueStrings = new ArrayList<String>();
                                    for (int i = 0; i < valuesWereLookingFor.size(); i++)
                                        valueStrings.add(valuesWereLookingFor.get(i).toString());
                                    logger.fine("Set " + attributeMapping.getMatchingContextVariableName() + " to " + valueStrings.size() + " values");
                                    cacheEntry.cachedAttributes.put(attributeMapping.getMatchingContextVariableName(), new AttributeValue(valueStrings));
                                }
                            } else {
                                logger.fine("Set " + attributeMapping.getMatchingContextVariableName() + " to " + valuesWereLookingFor.get(0));
                                cacheEntry.cachedAttributes.put(attributeMapping.getMatchingContextVariableName(), new AttributeValue(valuesWereLookingFor.get(0).toString()));
                            }
                        } else {
                            logger.info("Attribute named " + attributeMapping.getAttributeName() + " was not present for ldap entry " + sr.getNameInNamespace());
                        }
                    }
                } else {
                    logger.warning("The search filter " + filter + " did not return any ldap entry.");
                }
                return cacheEntry;
            } catch (Exception e) {
                if (ExceptionUtils.causedBy(e, NamingException.class)) {
                    String extraDetail = "";
                    if ( e instanceof NamingException && ((NamingException)e).getRemainingName()!=null ) {
                        extraDetail = "; remaining name '" + ((NamingException)e).getRemainingName().toString().trim() + "'";
                    }
                    throw new FindException("Error searching for LDAP entry: " + e.getMessage() + extraDetail, e);
                } else {
                    throw new FindException("Error searching for LDAP entry", e);
                }
            } finally {
                ResourceUtils.closeQuietly(answer);
                ResourceUtils.closeQuietly(dirContext);
            }
        } else {
            throw new FindException("Id provider is not ldap");
        }
    }

    private void cacheCleanup() {
        synchronized( cachedAttributeValues ) {
            final Set<String> keysToRemove = new HashSet<String>();

            final long currentTime = System.currentTimeMillis();
            for ( final Map.Entry<?,?> entry : cachedAttributeValues.entrySet() ) {
                final String filterExpression = (String) entry.getKey();
                final CacheEntry cacheEntry = (CacheEntry) entry.getValue();

                if ( (currentTime - cacheEntry.timestamp) > TimeUnit.MINUTES.toMillis(assertion.getCachePeriod()) ) {
                    keysToRemove.add( filterExpression );
                }
            }

            if ( !keysToRemove.isEmpty() ) {
                if ( logger.isLoggable( Level.FINE )) {
                    logger.log( Level.FINE, "Removing {0} expired entries from cache.", keysToRemove.size() );
                }
                cachedAttributeValues.keySet().removeAll( keysToRemove );
            }
        }
    }

    /**
     * Single or multivalued attribute value.
     */
    private static class AttributeValue {
        private final boolean multivalued;
        private final List<String> values;

        private AttributeValue( final String value ) {
            if (value == null) throw new NullPointerException("Value may not be null");
            this.multivalued = false;
            this.values = Collections.singletonList( value );
        }

        private AttributeValue( final List<String> value ) {
            if (value == null) throw new NullPointerException("Value may not be null");
            this.multivalued = true;
            this.values = Collections.unmodifiableList(new ArrayList<String>(value));
        }

        public String getString() {
            return values.isEmpty() ? null : values.get( 0 );
        }

        public String[] getStrings() {
            return values.toArray( new String[values.size()] );
        }

        /** @return true if multivalued */
        public boolean isMultivalued() {
            return multivalued;
        }

        /** @return either a String or a String[]  */
        public Object getStringOrStrings() {
            return multivalued ? getStrings() : getString();
        }
    }

    private static class CacheEntry {
        private final long timestamp;
        // key: attribute name, value: attribute value
        private final Map<String, AttributeValue> cachedAttributes;

        CacheEntry() {
            timestamp = System.currentTimeMillis();
            cachedAttributes = new HashMap<String, AttributeValue>();
        }
    }    
}