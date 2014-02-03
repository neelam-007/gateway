package com.l7tech.server.policy.module;

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

    // Legacy behaviour note:
    // All 3 values below are hardcoded inside serverconfig.properties, which means they cannot be changed during runtime.
    // In order to modify the default values, serverconfig.properties needs to be changed, which requires SSG compilation (i.e. new version).
    private static final String KEY_CONFIG_FILE = "custom.assertions.file";
    private static final String KEY_CUSTOM_MODULES_TEMP = "custom.assertions.temp";
    private static final String KEY_CUSTOM_MODULES = "custom.assertions.modules";

    // for both properties below:
    // * systemProperty is enabled, which means that default values can be overridden at runtime from system.properties file.
    // * clusterProperty is disabled, which means these are not cluster wide properties.
    private static final String KEY_HOTSWAP_ENABLED = "custom.assertions.rescan.enabled";
    private static final String KEY_HOTSWAP_RESCAN_MILLIS = "custom.assertions.rescan.millis";

    private static final boolean HOTSWAP_ENABLED_DEFAULT_VALUE = true;
    private static final long HOTSWAP_RESCAN_MILLIS_DEFAULT_VALUE = 4523;

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
        return caPropFileName != null && !caPropFileName.isEmpty() &&
                config.getBooleanProperty(KEY_HOTSWAP_ENABLED, HOTSWAP_ENABLED_DEFAULT_VALUE);
    }

    @Override
    public File getModuleDir() {
        // read the custom assertions modules installation dir
        final String moduleDirName = config.getProperty(KEY_CUSTOM_MODULES);
        if (moduleDirName == null) {
            logger.config("'" + KEY_CUSTOM_MODULES + "' not specified");
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
        return System.getProperty("com.l7tech.server.hotswap.win.workaround.custom.suffix");
    }

    public String getCustomAssertionPropertyFileName() {
        // read the custom assertion resource properties file
        final String caPropFileName = config.getProperty(KEY_CONFIG_FILE);
        if (caPropFileName == null) {
            logger.config("'" + KEY_CONFIG_FILE + "' not specified");
        }
        return caPropFileName;
    }

    public String getModuleWorkDirectory() {
        final String moduleWorkDirectory = config.getProperty(KEY_CUSTOM_MODULES_TEMP);
        if (moduleWorkDirectory == null) {
            logger.config("'" + KEY_CUSTOM_MODULES_TEMP + "' not specified");
        }
        return moduleWorkDirectory;
    }

    public long getRescanPeriodMillis() {
        return config.getLongProperty(KEY_HOTSWAP_RESCAN_MILLIS, HOTSWAP_RESCAN_MILLIS_DEFAULT_VALUE);
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
