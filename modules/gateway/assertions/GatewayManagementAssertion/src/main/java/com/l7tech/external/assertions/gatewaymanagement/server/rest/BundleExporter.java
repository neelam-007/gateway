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
import com.l7tech.util.ResourceUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.security.GeneralSecurityException;
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
    public static final String EncryptSecrets = "EncryptSecrets";
    public static final String ServiceUsed = "ServiceUsed";
    @Inject
    private EntityBundleExporter entityBundleExporter;
    @Inject
    private BundleTransformer bundleTransformer;
    @Inject
    private DependencyTransformer dependencyTransformer;
    @Inject
    private SecretsEncryptorFactory secretsEncryptorFactory;

    public BundleExporter() {
    }

    BundleExporter(final EntityBundleExporter entityBundleExporter,
                   final BundleTransformer bundleTransformer,
                   final DependencyTransformer dependencyTransformer,
                   final SecretsEncryptorFactory secretsEncryptorFactory) {
        this.entityBundleExporter = entityBundleExporter;
        this.bundleTransformer = bundleTransformer;
        this.dependencyTransformer = dependencyTransformer;
        this.secretsEncryptorFactory = secretsEncryptorFactory;
    }

    /**
     * Creates a bundle export given the export options
     *
     * @param bundleExportOptions  A map of export options. Can be null to use the defaults
     * @param includeDependencies  Include dependency analysis results in the bundle.
     * @param encryptSecrets       True if encrypted secrets should be included in the bundle.
     * @param encodedKeyPassphrase The optional base-64 encoded passphrase to use for the encryption key when encrypting secrets.
     * @param headers              The list of headers to create the export from. If the headers list is empty the full gateway will be exported.
     * @return The bundle generated from the headers given
     * @throws FindException
     */
    @NotNull
    public Bundle exportBundle(@Nullable Properties bundleExportOptions, Boolean includeDependencies, boolean encryptSecrets, @Nullable String encodedKeyPassphrase, @NotNull EntityHeader... headers) throws FindException, CannotRetrieveDependenciesException, FileNotFoundException, GeneralSecurityException {
        final SecretsEncryptor secretsEncryptor = encryptSecrets ? secretsEncryptorFactory.createSecretsEncryptor( encodedKeyPassphrase): null;
        EntityBundle entityBundle = entityBundleExporter.exportBundle(bundleExportOptions == null ? new Properties() : bundleExportOptions, headers);
        Bundle bundle = bundleTransformer.convertToMO(entityBundle, secretsEncryptor);
        if (includeDependencies) {
            final DependencyListMO dependencyMOs = dependencyTransformer.convertToMO(entityBundle.getDependencySearchResults());
            bundle.setDependencyGraph(dependencyMOs);
        }
        ResourceUtils.closeQuietly(secretsEncryptor);
        return bundle;
    }
}
