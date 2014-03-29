package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.HttpConfigurationMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.HttpProxyConfiguration;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.*;
import com.l7tech.server.globalresources.HttpConfigurationManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * This is the resource factory for Http configurations
 *
 * @author Victor Kazakov
 */
@ResourceFactory.ResourceType(type = HttpConfigurationMO.class)
public class HttpConfigurationResourceFactory extends SecurityZoneableEntityManagerResourceFactory<HttpConfigurationMO, HttpConfiguration, EntityHeader> {
    private final SecurePasswordManager securePasswordManager;

    public HttpConfigurationResourceFactory(final RbacServices services,
                                            final SecurityFilter securityFilter,
                                            final PlatformTransactionManager transactionManager,
                                            final HttpConfigurationManager httpConfigurationManager,
                                            final SecurePasswordManager securePasswordManager,
                                            final SecurityZoneManager securityZoneManager) {
        super(false, false, services, securityFilter, transactionManager, httpConfigurationManager, securityZoneManager);
        this.securePasswordManager = securePasswordManager;
    }

    /**
     * Converts from an HttpConfiguration to an HttpConfigurationMO
     */
    @Override
    public HttpConfigurationMO asResource(final HttpConfiguration httpConfiguration) {
        HttpConfigurationMO httpConfigurationMO = ManagedObjectFactory.createHttpConfiguration();

        httpConfigurationMO.setId(httpConfiguration.getId());
        httpConfigurationMO.setVersion(httpConfiguration.getVersion());
        httpConfigurationMO.setHost(httpConfiguration.getHost());
        httpConfigurationMO.setPort(httpConfiguration.getPort());
        httpConfigurationMO.setProtocol(httpConfiguration.getProtocol() == null ? null : HttpConfigurationMO.Protocol.valueOf(httpConfiguration.getProtocol().toString()));
        httpConfigurationMO.setPath(httpConfiguration.getPath());
        httpConfigurationMO.setUsername(httpConfiguration.getUsername());
        httpConfigurationMO.setPasswordId(httpConfiguration.getPasswordGoid() == null ? null : httpConfiguration.getPasswordGoid().toString());
        httpConfigurationMO.setNtlmHost(httpConfiguration.getNtlmHost());
        httpConfigurationMO.setNtlmDomain(httpConfiguration.getNtlmDomain());
        httpConfigurationMO.setTlsVersion(httpConfiguration.getTlsVersion());
        httpConfigurationMO.setTlsKeyUse(httpConfiguration.getTlsKeyUse() == null ? null : HttpConfigurationMO.Option.valueOf(httpConfiguration.getTlsKeyUse().toString()));
        //The default keystore id is 0. in this return null as it means no keystore id had been set.
        httpConfigurationMO.setTlsKeystoreId((httpConfiguration.getTlsKeystoreGoid() == null || Goid.equals(new Goid(0, 0), httpConfiguration.getTlsKeystoreGoid())) ? null : httpConfiguration.getTlsKeystoreGoid().toString());
        httpConfigurationMO.setTlsKeystoreAlias(httpConfiguration.getTlsKeystoreAlias());
        httpConfigurationMO.setTlsCipherSuites(httpConfiguration.getTlsCipherSuites());
        httpConfigurationMO.setConnectTimeout(httpConfiguration.getConnectTimeout());
        httpConfigurationMO.setReadTimeout(httpConfiguration.getReadTimeout());
        httpConfigurationMO.setFollowRedirects(httpConfiguration.isFollowRedirects());
        if (httpConfiguration.getProxyUse() != null) {
            httpConfigurationMO.setProxyUse(HttpConfigurationMO.Option.valueOf(httpConfiguration.getProxyUse().toString()));
            httpConfigurationMO.setProxyConfiguration(HttpConfiguration.Option.CUSTOM.equals(httpConfiguration.getProxyUse()) ? convertProxyAsResource(httpConfiguration.getProxyConfiguration()) : null);
        } else {
            httpConfigurationMO.setProxyUse(null);
            httpConfigurationMO.setProxyConfiguration(null);
        }

        // handle SecurityZone
        doSecurityZoneAsResource(httpConfigurationMO, httpConfiguration);

        return httpConfigurationMO;
    }

    private HttpConfigurationMO.HttpProxyConfiguration convertProxyAsResource(HttpProxyConfiguration proxyConfiguration) {
        if (proxyConfiguration == null)
            return null;
        HttpConfigurationMO.HttpProxyConfiguration httpProxyConfiguration = new HttpConfigurationMO.HttpProxyConfiguration();
        httpProxyConfiguration.setHost(proxyConfiguration.getHost());
        httpProxyConfiguration.setPort(proxyConfiguration.getPort());
        httpProxyConfiguration.setUsername(proxyConfiguration.getUsername());
        httpProxyConfiguration.setPasswordId(proxyConfiguration.getPasswordGoid() == null ? null : proxyConfiguration.getPasswordGoid().toString());
        return httpProxyConfiguration;
    }

    @Override
    public HttpConfiguration fromResource(final Object resource, boolean strict) throws InvalidResourceException {
        if (!(resource instanceof HttpConfigurationMO))
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected http configuration");

        final HttpConfigurationMO httpConfigurationMO = (HttpConfigurationMO) resource;

        final HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setHost(trimNullable(httpConfigurationMO.getHost()));
        httpConfiguration.setPort(httpConfigurationMO.getPort());
        httpConfiguration.setProtocol(httpConfigurationMO.getProtocol() == null || HttpConfigurationMO.Protocol.ANY.equals(httpConfigurationMO.getProtocol()) ? null : HttpConfiguration.Protocol.valueOf(httpConfigurationMO.getProtocol().toString()));
        httpConfiguration.setPath(trimNullable(httpConfigurationMO.getPath()));
        httpConfiguration.setUsername(trimNullable(httpConfigurationMO.getUsername()));
        httpConfiguration.setPasswordGoid(httpConfigurationMO.getPasswordId() == null ? null : validatePassword(httpConfigurationMO.getPasswordId(), strict));
        httpConfiguration.setNtlmHost(trimNullable(httpConfigurationMO.getNtlmHost()));
        httpConfiguration.setNtlmDomain(trimNullable(httpConfigurationMO.getNtlmDomain()));
        httpConfiguration.setTlsVersion(httpConfigurationMO.getTlsVersion());
        httpConfiguration.setTlsKeyUse(httpConfigurationMO.getTlsKeyUse() == null ? null : HttpConfiguration.Option.valueOf(httpConfigurationMO.getTlsKeyUse().toString()));
        //if the keystore id is null we must set it to the default 0, otherwise we get data integrity errors.
        httpConfiguration.setTlsKeystoreGoid(httpConfigurationMO.getTlsKeystoreId() == null ? new Goid(0, 0) : Goid.parseGoid(httpConfigurationMO.getTlsKeystoreId()));
        httpConfiguration.setTlsKeystoreAlias(httpConfigurationMO.getTlsKeystoreAlias());
        httpConfiguration.setTlsCipherSuites(httpConfigurationMO.getTlsCipherSuites());
        httpConfiguration.setConnectTimeout(httpConfigurationMO.getConnectTimeout());
        httpConfiguration.setReadTimeout(httpConfigurationMO.getReadTimeout());
        httpConfiguration.setFollowRedirects(httpConfigurationMO.isFollowRedirects());
        httpConfiguration.setProxyUse(httpConfigurationMO.getProxyUse() == null ? null : HttpConfiguration.Option.valueOf(httpConfigurationMO.getProxyUse().toString()));
        httpConfiguration.setProxyConfiguration(httpConfigurationMO.getProxyConfiguration() == null ? new HttpProxyConfiguration() : convertProxyFromResource(httpConfigurationMO.getProxyConfiguration(), strict));

        // handle SecurityZone
        doSecurityZoneFromResource(httpConfigurationMO, httpConfiguration, strict);

        return httpConfiguration;
    }

    private HttpProxyConfiguration convertProxyFromResource(HttpConfigurationMO.HttpProxyConfiguration proxyConfigurationMO, boolean strict) throws InvalidResourceException {
        if (proxyConfigurationMO == null)
            return null;
        HttpProxyConfiguration httpProxyConfiguration = new HttpProxyConfiguration();
        httpProxyConfiguration.setHost(trimNullable(proxyConfigurationMO.getHost()));
        httpProxyConfiguration.setPort(proxyConfigurationMO.getPort());
        httpProxyConfiguration.setUsername(trimNullable(proxyConfigurationMO.getUsername()));
        httpProxyConfiguration.setPasswordGoid(proxyConfigurationMO.getPasswordId() == null ? null : validatePassword(proxyConfigurationMO.getPasswordId(), strict));
        return httpProxyConfiguration;
    }

    /**
     * Validates a password string id. This will check that it is a proper goid. and it will thrown an exception if the
     * password does not exist and strict is true
     *
     * @param passwordId The password id to validate
     * @param strict     Weather to throw an error if the password exists.
     * @return The password goid.
     * @throws InvalidResourceException
     */
    private Goid validatePassword(@NotNull final String passwordId, final boolean strict) throws InvalidResourceException {
        final Goid passwordGoid;
        try {
            //validate that the id is a proper goid
            passwordGoid = Goid.parseGoid(passwordId);
        } catch (IllegalArgumentException e) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "Invalid password id: " + e.getMessage());
        }
        if (strict) {
            //check if the password exists
            SecurePassword password;
            try {
                password = securePasswordManager.findByPrimaryKey(passwordGoid);
            } catch (FindException e) {
                throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "Cannot find password with ID " + passwordGoid);
            }
            if (password == null) {
                throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "Cannot find password with ID " + passwordGoid);
            }
        }
        return passwordGoid;
    }

    private static String trimNullable(@Nullable String str) {
        return str == null ? null : str.trim();
    }

    @Override
    protected void updateEntity(final HttpConfiguration oldEntity, final HttpConfiguration newEntity) throws InvalidResourceException {
        oldEntity.setHost(newEntity.getHost());
        oldEntity.setPort(newEntity.getPort());
        oldEntity.setProtocol(newEntity.getProtocol());
        oldEntity.setPath(newEntity.getPath());
        oldEntity.setUsername(newEntity.getUsername());
        oldEntity.setPasswordGoid(newEntity.getPasswordGoid());
        oldEntity.setNtlmHost(newEntity.getNtlmHost());
        oldEntity.setNtlmDomain(newEntity.getNtlmDomain());
        oldEntity.setTlsVersion(newEntity.getTlsVersion());
        oldEntity.setTlsKeyUse(newEntity.getTlsKeyUse());
        oldEntity.setTlsKeystoreGoid(newEntity.getTlsKeystoreGoid());
        oldEntity.setTlsKeystoreAlias(newEntity.getTlsKeystoreAlias());
        oldEntity.setTlsCipherSuites(newEntity.getTlsCipherSuites());
        oldEntity.setConnectTimeout(newEntity.getConnectTimeout());
        oldEntity.setReadTimeout(newEntity.getReadTimeout());
        oldEntity.setFollowRedirects(newEntity.isFollowRedirects());
        oldEntity.setProxyUse(newEntity.getProxyUse());

        HttpProxyConfiguration oldEntityProxyConfiguration = oldEntity.getProxyConfiguration();
        HttpProxyConfiguration newEntityProxyConfiguration = newEntity.getProxyConfiguration();

        oldEntityProxyConfiguration.setHost(newEntityProxyConfiguration.getHost());
        oldEntityProxyConfiguration.setPort(newEntityProxyConfiguration.getPort());
        oldEntityProxyConfiguration.setUsername(newEntityProxyConfiguration.getUsername());
        oldEntityProxyConfiguration.setPasswordGoid(newEntityProxyConfiguration.getPasswordGoid());

        oldEntity.setSecurityZone(newEntity.getSecurityZone());
    }

    protected void beforeCreateEntity(final EntityBag<HttpConfiguration> entityBag) throws ObjectModelException {
        validateHttpConfiguration(entityBag.getEntity());
    }

    protected void beforeUpdateEntity(final EntityBag<HttpConfiguration> entityBag) throws ObjectModelException {
        validateHttpConfiguration(entityBag.getEntity());
    }

    /**
     * Validates that some needed properties are set depending on the options specified in the HttpConfiguration
     */
    private void validateHttpConfiguration(HttpConfiguration httpConfiguration) throws ConstraintViolationException {
        if (HttpConfiguration.Option.CUSTOM.equals(httpConfiguration.getTlsKeyUse())) {
            if (httpConfiguration.getTlsKeystoreGoid() == null) {
                throw new ConstraintViolationException("Must specify a tls keystore goid if a custom tls key is used.");
            } else if (httpConfiguration.getTlsKeystoreAlias() == null || httpConfiguration.getTlsKeystoreAlias().isEmpty()) {
                throw new ConstraintViolationException("Must specify a tls keystore alias if a custom tls key is used.");
            }
        }
        if (HttpConfiguration.Option.CUSTOM.equals(httpConfiguration.getProxyUse())) {
            HttpProxyConfiguration proxyConfiguration = httpConfiguration.getProxyConfiguration();
            if (proxyConfiguration == null) {
                throw new ConstraintViolationException("Must specify a proxy configuration if a custom proxy is used.");
            } else if (proxyConfiguration.getHost() == null || proxyConfiguration.getHost().isEmpty()) {
                throw new ConstraintViolationException("Must specify a proxy host if a custom proxy is used.");
            } else if (proxyConfiguration.getPort() == 0) {
                throw new ConstraintViolationException("Must specify a valid proxy port if a custom proxy is used.");
            } else if (proxyConfiguration.getUsername() != null && proxyConfiguration.getPasswordGoid() == null) {
                throw new ConstraintViolationException("Must specify a valid proxy password id if a custom proxy is used and a user name is specified.");
            }
        }
    }
}
