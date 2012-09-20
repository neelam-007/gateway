package com.l7tech.external.assertions.oauthinstaller;

import com.l7tech.gateway.common.AsyncAdminMethods;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Admin interface for installing the OAuth Toolkit.
 */
public interface OAuthInstallerAdmin extends AsyncAdminMethods{

    /**
     * Get the overall version of the OAuth Toolkit this installer will install.
     *
     * Each time a new AAR is created due to a change the version should be increased.
     *
     * @return String representing the version.
     */
    @NotNull
    String getOAuthToolkitVersion() throws OAuthToolkitInstallationException;

    /**
     * Get the list of OTK components (name + description) available for installation.
     * @return list of triples of bundle id, bundle name and bundle description. Never null and no null contents.
     * @throws OAuthToolkitInstallationException
     */
    @NotNull
    List<BundleInfo> getAllOtkComponents() throws OAuthToolkitInstallationException;

    /**
     * Install the bundle identified by the supplied name
     *
     * @param otkComponentId     collection of all bundle ids to install. Bundles may depend on each others items, but there is no
     *                      install dependency order.
     * @param folderOid     oid of the folder to install into.
     * @param installFolder if not null or empty, this folder will be the install into folder. It may already exist.
     *                      If it does not exist it will be created.
     * @return the name of each bundle installed. If successful this will be each bundle requested.
     */
    @NotNull
    AsyncAdminMethods.JobId<ArrayList> installOAuthToolkit(@NotNull Collection<String> otkComponentId,
                                                           long folderOid,
                                                           @Nullable String installFolder) throws OAuthToolkitInstallationException;

    public static class OAuthToolkitInstallationException extends Exception{
        public OAuthToolkitInstallationException(String message) {
            super(message);
        }

        public OAuthToolkitInstallationException(String message, Throwable cause) {
            super(message, cause);
        }

        public OAuthToolkitInstallationException(Throwable cause) {
            super(cause);
        }
    }
}
