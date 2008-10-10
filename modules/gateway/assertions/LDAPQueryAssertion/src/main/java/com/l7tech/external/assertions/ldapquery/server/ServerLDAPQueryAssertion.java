package com.l7tech.external.assertions.ldapquery.server;

import com.l7tech.util.ResourceUtils;
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
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.springframework.context.ApplicationContext;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
    private LDAPQueryAssertion assertion;
    private final String[] varsUsed;
    // key: resolved search filter value value: cached entry
    private final HashMap<String, CacheEntry> cachedAttributeValues = new HashMap<String, CacheEntry>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    public ServerLDAPQueryAssertion(LDAPQueryAssertion assertion, ApplicationContext appCntx) {
        super(assertion);
        auditor = new Auditor(this, appCntx, logger);
        this.assertion = assertion;
        identityProviderFactory = (IdentityProviderFactory) appCntx.getBean("identityProviderFactory", IdentityProviderFactory.class);
        varsUsed = assertion.getVariablesUsed();
    }

    public class CacheEntry {
        public CacheEntry() {
            entryBirthdate = System.currentTimeMillis();
            cachedAttributes = new HashMap<String, String>();
        }

        public long entryBirthdate;
        // key: attribute name, value: attribute value
        public HashMap<String, String> cachedAttributes;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext pec) throws IOException, PolicyAssertionException {
        // reconstruct filter expression
        Map vars = pec.getVariableMap(varsUsed, auditor);
        String filterExpression = ExpandVariables.process(assertion.getSearchFilter(), vars, auditor);

        if (assertion.isEnableCache()) {
            CacheEntry cachedvalues = null;
            cacheLock.readLock().lock();
            try {
                cachedvalues = cachedAttributeValues.get(filterExpression);
            } finally {
                cacheLock.readLock().unlock();
            }
            if (cachedvalues == null || (System.currentTimeMillis() - cachedvalues.entryBirthdate) > (assertion.getCachePeriod() * 1000 * 60)) {
                try {
                    cachedvalues = createNewCacheEntry(filterExpression);
                } catch (FindException e) {
                    logger.log(Level.WARNING, "error reading from ldap", e);
                    return AssertionStatus.SERVER_ERROR;
                }
                cacheLock.writeLock().lock();
                try {
                    cachedAttributeValues.remove(filterExpression);
                    cachedAttributeValues.put(filterExpression, cachedvalues);
                } finally {
                    cacheLock.writeLock().unlock();
                }
            } else {
                logger.info("using cached value");
            }

            if (assertion.isFailIfNoResults() && cachedvalues.cachedAttributes.size() < 1) {
                return AssertionStatus.FAILED;
            }
            pushToPec(pec, cachedvalues);
            return AssertionStatus.NONE;
        } else {
            // no caching
            CacheEntry values = null;
            try {
                values = createNewCacheEntry(filterExpression);
            } catch (FindException e) {
                logger.log(Level.WARNING, "error reading from ldap", e);
                return AssertionStatus.SERVER_ERROR;
            }

            if (assertion.isFailIfNoResults() && values.cachedAttributes.size() < 1) {
                return AssertionStatus.FAILED;
            }
            pushToPec(pec, values);
            return AssertionStatus.NONE;
        }
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
        for (String key : cachedvalues.cachedAttributes.keySet()) {
            String value = cachedvalues.cachedAttributes.get(key);
            pec.setVariable(key, value);
        }
    }

    private CacheEntry createNewCacheEntry(String filter) throws FindException {
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
                answer = ldapcontext.search(((LdapIdentityProviderConfig) idprov.getConfig()).getSearchBase(), filter, sc);

                if (answer.hasMore()) {
                    SearchResult sr = (SearchResult) answer.next();
                    if (answer.hasMore()) {
                        logger.warning("Search filter returned more than one ldap entry: " + filter);
                    }
                    logger.info("Reading LDAP attributes for " + sr.getNameInNamespace());
                    for (QueryAttributeMapping attrMapping : assertion.currentQueryMappings()) {
                        Attribute valuesWereLookingFor = sr.getAttributes().get(attrMapping.getAttributeName());
                        if (valuesWereLookingFor != null && valuesWereLookingFor.size() > 0) {
                            if (attrMapping.isMultivalued()) {
                                StringBuffer sbuf = new StringBuffer();
                                for (int i = 0; i < valuesWereLookingFor.size(); i++) {
                                    if (i > 0) {
                                        sbuf.append(", ");
                                    }
                                    sbuf.append(valuesWereLookingFor.get(i).toString());
                                }
                                logger.fine("Set " + attrMapping.getMatchingContextVariableName() + " to " + sbuf.toString());
                                cachedvalues.cachedAttributes.put(attrMapping.getMatchingContextVariableName(), sbuf.toString());

                            } else {
                                logger.fine("Set " + attrMapping.getMatchingContextVariableName() + " to " + valuesWereLookingFor.get(0));
                                cachedvalues.cachedAttributes.put(attrMapping.getMatchingContextVariableName(), valuesWereLookingFor.get(0).toString());
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
                throw new FindException("Error searching for LDAP entry", e);
            } finally {
                if (ldapcontext != null) {
                    if (answer != null) {
                        ResourceUtils.closeQuietly(answer);
                    }
                    ResourceUtils.closeQuietly(ldapcontext);
                }
            }
        } else {
            throw new FindException("Id provider is not ldap");
        }
    }
}