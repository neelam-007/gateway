package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.common.password.IncorrectPasswordException;
import com.l7tech.common.password.PasswordHasher;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.UserTransformer;
import com.l7tech.gateway.api.UserMO;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.TrustedEsmUserManager;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.internal.InternalUserManager;
import com.l7tech.server.identity.internal.InternalUserPasswordManager;
import com.l7tech.server.logon.LogonInfoManager;
import com.l7tech.server.security.PasswordEnforcerManager;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.Charsets;
import com.l7tech.util.Functions;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 */
@Component
public class UserRestResourceFactory {

    private final Logger logger = Logger.getLogger(getClass().getName());

    @Inject
    private IdentityProviderFactory identityProviderFactory;

    @Inject
    private UserTransformer userTransformer;

    @Inject
    private TrustedEsmUserManager trustedEsmUserManager;

    @Inject
    private TrustedCertManager trustedCertManager;

    @Inject
    private PasswordEnforcerManager passwordEnforcerManager;

    @Inject
    private PasswordHasher passwordHasher;

    @Inject
    private LogonInfoManager logonManager;

    @Inject
    private RbacAccessService rbacAccessService;

    @Inject
    private ClientCertManager clientCertManager;

    public List<UserMO> listResources(@NotNull String providerId, @Nullable Map<String, List<Object>> filters) {
        try {
            UserManager userManager  = retrieveUserManager(providerId);
            List<IdentityHeader> users = new ArrayList<>();
            if(filters.containsKey("login")){
                for(Object login: filters.get("login")){
                    users.add(userManager.userToHeader(userManager.findByLogin(login.toString())));
                }
            }else if(filters.containsKey("id")){
                for(Object id: filters.get("id")){
                    users.add(userManager.userToHeader(userManager.findByPrimaryKey(id.toString())));
                }
            }else{
                users.addAll(userManager.findAllHeaders());
            }
            users = rbacAccessService.accessFilter(users, EntityType.USER, OperationType.READ, null);
            return Functions.map(users, new Functions.Unary<UserMO, IdentityHeader>() {
                @Override
                public UserMO call(IdentityHeader userHeader) {
                    if(userHeader.getType().equals(EntityType.USER))
                    {
                        return userTransformer.convertToMO(userHeader);
                    }
                    return null;
                }
            });
        } catch (FindException e) {
            throw new ResourceFactory.ResourceAccessException(e.getMessage(), e);
        }
    }

    public UserMO getResource(@NotNull String providerId, @NotNull String login) throws FindException, ResourceFactory.ResourceNotFoundException {
        User user = getUser(providerId, login);
        return userTransformer.convertToMO(user);
    }

    private User getUser(String providerId, String login) throws FindException, ResourceFactory.ResourceNotFoundException {
        User user = retrieveUserManager(providerId).findByPrimaryKey(login);
        if(user== null){
            throw new ResourceFactory.ResourceNotFoundException( "Resource not found: " + login);
        }
        rbacAccessService.validatePermitted(user, OperationType.READ);
        return user;
    }

    private UserManager retrieveUserManager(String providerId) throws FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(Goid.parseGoid(providerId));

        if (provider == null)
            throw new FindException("IdentityProvider could not be found");

        return provider.getUserManager();
    }

    public String createResource(String providerId, UserMO resource)  throws ResourceFactory.InvalidResourceException{
        // only allow for internal user
        if(!isInternalProvider(providerId)) throw new NotImplementedException();
        validateCreateResource(providerId, null, resource);

        try {
            UserManager userManager = retrieveUserManager(providerId);
            User newUser = userTransformer.convertFromMO(resource).getEntity();
            if (newUser instanceof UserBean) newUser = userManager.reify((UserBean) newUser);
            rbacAccessService.validatePermitted(newUser, OperationType.CREATE);

            if(userManager instanceof InternalUserManager && newUser instanceof  InternalUser){
                checkPasswordCompliance((InternalUserManager)userManager,(InternalUser)newUser,resource.getPassword());
                String id =  userManager.save(newUser,null);
                resource.setId(id);
                return id;
            }else{
                throw new ResourceFactory.ResourceAccessException("Unable to create user for non-internal identity provider.");
            }
        } catch (ObjectModelException ome) {
            throw new ResourceFactory.ResourceAccessException("Unable to create user.", ome);
        }
    }

    private void checkPasswordCompliance(InternalUserManager userManager,InternalUser newUser, String clearTextPassword) throws ObjectModelException {
        passwordEnforcerManager.isPasswordPolicyCompliant(clearTextPassword);
        // Reset password expiration and force password change
        passwordEnforcerManager.setUserPasswordPolicyAttributes(newUser, true);
        InternalUserManager internalManager = userManager;
        final InternalUserPasswordManager passwordManager = internalManager.getUserPasswordManager();
        if( !passwordManager.configureUserPasswordHashes(newUser, clearTextPassword)){
            throw new SaveException("Unable to save user password");
        }
    }

    private void validateCreateResource(String providerId, String id, UserMO resource) {
        if ( resource.getProviderId() != null && !StringUtils.equals(providerId, resource.getProviderId())) {
            throw new IllegalArgumentException("Must specify the same provider ID");
        }
        if (resource.getId() != null && !StringUtils.equals(id, resource.getId())) {
            throw new IllegalArgumentException("Must not specify an ID when creating a new entity, or id must equal new entity id");
        }
    }


    private void validateUpdateResource(String providerId, String id, UserMO resource) {
        if ( resource.getProviderId() != null && !StringUtils.equals(providerId, resource.getProviderId())) {
            throw new IllegalArgumentException("Must specify the same provider ID");
        }
        if (resource.getId() != null && !StringUtils.equals(id, resource.getId())) {
            throw new IllegalArgumentException("Must not specify an ID when updating a new entity, or id must equal entity id");
        }
    }

    public boolean resourceExists(String providerId, String id) {
        try {
            User user = retrieveUserManager(providerId).findByPrimaryKey(id);
            return user != null;
        } catch (FindException e) {
            return false;
        }
    }

    public void updateResource(String providerId, String id, UserMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException{
        // only allow for internal user
        if(!isInternalProvider(providerId)) throw new NotImplementedException("Cannot update non internal users");

        validateUpdateResource(providerId, id, resource);

        try {
            UserManager userManager = retrieveUserManager(providerId);
            User newUser = userTransformer.convertFromMO(resource).getEntity();
            if (newUser instanceof UserBean) newUser = userManager.reify((UserBean) newUser);
            if (newUser instanceof InternalUser){

                if (resource.getPassword()!=null) {
                    throw new UpdateException("Cannot modify existing users password using this api.");
                }

                final InternalUser originalUser = (InternalUser) userManager.findByPrimaryKey(id);
                rbacAccessService.validatePermitted(originalUser, OperationType.UPDATE);

                // update user
                final InternalUser newInternalUser = (InternalUser) newUser;
                originalUser.setName(newInternalUser.getName());
                originalUser.setLogin(newInternalUser.getLogin());
                originalUser.setFirstName(newInternalUser.getFirstName());
                originalUser.setLastName(newInternalUser.getLastName());
                originalUser.setEmail(newInternalUser.getEmail());
                originalUser.setDepartment(newInternalUser.getDepartment());

                userManager.update(originalUser, null);
            }else{
                throw new UpdateException("Cannot update non internal users");
            }
        } catch (ObjectModelException ome) {
            throw new ResourceFactory.ResourceAccessException("Unable to update user.", ome);
        }
    }


    public void createResource(String providerId, String id, UserMO resource) throws ResourceFactory.InvalidResourceException{
        // only allow for internal user
        if(!isInternalProvider(providerId)) throw new NotImplementedException("Cannot create non internal users");

        throw new NotImplementedException("Cannot create user with specified ID"); // todo requires core change
    }

    public void deleteResource(String providerId, String id) throws ResourceFactory.ResourceNotFoundException{
        // only allow for internal user
        if(!isInternalProvider(providerId)) throw new NotImplementedException("Cannot delete non internal users");

        try {
            UserManager userManager = retrieveUserManager(providerId);
            User user = userManager.findByPrimaryKey(id);
            rbacAccessService.validatePermitted(user, OperationType.DELETE);

            if (user.equals(JaasUtils.getCurrentUser()))
                throw new DeleteException("The currently used user cannot be deleted");
            userManager.delete(user);
            trustedEsmUserManager.deleteMappingsForUser(user);
        } catch (ObjectModelException ome) {
            throw new ResourceFactory.ResourceNotFoundException("Unable to delete user.", ome);
        }
    }

    private boolean isInternalProvider(String providerId){
        return providerId.equalsIgnoreCase(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString());
    }

    public void changePassword(String providerId, String id, String newClearTextPassword) throws ResourceFactory.ResourceNotFoundException {
        // only allow for internal user
        if(!isInternalProvider(providerId)) throw new NotImplementedException("Cannot change non internal users password.");

        try{
            InternalUserManager userManager = (InternalUserManager) retrieveUserManager(providerId);
            final InternalUser disconnectedUser = new InternalUser();
            {//limit session connected internal user scope
                //were ignoring the incoming entity, were just using it for rbac and a container for it's id and provider id
                final InternalUser internalUser = userManager.findByPrimaryKey(id);
                rbacAccessService.validatePermitted(internalUser, OperationType.UPDATE);
                disconnectedUser.copyFrom(internalUser);
                disconnectedUser.setVersion(internalUser.getVersion());
            }

            //check if users password is the same
            final String hashedPassword = disconnectedUser.getHashedPassword();

            if (passwordHasher.isVerifierRecognized(hashedPassword)) {
                try {
                    passwordHasher.verifyPassword(newClearTextPassword.getBytes(Charsets.UTF8), hashedPassword);
                    //same error string that used to be shown in PasswordDialog
                    throw new InvalidPasswordException("The new password is the same as the old one.\nPlease enter a different password.");
                } catch (IncorrectPasswordException e) {
                    //fall through ok - this is the expected case - the password being set is different.
                }
            }

            passwordEnforcerManager.isPasswordPolicyCompliant(newClearTextPassword);
            final InternalUserPasswordManager passwordManager = userManager.getUserPasswordManager();
            final boolean updateUser = passwordManager.configureUserPasswordHashes(disconnectedUser, newClearTextPassword);
            if (!updateUser) {
                throw new IllegalStateException("Users should require update.");
            }
            passwordEnforcerManager.setUserPasswordPolicyAttributes(disconnectedUser, true);
            logger.info("Updated password for Internal User " + disconnectedUser.getLogin() + " [" + disconnectedUser.getGoid() + "]");
            // activate user
            activateUser(disconnectedUser);

            userManager.update(disconnectedUser);
        } catch (ObjectModelException ome) {
            throw new ResourceFactory.ResourceAccessException("Unable to change user password.", ome);
        }
    }

    private void activateUser(User user) throws FindException, UpdateException {
        try {
            LogonInfo info = logonManager.findByCompositeKey(user.getProviderId(), user.getLogin(), false);
            // if user has logged in before
            if (info != null && info.getLastAttempted() > 0) {
                String action = null;
                info.setLastActivity(System.currentTimeMillis());
                info.resetFailCount(System.currentTimeMillis());
                if (info.getState().equals(LogonInfo.State.INACTIVE))
                    action = "activated";
                else if (info.getState().equals(LogonInfo.State.EXCEED_ATTEMPT))
                    action = "unlocked";
                info.setState(LogonInfo.State.ACTIVE);
                logonManager.update(info);
                if (action != null) {
                    logger.info("User '" + user.getLogin() + "' is " + action);
                }
            }

        } catch (FindException e) {
            throw new FindException("No logon info for '" + user.getLogin() + "'", e);
        } catch (UpdateException e) {
            throw new UpdateException("No logon info for '" + user.getLogin() + "'", e);
        }
    }

    public X509Certificate setCertificate(String providerId, String id, String certificateId) throws ResourceFactory.ResourceNotFoundException, ObjectModelException, ResourceFactory.InvalidResourceException {
        User user = getUser(providerId,id);
        TrustedCert cert =  trustedCertManager.findByPrimaryKey(Goid.parseGoid(certificateId));
        if(cert == null){
            throw new ResourceFactory.ResourceNotFoundException("Certificate not found: " + id);
        }
        rbacAccessService.validatePermitted(cert, OperationType.READ);
        clientCertManager.recordNewUserCert(user, cert.getCertificate(), false);
        return (X509Certificate) clientCertManager.getUserCert(user);
    }

    public void revokeCertificate(String providerId, String id) throws ResourceFactory.ResourceNotFoundException, ObjectModelException{
        User user = getUser(providerId, id);
        clientCertManager.revokeUserCert(user);
    }

    public X509Certificate getCertificate(String providerId, String id)throws ResourceFactory.ResourceNotFoundException, ObjectModelException {
        User user = getUser(providerId, id);
        Certificate userCert = clientCertManager.getUserCert(user);
        if(userCert == null){
            throw new ResourceFactory.ResourceNotFoundException("User certificate not found: " + id);
        }
        return (X509Certificate) userCert;
    }
}
