package com.l7tech.external.assertions.oauthinstaller;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Admin interface for installing OAuth Toolkit bundles.
 */
public interface OAuthInstallerAdmin extends AsyncAdminMethods{

    /**
     * Get the list of bundles (name + description) available for installation.
     * @return
     * @throws IOException
     */
    @NotNull
    List<Pair<String, String>> getAllAvailableBundles() throws IOException;

    /**
     * Install the bundle identified by the supplied name
     *
     * @param bundleNames    names of all bundles to install. Bundles may depend on each others items, but there is no
     *                       install dependency order.
     * @param folderOid oid of the folder to install into.
     * @param installFolder if not null or empty, this folder will be the install into folder. It may already exist.
     *                      If it does not exist it will be created.
     * @return the name of each bundle installed. If successful this will be each bundle requested.
     */
    @NotNull
    AsyncAdminMethods.JobId<ArrayList> installBundles(@NotNull Collection<String> bundleNames,
                                                      long folderOid,
                                                      @Nullable String installFolder) throws IOException;
}
