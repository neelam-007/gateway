package com.l7tech.gateway.common.module;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Data transport from server to the client, holding server configuration about custom and modular assertion setup.
 */
public class ServerModuleConfig implements Serializable {
    private static final long serialVersionUID = 4776013718981694948L;

    @NotNull private String customAssertionPropertyFileName;
    @NotNull private String modularAssertionManifestAssertionListKey;
    @NotNull private List<String> customAssertionModulesExt;
    @NotNull private List<String> modularAssertionModulesExt;

    public void setCustomAssertionPropertyFileName(@Nullable final String customAssertionPropertyFileName) {
        this.customAssertionPropertyFileName = (customAssertionPropertyFileName != null)
                ? customAssertionPropertyFileName : "custom_assertions.properties";
    }

    public void setModularAssertionManifestAssertionListKey(@Nullable final String modularAssertionManifestAssertionListKey) {
        this.modularAssertionManifestAssertionListKey = (modularAssertionManifestAssertionListKey != null)
                ? modularAssertionManifestAssertionListKey : "ModularAssertion-List";
    }

    public void setCustomAssertionModulesExt(@Nullable final List<String> customAssertionModulesExt) {
        this.customAssertionModulesExt = customAssertionModulesExt != null && !customAssertionModulesExt.isEmpty()
                ? new ArrayList<>(customAssertionModulesExt) : Arrays.asList(".jar");
    }

    public void setModularAssertionModulesExt(@Nullable final List<String> modularAssertionModulesExt) {
        this.modularAssertionModulesExt = modularAssertionModulesExt != null && !modularAssertionModulesExt.isEmpty()
                ? new ArrayList<>(modularAssertionModulesExt) : Arrays.asList(".jar", ".assertion", ".ass", ".assn", ".aar");
    }

    @NotNull
    public String getCustomAssertionPropertyFileName() {
        return customAssertionPropertyFileName;
    }

    @NotNull
    public String getModularAssertionManifestAssertionListKey() {
        return modularAssertionManifestAssertionListKey;
    }

    @NotNull
    public List<String> getCustomAssertionModulesExt() {
        return Collections.unmodifiableList(customAssertionModulesExt);
    }

    @NotNull
    public List<String> getModularAssertionModulesExt() {
        return Collections.unmodifiableList(modularAssertionModulesExt);
    }
}
