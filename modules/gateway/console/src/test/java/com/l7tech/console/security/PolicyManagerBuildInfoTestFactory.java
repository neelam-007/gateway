package com.l7tech.console.security;

import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Config;
import com.l7tech.util.MockConfig;

public class PolicyManagerBuildInfoTestFactory {
    public static PolicyManagerBuildInfo getNewInstance(String policyManagerProductName, String policyManagerVersion, String policyManagerVersionModifier, String gatewayMinimumVersion) {
        return new PolicyManagerBuildInfo(new MockConfig(CollectionUtils.MapBuilder.<String, String>builder()
                .put("policyManager.productName", policyManagerProductName)
                .put("policyManager.version", policyManagerVersion)
                .put("policyManager.versionModifier", policyManagerVersionModifier)
                .put("gateway.version.minimum", gatewayMinimumVersion)
                .map()));
    }

    public static PolicyManagerBuildInfo getNewInstance(Config config) {
        return new PolicyManagerBuildInfo(config);
    }
}