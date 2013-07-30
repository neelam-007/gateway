package com.l7tech.internal.license;

import com.l7tech.gateway.common.licensing.FeatureSetExpander;
import com.l7tech.server.GatewayFeatureSet;
import com.l7tech.server.GatewayFeatureSets;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper for GatewayFeatureSets that adds a fake entry for the ESM.  This is a hack but is quicker and safer
 * in the short term than rewriting EsmFeatureSets and GatewayFeatureSets to merge EsmFeatureSet and GatewayFeatureSet
 * into some superclass.
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
        Map<String, GatewayFeatureSet> profs = new LinkedHashMap<String, GatewayFeatureSet>(GatewayFeatureSets.getProductProfiles());
        profs.remove(GatewayFeatureSets.PROFILE_LICENSE_NAMES_NO_FEATURES.getName()); // don't show this as an option in GUI
        profs.put(PLACEHOLDER_PROFILE_ESM.getName(), PLACEHOLDER_PROFILE_ESM);
        return profs;
    }

    public static Map<String, GatewayFeatureSet> getAllFeatureSets() {
        Map<String, GatewayFeatureSet> sets = new LinkedHashMap<String, GatewayFeatureSet>(GatewayFeatureSets.getAllFeatureSets());
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
            Set<String> ret = new HashSet<String>();
            ret.addAll(inputSet);
            ret.addAll(delegate.getAllEnabledFeatures(inputSet));
            return ret;
        }
    }
}
