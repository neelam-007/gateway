package com.l7tech.server.policy.bundle;

import com.l7tech.policy.bundle.BundleInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

import java.util.List;


/**
 * Used by BundleInstaller to get bundle resources to install.
 */
public interface BundleResolver {

    public enum BundleItem {
        ASSERTION("Assertion.xml"),
        CERTIFICATE("TrustedCertificate.xml"),
        ENCAPSULATED_ASSERTION("EncapsulatedAssertion.xml"),
        FOLDER("Folder.xml"),
        POLICY("Policy.xml"),
        SERVICE("Service.xml");

        private BundleItem(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }

        private String fileName;
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
     * @param allowMissing true if no exception should be thrown if the item is missing. Provided as a convenience
     * @return Document representing the gateway management enumeration for the specific item
     * @throws UnknownBundleException if the bundle item or bundle is not known and allowMissing is false.
     */
    @Nullable
    Document getBundleItem(@NotNull final String bundleId, @NotNull final BundleItem bundleItem, final boolean allowMissing)
            throws UnknownBundleException, BundleResolverException, InvalidBundleException;

    /**
     * Set the installation prefix.
     * @param installationPrefix installation prefix
     */
    public void setInstallationPrefix(@Nullable String installationPrefix);

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