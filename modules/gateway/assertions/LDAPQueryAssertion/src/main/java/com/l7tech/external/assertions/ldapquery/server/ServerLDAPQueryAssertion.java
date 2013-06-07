package com.l7tech.external.assertions.ldapquery.server;

import com.l7tech.external.assertions.ldapquery.LDAPQueryAssertion;
import com.l7tech.external.assertions.ldapquery.QueryAttributeMapping;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.ldap.LdapIdentityProvider;
import com.l7tech.server.identity.ldap.LdapUtils;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.util.ManagedTimerTask;
import com.l7tech.util.*;
import org.apache.commons.collections.map.LRUMap;
import org.springframework.beans.factory.BeanFactory;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.directory.*;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

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

    public ServerLDAPQueryAssertion( final LDAPQueryAssertion assertion,
                                     final BeanFactory context ) {
        super(assertion);
        identityProviderFactory = context.getBean("identityProviderFactory", IdentityProviderFactory.class);
        varsUsed = assertion.getVariablesUsed();
        cachedAttributeValues = Collections.synchronizedMap( assertion.getCacheSize() > 0 ? new LRUMap( assertion.getCacheSize() ) : new HashMap());
        final Timer timer = context.getBean( "managedBackgroundTimer", Timer.class );
        timer.schedule( cacheCleanupTask, 5393L, cacheCleanupInterval );
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext pec ) throws IOException, PolicyAssertionException {
        // reconstruct filter expression
        final Map<String,Object> variableMap = pec.getVariableMap(varsUsed, getAudit());
        final String filterExpression = !assertion.isSearchFilterInjectionProtected() ?
            ExpandVariables.process(assertion.getSearchFilter(), variableMap, getAudit()) :
            ExpandVariables.process(assertion.getSearchFilter(), variableMap, getAudit(), new Functions.Unary<String,String>(){
                @Override
                public String call( final String replacement ) {
                    return LdapUtils.filterEscape( replacement );
                }
            });

        logAndAudit( AssertionMessages.LDAP_QUERY_SEARCH_FILTER, filterExpression );

        CacheEntry cacheEntry = null;
        if (assertion.isEnableCache()) {
            cacheEntry = (CacheEntry) cachedAttributeValues.get(filterExpression);
        }

        if (cacheEntry == null || (System.currentTimeMillis() - cacheEntry.timestamp) > TimeUnit.MINUTES.toMillis(assertion.getCachePeriod())) {
            try {
                cacheEntry = createNewCacheEntry(filterExpression, assertion.getAttrNames());
            } catch (FindException e) {
                logAndAudit( AssertionMessages.LDAP_QUERY_ERROR, new String[]{ ExceptionUtils.getMessage( e ) }, ExceptionUtils.getDebugException( e ) );
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
            return AssertionStatus.FALSIFIED;
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

    private static final long cacheCleanupInterval = ConfigFactory.getLongProperty( "com.l7tech.external.assertions.ldapquery.cacheCleanupInterval", 321123L ); // around every 5 minutes

    private final IdentityProviderFactory identityProviderFactory;
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

    /**
     * Search
     */
    protected int doSearch( final String filter,
                            final String[] attributeNames,
                            final int maxResults,
                            final Functions.BinaryVoidThrows<QueryAttributeMapping,SimpleAttribute,Exception> resultCallback ) throws FindException {
        int availableResultCount = 0;

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
                if ( maxResults > 0 ) {
                    sc.setCountLimit( (long) (maxResults + 1) );
                }
                answer = dirContext.search(LdapUtils.name(identityProvider.getConfig().getSearchBase()), filter, sc);

                while ( answer.hasMore() && (availableResultCount++ < maxResults || maxResults==0) ) {
                    final SearchResult sr = (SearchResult) answer.next();
                    if ( logger.isLoggable( Level.FINE ) ) {
                        logger.log( Level.FINE, "Reading LDAP attributes for " + sr.getNameInNamespace());
                    }
                    final Attributes attributes = sr.getAttributes();
                    for ( final QueryAttributeMapping attributeMapping : assertion.getQueryMappings() ) {
                        final String attributeName = attributeMapping.getAttributeName();
                        final Attribute attribute = attributes.get( attributeName );
                        if ( attribute != null && attribute.size() > 0 ) {
                            resultCallback.call( attributeMapping, new SimpleAttribute(sr.getNameInNamespace(), attribute) );
                        } else {
                            resultCallback.call( attributeMapping, new SimpleAttribute(sr.getNameInNamespace()) );
                        }
                    }
                }
            } catch (AssertionStatusException e) {
                throw e;
            } catch (Exception e) {
                @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
                Exception cause = ExceptionUtils.getCauseIfCausedBy(e, NamingException.class);
                if (cause instanceof NamingException) {
                    if (!(cause instanceof PartialResultException) || !LdapUtils.isIgnorePartialResultException()) {
                        String extraDetail = "";
                        if ( e instanceof NamingException && ((NamingException)e).getRemainingName()!=null ) {
                            extraDetail = "; remaining name '" + ((NamingException)e).getRemainingName().toString().trim() + "'";
                        }
                        throw new FindException("Error searching for LDAP entry: " + e.getMessage() + extraDetail, e);
                    } else {
                        /* FALLTHROUGH and ignore the PartialResultException */
                    }
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

        return availableResultCount;
    }

    private CacheEntry createNewCacheEntry( final String filter,
                                            final String[] attributeNames ) throws FindException {
        final int maxResults;
        if ( !assertion.isAllowMultipleResults() ) {
            maxResults = 1;
        } else {
            maxResults = assertion.getMaximumResults();
        }

        final Map<String,List<String>> attributeValues = new HashMap<String,List<String>>();
        final int availableResults = doSearch( filter, attributeNames, maxResults, new Functions.BinaryVoidThrows<QueryAttributeMapping,SimpleAttribute,Exception>(){
            @Override
            public void call( final QueryAttributeMapping attributeMapping,
                              final SimpleAttribute simpleAttribute ) throws Exception {
                final String attributeName = attributeMapping.getAttributeName();
                final String contextVariableName = attributeMapping.getMatchingContextVariableName();

                /* Treat DN Special - it is not an attribute but we're going to pass it back like it is */
                if ( attributeMapping.getAttributeName().equalsIgnoreCase("dn") ) {
                    if ( attributeValues.get(contextVariableName) == null ) {
                        List<String> dnValue = new LinkedList<String>();
                        dnValue.add(simpleAttribute.getEntryName());
                        attributeValues.put(contextVariableName,dnValue);
                    }  else {
                        attributeValues.get(contextVariableName).add(simpleAttribute.getEntryName());
                    }
                }

                if ( simpleAttribute.isPresent() && simpleAttribute.getSize() > 0 ) {
                    List<String> values = attributeValues.get( contextVariableName );
                    if ( values == null ) {
                        values = new ArrayList<String>();
                        attributeValues.put( contextVariableName, values );
                    }

                    if ( attributeMapping.isMultivalued() ) {
                        if ( attributeMapping.isJoinMultivalued() ) {
                            final String value = simpleAttribute.getJoinedValue();
                            if ( logger.isLoggable( Level.FINE ) ) {
                                logger.log( Level.FINE, "Attribute " + attributeName + " as " + value);
                            }
                            values.add( value );
                        } else {
                            if ( logger.isLoggable( Level.FINE ) ) {
                                logger.log( Level.FINE, "Attribute " + attributeName + " as " + simpleAttribute.getSize() + " values");
                            }
                            values.addAll( simpleAttribute.getValues() );
                        }
                    } else {
                        if ( attributeMapping.isFailMultivalued() && simpleAttribute.getSize() > 1 ) {
                            logAndAudit( AssertionMessages.LDAP_QUERY_MULTIVALUED_ATTR, attributeName );
                            throw new AssertionStatusException( AssertionStatus.FALSIFIED );
                        }
                        if ( logger.isLoggable( Level.FINE ) ) {
                            logger.log( Level.FINE, "Attribute " + attributeName + " as " + simpleAttribute.getFirstValue());
                        }
                        values.add( simpleAttribute.getFirstValue() );
                    }
                } else {
                    if ( logger.isLoggable( Level.FINE ) ) {
                        logger.log( Level.FINE, "Attribute named " + attributeName + " was not present for ldap entry " + simpleAttribute.getEntryName());
                    }
                }
            }
        } );

        if ( maxResults > 0 && availableResults > maxResults ) {
            logAndAudit( AssertionMessages.LDAP_QUERY_TOO_MANY_RESULTS, filter, Integer.toString( maxResults ) );
            if ( assertion.isFailIfTooManyResults() ) {
                throw new AssertionStatusException( AssertionStatus.FALSIFIED );
            }
        } else if ( availableResults == 0 ) {
            logAndAudit( AssertionMessages.LDAP_QUERY_NO_RESULTS, filter );
        }

        final Map<String, AttributeValue> cachedAttributes = new HashMap<String,AttributeValue>();
        for ( final QueryAttributeMapping attributeMapping : assertion.getQueryMappings() ) {
            final String entryKey = attributeMapping.getMatchingContextVariableName();
            final List<String> values = attributeValues.get( entryKey );
            if ( values == null ) continue;
            cachedAttributes.put( entryKey, new AttributeValue( assertion.isAllowMultipleResults(), values ) );
        }
        return new CacheEntry( cachedAttributes );
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
     * An "easy to use" wrapper for an Attribute
     */
    protected static class SimpleAttribute {
        private final String entryName;
        private final Attribute attribute;

        protected SimpleAttribute( final String entryName ){
            this( entryName, null );
        }

        protected SimpleAttribute( final String entryName,
                                   final Attribute attribute ){
            this.entryName = entryName;
            this.attribute = attribute;
        }

        public String getEntryName() {
            return entryName;
        }

        public boolean isPresent() {
            return attribute != null;
        }

        public String getFirstValue() throws Exception {
            return getStringValue( 0 );
        }

        public String getJoinedValue() throws Exception {
            final StringBuilder builder = new StringBuilder();
            for (int i = 0; i < getSize(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append( getStringValue(i) );
            }
            return builder.toString();
        }

        public List<String> getValues() throws Exception {
            final List<String> values = new ArrayList<String>();
            for ( int i=0; i<getSize(); i++ ) {
                values.add( getStringValue(i) );
            }
            return values;
        }

        public int getSize() {
            return attribute.size();
        }

        protected String getStringValue( final int index ) throws Exception {
            final Object value = attribute.get( index );
            return value==null ?
                    null :
                    value instanceof byte[] ?
                            HexUtils.encodeBase64( (byte[])value, true ) :
                            value.toString();
        }
    }

    /**
     * Single or multivalued attribute value.
     */
    private static class AttributeValue {
        private final boolean multivalued;
        private final List<String> values;

        private AttributeValue( final boolean multivalued, final List<String> values ) {
            if (values == null) throw new NullPointerException("Values may not be null");
            this.multivalued = multivalued || values.size() > 1;
            this.values = Collections.unmodifiableList(new ArrayList<String>(values));
        }

        private String getString() {
            return values.isEmpty() ? null : values.get( 0 );
        }

        private String[] getStrings() {
            return values.toArray( new String[values.size()] );
        }

        /** @return either a String or a String[]  */
        private Object getStringOrStrings() {
            return multivalued ? getStrings() : getString();
        }
    }

    private static class CacheEntry {
        private final long timestamp;
        // key: attribute name, value: attribute value
        private final Map<String, AttributeValue> cachedAttributes;

        private CacheEntry( final Map<String, AttributeValue> cachedAttributes ) {
            this.timestamp = System.currentTimeMillis();
            this.cachedAttributes = Collections.unmodifiableMap( cachedAttributes );
        }
    }    
}
