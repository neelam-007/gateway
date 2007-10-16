package com.l7tech.server.policy.assertion.identity;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.AuditDetailMessage;
import com.l7tech.server.audit.Auditor;
import com.l7tech.common.security.token.KerberosSecurityToken;
import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.security.token.XmlSecurityToken;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.Identity;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.mapping.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.identity.MappingAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.server.identity.mapping.AttributeConfigManagerImpl;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

public class ServerMappingAssertion extends ServerIdentityAssertion {
    private static final Logger logger = Logger.getLogger(ServerMappingAssertion.class.getName());

    private final Auditor auditor;
    private final MappingAssertion assertion;

    private transient AttributeConfig attributeConfig;
    private transient IdentityMapping[] identitySearchMappings;
    private transient IdentityMapping[] identityRetrieveMappings;
    private transient SecurityTokenMapping[] tokenMappings;

    private final AttributeConfigManagerImpl attributeConfigManager;
    private static final int MAX_AGE = 30000;

    public ServerMappingAssertion(MappingAssertion assertion, ApplicationContext springContext) {
        super(assertion, springContext);
        this.auditor = new Auditor(this, springContext, logger);
        this.assertion = assertion;

        attributeConfigManager = (AttributeConfigManagerImpl)springContext.getBean("attributeConfigManager");

//        try {
            refreshData();
//        } catch (FindException e) {
//            throw new RuntimeException("Invalid AttributeConfig #" + assertion.getAttributeConfigOid());
//        }
    }

    private void refreshData() {
        attributeConfig = new AttributeConfig(new AttributeHeader(assertion.getVariableName(), null, DataType.STRING, UsersOrGroups.BOTH));

        UsersOrGroups uog;
        if (assertion.isValidForGroups() && assertion.isValidForUsers()) {
            uog = UsersOrGroups.BOTH;
        } else if (assertion.isValidForGroups()) {
            uog = UsersOrGroups.GROUPS;
        } else if (assertion.isValidForUsers()) {
            uog = UsersOrGroups.USERS;
        } else {
            uog = null;
        }
        LdapAttributeMapping lmaps = new LdapAttributeMapping(attributeConfig, assertion.getIdentityProviderOid(), uog);
        lmaps.setCustomAttributeName(assertion.getSearchAttributeName());
        identitySearchMappings = new IdentityMapping[] { lmaps };

        LdapAttributeMapping lmapr = new LdapAttributeMapping(attributeConfig, assertion.getIdentityProviderOid(), uog);
        lmapr.setCustomAttributeName(assertion.getRetrieveAttributeName());
        identityRetrieveMappings = new IdentityMapping[] { lmapr };

        SecurityTokenMapping kmap = new KerberosSecurityTokenMapping();
        kmap.setTokenType(SecurityTokenType.WSS_KERBEROS_BST);
        tokenMappings = new SecurityTokenMapping[] { kmap };
    }

    private void REALrefreshData() throws FindException {
        try {
            long oid = assertion.getAttributeConfigOid();
            if (attributeConfig != null && attributeConfigManager.isCacheCurrent(oid, MAX_AGE)) {
                logger.fine("Cached AttributeConfig is still current");
                return;
            }

            logger.fine("Checking for newer AttributeConfig");
            attributeConfig = (AttributeConfig)attributeConfigManager.getCachedEntity(oid, MAX_AGE);

            ArrayList mappings = new ArrayList();
            for (Iterator i = attributeConfig.getIdentityMappings().iterator(); i.hasNext();) {
                IdentityMapping mapping = (IdentityMapping) i.next();
                if (mapping.getProviderOid() == assertion.getIdentityProviderOid()) {
                    mappings.add(mapping);
                }
            }

            if (!mappings.isEmpty()) {
                identitySearchMappings = (IdentityMapping[])mappings.toArray(new IdentityMapping[0]);
            } else {
                identitySearchMappings = new IdentityMapping[0];
                logMissingMap(AssertionMessages.MAPPING_NO_IDMAP);
            }

            mappings.clear();
            for (Iterator i = attributeConfig.getSecurityTokenMappings().iterator(); i.hasNext();) {
                SecurityTokenMapping mapping = (SecurityTokenMapping)i.next();
                mappings.add(mapping);
            }

            if (!mappings.isEmpty()) {
                tokenMappings = (SecurityTokenMapping[])mappings.toArray(new SecurityTokenMapping[0]);
            } else {
                tokenMappings = new SecurityTokenMapping[0];
                logMissingMap(AssertionMessages.MAPPING_NO_TOKMAP);
            }

        } catch (EntityManager.CacheVeto cacheVeto) {
            throw new RuntimeException(cacheVeto); // Can't happen
        }
    }

    private void logMissingMap(AuditDetailMessage message) {
        auditor.logAndAudit(message, new String[] {
            Long.toString(assertion.getIdentityProviderOid()),
            Long.toString(assertion.getAttributeConfigOid())
        });
    }

    protected AssertionStatus validateCredentials(IdentityProvider provider,
                                                  LoginCredentials pc,
                                                  PolicyEnforcementContext context)
            throws AuthenticationException
    {
        String clientName = null;
        XmlSecurityToken[] tokens = context.getRequest().getSecurityKnob().getProcessorResult().getXmlSecurityTokens();
        for (int i = 0; i < tokens.length; i++) {
            SecurityToken securityToken = tokens[i];
            if (securityToken instanceof KerberosSecurityToken) {
                KerberosSecurityToken ktok = (KerberosSecurityToken) securityToken;
                clientName = ktok.getTicket().getServiceTicket().getClientPrincipalName();
            }
        }
        if (clientName == null) {
            auditor.logAndAudit(AssertionMessages.MAPPING_NO_TOKVALUE);
            return AssertionStatus.FAILED;
        }

        try {
            // TODO caching here?
            // TODO caching here?
            // TODO caching here?
            // TODO caching here?
            // TODO caching here?
            Collection results = provider.search(assertion.isValidForUsers(), assertion.isValidForGroups(), identitySearchMappings[0], clientName);
            if (results.size() == 1) {

                EntityHeader header = (EntityHeader) results.iterator().next();
                Identity identity;
                if (header.getType() == EntityType.USER) {
                    identity = provider.getUserManager().findByPrimaryKey(header.getStrId());
                } else if (header.getType() == EntityType.GROUP) {
                    identity = provider.getGroupManager().findByPrimaryKey(header.getStrId());
                } else {
                    throw new AuthenticationException("Identity was neither a User nor a Group");
                }

                Object[] values = attributeExtractors.get(identityRetrieveMappings[0]).extractValues(identity);
                if (values.length == 1) {
                    context.setVariable(assertion.getVariableName(), values[0]);
                    return AssertionStatus.NONE;
                } else {
                    auditor.logAndAudit(AssertionMessages.MAPPING_NO_IDVALUE);
                    return AssertionStatus.FAILED;
                }
            } else {
                auditor.logAndAudit(AssertionMessages.MAPPING_NO_IDVALUE);
                return AssertionStatus.FAILED;
            }
        } catch (FindException e) {
            throw new AuthenticationException("Couldn't search for users and/or groups", e);
        }
    }

    protected AssertionStatus checkUser(AuthenticationResult authResult, PolicyEnforcementContext context) {
        // Doesn't care
        return AssertionStatus.NONE;
    }
}
