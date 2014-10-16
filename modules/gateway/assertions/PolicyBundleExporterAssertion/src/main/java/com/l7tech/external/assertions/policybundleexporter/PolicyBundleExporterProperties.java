package com.l7tech.external.assertions.policybundleexporter;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.FolderHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Policy bundle properties for generating Policy Bundle Installer .aar file.
 *      - subPackageName: name of sub package in lower case, ie "com.com.l7tech.external.assertions.sub_package_name"  Required.
 *      - camelName: name of assertion in camel case, ie "FooBar".  Required.
 */
public class PolicyBundleExporterProperties implements Serializable {
    final private String bundleName;
    final private String bundleVersion;
    final private FolderHeader bundleFolder;
    private String actionIconFilePath;

    final private List<ComponentInfo> componentInfoList;
    private Map<Goid, String> componentRestmanBundleXmls;

    public PolicyBundleExporterProperties(
            @NotNull String bundleName,
            @NotNull String bundleVersion,
            @NotNull FolderHeader bundleFolder,
            @NotNull List<ComponentInfo> componentInfoList) {
        this.bundleName = bundleName;
        this.bundleVersion = bundleVersion;
        this.bundleFolder = bundleFolder;
        this.componentInfoList = componentInfoList;
    }

    @NotNull
    public String getBundleName() {
        return bundleName;
    }

    @NotNull
    public String getBundleVersion() {
        return bundleVersion;
    }

    @NotNull
    public FolderHeader getBundleFolder() {
        return bundleFolder;
    }

    @NotNull
    public List<ComponentInfo> getComponentInfoList() {
        return componentInfoList;
    }

    public void setActionIconFilePath(@NotNull String actionIconFilePath) {
        this.actionIconFilePath = actionIconFilePath;
    }

    @Nullable
    public String getActionIconFilePath() {
        return actionIconFilePath;
    }

    public void setComponentRestmanBundleXmls(@NotNull Map<Goid, String> componentRestmanBundleXmls) {
        this.componentRestmanBundleXmls = componentRestmanBundleXmls;
    }

    @NotNull
    public Map<Goid, String> getComponentRestmanBundleXmls() {
        return componentRestmanBundleXmls;
    }
}