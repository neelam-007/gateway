package com.l7tech.console.security;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GatewayInfoHolder {
    @Nullable
    private Version gatewayVersion;
    @Nullable
    private Version minimumPolicyManagerVersionRequired;
    @Nullable
    private String gatewayProductName;
    @Nullable
    private String gatewayLegacyProductName;


    @NotNull
    private static final GatewayInfoHolder instance = new GatewayInfoHolder();

    @NotNull
    public static GatewayInfoHolder getInstance() {
        return instance;
    }

    GatewayInfoHolder() {
    }

    @Nullable
    public Version getGatewayVersion() {
        return gatewayVersion;
    }

    public void setGatewayVersion(@NotNull final Version gatewayVersion) {
        this.gatewayVersion = gatewayVersion;
    }

    @Nullable
    public Version getMinimumPolicyManagerVersionRequired() {
        return minimumPolicyManagerVersionRequired;
    }

    public void setMinimumPolicyManagerVersionRequired(@NotNull final Version minimumPolicyManagerVersionRequired) {
        this.minimumPolicyManagerVersionRequired = minimumPolicyManagerVersionRequired;
    }

    /**
     * Get the product name, ie "CA API Gateway".  NOTE: Licenses bind to this.
     *
     * @return The strict product name
     */
    @Nullable
    public String getGatewayProductName() {
        return gatewayProductName;
    }

    public void setGatewayProductName(@NotNull final String gatewayProductName) {
        this.gatewayProductName = gatewayProductName;
    }

    /**
     * Get the legacy product name, ie "Layer 7 SecureSpan Suite".  NOTE: Old Licenses are bind to this so
     * we need to support legacy product name in order to support upgrade.
     *
     * @return The strict product name
     */
    @Nullable
    public String getGatewayLegacyProductName() {
        return gatewayLegacyProductName;
    }

    public void setGatewayLegacyProductName(@NotNull final String gatewayLegacyProductName) {
        this.gatewayLegacyProductName = gatewayLegacyProductName;
    }

    /**
     * This can be called on logout to clear all the gateway settings
     */
    public void clear() {
        gatewayVersion = null;
        minimumPolicyManagerVersionRequired = null;
        gatewayProductName = null;
        gatewayLegacyProductName = null;
    }
}
