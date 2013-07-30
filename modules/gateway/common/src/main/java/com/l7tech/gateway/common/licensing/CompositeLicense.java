package com.l7tech.gateway.common.licensing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

/**
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
public class CompositeLicense implements Serializable {
    private final Set<String> enabledFeatures;
    private final Map<Long, FeatureLicense> validFeatureLicenses;
    private final Map<Long, FeatureLicense> invalidFeatureLicenses; // unsigned, untrusted, or wrong product/version
    private final Map<Long, FeatureLicense> expiredFeatureLicenses;
    private final List<LicenseDocument> invalidLicenseDocuments; // malformed or missing information


    public CompositeLicense(@NotNull final HashMap<Long, FeatureLicense> validFeatureLicenses,
                            @Nullable final HashMap<Long, FeatureLicense> invalidFeatureLicenses,
                            @Nullable final HashMap<Long, FeatureLicense> expiredFeatureLicenses,
                            @Nullable final List<LicenseDocument> invalidLicenseDocuments,
                            @NotNull final FeatureSetExpander expander) {
        Set<String> featureSets = new HashSet<>();

        for (FeatureLicense license : validFeatureLicenses.values()) {
            featureSets.addAll(expander.getAllEnabledFeatures(license.getFeatureSets()));
        }

        this.enabledFeatures = Collections.unmodifiableSet(featureSets);
        this.validFeatureLicenses = Collections.unmodifiableMap(validFeatureLicenses);
        this.invalidFeatureLicenses = Collections.unmodifiableMap(invalidFeatureLicenses);
        this.expiredFeatureLicenses = Collections.unmodifiableMap(expiredFeatureLicenses);
        this.invalidLicenseDocuments = Collections.unmodifiableList(invalidLicenseDocuments);
    }

    /**
     * Check if the current license would enable access to the given feature if it were valid.
     * <p>
     * Notes: For performance, this method does not check the validity of this license.  It is assumed that the
     * caller has already checked this before querying for individual features.
     *
     * @param name  the name of the feature.  Must not be null or empty.
     * @return true iff. this feature is enabled by this license.
     */
    public boolean isFeatureEnabled(String name) {
        return enabledFeatures.contains(name);
    }

    /**
     * Check if the CompositeLicense contains a FeatureLicense with the specified Id.
     * @param id the Id to check for
     * @return true iff the CompositeLicense contains a Feature License with the same Id
     */
    public boolean containsLicenseWithId(long id) {
        return null != validFeatureLicenses.get(id) || null != expiredFeatureLicenses.get(id);
    }

    /**
     * Gets a Map of all the valid Feature Licenses
     *
     * @return an immutable Map of valid Feature Licenses, keyed by their License Id
     */
    public Map<Long, FeatureLicense> getValidFeatureLicenses() {
        return validFeatureLicenses;
    }

    /**
     * Gets a Map of all the invalid Feature Licenses
     *
     * @return an immutable Map of invalid Feature Licenses, keyed by their License Id
     */
    public Map<Long,FeatureLicense> getInvalidFeatureLicenses() {
        return invalidFeatureLicenses;
    }

    /**
     * Gets a Map of all the expired Feature Licenses
     *
     * @return an immutable Map of expired Feature Licenses, keyed by their License Id
     */
    public Map<Long, FeatureLicense> getExpiredFeatureLicenses() {
        return expiredFeatureLicenses;
    }

    /**
     * Gets a Map of all the invalid License Documents
     *
     * @return an immutable Map of expired Feature Licenses, keyed by their License Id
     */
    public List<LicenseDocument> getInvalidLicenseDocuments() {
        return invalidLicenseDocuments;
    }

    public boolean hasValid() {
        return !validFeatureLicenses.isEmpty();
    }

    public boolean hasInvalidFeatureLicenses() {
        return !invalidFeatureLicenses.isEmpty();
    }

    public boolean hasExpired() {
        return !expiredFeatureLicenses.isEmpty();
    }

    public boolean hasInvalidLicenseDocuments() {
        return !invalidLicenseDocuments.isEmpty();
    }
}
