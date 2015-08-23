package com.l7tech.gateway.common.api.solutionkit;

import com.l7tech.common.io.TeeInputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.security.signer.SignedZipVisitor;
import com.l7tech.gateway.common.security.signer.SignerUtils;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gateway.common.solutionkit.UntrustedSolutionKitException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.solutionkit.SolutionKitManagerCallback;
import com.l7tech.policy.solutionkit.SolutionKitManagerContext;
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
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * SKAR file processing logic.  Must be accessible from both the console UI and in the server headless interface.
 */
public class SkarProcessor {
    private static final Logger logger = Logger.getLogger(SkarProcessor.class.getName());

    private static final String SK_FILENAME = "SolutionKit.xml";
    private static final String SK_INSTALL_BUNDLE_FILENAME = "InstallBundle.xml";
    private static final String SK_UPGRADE_BUNDLE_FILENAME = "UpgradeBundle.xml";
    private static final String SK_DELETE_BUNDLE_FILENAME = "DeleteBundle.xml";
    private static final String SK_CUSTOMIZATION_JAR_FILENAME = "Customization.jar";

    private static final String BUNDLE_ELE_MAPPINGS = "Mappings";

    public static final String MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE = "SK_AllowMappingOverride";

    @NotNull
    private final SolutionKitsConfig solutionKitsConfig;

    public SkarProcessor(@NotNull final SolutionKitsConfig solutionKitsConfig) {
        this.solutionKitsConfig = solutionKitsConfig;
    }

    /**
     * Invoke custom callback code support read and write of Solution Kit metadata and bundle.
     */
    public void invokeCustomCallback(final SolutionKit solutionKit) throws SolutionKitException {
        try {
            Document metadataDoc, bundleDoc;
            SolutionKitManagerContext skContext;

            Pair<SolutionKit, SolutionKitCustomization> customization;
            SolutionKitManagerCallback customCallback;
            SolutionKitManagerUi customUi;

            customization = solutionKitsConfig.getCustomizations().get(solutionKit.getSolutionKitGuid());
            if (customization == null || customization.right == null) return;

            // implementer provides a callback
            customCallback = customization.right.getCustomCallback();
            if (customCallback == null) return;

            customUi = customization.right.getCustomUi();

            // if implementer provides a context
            skContext = customUi != null ? customUi.getContext() : null;
            if (skContext != null) {
                // get from selected
                metadataDoc = SolutionKitUtils.createDocument(solutionKit);
                bundleDoc = solutionKitsConfig.getBundleAsDocument(solutionKit);

                // set to context
                skContext.setSolutionKitMetadata(metadataDoc);
                skContext.setMigrationBundle(bundleDoc);

                // execute callback
                customCallback.preMigrationBundleImport(skContext);

                // copy back metadata from xml version
                SolutionKitUtils.copyDocumentToSolutionKit(metadataDoc, solutionKit);

                // set (possible) changes made to metadata and bundle
                solutionKitsConfig.setBundle(solutionKit, bundleDoc);
            } else  {
                customCallback.preMigrationBundleImport(null);
            }
        } catch (SolutionKitManagerCallback.CallbackException | IOException | TooManyChildElementsException | MissingRequiredElementException e) {
            throw new SolutionKitException("Unexpected error during custom callback invocation.", e);
        }
    }

    /**
     * Install or upgrade the SKAR
     */
    public AsyncAdminMethods.JobId<Goid> installOrUpgrade(@NotNull final SolutionKitAdmin solutionKitAdmin, @NotNull final SolutionKit solutionKit) throws SolutionKitException {
        // Update resolved mapping target IDs.
        Pair<SolutionKit, Map<String, String>> resolvedEntityIds = solutionKitsConfig.getResolvedEntityIds(solutionKit.getSolutionKitGuid());
        Bundle bundle = solutionKitsConfig.getBundle(solutionKit);
        if (bundle != null) {
            String resolvedId;
            Boolean allowOverride;
            for (Mapping mapping : bundle.getMappings()) {
                resolvedId = resolvedEntityIds.right == null ? null : resolvedEntityIds.right.get(mapping.getSrcId());
                if (resolvedId != null) {
                    allowOverride = mapping.getProperty(MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE);
                    if (allowOverride != null && allowOverride) {
                        mapping.setTargetId(resolvedId);
                    } else {
                        throw new SolutionKitException("Unable to process entity ID replace for mapping with scrId=" + mapping.getSrcId() +
                                ".  Replacement id=" + resolvedId + " requires the .skar author to set mapping property '" + MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE + "' to true.");
                    }
                }
            }
        }

        boolean isUpgrade =! solutionKitsConfig.getSolutionKitsToUpgrade().isEmpty();
        String bundleXml = solutionKitsConfig.getBundleAsString(solutionKit);
        if (bundleXml == null) {
            throw new SolutionKitException("Unexpected error: unable to get Solution Kit bundle.");
        }

        return solutionKitAdmin.install(solutionKit, bundleXml, isUpgrade);
    }

    /**
     * Load and process the files inside the SKAR.<br/>
     * Note that the SKAR file must be signed with trusted signer.
     *
     * @param skarStream          An {@code InputStream} of signed SKAR file.  Required and cannot be {@code null}.
     * @param solutionKitAdmin    Solution kit admin interface used to verify SKAR signature.  Required and cannot be {@code null}.
     * @throws SolutionKitException if an error happens while processing the specified SKAR file
     * @throws UntrustedSolutionKitException if the specified SKAR is not signed
     * with trusted signer or SKAR file signature is not verified.
     */
    public void load(
            @NotNull final InputStream skarStream,
            @NotNull final SolutionKitAdmin solutionKitAdmin
    ) throws SolutionKitException {
        // todo: for now load in-memory, must revisit this logic later
        // skarStream is not markable, therefore the stream cannot be read multiple times (once for verifying the signature, the other to load the content)
        // perhaps use a temporary file to stash the raw bytes of the SKAR file (need to check if tmp file works from the applet)
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            // get SHA-256 the message digest
            final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            // process signed zip file to calculate digest
            final Pair<byte[], String> digestAndSignature = SignerUtils.walkSignedZip(
                    skarStream,
                    new SignedZipVisitor<byte[], String>() {
                        @Override
                        public byte[] visitData(@NotNull final InputStream inputStream) throws IOException {
                            // calc SHA-256 of SIGNED_DATA_ZIP_ENTRY
                            final TeeInputStream tis = new TeeInputStream(inputStream, bos);
                            final DigestInputStream dis = new DigestInputStream(tis, messageDigest);
                            IOUtils.copyStream(dis, new com.l7tech.common.io.NullOutputStream());
                            return dis.getMessageDigest().digest();
                        }

                        @Override
                        public String visitSignature(@NotNull final InputStream inputStream) throws IOException {
                            // read the signature properties
                            final StringWriter writer = new StringWriter();
                            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1))) {
                                IOUtils.copyStream(reader, writer);
                                writer.flush();
                            }
                            return writer.toString();
                        }
                    },
                    true
            );

            // verify SKAR signature
            solutionKitAdmin.verifySkarSignature(digestAndSignature.left, digestAndSignature.right);

        } catch (final IOException | NoSuchAlgorithmException | SignatureException e) {
            throw new UntrustedSolutionKitException("Error loading signed skar file :" + e.getMessage(), e);
        }

        // finally load the SKAR file without the signature
        loadWithoutSignatureCheck(new ByteArrayInputStream(bos.toByteArray()));
    }

    /**
     * Load and process the files inside the SKAR.
     *
     * @param inputStream    SKAR file {@code InputStream}.  Required and cannot be {@code null}.
     * @throws SolutionKitException if an error happens while processing the specified SKAR file.
     */
    void loadWithoutSignatureCheck(@NotNull InputStream inputStream) throws SolutionKitException {
        ZipInputStream zis = null;
        try {
            final SolutionKit solutionKit = new SolutionKit();
            boolean hasRequiredSolutionKitFile = false, hasRequiredInstallBundleFile = false, foundChildSkar = false;
            final DOMSource installBundleSource = new DOMSource();
            final DOMSource upgradeBundleSource = new DOMSource();
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
                            loadUpgradeBundleXml(zis, upgradeBundleSource);
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
                                loadWithoutSignatureCheck(new ByteArrayInputStream(IOUtils.slurpStream(zis)));
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
            final boolean isCollection = Boolean.parseBoolean(solutionKit.getProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY));
            validate(isCollection, hasRequiredSolutionKitFile, hasRequiredInstallBundleFile, foundChildSkar);

            // If the SKAR is a collection of SKARs, then the rest task (e.g., merge bundle, record upgrade info, etc.)
            // should be ignored, since the rest task is done when recursively call the load method.
            if (isCollection) {
                // Save the parent solution kit into solutionKitsConfig. Just in case, we need to use the parent solution kit object.
                // However, be aware that the parent solution kit has not been saved yet, so its GOID is a default dummy GOID.
                solutionKit.setMappings(SolutionKit.PARENT_SOLUTION_KIT_DUMMY_MAPPINGS); // Set a dummy mapping for a parent solution kit
                solutionKitsConfig.setParentSolutionKit(solutionKit);
                return;
            }

            // Continue the process when the SKAR is a SKAR without nesting other SKARs.
            final Bundle installBundle = MarshallingUtils.unmarshal(Bundle.class, installBundleSource, true);
            Bundle bundle = installBundle;

            if (upgradeBundleSource.getNode() != null) {
                final Bundle upgradeBundle = MarshallingUtils.unmarshal(Bundle.class, upgradeBundleSource, true);
                bundle = mergeBundle(solutionKit, installBundle, upgradeBundle);
                solutionKitsConfig.setUpgradeInfoProvided(solutionKit, true);
            } else {
                solutionKitsConfig.setUpgradeInfoProvided(solutionKit, false);
            }

            // copy existing entity ownership records to solutionKit (otherwise they will all be deleted)
            SolutionKit solutionKitToUpgrade = SolutionKitUtils.searchSolutionKitByGuidToUpgrade(solutionKitsConfig.getSolutionKitsToUpgrade(), solutionKit.getSolutionKitGuid());

            if (solutionKitToUpgrade != null) {
                solutionKit.setEntityOwnershipDescriptors(solutionKitToUpgrade.getEntityOwnershipDescriptors());
            }

            solutionKitsConfig.getLoadedSolutionKits().put(solutionKit, bundle);

            setCustomizationInstances(solutionKit, classLoader);
        } catch (IOException | SAXException | MissingRequiredElementException | TooManyChildElementsException e) {
            throw new SolutionKitException("Error loading skar file :" + e.getMessage(), e);
        } finally {
            ResourceUtils.closeQuietly(zis);
        }
    }

    private void validate(boolean isCollection, boolean hasRequiredSolutionKitFile, boolean hasRequiredInstallBundleFile, boolean foundLeafSkar) throws SolutionKitException {
        if (! isCollection) {
            if (!hasRequiredSolutionKitFile) {
                throw new SolutionKitException("Missing required file " + SK_FILENAME);
            } else if (!hasRequiredInstallBundleFile) {
                throw new SolutionKitException("Missing required file " + SK_INSTALL_BUNDLE_FILENAME);
            }
        } else if (! foundLeafSkar) {
            throw new SolutionKitException("Missing nested SKARs in the SKAR file.");
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

    // load matching upgrade bundle
    private void loadUpgradeBundleXml(final ZipInputStream zis, final DOMSource upgradeBundleSource) throws IOException, SAXException, TooManyChildElementsException, MissingRequiredElementException, SolutionKitException {
        final Document doc = XmlUtil.parse(new ByteArrayInputStream(IOUtils.slurpStream(zis)));
        final Element upgradeBundleEle = doc.getDocumentElement();

        // find upgrade mappings to replace install mappings with upgrade mappings
        Element upgradeMappingEle = DomUtils.findFirstDescendantElement(upgradeBundleEle, null, BUNDLE_ELE_MAPPINGS);
        if (upgradeMappingEle == null) {
            throw new SolutionKitException("Expected <" + BUNDLE_ELE_MAPPINGS + "> element in " + SK_UPGRADE_BUNDLE_FILENAME + ".");
        }

        upgradeBundleSource.setNode(upgradeBundleEle);
    }

    // load uninstall bundle for later use
    private void loadDeleteBundleXml(final ZipInputStream zis, final SolutionKit solutionKit) throws IOException, SAXException, TooManyChildElementsException, MissingRequiredElementException, SolutionKitException {
        final Document doc = XmlUtil.parse(new ByteArrayInputStream(IOUtils.slurpStream(zis)));
        final Element uninstallBundleEle = doc.getDocumentElement();
        solutionKit.setUninstallBundle(XmlUtil.nodeToString(uninstallBundleEle));
    }

    // merge bundles (if upgrade mappings exists, replace existing install mappings)
    private Bundle mergeBundle(final SolutionKit solutionKit, final Bundle installBundle, final Bundle upgradeBundle) {
        final SolutionKit solutionKitToUpgrade = SolutionKitUtils.searchSolutionKitByGuidToUpgrade(solutionKitsConfig.getSolutionKitsToUpgrade(), solutionKit.getSolutionKitGuid());
        if (solutionKitToUpgrade != null && upgradeBundle.getMappings() != null) {

            // set goid and version for upgrade
            solutionKit.setGoid(solutionKitToUpgrade.getGoid());
            solutionKit.setVersion(solutionKitToUpgrade.getVersion());

            // update previously resolved mapping target IDs
            Pair<SolutionKit, Map<String, String>> previouslyResolvedIds = solutionKitsConfig.getResolvedEntityIds().get(solutionKitToUpgrade.getSolutionKitGuid());
            for (Mapping mapping : upgradeBundle.getMappings()) {
                if (previouslyResolvedIds != null) {
                    String resolvedId = previouslyResolvedIds.right.get(mapping.getSrcId());
                    if (resolvedId != null) {
                        mapping.setTargetId(resolvedId);
                    }
                }
            }

            // replace with upgrade mappings
            installBundle.setMappings(upgradeBundle.getMappings());
        }

        return installBundle;
    }

    @Nullable
    private SolutionKitCustomizationClassLoader getCustomizationClassLoader(final ZipInputStream zis) throws SolutionKitException {
        SolutionKitCustomizationClassLoader classLoader = null;

        File outFile = new File("Customization-" + UUID.randomUUID() + ".jar");   // can we do this without writing to disk?
        OutputStream entryOut = null;
        try {
            logger.fine("JAR FILE: " + outFile.getCanonicalPath());
            // System.out.println("JAR FILE: " + outFile.getCanonicalPath());

            entryOut = new BufferedOutputStream(new FileOutputStream(outFile));
            IOUtils.copyStream(zis, entryOut);
            entryOut.flush();

            classLoader = new SolutionKitCustomizationClassLoader(
                    new URL[] {outFile.toURI().toURL()},
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

    // may need to move class loading logic to the server (i.e. admin) for headless to work
    private void setCustomizationInstances(final SolutionKit solutionKit, @Nullable final SolutionKitCustomizationClassLoader classLoader) throws SolutionKitException {
        if (classLoader != null) {
            try {
                SolutionKitManagerUi customUi = null;
                SolutionKitManagerCallback customCallback = null;
                final String uiClassName = solutionKit.getProperty(SolutionKit.SK_PROP_CUSTOM_UI_KEY);
                if (!StringUtils.isEmpty(uiClassName)) {
                    final Class cls = classLoader.loadClass(uiClassName);
                    customUi = ((SolutionKitManagerUi)cls.newInstance()).initialize();
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
