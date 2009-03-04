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
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
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

    /** Holds either a String or a List of Strings. */
    private class Stringses {
        private final String string;
        private final String[] strings;

        private Stringses(String string) {
            if (string == null) throw new NullPointerException("String may not be null");
            this.string = string;
            this.strings = null;
        }

        private Stringses(List<String> strings) {
            if (strings == null) throw new NullPointerException("Strings collection may not be null");
            this.string = null;
            this.strings = strings.toArray(new String[strings.size()]);
        }

        public String getString() {
            return string != null ? string : (strings == null || strings.length < 1 ? null : strings[0]);
        }

        public String[] getStrings() {
            return string != null ? new String[] { string } : strings;
        }

        /** @return true if {@link #getStringOrStrings} would return an array. */
        public boolean isMultivalued() {
            return strings != null;
        }

        /** @return either a String, a String[], or null.  */
        public Object getStringOrStrings() {
            return string != null ? string : strings;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Stringses stringses = (Stringses) o;

            return !(string != null ? !string.equals(stringses.string) : stringses.string != null) &&
                   Arrays.equals(strings, stringses.strings);
        }

        public int hashCode() {
            int result;
            result = (string != null ? string.hashCode() : 0);
            result = 31 * result + (strings != null ? Arrays.hashCode(strings) : 0);
            return result;
        }
    }

    public class CacheEntry {
        public CacheEntry() {
            entryBirthdate = System.currentTimeMillis();
            cachedAttributes = new HashMap<String, Stringses>();
        }

        public long entryBirthdate;
        // key: attribute name, value: attribute value
        public HashMap<String, Stringses> cachedAttributes;
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
                    logger.log(Level.WARNING, e.getMessage(), ExceptionUtils.getDebugException(e));
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
            final CacheEntry values;
            try {
                values = createNewCacheEntry(filterExpression);
            } catch (FindException e) {
                logger.log(Level.WARNING, e.getMessage(), ExceptionUtils.getMessage(e));
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
            Stringses value = cachedvalues.cachedAttributes.get(key);
            pec.setVariable(key, value.getStringOrStrings());
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
                sc.setReturningAttributes(assertion.getAttrNames());
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
                                    StringBuffer sbuf = new StringBuffer();
                                    for (int i = 0; i < valuesWereLookingFor.size(); i++) {
                                        if (i > 0) {
                                            sbuf.append(", ");
                                        }
                                        sbuf.append(valuesWereLookingFor.get(i).toString());
                                    }
                                    logger.fine("Set " + attrMapping.getMatchingContextVariableName() + " to " + sbuf.toString());
                                    cachedvalues.cachedAttributes.put(attrMapping.getMatchingContextVariableName(), new Stringses(sbuf.toString()));
                                } else {
                                    List<String> valueStrings = new ArrayList<String>();
                                    for (int i = 0; i < valuesWereLookingFor.size(); i++)
                                        valueStrings.add(valuesWereLookingFor.get(i).toString());
                                    logger.fine("Set " + attrMapping.getMatchingContextVariableName() + " to " + valueStrings.size() + " values");
                                    cachedvalues.cachedAttributes.put(attrMapping.getMatchingContextVariableName(), new Stringses(valueStrings));
                                }
                            } else {
                                logger.fine("Set " + attrMapping.getMatchingContextVariableName() + " to " + valuesWereLookingFor.get(0));
                                cachedvalues.cachedAttributes.put(attrMapping.getMatchingContextVariableName(), new Stringses(valuesWereLookingFor.get(0).toString()));
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
                    throw new FindException("Error searching for LDAP entry: " + e.getMessage(), e);
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