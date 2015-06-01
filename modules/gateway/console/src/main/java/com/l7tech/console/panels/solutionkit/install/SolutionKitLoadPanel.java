package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.solutionkit.SolutionKitCustomization;
import com.l7tech.console.panels.solutionkit.SolutionKitsConfig;
import com.l7tech.console.panels.solutionkit.SolutionKitCustomizationClassLoader;
import com.l7tech.console.panels.solutionkit.SolutionKitUtils;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.PauseListenerAdapter;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.policy.solutionkit.SolutionKitManagerCallback;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;
import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Wizard panel which allows the user to select a solution kit file to install.
 */
public class SolutionKitLoadPanel extends WizardStepPanel<SolutionKitsConfig> {
    private static final Logger logger = Logger.getLogger(SolutionKitLoadPanel.class.getName());

    private static final String STEP_LABEL = "Choose Solution Kit File";
    private static final String STEP_DESC = "Specify the location of solution kit file (skar) to install.";

    private static final FileFilter SK_FILE_FILTER = FileChooserUtil.buildFilter(".skar", "Skar (*.skar)");
    private static final String SK_FILENAME = "SolutionKit.xml";
    private static final String SK_INSTALL_BUNDLE_FILENAME = "InstallBundle.xml";
    private static final String SK_UPGRADE_BUNDLE_FILENAME = "UpgradeBundle.xml";
    private static final String SK_DELETE_BUNDLE_FILENAME = "DeleteBundle.xml";
    private static final String SK_CUSTOMIZATION_JAR_FILENAME = "Customization.jar";

    private static final String BUNDLE_ELE_MAPPINGS = "Mappings";

    private JPanel mainPanel;
    private JTextField fileTextField;
    private JButton fileButton;

    private SolutionKitsConfig solutionKitsConfig;

    public SolutionKitLoadPanel() {
        super(null);
        initialize();
    }

    @Override
    public String getStepLabel() {
        return STEP_LABEL;
    }

    @Override
    public String getDescription() {
        return STEP_DESC;
    }

    @Override
    public boolean canAdvance() {
        return !fileTextField.getText().trim().isEmpty();
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public void readSettings(SolutionKitsConfig settings) throws IllegalArgumentException {
        solutionKitsConfig = settings;
        solutionKitsConfig.clear();
    }

    @Override
    public boolean onNextButton() {
        File file = new File(fileTextField.getText().trim());
        if (!file.exists()) {
            DialogDisplayer.showMessageDialog(this.getOwner(), "The file does not exist.", "Error", JOptionPane.ERROR_MESSAGE, null);
            return false;
        }

        if (!file.isFile()) {
            DialogDisplayer.showMessageDialog(this.getOwner(), "The file must be a file type.", "Error", JOptionPane.ERROR_MESSAGE, null);
            return false;
        }

        FileInputStream fis = null;
        ZipInputStream zis = null;
        try {
            final SolutionKit solutionKit = new SolutionKit();
            boolean hasRequiredSolutionKitFile = false, hasRequiredInstallBundleFile = false;
            final DOMSource installBundleSource = new DOMSource();
            final DOMSource upgradeBundleSource = new DOMSource();
            SolutionKitCustomizationClassLoader classLoader = null;

            fis = new FileInputStream(file);
            zis = new ZipInputStream(fis);
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
        } catch (IOException | SAXException | MissingRequiredElementException | TooManyChildElementsException | SolutionKitException e) {
            solutionKitsConfig.clear();
            final String msg = "Unable to open solution kit: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            DialogDisplayer.showMessageDialog(this.getOwner(), msg, "Error", JOptionPane.ERROR_MESSAGE, null);
            return false;
        } finally {
            ResourceUtils.closeQuietly(zis);
            ResourceUtils.closeQuietly(fis);
        }

        return true;
    }

    private void initialize() {
        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(fileTextField, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                notifyListeners();
            }
        }, 300);

        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onFileButton();
            }
        });

        setLayout(new BorderLayout());
        add(mainPanel);
    }

    private void onFileButton() {
        FileChooserUtil.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser fc) {
                fc.setDialogTitle("Choose Solution Kit");
                fc.setDialogType(JFileChooser.OPEN_DIALOG);
                fc.setMultiSelectionEnabled(false);
                fc.setFileFilter(SK_FILE_FILTER);

                int result = fc.showOpenDialog(SolutionKitLoadPanel.this);
                if (JFileChooser.APPROVE_OPTION != result) {
                    return;
                }

                File file = fc.getSelectedFile();
                if (file == null) {
                    return;
                }

                fileTextField.setText(file.getAbsolutePath());
                notifyListeners();
            }
        });
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