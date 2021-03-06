package com.l7tech.server.policy.bundle;

import com.l7tech.policy.bundle.BundleInfo;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

import java.util.List;


/**
 * Used by BundleInstaller to get bundle resources to install.
 */
public interface BundleResolver {
    final static String FILE_EXTENSION_XML = ".xml";

    public enum BundleItem {
        ASSERTION("Assertion"),
        CERTIFICATE("TrustedCertificate"),
        ENCAPSULATED_ASSERTION("EncapsulatedAssertion"),
        FOLDER("Folder"),
        MIGRATION_BUNDLE("MigrationBundle"),
        POLICY("Policy"),
        SERVICE("Service");

        BundleItem(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            if (StringUtils.isEmpty(version)) {
                return fileName + FILE_EXTENSION_XML;
            } else {
                return fileName + version + FILE_EXTENSION_XML;
            }
        }

        public void setVersion(String version) {
            this.version = version;
        }

        private String fileName;
        private String version;
    }

    /**
     * @return all BundleInfo's known by this resolver.
     */
    @NotNull
    List<BundleInfo> getResultList();

    /**
     * Get a Document representing the bundle item from the specified bundle with id bundleId
     *
     * @param bundleId bundle identifier
     * @param bundleItem item in the bundle, Folder.xml, Policy.xml or Service.xml
     * @param subFolder sub folder to look for the bundle item
     * @param allowMissing true if no exception should be thrown if the item is missing. Provided as a convenience
     * @return Document representing the gateway management enumeration for the specific item
     * @throws UnknownBundleException if the bundle item or bundle is not known and allowMissing is false.
     */
    @Nullable
    Document getBundleItem(@NotNull final String bundleId, @NotNull String subFolder, @NotNull final BundleItem bundleItem, final boolean allowMissing)
            throws UnknownBundleException, BundleResolverException, InvalidBundleException;

    /**
     * Get a Document representing the bundle item from the specified bundle with id bundleId
     *
     * @param bundleId bundle identifier
     * @param bundleItem item in the bundle, Folder.xml, Policy.xml or Service.xml
     * @param allowMissing true if no exception should be thrown if the item is missing. Provided as a convenience
     * @return Document representing the gateway management enumeration for the specific item
     * @throws UnknownBundleException if the bundle item or bundle is not known and allowMissing is false.
     */
    Document getBundleItem(@NotNull final String bundleId, @NotNull final BundleItem bundleItem, final boolean allowMissing)
            throws UnknownBundleException, BundleResolverException, InvalidBundleException;

    public static class UnknownBundleException extends Exception{
        public UnknownBundleException(String message) {
            super(message);
        }
    }

    /**
     * An item, e.g. Policy.xml, was requested from a bundle but it was not a part of the bundle
     */
    public static class UnknownBundleItemException extends UnknownBundleException{
        public UnknownBundleItemException(String message) {
            super(message);
        }
    }

    public static class BundleResolverException extends Exception{
        public BundleResolverException(String message) {
            super(message);
        }

        public BundleResolverException(Throwable cause) {
            super(cause);
        }
    }

    class InvalidBundleException extends Exception{
        public InvalidBundleException(String message) {
            super(message);
        }

        public InvalidBundleException(Throwable cause) {
            super(cause);
        }

        public InvalidBundleException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}