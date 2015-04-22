package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.solutionkit.SolutionKitsConfig;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.PauseListenerAdapter;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.util.*;
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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
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
    private static final String SK_NS = "http://ns.l7tech.com/2010/04/gateway-management";
    private static final String SK_FILENAME = "SolutionKit.xml";
    private static final String SK_ELE_ROOT = "SolutionKit";
    private static final String SK_ELE_ID = "Id";
    private static final String SK_ELE_VERSION = "Version";
    private static final String SK_ELE_NAME = "Name";
    private static final String SK_ELE_DESC = "Description";
    private static final String SK_ELE_TIMESTAMP = "TimeStamp";
    private static final String SK_ELE_IS_COLLECTION = "IsCollection";
    private static final String SK_ELE_CUSTOM_UI = "CustomUI";
    private static final String SK_ELE_CUSTOM_CALLBACK = "CustomCallback";
    private static final String SK_ELE_DEPENDENCIES = "Dependencies";
    private static final String SK_ELE_FEATURE_SET = "FeatureSet";
    private static final String SK_ELE_BUNDLE = "Bundle";
    private static final String SK_ELE_UPGRADE = "Upgrade";
    private static final String SK_ELE_UNINSTALL = "Uninstall";

    private static final String BUNDLE_ELE_MAPPINGS = "Mappings";

    private JPanel mainPanel;
    private JTextField fileTextField;
    private JButton fileButton;

    private Map<SolutionKit, Bundle> loaded;
    private Map<SolutionKit, Map<String, String>> resolvedEntityIds;

    @Nullable
    private SolutionKit solutionKitToUpgrade;

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
        solutionKitToUpgrade = settings.getSolutionKitToUpgrade();
        loaded = settings.getLoadedSolutionKits();
        loaded.clear();
        resolvedEntityIds = settings.getResolvedEntityIds();
        resolvedEntityIds.clear();
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
            fis = new FileInputStream(file);
            zis = new ZipInputStream(fis);
            ZipEntry entry = zis.getNextEntry();

            while (entry != null) {
                if (!entry.isDirectory()) {
                    final String fileName = new File(entry.getName()).getName();
                    switch (fileName) {
                        case SK_FILENAME:
                            loadSolutionKitXml(zis);
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
        } catch (IOException | SAXException | MissingRequiredElementException | TooManyChildElementsException | SolutionKitException e) {
            solutionKitToUpgrade = null;
            loaded.clear();
            resolvedEntityIds.clear();
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

    private void loadSolutionKitXml(final ZipInputStream zis) throws IOException, SAXException, TooManyChildElementsException, MissingRequiredElementException, SolutionKitException {
        final Document doc = XmlUtil.parse(new ByteArrayInputStream(IOUtils.slurpStream(zis)));
        final Element docEle = doc.getDocumentElement();
        final String solutionKitGuid = DomUtils.getTextValue(DomUtils.findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_ID));

        // 1) load solution kit for install or 2) load matching solution kit for upgrade
        if (solutionKitToUpgrade == null || solutionKitToUpgrade.getSolutionKitGuid().equals(solutionKitGuid)) {
            final SolutionKit solutionKit = new SolutionKit();

            // upgrade
            if (solutionKitToUpgrade != null) {
                solutionKit.setGoid(solutionKitToUpgrade.getGoid());
                solutionKit.setVersion(solutionKitToUpgrade.getVersion());
            }

            solutionKit.setSolutionKitGuid(solutionKitGuid);
            solutionKit.setSolutionKitVersion(DomUtils.getTextValue(DomUtils.findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_VERSION)));
            solutionKit.setName(DomUtils.getTextValue(DomUtils.findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_NAME)));
            solutionKit.setProperty(SolutionKit.SK_PROP_DESC_KEY, DomUtils.getTextValue(DomUtils.findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_DESC)));
            solutionKit.setProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, DomUtils.getTextValue(DomUtils.findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_TIMESTAMP)));

            final Element featureSetElement = DomUtils.findFirstChildElementByName(docEle, SK_NS, SK_ELE_FEATURE_SET);
            if (featureSetElement != null) {
                solutionKit.setProperty(SolutionKit.SK_PROP_FEATURE_SET_KEY, DomUtils.getTextValue(featureSetElement));
            }

            Element bundleEle = DomUtils.findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_BUNDLE);
            if (bundleEle.getAttributeNodeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "l7") == null) {
                bundleEle.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE + ":" + "l7", SK_NS);
            }

            // upgrade - get bundle, find upgrade mappings to replace install mappings with upgrade mappings
            if (solutionKitToUpgrade != null) {
                bundleEle = DomUtils.findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_BUNDLE);
                Element upgradeEle = DomUtils.findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_UPGRADE);
                Element upgradeMappingEle = DomUtils.findFirstDescendantElement(upgradeEle, null, BUNDLE_ELE_MAPPINGS);
                if (upgradeMappingEle == null) {
                    throw new SolutionKitException("Expected <" + BUNDLE_ELE_MAPPINGS + "> element during upgrade in " + SK_FILENAME + ".");
                }
                DomUtils.removeChildElementsByName(bundleEle, SK_NS, BUNDLE_ELE_MAPPINGS);
                bundleEle.appendChild(upgradeMappingEle);
            }

            // save uninstall bundle for later use
            Element uninstallEle = DomUtils.findFirstChildElementByName(docEle, SK_NS, SK_ELE_UNINSTALL);
            if (uninstallEle != null) {
                Element uninstallBundleEle = DomUtils.findExactlyOneChildElementByName(uninstallEle, SK_NS, SK_ELE_BUNDLE);
                solutionKit.setUninstallBundle(XmlUtil.nodeToString(uninstallBundleEle));
            }

            DOMSource source = new DOMSource();
            source.setNode(bundleEle);
            Bundle bundle = MarshallingUtils.unmarshal(Bundle.class, source, true);

            // upgrade - update previously resolved mapping target IDs
            if (solutionKitToUpgrade != null) {
                Map<String, String> previouslyResolvedIds = resolvedEntityIds.get(solutionKitToUpgrade);
                for (Mapping mapping : bundle.getMappings()) {
                    if (previouslyResolvedIds != null) {
                        String resolvedId = previouslyResolvedIds.get(mapping.getSrcId());
                        if (resolvedId != null) {
                            mapping.setTargetId(resolvedId);
                        }
                    }
                }
            }

            loaded.put(solutionKit, bundle);
        }
    }
}