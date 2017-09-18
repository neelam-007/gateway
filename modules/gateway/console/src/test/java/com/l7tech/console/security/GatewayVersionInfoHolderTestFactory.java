package com.l7tech.console.security;

public class GatewayVersionInfoHolderTestFactory {
    public static GatewayInfoHolder getNewInstance(String gatewayVersion, String minimumPolicyManagerVersion, String gatewayProductName, String gatewayLegacyProductName) {
        GatewayInfoHolder gatewayInfoHolder = new GatewayInfoHolder();
        gatewayInfoHolder.setGatewayVersion(new Version(gatewayVersion));
        gatewayInfoHolder.setMinimumPolicyManagerVersionRequired(new Version(minimumPolicyManagerVersion));
        gatewayInfoHolder.setGatewayProductName(gatewayProductName);
        gatewayInfoHolder.setGatewayLegacyProductName(gatewayLegacyProductName);
        return gatewayInfoHolder;
    }

    public static GatewayInfoHolder getNewInstance() {
        return new GatewayInfoHolder();
    }
}