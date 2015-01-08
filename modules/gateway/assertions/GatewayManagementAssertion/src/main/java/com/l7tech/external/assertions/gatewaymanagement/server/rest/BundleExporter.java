package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.BundleTransformer;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.DependencyTransformer;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.DependencyListMO;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.bundling.EntityBundle;
import com.l7tech.server.bundling.EntityBundleExporter;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.util.DefaultMasterPasswordFinder;
import com.l7tech.util.HexUtils;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.SyspropUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Properties;

/**
 * This is used to create a bundle export.
 */
public class BundleExporter {
    //DEFAULTS and options
    public static final String IncludeRequestFolderOption = "IncludeRequestFolder";
    public static final String DefaultMappingActionOption = "DefaultMappingAction";
    public static final String DefaultMapByOption = "DefaultMapBy";
    public static final String IgnoredEntityIdsOption = "IgnoredEntityIds";
    @Inject
    private EntityBundleExporter entityBundleExporter;
    @Inject
    private BundleTransformer bundleTransformer;
    @Inject
    private DependencyTransformer dependencyTransformer;

    public BundleExporter() {
    }

    BundleExporter(final EntityBundleExporter entityBundleExporter,
                   final BundleTransformer bundleTransformer,
                   final DependencyTransformer dependencyTransformer) {
        this.entityBundleExporter = entityBundleExporter;
        this.bundleTransformer = bundleTransformer;
        this.dependencyTransformer = dependencyTransformer;
    }

    /**
     * Creates a bundle export given the export options
     *
     * @param bundleExportOptions  A map of export options. Can be null to use the defaults
     * @param includeDependencies  Include dependency analysis results in the bundle.
     * @param encryptPasswords     true if encrypted passwords should be included in the bundle.
     * @param encodedKeyPassphrase The optional base-64 encoded passphrase to use for the encryption key when encrypting passwords.
     * @param headers              The list of headers to create the export from. If the headers list is empty the full gateway will be exported.
     * @return The bundle generated from the headers given
     * @throws FindException
     */
    @NotNull
    public Bundle exportBundle(@Nullable Properties bundleExportOptions, Boolean includeDependencies, boolean encryptPasswords, @Nullable String encodedKeyPassphrase, @NotNull EntityHeader... headers) throws FindException, CannotRetrieveDependenciesException, FileNotFoundException {
        final SecretsEncryptor secretsEncryptor = createPasswordManager(encryptPasswords, encodedKeyPassphrase);
        EntityBundle entityBundle = entityBundleExporter.exportBundle(bundleExportOptions == null ? new Properties() : bundleExportOptions, headers);
        Bundle bundle = bundleTransformer.convertToMO(entityBundle, secretsEncryptor);
        if (includeDependencies) {
            final DependencyListMO dependencyMOs = dependencyTransformer.convertToMO(entityBundle.getDependencySearchResults());
            bundle.setDependencyGraph(dependencyMOs);
        }
        return bundle;
    }

    @Nullable
    private SecretsEncryptor createPasswordManager(final boolean encryptPasswords, @Nullable final String encodedKeyPassphrase) throws FileNotFoundException {
        SecretsEncryptor passMgr = null;
        if (encryptPasswords) {
            if (encodedKeyPassphrase != null) {
                passMgr = new SecretsEncryptor(HexUtils.decodeBase64(encodedKeyPassphrase));
            } else {
                final String confDir = SyspropUtil.getString("com.l7tech.config.path", "../node/default/etc/conf");
                final File ompDat = new File(new File(confDir), "omp.dat");
                if (ompDat.exists()) {
                    passMgr = new SecretsEncryptor(new DefaultMasterPasswordFinder(ompDat).findMasterPasswordBytes());
                } else {
                    throw new FileNotFoundException("Cannot encrypt because " + ompDat.getAbsolutePath() + " does not exist");
                }
            }
        }
        return passMgr;
    }
}
