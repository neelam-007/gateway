package com.l7tech.external.assertions.ldapquery.server;

import com.l7tech.util.ResourceUtils;
import com.l7tech.external.assertions.ldapquery.QueryAttributeMapping;
import com.l7tech.external.assertions.ldapquery.LDAPQueryAssertion;
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
import java.util.Map;
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

    public ServerLDAPQueryAssertion(LDAPQueryAssertion assertion, ApplicationContext appCntx) {
        super(assertion);
        auditor = new Auditor(this, appCntx, logger);
        this.assertion = assertion;
        identityProviderFactory = (IdentityProviderFactory)appCntx.getBean("identityProviderFactory", IdentityProviderFactory.class);
        varsUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext pec) throws IOException, PolicyAssertionException {
        // reconstruct filter expression
        Map vars = pec.getVariableMap(varsUsed, auditor);
        String filterExpression = ExpandVariables.process(assertion.getSearchFilter(), vars, auditor);

        // get identity provider
        IdentityProvider provider;
        try {
            provider = identityProviderFactory.getProvider(assertion.getLdapProviderOid());
        } catch (FindException e) {
            logger.log(Level.WARNING, "error retrieving identity provider", e);
            return AssertionStatus.SERVER_ERROR;
        }
        if (provider == null) {
            logger.warning("The ldap identity provider attached to this LDAP Query assertion cannot be found. Perhaps" +
                           " it has been deleted since the assertion was created. " + assertion.getLdapProviderOid());
            return AssertionStatus.SERVER_ERROR;
        }
        if (provider instanceof LdapIdentityProvider) {
            return readLdapAttributes((LdapIdentityProvider)provider, filterExpression, pec);
        } else {
            logger.warning("The identity provider attached to this LDAP Query assertion is not LDAP. " + assertion.getLdapProviderOid());
            return AssertionStatus.SERVER_ERROR;
        }
    }

    private AssertionStatus readLdapAttributes(LdapIdentityProvider idprov, String filter, PolicyEnforcementContext pec) {
        DirContext ldapcontext = null;
        NamingEnumeration answer = null;
        try {
            ldapcontext = idprov.getBrowseContext();
            SearchControls sc = new SearchControls();
            sc.setCountLimit(idprov.getMaxSearchResultSize());
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            answer = ldapcontext.search(((LdapIdentityProviderConfig)idprov.getConfig()).getSearchBase(), filter, sc);

            if (answer.hasMore()) {
                SearchResult sr = (SearchResult)answer.next();
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
                            logger.info("Set " + attrMapping.getMatchingContextVariableName() + " to " + sbuf.toString());
                            pec.setVariable(attrMapping.getMatchingContextVariableName(), sbuf.toString());
                        } else {
                            logger.info("Set " + attrMapping.getMatchingContextVariableName() + " to " + valuesWereLookingFor.get(0));
                            pec.setVariable(attrMapping.getMatchingContextVariableName(), valuesWereLookingFor.get(0));
                        }
                    } else {
                        logger.info("Attribute named " + attrMapping.getAttributeName() + " was not present for ldap entry " + sr.getNameInNamespace());
                    }
                }
            } else {
                logger.warning("The search filter " + filter + " did not return any ldap entry.");
            }
            return AssertionStatus.NONE;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error searching for LDAP entry", e);
            return AssertionStatus.SERVER_ERROR;
        } finally {
            if (ldapcontext != null) {
                if (answer != null) {
                    ResourceUtils.closeQuietly(answer);
                }
                ResourceUtils.closeQuietly(ldapcontext);
            }
        }
    }
}
