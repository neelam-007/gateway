package com.l7tech.identity;

import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.IdentityHeader;
import org.jetbrains.annotations.Nullable;

import java.security.cert.X509Certificate;

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
    EntityHeaderSet<IdentityHeader> search(EntityType[] types, String searchString) throws FindException;

    String getAuthRealm();

    /**
     * Test this identity provider.
     *
     *
     * @param fast True for a quick test, false for a full test
     * @param testUser example username to use for testing, or null if not required.
     * @param testPassword exmaple password to use for testing, or null if not required.
     * @throws InvalidIdProviderCfgException if the test fails
     */
    void test(boolean fast, @Nullable String testUser, @Nullable char[] testPassword) throws InvalidIdProviderCfgException;

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

    /**
     * Checks if the user already has a  client certificate for the given provider.
     *
     * @param login The users login to use for checking client cert
     * @return  TRUE if user has client cert
     * @throws AuthenticationException
     */
    public boolean hasClientCert(String login) throws AuthenticationException;
}
