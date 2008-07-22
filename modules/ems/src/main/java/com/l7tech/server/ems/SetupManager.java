package com.l7tech.server.ems;

import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.server.GatewayLicenseManager;
import com.l7tech.server.identity.internal.InternalUserManager;
import com.l7tech.gateway.common.InvalidLicenseException;
import static org.springframework.transaction.annotation.Propagation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Encapsulates behavior for initial setup of a new EMS instance.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class SetupManager {
    private GatewayLicenseManager licenseManager;
    private InternalUserManager internalUserManager;

    public void setLicenseManager(GatewayLicenseManager licenseManager) {
        this.licenseManager = licenseManager;
    }

    public void setInternalUserManager(InternalUserManager internalUserManager) {
        this.internalUserManager = internalUserManager;
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
            return licenseManager.isFeatureEnabled("set:Core") || !internalUserManager.findAllHeaders().isEmpty();
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

            internalUserManager.save(user, Collections.<IdentityHeader>emptySet());
        } catch (InvalidLicenseException e) {
            throw new SetupException(e);
        } catch (UpdateException e) {
            throw new SetupException(e);
        } catch (InvalidPasswordException e) {
            throw new SetupException(e);
        } catch (SaveException e) {
            throw new SetupException(e);
        }
    }
}
