package com.l7tech.server.policy.module;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Modular assertion modules configuration holder class.
 */
public class ModularAssertionModulesConfig implements ModulesConfig {

    private static final String MANIFEST_HDR_ASSERTION_LIST = "ModularAssertion-List";
    private static final String MANIFEST_HDR_PRIVATE_LIBRARIES = "ModularAssertion-Private-Libraries";
    private static final Pattern PATTERN_WHITESPACE = Pattern.compile("\\s+");

    // defaults modules extensions an case they are not configured
    private static final String DEFAULT_EXT_LIST = ".jar .assertion .ass .assn .aar";
    // default value for scanning timer
    private static final int DEFAULT_SCAN_TIME_MILLIS = 4523;

    private static final String PARAM_MODULAR_ASSERTIONS_HOTSWAP_WIN_WORKAROUND_SUFFIX = "com.l7tech.server.hotswap.win.workaround.modular.suffix";

    private final ServerConfig config;
    private final LicenseManager licenseManager;

    public ModularAssertionModulesConfig(final ServerConfig config, final LicenseManager licenseManager) {
        this.config = config;
        this.licenseManager = licenseManager;
    }

    @Override
    public boolean isFeatureEnabled() {
        return licenseManager.isFeatureEnabled(GatewayFeatureSets.SERVICE_MODULELOADER);
    }

    @Override
    public boolean isScanningEnabled() {
        final String extList = config.getProperty(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_FILE_EXTENSIONS);
        return (!("-".equals(extList))); // scanning disabled
    }

    /**
     * For now each run we are reading the custom assertions modules folder from properties file,
     * in case it got changed in between. This is how the original logic is.<br/>
     * Anther option is to cache it in a variable and ignore any changes in the properties files after SSG startup.
     */
    @Override
    public File getModuleDir() {
        return config.getLocalDirectoryProperty(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_DIRECTORY, false).getAbsoluteFile();
    }

    @Override
    public List<String> getModulesExt() {
        String extList = config.getProperty(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_FILE_EXTENSIONS, DEFAULT_EXT_LIST);
        if (extList == null || extList.length() < 1) {
            extList = DEFAULT_EXT_LIST;
        }
        return Collections.unmodifiableList(Arrays.asList(PATTERN_WHITESPACE.split(extList.toLowerCase())));
    }

    @Override
    public String getDisabledSuffix() {
        return System.getProperty(PARAM_MODULAR_ASSERTIONS_HOTSWAP_WIN_WORKAROUND_SUFFIX);
    }

    public long getRescanPeriodMillis() {
        return config.getLongProperty(ServerConfigParams.PARAM_MODULAR_ASSERTIONS_RESCAN_MILLIS, DEFAULT_SCAN_TIME_MILLIS);
    }

    public String getManifestHdrAssertionList() {
        return MANIFEST_HDR_ASSERTION_LIST;
    }

    public String getManifestHdrPrivateLibraries() {
        return MANIFEST_HDR_PRIVATE_LIBRARIES;
    }
}
