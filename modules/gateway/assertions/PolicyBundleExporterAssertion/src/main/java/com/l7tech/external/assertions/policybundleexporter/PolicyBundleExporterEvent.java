package com.l7tech.external.assertions.policybundleexporter;

import com.l7tech.external.assertions.policybundleexporter.server.PolicyBundleExporterContext;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.event.bundle.PolicyBundleEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PolicyBundleExporterEvent extends PolicyBundleEvent {
    @NotNull
    private final PolicyBundleExporterContext exportContext;

    @NotNull
    private final Map<Goid, String> componentRestmanBundleXmls = new HashMap<>();

    @NotNull
    private final Set<String> serverModuleFileNames = new HashSet<>();

    @NotNull
    private final Set<String> assertionFeatureSetNames = new HashSet<>();

    public PolicyBundleExporterEvent(@NotNull final Object source, @NotNull final PolicyBundleExporterContext exportContext) {
        super(source);
        this.exportContext = exportContext;
    }

    @NotNull
    public PolicyBundleExporterContext getExportContext() {
        return exportContext;
    }

    @NotNull
    public Map<Goid, String> getComponentRestmanBundleXmls() {
        return componentRestmanBundleXmls;
    }

    public void setComponentRestmanBundleXmls(@NotNull Goid componentFolderGoid, @NotNull String bundleXml) {
        componentRestmanBundleXmls.put(componentFolderGoid, bundleXml);
    }

    @NotNull
    public Set<String> getServerModuleFileNames() {
        return serverModuleFileNames;
    }

    @NotNull
    public Set<String> getAssertionFeatureSetNames() {
        return assertionFeatureSetNames;
    }
}
