package com.l7tech.server.processcontroller.patching;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

/**
 * PatchPackage API.
 */
public interface PatchPackage {

    public static final String PATCH_PROPERTIES_ENTRY = "patch.properties";

    enum Property {

        ID(true),
        DESCRIPTION(true),
        ROLLBACK_ALLOWED(true),
        ROLLBACK_FOR_ID(false),
        JAVA_BINARY(false);

        Property(boolean required) {
            this.required = required;
        }

        public boolean isRequired() {
            return required;
        }

        private boolean required;
    }

    public File getFile();

    public String getProperty(Property prop);

    public Set<List<X509Certificate>> getCertificatePaths();
}
