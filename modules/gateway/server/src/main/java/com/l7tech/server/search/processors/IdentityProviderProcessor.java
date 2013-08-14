package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
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
public class IdentityProviderProcessor extends GenericDependencyProcessor<IdentityProviderConfig> {

    @Inject
    private SecurePasswordManager securePasswordManager;


    @Override
    @NotNull
    public List<Dependency> findDependencies(IdentityProviderConfig identityProviderConfig, DependencyFinder finder) throws FindException {
        List<Dependency> dependencies = super.findDependencies(identityProviderConfig, finder);

        //Special handling for NTLM Configuration passwords. The password should only be a dependency if NTLM is enabled.
        if (identityProviderConfig instanceof LdapIdentityProviderConfig) {
            Map<String, String> ntlmProperties = ((LdapIdentityProviderConfig) identityProviderConfig).getNtlmAuthenticationProviderProperties();
            if (Boolean.parseBoolean(ntlmProperties.get("enabled"))) {
                String passwordGOID = ntlmProperties.get("service.passwordOid");
                SecurePassword securePassword = securePasswordManager.findByPrimaryKey(GoidUpgradeMapper.mapId(EntityType.SECURE_PASSWORD, passwordGOID));
                dependencies.add(finder.getDependency(securePassword));
            }
        }

        return dependencies;
    }
}
