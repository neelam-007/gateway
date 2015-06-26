package com.l7tech.gateway.common.api.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
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
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
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

    @NotNull
    private final SolutionKitsConfig solutionKitsConfig;

    public SkarProcessor(@NotNull final SolutionKitsConfig solutionKitsConfig) {
        this.solutionKitsConfig = solutionKitsConfig;
    }

    /**
     * Invoke custom callback code support read and write of Solution Kit metadata and bundle.
     */
    public void invokeCustomCallback() throws SolutionKitException {
        try {
            Document metadataDoc, bundleDoc;
            SolutionKitManagerContext skContext;

            SolutionKitCustomization customization;
            SolutionKitManagerCallback customCallback;
            SolutionKitManagerUi customUi;

            for (SolutionKit sk : solutionKitsConfig.getCustomizations().keySet()) {
                customization = solutionKitsConfig.getCustomizations().get(sk);

                // implementer provides a callback
                customCallback = customization.getCustomCallback();
                if (customCallback != null) {
                    customUi = customization.getCustomUi();

                    // if implementer provides a context
                    skContext = customUi != null ? customUi.getContext() : null;
                    if (skContext != null) {
                        // get from selected
                        metadataDoc = SolutionKitUtils.createDocument(sk);
                        bundleDoc = solutionKitsConfig.getBundleAsDocument(sk);

                        // set to context
                        skContext.setSolutionKitMetadata(metadataDoc);
                        skContext.setMigrationBundle(bundleDoc);

                        // execute callback
                        customCallback.preMigrationBundleImport(skContext);

                        // copy back metadata from xml version
                        SolutionKitUtils.copyDocumentToSolutionKit(metadataDoc, sk);

                        // set (possible) changes made to metadata and bundle
                        // TODO fix duplicate HashMap bug where put(...) does not replace previous value
                        solutionKitsConfig.setBundle(sk, bundleDoc);
                    } else  {
                        customCallback.preMigrationBundleImport(null);
                    }
                }
            }
        } catch (SolutionKitManagerCallback.CallbackException | IOException | TooManyChildElementsException | MissingRequiredElementException e) {
            throw new SolutionKitException("Unexpected error during custom callback invocation.", e);
        }
    }

    /**
     * Install or upgrade the SKAR
     */
    public AsyncAdminMethods.JobId<Goid> installOrUpgrade(@NotNull SolutionKitAdmin solutionKitAdmin) throws SolutionKitException {
        SolutionKit solutionKit = solutionKitsConfig.getSingleSelectedSolutionKit();
        if (solutionKit == null) {
            throw new SolutionKitException("Unexpected error: unable to get selected Solution Kit.");
        }

        // Update resolved mapping target IDs.
        Map<String, String> resolvedEntityIds = solutionKitsConfig.getResolvedEntityIds(solutionKit);
        Bundle bundle = solutionKitsConfig.getBundle(solutionKit);
        if (bundle != null) {
            for (Mapping mapping : bundle.getMappings()) {
                String resolvedId = resolvedEntityIds.get(mapping.getSrcId());
                if (resolvedId != null) {
                    mapping.setTargetId(resolvedId);
                }
            }
        }

        boolean isUpgrade = solutionKitsConfig.getSolutionKitToUpgrade() != null;
        String bundleXml = solutionKitsConfig.getBundleAsString(solutionKit);
        if (bundleXml == null) {
            throw new SolutionKitException("Unexpected error: unable to get Solution Kit bundle.");
        }

        return solutionKitAdmin.install(solutionKit, bundleXml, isUpgrade);
    }

    /**
     * Load and process the files inside the SKAR.
     */
    public void load(@NotNull InputStream inputStream) throws SolutionKitException {
        ZipInputStream zis = null;
        try {
            final SolutionKit solutionKit = new SolutionKit();
            boolean hasRequiredSolutionKitFile = false, hasRequiredInstallBundleFile = false;
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
                            logger.log(Level.WARNING, "Unexpected entry in solution kit: " + entry.getName());
                            break;
                    }
                } else {
                    logger.log(Level.WARNING, "Unexpected entry in solution kit: " + entry.getName());
                }
                zis.closeEntry();
                entry = zis.getNextEntry();
            }

            validate(hasRequiredSolutionKitFile, hasRequiredInstallBundleFile);
            final Bundle installBundle = MarshallingUtils.unmarshal(Bundle.class, installBundleSource, true);
            Bundle bundle = installBundle;

            if (upgradeBundleSource.getNode() != null) {
                final Bundle upgradeBundle = MarshallingUtils.unmarshal(Bundle.class, upgradeBundleSource, true);
                bundle = mergeBundle(solutionKit, installBundle, upgradeBundle);
            }

            solutionKitsConfig.getLoadedSolutionKits().put(solutionKit, bundle);

            setCustomizationInstances(solutionKit, classLoader);
        } catch (IOException | SAXException | MissingRequiredElementException | TooManyChildElementsException e) {
            throw new SolutionKitException("Error loading skar file.", e);
        } finally {
            ResourceUtils.closeQuietly(zis);
        }
    }

    private void validate(boolean hasRequiredSolutionKitFile, boolean hasRequiredInstallBundleFile) throws SolutionKitException {
        if (!hasRequiredSolutionKitFile) {
            throw new SolutionKitException("Missing required file " + SK_FILENAME);
        } else if (!hasRequiredInstallBundleFile) {
            throw new SolutionKitException("Missing required file " + SK_INSTALL_BUNDLE_FILENAME);
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
        final SolutionKit solutionKitToUpgrade = solutionKitsConfig.getSolutionKitToUpgrade();
        if (solutionKitToUpgrade != null && solutionKitToUpgrade.getSolutionKitGuid().equals(solutionKit.getSolutionKitGuid()) && upgradeBundle.getMappings() != null) {

            // set goid and version for upgrade
            solutionKit.setGoid(solutionKitToUpgrade.getGoid());
            solutionKit.setVersion(solutionKitToUpgrade.getVersion());

            // update previously resolved mapping target IDs
            Map<String, String> previouslyResolvedIds = solutionKitsConfig.getResolvedEntityIds().get(solutionKitsConfig.getSolutionKitToUpgrade());
            for (Mapping mapping : upgradeBundle.getMappings()) {
                if (previouslyResolvedIds != null) {
                    String resolvedId = previouslyResolvedIds.get(mapping.getSrcId());
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
                    solutionKitsConfig.getCustomizations().put(solutionKit, new SolutionKitCustomization(classLoader, customUi, customCallback));
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new SolutionKitException("Error loading the customization class(es).", e);
            }
        }
    }
}