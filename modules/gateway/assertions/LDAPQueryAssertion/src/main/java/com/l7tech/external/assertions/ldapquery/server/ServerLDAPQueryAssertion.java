package com.l7tech.external.assertions.ldapquery.server;

import com.l7tech.external.assertions.ldapquery.LDAPQueryAssertion;
import com.l7tech.external.assertions.ldapquery.QueryAttributeMapping;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
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
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
    private final Logger logger = Logger.getLogger(ServerLDAPQueryAssertion.class.getName());
    private final IdentityProviderFactory identityProviderFactory;
    private final Auditor auditor;
    private final String[] varsUsed;
    // key: resolved search filter value value: cached entry
    private final HashMap<String, CacheEntry> cachedAttributeValues = new HashMap<String, CacheEntry>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    public ServerLDAPQueryAssertion( final LDAPQueryAssertion assertion,
                                     final ApplicationContext appCntx ) {
        super(assertion);
        auditor = new Auditor(this, appCntx, logger);
        identityProviderFactory = (IdentityProviderFactory) appCntx.getBean("identityProviderFactory", IdentityProviderFactory.class);
        varsUsed = assertion.getVariablesUsed();
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
        private final long entryBirthdate;
        // key: attribute name, value: attribute value
        private final Map<String, AttributeValue> cachedAttributes;

        public CacheEntry() {
            entryBirthdate = System.currentTimeMillis();
            cachedAttributes = new HashMap<String, AttributeValue>();
        }
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext pec ) throws IOException, PolicyAssertionException {
        // reconstruct filter expression
        final Map vars = pec.getVariableMap(varsUsed, auditor);
        final String filterExpression = !assertion.isSearchFilterInjectionProtected() ?
            ExpandVariables.process(assertion.getSearchFilter(), vars, auditor) :
            ExpandVariables.process(assertion.getSearchFilter(), vars, auditor, new Functions.Unary<String,String>(){
                @Override
                public String call( final String replacement ) {
                    return LdapUtils.filterEscape( replacement );
                }
            });

        CacheEntry cachedvalues = null;
        if (assertion.isEnableCache()) {
            cacheLock.readLock().lock();
            try {
                cachedvalues = cachedAttributeValues.get(filterExpression);
            } finally {
                cacheLock.readLock().unlock();
            }
        }

        if (cachedvalues == null || (System.currentTimeMillis() - cachedvalues.entryBirthdate) > (assertion.getCachePeriod() * 1000 * 60)) {
            try {
                cachedvalues = createNewCacheEntry(filterExpression, assertion.getAttrNames());
            } catch (FindException e) {
                logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                return AssertionStatus.SERVER_ERROR;
            }
            
            if (assertion.isEnableCache()) {
                cacheLock.writeLock().lock();
                try {
                    cachedAttributeValues.put(filterExpression, cachedvalues);
                } finally {
                    cacheLock.writeLock().unlock();
                }
            }
        } else {
            logger.info("using cached value");
        }

        if (assertion.isFailIfNoResults() && cachedvalues.cachedAttributes.size() < 1) {
            return AssertionStatus.FAILED;
        }

        pushToPec(pec, cachedvalues);

        return AssertionStatus.NONE;
    }

    private IdentityProvider getIdProvider() throws FindException {
        // get identity provider
        IdentityProvider output = identityProviderFactory.getProvider(assertion.getLdapProviderOid());
        if (output == null) {
            throw new FindException("The ldap identity provider attached to this LDAP Query assertion cannot be found. Perhaps" +
                    " it has been deleted since the assertion was created. " + assertion.getLdapProviderOid());
        }
        return output;
    }

    private void pushToPec(PolicyEnforcementContext pec, CacheEntry cachedvalues) {
        for (Map.Entry<String,AttributeValue> entry : cachedvalues.cachedAttributes.entrySet()) {
            pec.setVariable(entry.getKey(), entry.getValue().getStringOrStrings());
        }
    }

    private CacheEntry createNewCacheEntry( final String filter,
                                            final String[] attributeNames ) throws FindException {
        CacheEntry cachedvalues = new CacheEntry();
        // get identity provider
        IdentityProvider provider = getIdProvider();
        if (provider instanceof LdapIdentityProvider) {
            LdapIdentityProvider idprov = (LdapIdentityProvider) provider;
            DirContext ldapcontext = null;
            NamingEnumeration answer = null;
            try {
                ldapcontext = idprov.getBrowseContext();
                SearchControls sc = new SearchControls();
                sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
                sc.setReturningAttributes( attributeNames );
                sc.setCountLimit( 2 );
                answer = ldapcontext.search(((LdapIdentityProviderConfig) idprov.getConfig()).getSearchBase(), filter, sc);

                if (answer.hasMore()) {
                    SearchResult sr = (SearchResult) answer.next();
                    if (answer.hasMore()) {
                        logger.warning("Search filter returned more than one ldap entry: " + filter);
                    }
                    logger.info("Reading LDAP attributes for " + sr.getNameInNamespace());
                    for (QueryAttributeMapping attrMapping : assertion.getQueryMappings()) {
                        Attribute valuesWereLookingFor = sr.getAttributes().get(attrMapping.getAttributeName());
                        if (valuesWereLookingFor != null && valuesWereLookingFor.size() > 0) {
                            if (attrMapping.isMultivalued()) {
                                if (attrMapping.isJoinMultivalued()) {
                                    StringBuilder sbuf = new StringBuilder();
                                    for (int i = 0; i < valuesWereLookingFor.size(); i++) {
                                        if (i > 0) {
                                            sbuf.append(", ");
                                        }
                                        sbuf.append(valuesWereLookingFor.get(i).toString());
                                    }
                                    logger.fine("Set " + attrMapping.getMatchingContextVariableName() + " to " + sbuf.toString());
                                    cachedvalues.cachedAttributes.put(attrMapping.getMatchingContextVariableName(), new AttributeValue(sbuf.toString()));
                                } else {
                                    List<String> valueStrings = new ArrayList<String>();
                                    for (int i = 0; i < valuesWereLookingFor.size(); i++)
                                        valueStrings.add(valuesWereLookingFor.get(i).toString());
                                    logger.fine("Set " + attrMapping.getMatchingContextVariableName() + " to " + valueStrings.size() + " values");
                                    cachedvalues.cachedAttributes.put(attrMapping.getMatchingContextVariableName(), new AttributeValue(valueStrings));
                                }
                            } else {
                                logger.fine("Set " + attrMapping.getMatchingContextVariableName() + " to " + valuesWereLookingFor.get(0));
                                cachedvalues.cachedAttributes.put(attrMapping.getMatchingContextVariableName(), new AttributeValue(valuesWereLookingFor.get(0).toString()));
                            }
                        } else {
                            logger.info("Attribute named " + attrMapping.getAttributeName() + " was not present for ldap entry " + sr.getNameInNamespace());
                        }
                    }
                } else {
                    logger.warning("The search filter " + filter + " did not return any ldap entry.");
                }
                return cachedvalues;
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
                ResourceUtils.closeQuietly(ldapcontext);
            }
        } else {
            throw new FindException("Id provider is not ldap");
        }
    }
}