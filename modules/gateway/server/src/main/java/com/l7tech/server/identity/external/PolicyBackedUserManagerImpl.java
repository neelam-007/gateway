package com.l7tech.server.identity.external;

import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.external.PolicyBackedIdentityProviderConfig;
import com.l7tech.identity.external.VirtualPolicyUser;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;

import javax.inject.Inject;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class PolicyBackedUserManagerImpl implements PolicyBackedUserManager {
    private static final Logger logger = Logger.getLogger(PolicyBackedUserManagerImpl.class.getName());

    private PolicyBackedIdentityProviderConfig identityProviderConfig;

    @Inject
    private PolicyCache policyCache;

    @Override
    public VirtualPolicyUser findByPrimaryKey(String identifier) throws FindException {
        return findByLogin(identifier);
    }

    @Override
    public VirtualPolicyUser findByLogin(String login) throws FindException {
        return makeUser(login);
    }

    @Override
    public void delete(VirtualPolicyUser user) throws DeleteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String identifier) throws DeleteException, FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll(Goid ipoid) throws DeleteException, FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(VirtualPolicyUser user) throws UpdateException, FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String save(VirtualPolicyUser user, Set<IdentityHeader> groupHeaders) throws SaveException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String save(Goid id, VirtualPolicyUser user, Set<IdentityHeader> groupHeaders) throws SaveException {
        throw new UnsupportedOperationException();
    }

    @Override
    public VirtualPolicyUser reify(UserBean bean) {
        if (!bean.getProviderId().equals(identityProviderConfig.getGoid()))
            throw new IllegalArgumentException("User bean does not belong to this provider");

        VirtualPolicyUser user = makeUser(bean.getLogin());
        user.setProviderId(bean.getProviderId());
        user.setLogin(bean.getLogin());
        return user;
    }

    @Override
    public void update(VirtualPolicyUser user, Set<IdentityHeader> groupHeaders) throws UpdateException, FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityHeaderSet<IdentityHeader> search(String searchString) throws FindException {
        return new EntityHeaderSet<>();
    }

    @Override
    public IdentityHeader userToHeader(VirtualPolicyUser user) {
        return new IdentityHeader(user.getProviderId(), user.getId(), EntityType.USER, user.getLogin(), user.getName(), null, null);
    }

    @Override
    public VirtualPolicyUser headerToUser(IdentityHeader header) {
        VirtualPolicyUser user = makeUser(header.getStrId());
        user.setProviderId(identityProviderConfig.getGoid());
        user.setLogin(header.getStrId());
        return user;
    }

    @Override
    public Class<? extends User> getImpClass() {
        return LdapUser.class;
    }

    @Override
    public EntityHeaderSet<IdentityHeader> findAllHeaders() throws FindException {
        return new EntityHeaderSet<>();
    }

    @Override
    public void configure(PolicyBackedIdentityProvider provider) {
        this.identityProviderConfig = (PolicyBackedIdentityProviderConfig)provider.getConfig();
    }

    @Override
    public AuthenticationResult authenticatePasswordCredentials(LoginCredentials pc) throws BadCredentialsException {
        // TODO use the policy backed authenticator

        Goid policyGoid = identityProviderConfig.getPolicyId();

        Message request = new Message();
        Message response = new Message();
        PolicyEnforcementContext context = null;
        try {
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( request, response );

            // TODO we should find a better way to send the credentials into the backing policy
            context.setVariable("idp.userid", pc.getLogin());
            context.setVariable("idp.password", new String(pc.getCredentials()));

            AssertionStatus status = executePolicy(policyGoid, context);

            final String login = pc.getLogin();
            if (!AssertionStatus.NONE.equals(status)) {
                final String msg = "backing policy failed with status \"" + status.getMessage() + "\" for login " + login;
                logger.info(msg);
                throw new BadCredentialsException(msg);
            }

            // TODO we should probably grab the authetnicated user from the policy PEC, instead of just ginning up a fake one here
            return new AuthenticationResult(makeUser(login), pc.getSecurityTokens());
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    private AssertionStatus executePolicy(Goid policyGoid, PolicyEnforcementContext context) {
        try (ServerPolicyHandle sph = policyCache.getServerPolicy(policyGoid)) {
            if (sph == null) {
                logger.log(Level.WARNING, "Unable to authenticate with policy-backed identity provider #{0} -- no policy with ID #{1} is present in policy cache (invalid policy?)",
                    new Object[] { identityProviderConfig.getGoid(), policyGoid });
                return AssertionStatus.SERVER_ERROR;
            }
            return sph.checkRequest(context);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to authenticate with policy-backed identity provider #" + identityProviderConfig.getGoid() +
                " -- error while executing policy: " + ExceptionUtils.getMessage(e), e);
            return AssertionStatus.SERVER_ERROR;
        }
    }

    private VirtualPolicyUser makeUser(String login) {
        return new VirtualPolicyUser(identityProviderConfig.getGoid(), login);
    }
}
