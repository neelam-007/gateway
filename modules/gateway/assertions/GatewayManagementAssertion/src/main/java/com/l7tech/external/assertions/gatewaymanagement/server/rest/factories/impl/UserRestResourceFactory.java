package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.password.IncorrectPasswordException;
import com.l7tech.common.password.PasswordHasher;
import com.l7tech.common.password.Sha512Crypt;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.UserTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
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
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
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

    public List<UserMO> listResources(final String sortKey, final Boolean asc, @NotNull String providerId, @Nullable Map<String, List<Object>> filters) throws ResourceFactory.ResourceNotFoundException {
        try {
            UserManager userManager  = retrieveUserManager(providerId);
            List<User> users = new ArrayList<>();
            if(filters!=null && filters.containsKey("login")){
                for(Object login: filters.get("login")){
                    User user =  userManager.findByLogin(login.toString());
                    if(user!=null){
                        users.add(user);
                    }
                }
            }else{
                Collection<IdentityHeader> userHeaders = userManager.findAllHeaders();
                for(IdentityHeader idHeader: userHeaders){
                    users.add(userManager.findByPrimaryKey(idHeader.getStrId()));
                }
            }
            users = rbacAccessService.accessFilter(users, EntityType.USER, OperationType.READ, null);

            // sort list
            if (sortKey != null) {
                Collections.sort(users, new Comparator<User>() {
                    @Override
                    public int compare(User o1, User o2) {
                    if (sortKey.equals("login")) {
                        return (asc == null || asc) ? o1.getLogin().compareTo(o2.getLogin()) : o2.getLogin().compareTo(o1.getLogin());
                    }
                    if (sortKey.equals("id")) {
                        return (asc == null || asc) ? o1.getId().compareTo(o2.getId()) : o2.getId().compareTo(o1.getId());
                    }
                    return 0;
                    }
                });
            }

            return Functions.map(users, new Functions.Unary<UserMO, User>() {
                @Override
                public UserMO call(User user) {
                    return userTransformer.convertToMO(user);
                }
            });
        } catch (FindException e) {
            throw new ResourceFactory.ResourceAccessException(e.getMessage(), e);
        }
    }

    public UserMO getResource(@NotNull String providerId, @NotNull String id) throws FindException, ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        User user = getUser(providerId, id, false);
        return userTransformer.convertToMO(user);
    }

    private User getUser(String providerId, String id, boolean allowOnlyInternal) throws FindException, ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        UserManager userManager = retrieveUserManager(providerId);
        if(allowOnlyInternal && !(userManager instanceof InternalUserManager)){
            throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Not supported for non-internal identity provider.");
        }

        User user = userManager.findByPrimaryKey(id);
        if(user== null){
            throw new ResourceFactory.ResourceNotFoundException( "Resource not found: " + id);
        }
        if(allowOnlyInternal && !(user instanceof InternalUser)){
            throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Not supported for non-internal user.");
        }

        rbacAccessService.validatePermitted(user, OperationType.READ);
        return user;
    }

    private UserManager retrieveUserManager(String providerId) throws FindException, ResourceFactory.ResourceNotFoundException {
        IdentityProvider provider = identityProviderFactory.getProvider(Goid.parseGoid(providerId));

        if (provider == null)
            throw new ResourceFactory.ResourceNotFoundException("IdentityProvider not found");

        return provider.getUserManager();
    }

    public String createResource(String providerId, UserMO resource)  throws ResourceFactory.InvalidResourceException,ResourceFactory.ResourceNotFoundException{

        validateCreateResource(providerId, resource);

        try {
            UserManager userManager = retrieveUserManager(providerId);
            if(userManager instanceof InternalUserManager){
                User newUser = userTransformer.convertFromMO(resource).getEntity();
                if (newUser instanceof UserBean) newUser = userManager.reify((UserBean) newUser);
                rbacAccessService.validatePermitted(newUser, OperationType.CREATE);

                checkPasswordCompliance((InternalUserManager)userManager,(InternalUser)newUser,resource.getPassword());
                String id =  userManager.save(newUser,null);
                resource.setId(id);
                return id;
            }else{
                throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Unable to create user for non-internal identity provider.");
            }
        }catch(InvalidPasswordException invalidPassword){
            throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Unable to create user, invalid password: " +  invalidPassword.getMessage());
        }catch (ObjectModelException ome) {
            throw new ResourceFactory.ResourceAccessException("Unable to create user.", ome);
        }
    }

    private void checkPasswordCompliance(InternalUserManager userManager,InternalUser newUser, final PasswordFormatted password) throws ObjectModelException, ResourceFactory.InvalidResourceException {
        if(password == null){
            throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES, "Password required");
        }
        if(isPlainTextPassword(password.getFormat())) {
            passwordEnforcerManager.isPasswordPolicyCompliant(password.getPassword());
            // Reset password expiration and force password change
            passwordEnforcerManager.setUserPasswordPolicyAttributes(newUser, true);
            InternalUserManager internalManager = userManager;
            final InternalUserPasswordManager passwordManager = internalManager.getUserPasswordManager();
            // update user password
            if (!passwordManager.configureUserPasswordHashes(newUser, password.getPassword())) {
                throw new SaveException("Unable to save user password");
            }
        }else if(isSHA512cryptHashedPassword(password.getFormat())){
            // check password smells like
            if(!Sha512Crypt.verifyHashTextFormat(password.getPassword())){
                throw new SaveException("Unable to save user password, password is not SHA512crypt formatted");
            }
            // Reset password expiration and force password change
            passwordEnforcerManager.setUserPasswordPolicyAttributes(newUser, true);
            // update user password
            newUser.setHashedPassword(password.getPassword());
        }else{
            throw new SaveException("Invalid password format:"+ password.getFormat());
        }
    }

    private boolean isPlainTextPassword(final String format){
        return "plain".equals(format);
    }
    private boolean isSHA512cryptHashedPassword(final String format){
        return "sha512crypt".equals(format);
    }


    private void validateCreateResource(String providerId, UserMO resource) {
        if ( resource.getProviderId() != null && !StringUtils.equals(providerId, resource.getProviderId())) {
            throw new InvalidArgumentException("providerId", "Must specify the same provider ID");
        }
        if (resource.getId() != null ) {
            throw new InvalidArgumentException("id", "Must not specify an ID when creating a new entity");
        }
    }


    private void validateUpdateResource(String providerId, String id, UserMO resource) {
        if ( resource.getProviderId() != null && !StringUtils.equals(providerId, resource.getProviderId())) {
            throw new InvalidArgumentException("providerId", "Must specify the same provider ID");
        }
        if (resource.getId() != null && !StringUtils.equals(id, resource.getId())) {
            throw new InvalidArgumentException("id", "Must not specify an ID when updating a new entity, or id must equal entity id");
        }
    }

    public boolean resourceExists(String providerId, String id) {
        try {
            User user = retrieveUserManager(providerId).findByPrimaryKey(id);
            return user != null;
        }catch (ResourceFactory.ResourceNotFoundException | FindException e) {
            return false;
        }
    }

    public void updateResource(String providerId, String id, UserMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException{

        validateUpdateResource(providerId, id, resource);

        try {
            final User originalUser = getUser(providerId,id,true);
            rbacAccessService.validatePermitted(originalUser, OperationType.UPDATE);

            UserManager userManager = retrieveUserManager(providerId);
            User newUser = userTransformer.convertFromMO(resource).getEntity();
            if (newUser instanceof UserBean) newUser = userManager.reify((UserBean) newUser);
            if (newUser instanceof InternalUser){

                if (resource.getPassword()!=null) {
                    throw new UpdateException("Cannot modify existing users password using this api.");
                }

                final InternalUser originalInternalUser = (InternalUser) originalUser;
                rbacAccessService.validatePermitted(originalUser, OperationType.UPDATE);

                // update user
                final InternalUser newInternalUser = (InternalUser) newUser;
                originalInternalUser.setName(newInternalUser.getName());
                originalInternalUser.setLogin(newInternalUser.getLogin());
                originalInternalUser.setFirstName(newInternalUser.getFirstName());
                originalInternalUser.setLastName(newInternalUser.getLastName());
                originalInternalUser.setEmail(newInternalUser.getEmail());
                originalInternalUser.setDepartment(newInternalUser.getDepartment());

                userManager.update(originalInternalUser, null);
            }else{
                throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Not supported for non-internal user.");
            }
        } catch (ObjectModelException ome) {
            throw new ResourceFactory.ResourceAccessException("Unable to update user.", ome);
        }
    }

    public void deleteResource(String providerId, String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {

        try {
            UserManager userManager = retrieveUserManager(providerId);

            if(userManager instanceof InternalUserManager) {
                User user = userManager.findByPrimaryKey(id);
                if (user == null) {
                    throw new ResourceFactory.ResourceNotFoundException("Resource not found: " + id);
                }
                rbacAccessService.validatePermitted(user, OperationType.DELETE);

                if (user.equals(JaasUtils.getCurrentUser()))
                    throw new DeleteException("The currently used user cannot be deleted");

                userManager.delete(user);
                trustedEsmUserManager.deleteMappingsForUser(user);
            }else{
                throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Cannot delete non-internal users");
            }
        } catch (ObjectModelException ome) {
            throw new ResourceFactory.ResourceNotFoundException("Unable to delete user.", ome);
        }
    }

    private boolean isInternalProvider(String providerId){
        return providerId.equalsIgnoreCase(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString());
    }

    public void changePassword(String providerId, String id, String password, String format) throws ResourceFactory.ResourceNotFoundException,ResourceFactory.InvalidResourceException {
        // only allow for internal user
        if(!isInternalProvider(providerId)) throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Cannot change non internal users password.");

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

            final String hashedPassword = disconnectedUser.getHashedPassword();
            if(isPlainTextPassword(format)) {

                //check if users password is the same
                if (passwordHasher.isVerifierRecognized(hashedPassword)) {
                    try {
                        passwordHasher.verifyPassword(password.getBytes(Charsets.UTF8), hashedPassword);
                        //same error string that used to be shown in PasswordDialog
                        throw new InvalidPasswordException("The new password is the same as the old one.\nPlease enter a different password.");
                    } catch (IncorrectPasswordException e) {
                        //fall through ok - this is the expected case - the password being set is different.
                    }
                }

                passwordEnforcerManager.isPasswordPolicyCompliant(password);
                final InternalUserPasswordManager passwordManager = userManager.getUserPasswordManager();
                final boolean updateUser = passwordManager.configureUserPasswordHashes(disconnectedUser, password);
                if (!updateUser) {
                    throw new IllegalStateException("Users should require update.");
                }
            }else if(isSHA512cryptHashedPassword(format)){
                if(!Sha512Crypt.verifyHashTextFormat(password)){
                    throw new InvalidPasswordException("Password is not SHA512crypt formatted");
                }
                //check if users password is the same
                if (hashedPassword.equals(password)) {
                    throw new InvalidPasswordException("The new password is the same as the old one.\nPlease enter a different password.");
                }
                disconnectedUser.setHashedPassword(password);
            }else{
                throw new InvalidPasswordException("Invalid password format:"+format);
            }
            passwordEnforcerManager.setUserPasswordPolicyAttributes(disconnectedUser, true);
            logger.info("Updated password for Internal User " + disconnectedUser.getLogin() + " [" + disconnectedUser.getGoid() + "]");
            // activate user
            activateUser(disconnectedUser);

            userManager.update(disconnectedUser);
        } catch(InvalidPasswordException invalidPassword){
            throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Unable to change user password, invalid password: " +  invalidPassword.getMessage());
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

    public X509Certificate setCertificate(String providerId, String id, CertificateData certificateData) throws ResourceFactory.ResourceNotFoundException, ObjectModelException, ResourceFactory.InvalidResourceException {
        User user = getUser(providerId, id, true);
        try {
            final Certificate certificate = CertUtils.getFactory().generateCertificate(
                    new ByteArrayInputStream( getEncoded(certificateData) ) );

            if ( !(certificate instanceof X509Certificate) )
                throw new ResourceFactory.InvalidResourceException( ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "unexpected encoded certificate type");

            final X509Certificate x509Certificate = (X509Certificate) certificate;
            // check common name is same as login
            String certCn = CertUtils.extractFirstCommonNameFromCertificate(x509Certificate);
            if(!certCn.equals(user.getLogin())){
                throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,"Certificate subject name ("+certCn+")does not match user login");
            }
            clientCertManager.recordNewUserCert(user,x509Certificate , false);
            return (X509Certificate) clientCertManager.getUserCert(user);
        } catch (CertificateException e) {
            throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Error importing certificate:"+e.getMessage());
        }
    }

    private byte[] getEncoded( final CertificateData certificateData ) throws ResourceFactory.InvalidResourceException {
        if ( certificateData == null || certificateData.getEncoded().length == 0 ) {
            throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES, "encoded certificate data");
        }
        return certificateData.getEncoded();
    }

    public void revokeCertificate(String providerId, String id) throws ResourceFactory.ResourceNotFoundException, ObjectModelException, ResourceFactory.InvalidResourceException {
        User user = getUser(providerId, id, true);
        clientCertManager.revokeUserCert(user);
    }

    public X509Certificate getCertificate(String providerId, String id) throws ResourceFactory.ResourceNotFoundException, ObjectModelException, ResourceFactory.InvalidResourceException {
        User user = getUser(providerId, id, true);
        Certificate userCert = clientCertManager.getUserCert(user);
        if(userCert == null){
            throw new ResourceFactory.ResourceNotFoundException("User certificate not found: " + id);
        }
        return (X509Certificate) userCert;
    }
}
