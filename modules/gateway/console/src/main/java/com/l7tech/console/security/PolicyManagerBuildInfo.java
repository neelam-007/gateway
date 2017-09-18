package com.l7tech.console.security;

import com.l7tech.util.BuildInfo;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Retrieves info about the policy manager build.
 */
public class PolicyManagerBuildInfo {

    @NotNull
    private final Version policyManagerVersion;
    @NotNull
    private final Version gatewayMinimumVersion;
    @NotNull
    private final String policyManagerVersionModifier;
    @NotNull
    private final String policyManagerProductName;

    @NotNull
    private static final PolicyManagerBuildInfo instance = new PolicyManagerBuildInfo(ConfigFactory.getCachedConfig());

    @NotNull
    public static PolicyManagerBuildInfo getInstance() {
        return instance;
    }

    /**
     * Initializes the build info by reading from the config.
     *
     * @param config The config to read from.
     */
    PolicyManagerBuildInfo(@NotNull final Config config) {
        //TODO: should there be defaults here?
        policyManagerProductName = config.getProperty("policyManager.productName", "");
        policyManagerVersion = new Version(config.getProperty("policyManager.version", ""));
        policyManagerVersionModifier = config.getProperty("policyManager.versionModifier", "");
        gatewayMinimumVersion = new Version(config.getProperty("gateway.version.minimum", ""));
    }

    /**
     * Get the long form of the build string.
     * <p>
     * <code>CA API Gateway X.Y.Z build 1234, built 20001231235959 by user at host.l7tech.com</code>
     *
     * @return The build string
     */
    public String getLongBuildString() {
        return getBuildString() +
                ", built " + getBuildDate() + getBuildTime() +
                " by " + getBuildUser() + " at " + getBuildMachine();
    }

    /**
     * Get the long form of the build string.
     * <p>
     * <code>CA API Gateway X.Y.Z build 1234</code>
     *
     * @return The build string
     */
    public String getBuildString() {
        return getProductName() + " " +
                getProductVersion() +
                " build " + getBuildNumber();
    }

    /**
     * The minimum version of the gateway that this policy manager can connect to.
     *
     * @return The minimum version of the gateway that this policy manager can connect to
     */
    @NotNull
    public Version getMinimumGatewayVersionRequired() {
        return gatewayMinimumVersion;
    }

    /**
     * The version of the policy manager
     *
     * @return The version of the policy manager
     */
    @NotNull
    public Version getPolicyManagerVersion() {
        return policyManagerVersion;
    }

    @NotNull
    public String getProductVersion() {
        return policyManagerVersion.toString() + policyManagerVersionModifier;
    }

    public String getBuildUser() {
        return BuildInfo.getBuildUser();
    }

    public String getBuildMachine() {
        return BuildInfo.getBuildMachine();
    }

    public String getBuildDate() {
        return BuildInfo.getBuildDate();
    }

    public String getBuildTime() {
        return BuildInfo.getBuildTime();
    }

    public Integer compareVersions(String s, @NotNull String gatewayVersion) {
        return BuildInfo.compareVersions(s, gatewayVersion);
    }

    public String getBuildNumber() {
        return BuildInfo.getBuildNumber();
    }

    public String getProductName() {
        return policyManagerProductName;
    }

    public static int[] parseVersionString(@NotNull String version) {
        return BuildInfo.parseVersionString(version);
    }
}
