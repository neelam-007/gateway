package com.l7tech.internal.license;

import com.l7tech.gateway.common.licensing.FeatureSetExpander;
import com.l7tech.server.GatewayFeatureSet;
import com.l7tech.server.GatewayFeatureSets;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper for GatewayFeatureSets that excludes legacy product profile entries and adds a fake entry for the ESM.
 * The fake ESM entry is a hack, but is quicker and safer in the short term than rewriting EsmFeatureSets and
 * GatewayFeatureSets to merge EsmFeatureSet and GatewayFeatureSet into some superclass.
 */
public class LicenseGeneratorFeatureSets {
    private static final GatewayFeatureSet PLACEHOLDER_PROFILE_ESM = new GatewayFeatureSet("set:Profile:EnterpriseServiceManager",
            "Enterprise Service Manager server",
            "Required in order for Enterprise Service Manager server to start given this as its license.", null);

    public static FeatureSetExpander getFeatureSetExpander() {
        final FeatureSetExpander expander = GatewayFeatureSets.getFeatureSetExpander();
        return new UnknownFeaturePreservingFeatureSetExpander(expander);
    }

    public static Map<String, GatewayFeatureSet> getProductProfiles() {
        Map<String, GatewayFeatureSet> allProfiles = GatewayFeatureSets.getProductProfiles();

        Map<String, GatewayFeatureSet> availableProfiles = new LinkedHashMap<>();

        availableProfiles.put(GatewayFeatureSets.PROFILE_API_PROXY,
                allProfiles.get(GatewayFeatureSets.PROFILE_API_PROXY));
        availableProfiles.put(GatewayFeatureSets.PROFILE_FIREWALL,
                allProfiles.get(GatewayFeatureSets.PROFILE_FIREWALL));
        availableProfiles.put(GatewayFeatureSets.PROFILE_GATEWAY,
                allProfiles.get(GatewayFeatureSets.PROFILE_GATEWAY));
        availableProfiles.put(GatewayFeatureSets.PROFILE_NCES_EXTENSION,
                allProfiles.get(GatewayFeatureSets.PROFILE_NCES_EXTENSION));
        availableProfiles.put(GatewayFeatureSets.PROFILE_SALESFORCE_EXTENSION,
                allProfiles.get(GatewayFeatureSets.PROFILE_SALESFORCE_EXTENSION));
        availableProfiles.put(GatewayFeatureSets.PROFILE_MOBILE_EXTENSION,
                allProfiles.get(GatewayFeatureSets.PROFILE_MOBILE_EXTENSION));
        availableProfiles.put(GatewayFeatureSets.PROFILE_MAS_EXTENSION,
                allProfiles.get(GatewayFeatureSets.PROFILE_MAS_EXTENSION));

        availableProfiles.put(PLACEHOLDER_PROFILE_ESM.getName(), PLACEHOLDER_PROFILE_ESM);

        return availableProfiles;
    }

    public static Map<String, GatewayFeatureSet> getAllFeatureSets() {
        Map<String, GatewayFeatureSet> sets = new LinkedHashMap<>(GatewayFeatureSets.getAllFeatureSets());
        sets.put(PLACEHOLDER_PROFILE_ESM.getName(), PLACEHOLDER_PROFILE_ESM);
        return sets;
    }

    /**
     * A feature set expander that will preserve any unrecognized input feature set names.
     */
    private static class UnknownFeaturePreservingFeatureSetExpander implements FeatureSetExpander {
        private final FeatureSetExpander delegate;

        public UnknownFeaturePreservingFeatureSetExpander(FeatureSetExpander delegate) {
            this.delegate = delegate;
        }

        @Override
        public Set<String> getAllEnabledFeatures(Set<String> inputSet) {
            Set<String> ret = new HashSet<>();
            ret.addAll(inputSet);
            ret.addAll(delegate.getAllEnabledFeatures(inputSet));
            return ret;
        }
    }
}
