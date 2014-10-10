package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.GoidUpgradeMapper;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * This will properly handle LDAP identity providers. It will only add the ntlm referenced password if the ntlm enabled
 * property is set to true.
 *
 * @author Victor Kazakov
 */
public class IdentityProviderProcessor extends DefaultDependencyProcessor<IdentityProviderConfig> {

    @Inject
    private SecurePasswordManager securePasswordManager;


    @Override
    @NotNull
    public List<Dependency> findDependencies(@NotNull final IdentityProviderConfig identityProviderConfig, @NotNull final DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        final List<Dependency> dependencies = super.findDependencies(identityProviderConfig, finder);

        //Special handling for NTLM Configuration passwords. The password should only be a dependency if NTLM is enabled.
        if (identityProviderConfig instanceof LdapIdentityProviderConfig) {
            final Map<String, String> ntlmProperties = ((LdapIdentityProviderConfig) identityProviderConfig).getNtlmAuthenticationProviderProperties();
            if (Boolean.parseBoolean(ntlmProperties.get("enabled"))) {
                final String passwordGOID = ntlmProperties.get("service.passwordOid");
                final Goid passwordId = GoidUpgradeMapper.mapId(EntityType.SECURE_PASSWORD, passwordGOID);
                final SecurePassword securePassword = securePasswordManager.findByPrimaryKey(passwordId);
                final Dependency dependency = finder.getDependency(DependencyFinder.FindResults.create(securePassword, new EntityHeader(passwordId,EntityType.SECURE_PASSWORD,null,null)));
                if (dependency != null)
                    dependencies.add(dependency);
            }
        }

        return dependencies;
    }
}
