package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.BundleTransformer;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.BundleList;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.server.bundling.EntityBundle;
import com.l7tech.server.bundling.EntityBundleImporter;
import com.l7tech.server.bundling.EntityMappingResult;
import com.l7tech.util.ResourceUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

/**
 * Imports a bundle or a list of bundles. This transforms each bundle to an entity bundle and used the entity bundle importer to import the
 * bundle(s).
 */
public class BundleImporter {
    @Inject
    private EntityBundleImporter entityBundleImporter;
    @Inject
    private BundleTransformer bundleTransformer;
    @Inject
    private SecretsEncryptorFactory secretsEncryptorFactory;

    /**
     * Imports the bundle(s)
     * @param bundles A list of bundles to import.  If the list size is 1, then this function will import one bundle.
     * @param test if true the bundle import is tested, no changes will be persisted
     * @param active True to activate the updated services and policies.
     * @param versionComment The comment to set for updated/created services and policies
     * @param encodedKeyPassphrase The optional base-64 encoded passphrase to use for the encryption key when encrypting passwords
     * @return Returns the resulting mappings for the bundle(s) import
     * @throws ResourceFactory.InvalidResourceException
     */
    @NotNull
    public List<List<Mapping>> importBundles(
        @NotNull final BundleList bundles, final boolean test, final boolean active,
        @Nullable final String versionComment, @Nullable final String encodedKeyPassphrase)
        throws ResourceFactory.InvalidResourceException, FileNotFoundException, GeneralSecurityException {

        final SecretsEncryptor secretsEncryptor = secretsEncryptorFactory.createSecretsEncryptor(encodedKeyPassphrase != null ? encodedKeyPassphrase : null);
        final List<EntityBundle> entityBundles = bundleTransformer.convertFromMO(bundles, secretsEncryptor);
        final List<List<EntityMappingResult>> mappingsPerformed = entityBundleImporter.importBundles(entityBundles, test, active, versionComment);
        ResourceUtils.closeQuietly(secretsEncryptor);

        final List<Bundle> bundleList = bundles.getBundles();
        final List<List<Mapping>> mappingList = new ArrayList<>(bundleList.size());

        for (int i = 0; i < bundleList.size(); i++) {
            mappingList.add(bundleTransformer.updateMappings(bundleList.get(i).getMappings(), mappingsPerformed.get(i)));
        }

        return mappingList;
    }
}
