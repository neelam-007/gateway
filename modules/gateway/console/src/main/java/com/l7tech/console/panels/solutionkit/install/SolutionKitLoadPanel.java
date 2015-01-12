package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.solutionkit.SolutionKitsConfig;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.PauseListenerAdapter;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.util.*;
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
import java.util.*;
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
    private static final String SK_ELE_BUNDLE = "Bundle";

    private JPanel mainPanel;
    private JTextField fileTextField;
    private JButton fileButton;

    private final Map<SolutionKit, Bundle> loaded = new HashMap<>();

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
        loaded.clear();
    }

    @Override
    public void storeSettings(SolutionKitsConfig settings) throws IllegalArgumentException {
        settings.setLoadedSolutionKits(loaded);
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
                    if (SK_FILENAME.equals(new File(entry.getName()).getName())) {
                        loadSolutionKitXml(zis);
                    } else {
                        logger.log(Level.WARNING, "Unexpected entry in solution kit: " + entry.getName());
                    }
                } else {
                    logger.log(Level.WARNING, "Unexpected entry in solution kit: " + entry.getName());
                }
                zis.closeEntry();
                entry = zis.getNextEntry();
            }
        } catch (IOException | SAXException | MissingRequiredElementException | TooManyChildElementsException e) {
            loaded.clear();
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

    private void loadSolutionKitXml(final ZipInputStream zis) throws IOException, SAXException, TooManyChildElementsException, MissingRequiredElementException {
        Document doc = XmlUtil.parse(new ByteArrayInputStream(IOUtils.slurpStream(zis)));
        Element docEle = doc.getDocumentElement();

        SolutionKit solutionKit = new SolutionKit();
        solutionKit.setSolutionKitGuid(DomUtils.getTextValue(DomUtils.findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_ID)));
        solutionKit.setSolutionKitVersion(DomUtils.getTextValue(DomUtils.findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_VERSION)));
        solutionKit.setName(DomUtils.getTextValue(DomUtils.findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_NAME)));
        solutionKit.setProperty(SolutionKit.SK_PROP_DESC_KEY, DomUtils.getTextValue(DomUtils.findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_DESC)));
        solutionKit.setProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, DomUtils.getTextValue(DomUtils.findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_TIMESTAMP)));

        Element bundleEle = DomUtils.findExactlyOneChildElementByName(docEle, SK_NS, SK_ELE_BUNDLE);
        if (bundleEle.getAttributeNodeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "l7") == null) {
            bundleEle.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE + ":" + "l7", SK_NS);
        }
        DOMSource source = new DOMSource();
        source.setNode(bundleEle);
        Bundle bundle = MarshallingUtils.unmarshal(Bundle.class, source, true);
        loaded.put(solutionKit, bundle);
    }
}