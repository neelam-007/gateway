package com.l7tech.identity;

import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.mapping.IdentityMapping;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.IdentityHeader;

import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * @author alex
 */
public interface IdentityProvider<UT extends User, GT extends Group, UMT extends UserManager<UT>, GMT extends GroupManager<UT, GT>>
{
    /**
     * @return the {@link IdentityProviderConfig} for this IdentityProvider.
     */
    IdentityProviderConfig getConfig();

    /**
     * @return the {@link UserManager} for this IdentityProvider. Currently should never be null, but might be in future.
     */
    UMT getUserManager();

    /**
     * @return the {@link GroupManager} for this IdentityProvider. Currently should never be null, but might be in future.
     */
    GMT getGroupManager();

    /**
     * searches for users and groups whose name match the pattern described in searchString
     * pattern may include wildcard such as * character
     */
    Collection<IdentityHeader> search(EntityType[] types, String searchString) throws FindException;

    Collection<IdentityHeader> search(boolean users, boolean groups, IdentityMapping mapping, Object value) throws FindException;

    String getAuthRealm();

    /**
     * Test this identity provider.
     *
     * @param fast True for a quick test, false for a full test
     * @throws InvalidIdProviderCfgException
     */
    void test(boolean fast) throws InvalidIdProviderCfgException;

    /**
     * Allows an IdentityProvider to veto the saving of a client cert.
     * 
     * @param user the user for whom the cert is to be saved
     * @param certChain the client certificate chain
     * @throws ClientCertManager.VetoSave if the provider wants to prevent the cert from being saved
     */
    void preSaveClientCert(UT user, X509Certificate[] certChain) throws ClientCertManager.VetoSave;

    void setUserManager(UMT userManager);

    void setGroupManager(GMT groupManager);

    /**
     * Called to validate that the User is still valid.
     *
     * <P>This validates that the user still exists in the Identity Provider and that the User has not been disabled
     * Does not authenticate the user</P>
     *
     * @param user The user to validate
     * @throws ValidationException If the user is no longer valid.
     */
    void validate(UT user) throws ValidationException;
}
