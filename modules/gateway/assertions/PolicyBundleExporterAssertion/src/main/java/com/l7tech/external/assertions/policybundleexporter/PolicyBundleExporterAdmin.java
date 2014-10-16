package com.l7tech.external.assertions.policybundleexporter;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;

/**
 * Admin interface for client side (Policy Manager) calls to the server (Gateway) to support installer export.
 */

@Secured
@Administrative
public interface PolicyBundleExporterAdmin extends AsyncAdminMethods {

    /**
     * Generate Policy Bundle Installer .aar file bytes.
     * @param exportProperties Policy bundle properties for generating Policy Bundle Installer .aar file.  Required.
     * @return Policy Bundle Installer .aar file bytes (as ArrayList of Bytes).
     * @throws java.io.IOException if an .aar file cannot be generated
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    JobId<InstallerAarFile> generateInstallerAarFile(@NotNull final PolicyBundleExporterProperties exportProperties) throws IOException;

    /**
     * Get the Custom or Modular Assertion module for a given name.
     * @param serverModuleFileName name of the module
     * @return module bytes (e.g. .aar, .jar)
     * @throws java.io.IOException
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    JobId<ServerModuleFile> getServerModuleFile(@NotNull final String serverModuleFileName) throws IOException;

    public class ServerModuleFile implements Serializable {
        protected final byte[] bytes;

        public ServerModuleFile(byte[] bytes) {
            this.bytes = bytes;
        }

        public byte[] getBytes() {
            return bytes;
        }
    }

    public class InstallerAarFile extends ServerModuleFile {
        private Set<String> serverModuleFileNames;
        private Set<String> assertionFeatureSetNames;

        public InstallerAarFile(byte[] bytes, Set<String> serverModuleFileNames, Set<String> assertionFeatureSetNames) {
            super(bytes);
            this.serverModuleFileNames = serverModuleFileNames;
            this.assertionFeatureSetNames = assertionFeatureSetNames;
        }

        public Set<String> getServerModuleFileNames() {
            return serverModuleFileNames;
        }

        public Set<String> getAssertionFeatureSetNames() {
            return assertionFeatureSetNames;
        }
    }
}
