package com.l7tech.server.ems;

import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.*;
import com.l7tech.server.GatewayLicenseManager;
import com.l7tech.server.identity.internal.InternalUserManager;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.gateway.common.InvalidLicenseException;
import static org.springframework.transaction.annotation.Propagation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collections;
import java.util.logging.Logger;

/**
 * Encapsulates behavior for initial setup of a new EMS instance.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class SetupManagerImpl implements InitializingBean, SetupManager {
    private static final Logger logger = Logger.getLogger(SetupManagerImpl.class.getName());

    private GatewayLicenseManager licenseManager;
    private IdentityProviderFactory identityProviderFactory;
    private IdentityProviderConfigManager identityProviderConfigManager;

    public SetupManagerImpl(final GatewayLicenseManager licenseManager,
                        final IdentityProviderFactory identityProviderFactory,
                        final IdentityProviderConfigManager identityProviderConfigManager) {
        this.licenseManager = licenseManager;
        this.identityProviderFactory = identityProviderFactory;
        this.identityProviderConfigManager = identityProviderConfigManager;
    }

    /**
     * Check if this EMS instance has already had initial setup performed.
     * This returns true if any of the following are true:
     * <ul>
     * <li>A valid license is currently installed.</li>
     * <li>At least one internal user currently exists.</li>
     * </ul>
     * @return true if initial setup has been performed per the above.
     * @throws SetupException if there is a problem checking whether any internal users exist
     */
    @Transactional(propagation=SUPPORTS, readOnly=true)
    public boolean isSetupPerformed() throws SetupException  {
        try {
            InternalUserManager internalUserManager = getInternalUserManager();
            return licenseManager.isFeatureEnabled("set:Core") || internalUserManager==null || !internalUserManager.findAllHeaders().isEmpty();
        } catch (FindException e) {
            throw new SetupException(e);
        }
    }


    /**
     * Perform initial setup of this EMS instance.
     * This sets a license and creates the initial administrator user in a single transaction.
     * 
     * @param licenseXml  XML license file to install.  Required.
     * @param initialAdminUsername  username for initial administrator user.  Required.
     * @param initialAdminPassword  password for iniital administrator user.  Required.
     * @throws SetupException if this EMS instance has already been set up.
     */
    @Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
    public void performInitialSetup(String licenseXml, String initialAdminUsername, String initialAdminPassword) throws SetupException {
        try {
            if (isSetupPerformed())
                throw new SetupException("This EMS instance has already been set up.");
            licenseManager.installNewLicense(licenseXml);

            InternalUser user = new InternalUser();
            user.setName(initialAdminUsername);
            user.setLogin(initialAdminUsername);
            user.setCleartextPassword(initialAdminPassword);

            InternalUserManager internalUserManager = getInternalUserManager();
            if ( internalUserManager != null) internalUserManager.save(user, Collections.<IdentityHeader>emptySet());
        } catch (InvalidLicenseException e) {
            throw new SetupException(e);
        } catch (FindException e) {
            throw new SetupException(e);
        } catch (UpdateException e) {
            throw new SetupException(e);
        } catch (InvalidPasswordException e) {
            throw new SetupException(e);
        } catch (SaveException e) {
            throw new SetupException(e);
        }
    }

    private InternalUserManager getInternalUserManager() throws FindException {
        InternalUserManager internalUserManager = null;

        for ( IdentityProvider identityProvider : identityProviderFactory.findAllIdentityProviders() ) {
            if ( IdentityProviderType.INTERNAL.equals( identityProvider.getConfig().type() ) ) {
                internalUserManager = (InternalUserManager) identityProvider.getUserManager();
                break;
            }
        }

        return internalUserManager;
    }

    /**
     * Add initial identity provider configuration if not present. 
     */
    public void afterPropertiesSet() throws Exception {
        if ( identityProviderConfigManager.findAll().isEmpty() ) {
            logger.info("Creating configuration for internal identity provider.");
            IdentityProviderConfig config = new IdentityProviderConfig();
            config.setOid(-2);
            config.setTypeVal(1);
            config.setAdminEnabled(true);
            config.setName("Internal Identity Provider");
            config.setDescription("Internal Identity Provider");
            long id = identityProviderConfigManager.save(config);
            logger.info("Created configuration for internal identity provider with identifier '" + id + "'.");
        }
    }
}
