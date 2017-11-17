package com.l7tech.gateway.common.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.security.signer.InnerPayloadFactory;
import com.l7tech.gateway.common.security.signer.SignerUtils;
import com.l7tech.policy.solutionkit.SolutionKitManagerCallback;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import java.io.*;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * SKAR payload processing logic.  Must be accessible from both the console UI and in the server headless interface.
 */
public class SkarPayload extends SignerUtils.SignedZip.InnerPayload {
    private static final Logger logger = Logger.getLogger(SkarPayload.class.getName());

    private static final String SK_FILENAME = "SolutionKit.xml";
    private static final String SK_INSTALL_BUNDLE_FILENAME = "InstallBundle.xml";
    private static final String SK_UPGRADE_BUNDLE_FILENAME = "UpgradeBundle.xml";
    private static final String SK_DELETE_BUNDLE_FILENAME = "DeleteBundle.xml";
    private static final String SK_CUSTOMIZATION_JAR_FILENAME = "Customization.jar";

    private static final String BUNDLE_ELE_MAPPINGS = "Mappings";

    /**
     * Override {@link com.l7tech.gateway.common.security.signer.SignerUtils.SignedZip.InnerPayload#FACTORY} to avoid accidental usage.
     */
    @SuppressWarnings("UnusedDeclaration")
    @Nullable
    private static final InnerPayloadFactory<SkarPayload> FACTORY = null;

    @NotNull
    private final SolutionKitsConfig solutionKitsConfig;

    /**
     * Package access constructor, used by {@link com.l7tech.gateway.common.solutionkit.SkarPayloadFactory}.
     */
    SkarPayload(@NotNull final PoolByteArrayOutputStream dataStream,
                @NotNull final byte[] dataDigest,
                @NotNull final PoolByteArrayOutputStream signaturePropsStream,
                @NotNull final SolutionKitsConfig solutionKitsConfig
    ) {
        super(dataStream, dataDigest, signaturePropsStream);
        this.solutionKitsConfig = solutionKitsConfig;
    }

    @NotNull
    public SolutionKitsConfig process() throws SolutionKitException {
        load(getDataStream());
        return this.solutionKitsConfig;
    }

    /**
     * Load and process the files inside the SKAR.
     *
     * @param inputStream SKAR file {@code InputStream}.  Required and cannot be {@code null}.
     *                    Note: this MUST NOT just be the stream claimed by the sender, it must be freshly unwrapped
     *                    after signature is successfully verified.
     * @throws SolutionKitException if an error happens while processing the specified SKAR file.
     */
    void load(@NotNull final InputStream inputStream) throws SolutionKitException {
        ZipInputStream zis = null;
        try {
            final SolutionKit solutionKit = new SolutionKit();
            boolean hasRequiredSolutionKitFile = false, hasRequiredInstallBundleFile = false, foundChildSkar = false;
            final DOMSource installBundleSource = new DOMSource();
            SolutionKitCustomizationClassLoader classLoader = null;

            zis = new ZipInputStream(inputStream);
            ZipEntry entry = zis.getNextEntry();

            while (entry != null) {
                if (!entry.isDirectory()) {
                    final String fileName = new File(entry.getName()).getName();
                    switch (fileName) {
                        case SK_FILENAME:
                            hasRequiredSolutionKitFile = true;
                            loadSolutionKitXml(zis, solutionKit);
                            break;
                        case SK_INSTALL_BUNDLE_FILENAME:
                            hasRequiredInstallBundleFile = true;
                            loadInstallBundleXml(zis, installBundleSource);
                            break;
                        case SK_UPGRADE_BUNDLE_FILENAME:
                            //Since v9.3, Solution kit upgrades no longer require an upgrade bundle,
                            // since the upgrade process uses uninstall of old solution kits and install of new solution kits
                            logger.log(Level.FINE, "Ignoring " + SK_UPGRADE_BUNDLE_FILENAME + ".");
                            break;
                        case SK_DELETE_BUNDLE_FILENAME:
                            loadDeleteBundleXml(zis, solutionKit);
                            break;
                        case SK_CUSTOMIZATION_JAR_FILENAME:
                            classLoader = getCustomizationClassLoader(zis);
                            break;
                        default:
                            if (fileName.endsWith(".skar")) {
                                // Get the input bytes for a child SKAR, create a new input stream, and call recursively call the load method.
                                load(new ByteArrayInputStream(IOUtils.slurpStream(zis)));
                                foundChildSkar = true;
                            } else {
                                logger.warning("Unexpected entry in solution kit: " + entry.getName());
                            }
                            break;
                    }
                } else {
                    logger.warning("Unexpected entry in solution kit: " + entry.getName());
                }
                zis.closeEntry();
                entry = zis.getNextEntry();
            }

            // Validate the SKAR structure
            final boolean isCollection = SolutionKitUtils.isCollectionOfSkars(solutionKit);
            validate(isCollection, hasRequiredSolutionKitFile, hasRequiredInstallBundleFile, foundChildSkar);

            // If the SKAR is a collection of SKARs, then the rest task (e.g., merge bundle, record upgrade info, etc.)
            // should be ignored, since the rest task is done when recursively call the load method.
            if (isCollection) {
                // Save the parent solution kit into solutionKitsConfig. Just in case, we need to use the parent solution kit object.
                // However, be aware that the parent solution kit has not been saved yet, so its GOID is a default dummy GOID.
                solutionKit.setMappings("");
                solutionKitsConfig.setParentSolutionKitLoaded(solutionKit);
                return;
            }

            // Continue the process when the SKAR is a SKAR without nesting other SKARs.
            final Bundle installBundle = MarshallingUtils.unmarshal(Bundle.class, installBundleSource, true);
            Bundle bundle = installBundle;

            if (solutionKitsConfig.isUpgrade()) {
                bundle = updateInstallBundle(solutionKit, installBundle);
            }

            // copy existing entity ownership records to solutionKit (otherwise they will all be deleted)
            // they will be removed when the solution kit entity is removed, might need to see what tests need fixing
            final SolutionKit solutionKitToUpgrade = solutionKitsConfig.getSolutionKitToUpgrade(solutionKit.getSolutionKitGuid());
            if (solutionKitToUpgrade != null) {
                solutionKit.setEntityOwnershipDescriptors(solutionKitToUpgrade.getEntityOwnershipDescriptors());
            }

            solutionKitsConfig.getLoadedSolutionKits().put(solutionKit, bundle);

            setCustomizationInstances(solutionKit, classLoader);
        } catch (SAXException | MissingRequiredElementException | TooManyChildElementsException e) {
            throw new BadRequestException("Error loading skar file: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new SolutionKitException("Error loading skar file: " + e.getMessage(), e);
        } finally {
            ResourceUtils.closeQuietly(zis);
        }
    }

    private void validate(boolean isCollection, boolean hasRequiredSolutionKitFile, boolean hasRequiredInstallBundleFile, boolean foundLeafSkar) throws SolutionKitException {
        if (!isCollection) {
            if (!hasRequiredSolutionKitFile) {
                throw new BadRequestException("Missing required file " + SK_FILENAME);
            } else if (!hasRequiredInstallBundleFile) {
                throw new BadRequestException("Missing required file " + SK_INSTALL_BUNDLE_FILENAME);
            }
        } else if (!foundLeafSkar) {
            throw new BadRequestException("Missing nested SKARs in the SKAR file.");
        }
    }

    // load solution kit metadata
    private void loadSolutionKitXml(final ZipInputStream zis, final SolutionKit solutionKit) throws IOException, SAXException, TooManyChildElementsException, MissingRequiredElementException, SolutionKitException {
        final Document doc = XmlUtil.parse(new ByteArrayInputStream(IOUtils.slurpStream(zis)));
        SolutionKitUtils.copyDocumentToSolutionKit(doc, solutionKit);
    }

    // load install bundle
    private void loadInstallBundleXml(final ZipInputStream zis, final DOMSource installBundleSource) throws IOException, SAXException, TooManyChildElementsException, MissingRequiredElementException, SolutionKitException {
        final Document doc = XmlUtil.parse(new ByteArrayInputStream(IOUtils.slurpStream(zis)));
        final Element installBundleEle = doc.getDocumentElement();

        if (installBundleEle.getAttributeNodeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, SolutionKitUtils.SK_NS_PREFIX) == null) {
            installBundleEle.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE + ":" + SolutionKitUtils.SK_NS_PREFIX, SolutionKitUtils.SK_NS);
        }

        installBundleSource.setNode(installBundleEle);
    }

    // load uninstall bundle for later use
    private void loadDeleteBundleXml(final ZipInputStream zis, final SolutionKit solutionKit) throws IOException, SAXException, TooManyChildElementsException, MissingRequiredElementException, SolutionKitException {
        final Document doc = XmlUtil.parse(new ByteArrayInputStream(IOUtils.slurpStream(zis)));
        final Element uninstallBundleEle = doc.getDocumentElement();
        solutionKit.setUninstallBundle(XmlUtil.nodeToString(uninstallBundleEle));
    }

    /**
     * Updates the loaded solution kit to set the Goid and version to be the same as the ones on the database. Also
     * set the mapping target Ids to ones that were previously resolved if applicable.
     *
     * @param loadedSolutionKit The solution kit generated from SolutionKit.xml from the skar file.
     * @param installBundle     The install bundle generated from InstallBundle.xml
     * @return The updated install bundle
     */
    Bundle updateInstallBundle(final SolutionKit loadedSolutionKit, final Bundle installBundle) {
        final SolutionKit solutionKitToUpgrade = solutionKitsConfig.getSolutionKitToUpgrade(loadedSolutionKit.getSolutionKitGuid());

        if (solutionKitToUpgrade != null && installBundle.getMappings() != null) {
            // set goid and version for upgrade
            loadedSolutionKit.setGoid(solutionKitToUpgrade.getGoid());
            loadedSolutionKit.setVersion(solutionKitToUpgrade.getVersion());

            //this code was modified to handle a collection of SKARs - since we can have collections, we need to have
            //a Map of installMappings - each one identifiable by solutionKitGuid.  We're storing a handle to these initial installMappings
            //so that in case an entity is deleted from the original install, we'll have a way of identifying the upgrade install against
            //the original and warn the user
            final Map<String, Mapping> installMappings = solutionKitsConfig.getInstallMappings(loadedSolutionKit.getSolutionKitGuid());
            for (final Mapping installMapping : installBundle.getMappings()) {
                installMappings.put(installMapping.getSrcId(), installMapping);
            }

            // update previously resolved mapping target IDs
            solutionKitsConfig.setMappingTargetIdsFromPreviouslyResolvedIds(solutionKitToUpgrade, installBundle);
        }

        return installBundle;
    }

    @Nullable
    SolutionKitCustomizationClassLoader getCustomizationClassLoader(final InputStream zis) throws SolutionKitException {
        SolutionKitCustomizationClassLoader classLoader = null;

        // temporarily write customization jar to a temp directory
        File outFile = new File(SyspropUtil.getProperty("java.io.tmpdir"), "Customization-" + UUID.randomUUID() + ".jar");   // can we do this without writing to disk?
        OutputStream entryOut = null;
        try {
            logger.fine("Customization jar file: " + outFile.getCanonicalPath());
            // System.out.println("Customization jar file: " + outFile.getCanonicalPath());

            entryOut = new BufferedOutputStream(new FileOutputStream(outFile));
            IOUtils.copyStream(zis, entryOut);
            entryOut.flush();

            classLoader = new SolutionKitCustomizationClassLoader(
                    new URL[]{outFile.toURI().toURL()},
                    Thread.currentThread().getContextClassLoader(),
                    outFile);
        } catch (IOException ioe) {
            throw new SolutionKitException("Error loading the customization jar.", ioe);
        } finally {
            ResourceUtils.closeQuietly(entryOut);
            outFile.deleteOnExit();
        }

        return classLoader;
    }

    void setCustomizationInstances(@NotNull final SolutionKit solutionKit, @Nullable final SolutionKitCustomizationClassLoader classLoader) throws SolutionKitException {
        if (classLoader != null) {
            try {
                SolutionKitManagerUi customUi = null;
                SolutionKitManagerCallback customCallback = null;
                final String uiClassName = solutionKit.getProperty(SolutionKit.SK_PROP_CUSTOM_UI_KEY);
                if (!StringUtils.isEmpty(uiClassName)) {
                    final Class cls = classLoader.loadClass(uiClassName);
                    customUi = ((SolutionKitManagerUi) cls.newInstance()).initialize();
                }
                final String callbackClassName = solutionKit.getProperty(SolutionKit.SK_PROP_CUSTOM_CALLBACK_KEY);
                if (!StringUtils.isEmpty(callbackClassName)) {
                    final Class cls = classLoader.loadClass(callbackClassName);
                    customCallback = (SolutionKitManagerCallback) cls.newInstance();
                }
                if (customUi != null || customCallback != null) {
                    solutionKitsConfig.getCustomizations().put(solutionKit.getSolutionKitGuid(), new Pair<>(solutionKit, new SolutionKitCustomization(classLoader, customUi, customCallback)));
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new SolutionKitException("Error loading the customization class(es).", e);
            }
        }
    }
}