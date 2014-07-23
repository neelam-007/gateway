package com.l7tech.server.policy.module;

import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.Config;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Custom assertion modules configuration holder class.
 */
public class CustomAssertionModulesConfig implements ModulesConfig {
    private static final Logger logger = Logger.getLogger(CustomAssertionModulesConfig.class.getName());

    // constants
    private static final String JAR_EXTENSION = ".jar";
    private static final String EXPANDED_MODULE_DIR_PREFIX = "x-module-";
    private static final String EXPANDED_MODULE_DIR_ID = "customassertion";
    private static final String ZIP_EXTENSION = ".zip";
    private static final String LIB_PREFIX = "lib/";

    // default values
    private static final boolean SCANNER_DISABLE_DEFAULT_VALUE = false;
    private static final boolean HOTSWAP_ENABLE_DEFAULT_VALUE = true;
    private static final long HOTSWAP_RESCAN_MILLIS_DEFAULT_VALUE = 4523L;

    // hot-swap workaround for windows platform.
    public static final String PARAM_CUSTOM_ASSERTIONS_HOTSWAP_WIN_WORKAROUND_SUFFIX = "com.l7tech.server.hotswap.win.workaround.custom.suffix";

    // SSG config object
    private final Config config;

    // supported extensions
    private List<String> extensions;

    public CustomAssertionModulesConfig(@NotNull final Config config) {
        this.config = config;
        extensions = new ArrayList<String>() {{
            add(JAR_EXTENSION);
        }};
    }

    @Override
    public boolean isFeatureEnabled() {
        // allow custom assertions
        return true;
    }

    @Override
    public boolean isScanningEnabled() {
        final String caPropFileName = getCustomAssertionPropertyFileName();
        return (caPropFileName != null
                && !caPropFileName.isEmpty()
                && !config.getBooleanProperty(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_SCAN_DISABLE, SCANNER_DISABLE_DEFAULT_VALUE));
    }

    @Override
    public File getModuleDir() {
        // read the custom assertions modules installation dir
        final String moduleDirName = config.getProperty(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_MODULES_DIRECTORY);
        if (moduleDirName == null) {
            logger.config("'" + ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_MODULES_DIRECTORY + "' not specified");
            return null;
        }
        return new File(moduleDirName);
    }

    @Override
    public List<String> getModulesExt() {
        return extensions;
    }

    @Override
    public String getDisabledSuffix() {
        return System.getProperty(PARAM_CUSTOM_ASSERTIONS_HOTSWAP_WIN_WORKAROUND_SUFFIX);
    }

    public String getCustomAssertionPropertyFileName() {
        // read the custom assertion resource properties file
        final String caPropFileName = config.getProperty(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_PROPERTIES_FILE);
        if (caPropFileName == null) {
            logger.config("'" + ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_PROPERTIES_FILE + "' not specified");
        }
        return caPropFileName;
    }

    public String getModuleWorkDirectory() {
        final String moduleWorkDirectory = config.getProperty(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_TEMP_DIRECTORY);
        if (moduleWorkDirectory == null) {
            logger.config("'" + ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_TEMP_DIRECTORY + "' not specified");
        }
        return moduleWorkDirectory;
    }

    public long getRescanPeriodMillis() {
        return config.getLongProperty(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_RESCAN_MILLIS, HOTSWAP_RESCAN_MILLIS_DEFAULT_VALUE);
    }

    public boolean isHotSwapEnabled() {
        return config.getBooleanProperty(ServerConfigParams.PARAM_CUSTOM_ASSERTIONS_RESCAN_ENABLE, HOTSWAP_ENABLE_DEFAULT_VALUE);
    }

    public boolean isSupportedLibrary(@NotNull final String entryNameLowerCase) {
        return entryNameLowerCase.endsWith(JAR_EXTENSION) || entryNameLowerCase.endsWith(ZIP_EXTENSION);
    }

    public String getExpandedModuleDirPrefix() {
        return EXPANDED_MODULE_DIR_PREFIX;
    }

    public String getExpandedModuleDirId() {
        return EXPANDED_MODULE_DIR_ID;
    }

    public String getLibPrefix() {
        return LIB_PREFIX;
    }
}
