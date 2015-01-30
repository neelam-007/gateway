package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.common.password.PasswordHasher;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.UserRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.bundling.UserContainer;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.security.cert.X509Certificate;
import java.text.ParseException;

@Component
public class UserTransformer implements EntityAPITransformer<UserMO, User> {

    @Inject
    private IdentityProviderFactory identityProviderFactory;

    @Inject
    private UserRestResourceFactory userRestResourceFactory;

    @Inject
    private PasswordHasher passwordHasher;

    @Inject
    private CertificateTransformer certTransformer;

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.USER.toString();
    }

    @NotNull
    public UserMO convertToMO(@NotNull User user) {
        return convertToMO(user, null);
    }

    @NotNull
    @Override
    public UserMO convertToMO(@NotNull EntityContainer<User> userEntityContainer,  SecretsEncryptor secretsEncryptor) {
        return convertToMO(userEntityContainer.getEntity(), secretsEncryptor);
    }

    @NotNull
    @Override
    public UserMO convertToMO(@NotNull User user,  SecretsEncryptor secretsEncryptor) {
        return convertToMO(user, secretsEncryptor, false);
    }

    @NotNull
    public UserMO convertToMO(@NotNull User user,  SecretsEncryptor secretsEncryptor, final boolean includeUserCert) {
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setId(user.getId());
        userMO.setLogin(user.getLogin());
        userMO.setProviderId(user.getProviderId().toString());
        userMO.setFirstName(user.getFirstName());
        userMO.setLastName(user.getLastName());
        userMO.setEmail(user.getEmail());
        userMO.setDepartment(user.getDepartment());
        userMO.setSubjectDn(user.getSubjectDn());
        userMO.setName(user.getName());

        //set the encrypted password
        if (secretsEncryptor != null && user instanceof InternalUser && ((InternalUser) user).getHashedPassword() != null) {
            // encrypt password.
            PasswordFormatted formattedPassword = ManagedObjectFactory.createPasswordFormatted();
            formattedPassword.setFormat("bundleKey:sha512crypt");
            formattedPassword.setBundleKey(secretsEncryptor.getWrappedBundleKey());
            formattedPassword.setPassword(secretsEncryptor.encryptSecret(((InternalUser) user).getHashedPassword().getBytes(Charsets.UTF8)));
            userMO.setPassword(formattedPassword);
        }

        if(includeUserCert) {
            X509Certificate x509Certificate = null;
            try {
                x509Certificate = userRestResourceFactory.getCertificate(user.getProviderId().toString(), user.getId());
            } catch (ResourceFactory.ResourceNotFoundException | ResourceFactory.InvalidResourceException | ObjectModelException e) {
                //let it fall through, the certificate won't be included.
            }
            if(x509Certificate != null) {
                userMO.setCertificateData(certTransformer.getCertData(x509Certificate));
            }
        }

        return userMO;
    }

    @NotNull
    @Override
    public EntityContainer<User> convertFromMO(@NotNull UserMO userMO, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(userMO,true, secretsEncryptor);
    }

    @NotNull
    @Override
    public EntityContainer<User> convertFromMO(@NotNull UserMO userMO, boolean strict, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        UserBean user = new UserBean();
        user.setUniqueIdentifier(userMO.getId());
        user.setLogin(userMO.getLogin());
        user.setName(userMO.getName());

        Goid identityProviderGoid = Goid.parseGoid(userMO.getProviderId());
        try {
            identityProviderFactory.getProvider(identityProviderGoid);
        } catch (FindException e) {
            if(strict){
                throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES, "Cannot find identity provider with id: "+e.getMessage());
            }
        }
        user.setProviderId(identityProviderGoid);
        user.setFirstName(userMO.getFirstName());
        user.setLastName(userMO.getLastName());
        user.setEmail(userMO.getEmail());
        user.setDepartment(userMO.getDepartment());
        user.setSubjectDn(userMO.getSubjectDn());

        //if a password is set on the user need to set it on the user bean
        if (userMO.getPassword() != null) {
            if ("plain".equals(userMO.getPassword().getFormat())) {
                user.setHashedPassword(passwordHasher.hashPassword(userMO.getPassword().getPassword().getBytes(Charsets.UTF8)));
            } else if ("sha512crypt".equals(userMO.getPassword().getFormat())) {
                if (passwordHasher.isVerifierRecognized(userMO.getPassword().getPassword())) {
                    user.setHashedPassword(userMO.getPassword().getPassword());
                } else {
                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "The user hashed password is not recognizable.");
                }
            } else if ("bundleKey:plain".equals(userMO.getPassword().getFormat())) {
                try {
                    user.setHashedPassword(passwordHasher.hashPassword(secretsEncryptor.decryptSecret(userMO.getPassword().getPassword(), userMO.getPassword().getBundleKey())));
                } catch (ParseException e) {
                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "The user encrypted password in not valid. Message: " + ExceptionUtils.getMessage(e));
                }
            } else if ("bundleKey:sha512crypt".equals(userMO.getPassword().getFormat())) {
                String hashedPassword;
                try {
                    hashedPassword = new String(secretsEncryptor.decryptSecret(userMO.getPassword().getPassword(), userMO.getPassword().getBundleKey()), Charsets.UTF8);
                } catch (ParseException e) {
                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "The user encrypted password in not valid. Message: " + ExceptionUtils.getMessage(e));
                }
                //An empty hashed password is valid for a user
                if (hashedPassword.isEmpty() || passwordHasher.isVerifierRecognized(hashedPassword)) {
                    user.setHashedPassword(hashedPassword);
                } else {
                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "The user hashed password is not recognizable.");
                }
            } else {
                throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Unrecognized password format: " + userMO.getPassword().getFormat() + ". Expected one of: plain, sha512crypt, bundleKey:plain, bundleKey:sha512crypt");
            }
        }

        final CertificateData certificateData = userMO.getCertificateData();
        return new UserContainer(user, certificateData != null ? certTransformer.getX509Certificate(certificateData) : null);
    }

    @NotNull
    @Override
    public Item<UserMO> convertToItem(@NotNull UserMO m) {
        return new ItemBuilder<UserMO>(m.getLogin(), m.getId(), EntityType.USER.name())
                .setContent(m)
                .build();
    }

    public UserMO convertToMO(IdentityHeader userHeader){
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setId(userHeader.getStrId());
        userMO.setLogin(userHeader.getName());
        userMO.setFirstName(userHeader.getCommonName());
        userMO.setLastName(userHeader.getDescription());
        userMO.setProviderId(userHeader.getProviderGoid().toString());
        return userMO;
    }
}
