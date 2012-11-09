package com.l7tech.internal.license.gui;

import com.japisoft.xmlpad.XMLContainer;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.panels.EulaDialog;
import com.l7tech.console.panels.LicensePanel;
import com.l7tech.console.util.XMLContainerFactory;
import com.l7tech.gateway.common.License;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.internal.license.LicenseGenerator;
import com.l7tech.internal.license.LicenseGeneratorFeatureSets;
import com.l7tech.internal.license.LicenseGeneratorKeystoreUtils;
import com.l7tech.internal.license.LicenseSpec;
import com.l7tech.security.xml.DsigUtil;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.SyspropUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class LicenseGeneratorTopWindow extends JFrame {
    private static final Logger logger = Logger.getLogger(LicenseGeneratorTopWindow.class.getName());
    private static final long INACTIVITY_UPDATE_MILLIS = 500; // we'll fire an update when they've made a change then been idle for half a second

    public static final String PROPERTY_FEATURE_LABELS = "licenseGenerator.featureLabels";


    private LicensePanel licensePanel = new LicensePanel("Building License", true);
    private LicenseSpecPanel specPanel = new LicenseSpecPanel();

    private JPanel rootPanel;
    private JPanel displayPanel;
    private JPanel specHolderPanel;
    private JPanel xmlEditorPanel;

    private JButton signLicenseButton;
    private JButton saveLicenseButton;
    private JButton quitButton;
    private JButton stripSignatureButton;
    private JButton newLicenseButton;
    private JButton openLicenseButton;

    private boolean xmlChanged = false;
    private long xmlChangedTime = 0;
    private XMLContainer xmlContainer;

    private Action signLicenseAction;
    private Action stripSignaturesAction;
    private Action saveAsAction;
    private Action openAction;
    private Action newAction;
    private Action quitAction;
    private Action createNewKeystoreAction;
    private Action configureKeystoreAction;

    public LicenseGeneratorTopWindow() throws HeadlessException {
        setContentPane(rootPanel);
        init();
    }

    private Image loadAppIcon() {
        String path = "com/l7tech/internal/license/gui/resources/layer7_logo_small_32x32.png";
        URL url = LicenseGeneratorTopWindow.class.getClassLoader().getResource(path);
        return url == null ? (new ImageIcon()).getImage()
                           : Toolkit.getDefaultToolkit().createImage(url);
    }

    private void init() {
        setIconImage(loadAppIcon());
        getContentPane().setMinimumSize(new Dimension(1020, 760));
        setTitle("Layer 7 License Generator");
        setJMenuBar(makeMenuBar());
        displayPanel.add(licensePanel);
        specHolderPanel.setLayout(new BoxLayout(specHolderPanel, BoxLayout.X_AXIS));
        specHolderPanel.add(specPanel);

        xmlContainer = XMLContainerFactory.createXmlContainer(true);
        xmlContainer.setAutoFocus(false);

        xmlEditorPanel.setLayout(new BorderLayout());
        xmlEditorPanel.add(xmlContainer.getView(), BorderLayout.CENTER);

        //xmlContainer.getAccessibility().setText(SAMPLE_UNSIGNED);
        xmlContainer.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { xmlChanged = true; xmlChangedTime = System.currentTimeMillis(); }
            public void removeUpdate(DocumentEvent e) { xmlChanged = true; xmlChangedTime = System.currentTimeMillis(); }
            public void changedUpdate(DocumentEvent e) { xmlChanged = true; xmlChangedTime = System.currentTimeMillis(); }
        });

        specPanel.addPropertyChangeListener(LicenseSpecPanel.PROPERTY_LICENSE_SPEC, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateAllFromSpec();
            }
        });

        // Start monitoring GUI changes in the background
        Background.scheduleRepeated(new TimerTask() {
            public void run() {
                if (!xmlChanged) return;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (System.currentTimeMillis() - xmlChangedTime < INACTIVITY_UPDATE_MILLIS) return;
                        checkForChangedXml();
                    }
                });
            }
        }, 500, 500);

        licensePanel.setStatusNone("Nonexistent");
        licensePanel.setStatusInvalid("    License has errors    ");
        licensePanel.setStatusUnsigned("    Ready to be signed    ");
        licensePanel.setStatusValid("   Valid Signed License   ");

        signLicenseButton.setAction(getSignLicenseAction());
        stripSignatureButton.setAction(getStripSignaturesAction());
        saveLicenseButton.setAction(getSaveAsAction());
        quitButton.setAction(getQuitAction());
        newLicenseButton.setAction(getNewAction());
        openLicenseButton.setAction(getOpenAction());

        licensePanel.addEulaButtonActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                License license = licensePanel.getLicense();
                if (license == null)
                    return;

                final EulaDialog clickWrap;
                LicenseGeneratorTopWindow owner = LicenseGeneratorTopWindow.this;
                try {
                    clickWrap = new EulaDialog(owner, license);
                    clickWrap.pack();
                    Utilities.centerOnScreen(clickWrap);
                    clickWrap.setVisible(true);
                    if (!clickWrap.isEulaPresent()) {
                        JOptionPane.showMessageDialog(owner, "(This license would not trigger any EULA dialog as of SecureSpan version 4.0.)", "Nothing happens...", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(owner, "Unable to show EULA: " + ExceptionUtils.getMessage(e1), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        createTemplateLicense();

        //Utilities.attachDefaultContextMenu(xmlField);
        pack();
    }

    /** Initialize the currently-edited license to an empty document. */
    public void createBlankLicense() {
        // Initialize to an empty editor
        xmlContainer.getAccessibility().setText("");
        updateAllFromXml();
    }

    /** Initialize the currently-edited license to a skeleton document that is only missing the licensee name. */
    public void createTemplateLicense() {
        // Initialize the default spec
        specPanel.setDefaults();
        updateAllFromSpec();
    }

    private void checkForChangedXml() {
        if (!xmlChanged) return;
        xmlChangedTime = System.currentTimeMillis();
        updateAllFromXml();
        xmlChanged = false;
    }

    private JMenuBar makeMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(makeFileMenu());
        menuBar.add(makeEditMenu());
        //menuBar.add(makeSetupMenu());
        return menuBar;
    }

    private JMenu makeSetupMenu() {
        JMenu menu = new JMenu("Setup");
        menu.add(new JMenuItem(getConfigureKeystoreAction()));
        menu.add(new JMenuItem(getCreateNewKeystoreAction()));

        return menu;
    }

    private JMenu makeEditMenu() {
        JMenu menu = new JMenu("Edit");
        menu.add(new JMenuItem(getSignLicenseAction()));
        menu.add(new JMenuItem(getStripSignaturesAction()));

        return menu;
    }

    private JMenu makeFileMenu() {
        JMenu menu = new JMenu("File");
        menu.add(new JMenuItem(getNewAction()));
        menu.add(new JMenuItem(getOpenAction()));
        menu.add(new JMenuItem(getSaveAsAction()));
        menu.add(new JMenuItem(getQuitAction()));

        return menu;
    }

    private Action getCreateNewKeystoreAction() {
        if (createNewKeystoreAction != null) return createNewKeystoreAction;
        Action action = new AbstractAction("Create New Key Store") {
            public void actionPerformed(ActionEvent e) {
                // TODO
            }
        };
        action.setEnabled(false); // TODO enable after it's written
        action.putValue(Action.SHORT_DESCRIPTION, "Create a new signing certificate and key store.");
        action.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
        return createNewKeystoreAction = action;
    }

    private Action getConfigureKeystoreAction() {
        if (configureKeystoreAction != null) return configureKeystoreAction;
        Action action = new AbstractAction("Configure Existing Key Store") {
            public void actionPerformed(ActionEvent e) {
                // TODO
            }
        };
        action.setEnabled(false); // TODO enable after it's written
        action.putValue(Action.SHORT_DESCRIPTION, "Configure the License Generator to point at an existing key store.");
        action.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_K);
        return configureKeystoreAction = action;
    }

    private Action getSignLicenseAction() {
        if (signLicenseAction != null) return signLicenseAction;
        final Action action = new AbstractAction("Sign License") {
            public void actionPerformed(ActionEvent e) {
                checkForChangedXml();
                try {
                    X509Certificate cert = getSignerCert();
                    PrivateKey key = getSignerKey();

                    License license = licensePanel.getLicense();
                    if (license == null)
                        return;

                    String licenseXml = license.asXml();
                    Document licenseDoc = XmlUtil.stringToDocument(licenseXml);
                    Document signedLicense = LicenseGenerator.signLicenseDocument(licenseDoc, cert, key);

                    // Success.
                    licenseXml = XmlUtil.nodeToFormattedString(signedLicense);
                    xmlContainer.getAccessibility().setText(licenseXml);
                    xmlChanged = false; // suppress any spurious updates
                    doUpdateAllFromXml(licenseXml);
                } catch (Exception e1) {
                    final String msg = "Unable to sign certificate: " + ExceptionUtils.getMessage(e1);
                    logger.log(Level.WARNING, msg, e1);
                    JOptionPane.showMessageDialog(LicenseGeneratorTopWindow.this, msg, "Unable to Sign License",
                                                  JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        action.putValue(Action.SHORT_DESCRIPTION, "Sign this license with the currently-configured key store.");
        action.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
        return signLicenseAction = action;
    }

    private Action getStripSignaturesAction() {
        if (stripSignaturesAction != null) return stripSignaturesAction;
        AbstractAction action = new AbstractAction("Strip Signatures") {
            public void actionPerformed(ActionEvent e) {
                checkForChangedXml();
                try {
                    String licenseXml;
                    License license = licensePanel.getLicense();
                    if (license != null) {
                        // Use XML from license being displayed in bottom right panel, if possible
                        licenseXml = license.asXml();
                    } else {
                        // Otherwise try to use XML from XML editor window
                        licenseXml = xmlContainer.getAccessibility().getText();
                    }
                    Document licenseDoc = XmlUtil.stringToDocument(licenseXml);
                    DsigUtil.stripSignatures(licenseDoc.getDocumentElement());

                    // Success.
                    licenseXml = XmlUtil.nodeToFormattedString(licenseDoc);
                    xmlContainer.getAccessibility().setText(licenseXml);
                    xmlChanged = false; // suppress any spurious updates
                    doUpdateAllFromXml(licenseXml);
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "Unable to strip signatures", ex);
                } catch (SAXException ex) {
                    logger.log(Level.WARNING, "Unable to strip signatures", ex);
                    JOptionPane.showMessageDialog(LicenseGeneratorTopWindow.this,
                                                  "Unable to strip signature because XML is not well-formed: " + ExceptionUtils.getMessage(ex),
                                                  "Unable to Strip Signature",
                                                  JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        action.putValue(Action.SHORT_DESCRIPTION, "Remove any ds:Signature second-level elements from the current XML.");
        action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_G));
        return stripSignaturesAction = action;
    }

    private Action getSaveAsAction() {
        if (saveAsAction != null) return saveAsAction;
        AbstractAction action = new AbstractAction("Save As") {
            public void actionPerformed(ActionEvent e) {
                checkForChangedXml();
                JFileChooser fc = new JFileChooser(FileChooserUtil.getStartingDirectory());
                FileChooserUtil.addListenerToFileChooser(fc);
                fc.setFileFilter(new XmlFileFilter());
                fc.setMultiSelectionEnabled(false);
                int result = fc.showSaveDialog(LicenseGeneratorTopWindow.this);
                if (JFileChooser.APPROVE_OPTION != result)
                    return;

                File file = fc.getSelectedFile();

                if (file.getName().indexOf('.') < 0)
                    file = new File(file.getParent(), file.getName() + ".xml");

                if (file.isFile()) {
                    result = JOptionPane.showConfirmDialog(LicenseGeneratorTopWindow.this,
                                                           "Overwrite the existing file " + file.getName() + "?",
                                                           "Overwrite Existing File?",
                                                           JOptionPane.YES_NO_CANCEL_OPTION);
                    if (result != JOptionPane.YES_OPTION)
                        return;
                }

                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(file);
                    fos.write(xmlContainer.getAccessibility().getText().getBytes());
                    fos.close();
                    fos = null;
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(LicenseGeneratorTopWindow.this,
                                                  "Unable to save the license XML to the file " + file.getName() + "." +
                                                          "\n  The error was: " + ExceptionUtils.getMessage(ex),
                                                  "Unable to Save License XML",
                                                  JOptionPane.ERROR_MESSAGE);
                } finally {
                    if (fos != null) //noinspection EmptyCatchBlock
                        try { fos.close(); } catch (IOException ex) {}
                }
            }
        };
        action.putValue(Action.SHORT_DESCRIPTION, "Save the current license");
        action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
        return saveAsAction = action;
    }

    private Action getOpenAction() {
        if (openAction != null) return openAction;
        AbstractAction action = new AbstractAction("Open") {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(FileChooserUtil.getStartingDirectory());
                FileChooserUtil.addListenerToFileChooser(fc);
                fc.setFileFilter(new XmlFileFilter());
                fc.setMultiSelectionEnabled(false);
                int result = fc.showOpenDialog(LicenseGeneratorTopWindow.this);
                if (JFileChooser.APPROVE_OPTION != result)
                    return;

                File file = fc.getSelectedFile();

                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                    xmlContainer.getAccessibility().setText(new String( IOUtils.slurpStream(fis)));
                    fis.close();
                    fis = null;
                    updateAllFromXml();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(LicenseGeneratorTopWindow.this,
                                                  "Unable to open the license XML file " + file.getName() + "." +
                                                          "\n  The error was: " + ExceptionUtils.getMessage(ex),
                                                  "Unable to Open License XML",
                                                  JOptionPane.ERROR_MESSAGE);
                } finally {
                    if (fis != null) //noinspection EmptyCatchBlock
                        try { fis.close(); } catch (IOException ex) {}
                }
            }
        };
        action.putValue(Action.SHORT_DESCRIPTION, "Open an existing license");
        action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_O));
        return openAction = action;
    }

    private Action getNewAction() {
        if (newAction != null) return newAction;
        AbstractAction action = new AbstractAction("New") {
            public void actionPerformed(ActionEvent e) {
                createTemplateLicense();
            }
        };
        action.putValue(Action.SHORT_DESCRIPTION, "Start from a blank license document");
        action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
        return newAction = action;
    }

    private Action getQuitAction() {
        if (quitAction != null) return quitAction;
        AbstractAction action = new AbstractAction("Exit") {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        };
        action.putValue(Action.SHORT_DESCRIPTION, "Exit the License Generator");
        action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_X));
        return quitAction = action;
    }

    /** Parse the XML, and update the license panel and, if possible, the spec editor panel as well. */
    private void updateAllFromXml() {
        String xml = xmlContainer.getAccessibility().getText();

        if (xml == null || xml.trim().length() < 1) {
            licensePanel.setLicense(null);
            getSignLicenseAction().setEnabled(false);
            specPanel.setSpec(null);
            return;
        }

        doUpdateAllFromXml(xml);
    }

    private void doUpdateAllFromXml(String xml) {
        License license;
        try {
            license = new License(xml, getTrustedIssuers(), LicenseGeneratorFeatureSets.getFeatureSetExpander());
            licensePanel.setLicense(license);
            getSignLicenseAction().setEnabled(license != null && !license.isValidSignature());
            LicenseSpec spec = new LicenseSpec();
            spec.copyFrom(license);
            specPanel.setSpec(spec);
        } catch (RuntimeException e) {
            // Unchecked exceptions are probably bugs -- we'll log them
            final String msg = ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, "Unchecked license exception: " + msg, ExceptionUtils.unnestToRoot(e));
            licensePanel.setLicenseError(pad(ExceptionUtils.unnestToRoot(e).getClass().getName() + ": " + msg));
            getSignLicenseAction().setEnabled(false);
            // Leave spec panel alone if theres an error
            //pack();
        } catch (Exception e) {
            licensePanel.setLicenseError(pad(ExceptionUtils.unnestToRoot(e).getClass().getName() + ": " + ExceptionUtils.getMessage(e)));
            getSignLicenseAction().setEnabled(false);
            // Leave spec panel alone if theres an error
            //pack();
        }
    }

    private X509Certificate[] getTrustedIssuers() {
        X509Certificate cert = null;
        try {
            cert = getSignerCert();
        } catch (Exception e) {
            // Ignore here -- we'll deal with it when the user goes to sign something
        }
        try {
            if (cert == null)
                return new X509Certificate[] { getDemoSignerCert() };
            else
                return new X509Certificate[] { getDemoSignerCert(), cert };
        } catch (Exception e) {
            // Did the best we could.  Anyway this can't happen unless someone changes the hardcoded demo cert
            return null;
        }
    }

    /** Gather data from the License Spec panel and update the XML and the license panel from it. */
    private void updateAllFromSpec() {
        LicenseSpec spec = specPanel.getSpec();
        Document licenseDoc;
        String licenseXml;
        try {
            licenseDoc = LicenseGenerator.generateUnsignedLicense(spec, false);
        } catch (LicenseGenerator.LicenseGeneratorException e) {
            licensePanel.setLicenseError(pad("Invalid license specification: " + e.getClass().getName() + ": " + ExceptionUtils.getMessage(e)));
            getSignLicenseAction().setEnabled(false);
            try {
                licenseDoc = LicenseGenerator.generateUnsignedLicense(spec, true);
            } catch (LicenseGenerator.LicenseGeneratorException e1) {
                logger.log(Level.INFO, "LicenseGeneratorException that couldn't be overridden", e1);
                return;
            }
        }

        try {
            licenseXml = XmlUtil.nodeToFormattedString(licenseDoc);
        } catch (IOException e) {
            // This can't actually happen
            licensePanel.setLicenseError(pad("Invalid license specification: " + e.getClass().getName() + ": " + ExceptionUtils.getMessage(e)));
            getSignLicenseAction().setEnabled(false);
            return;
        }
        xmlContainer.getAccessibility().setText(licenseXml);
        xmlChanged = false; // just in case it fired another update
        doUpdateAllFromXml(licenseXml);
    }

    /* Add lines of spaces to s to pad it to at least 6 lines of text in the wrappingLabel. */
    private String pad(String s) {
        while (s.length() < (60 * 6)) {
            s += "\n                                                            ";
        }
        s += "\n";
        return s;
    }

    private KeyStore keyStore = null;
    private PrivateKey privateKey = null;
    private X509Certificate signerCert = null;

    private KeyStore getKeyStore() throws IOException, GeneralSecurityException {
        if(null == keyStore) {
            keyStore = LicenseGeneratorKeystoreUtils.loadKeyStore();
        }

        return keyStore;
    }

    private X509Certificate getSignerCert() throws GeneralSecurityException, IOException
    {
        if(null == signerCert) {
            signerCert = LicenseGeneratorKeystoreUtils.getSignerCert(getKeyStore());
        }

        return signerCert;
    }

    private PrivateKey getSignerKey() throws GeneralSecurityException, IOException
    {
        if(null == privateKey) {
            privateKey = LicenseGeneratorKeystoreUtils.getSignerKey(getKeyStore());
        }

        return privateKey;
    }

    private void mergeAndSaveProperties() throws IOException {
        Properties props = LicenseGeneratorMain.getProperties();
        String[] propsToMerge = {
                LicenseGeneratorKeystoreUtils.PROPERTY_KEYSTORE_PATH,
                LicenseGeneratorKeystoreUtils.PROPERTY_KEYSTORE_PASSWORD,
                LicenseGeneratorKeystoreUtils.PROPERTY_KEYSTORE_TYPE,
                LicenseGeneratorKeystoreUtils.PROPERTY_KEYSTORE_ALIAS,
                LicenseGeneratorKeystoreUtils.PROPERTY_KEYSTORE_ALIAS_PASSWORD,
        };

        for (String key : propsToMerge) {
            String sysVal = SyspropUtil.getProperty( key );
            if (sysVal != null && sysVal.length() > 0)
                props.setProperty(key, sysVal);
        }

        LicenseGeneratorMain.saveProperties();
    }

    private X509Certificate getDemoSignerCert() throws CertificateException, IOException {
        CertificateFactory cfac = CertificateFactory.getInstance("X.509");
        return (X509Certificate)cfac.generateCertificate(
                new ByteArrayInputStream(HexUtils.unHexDump(EXAMPLE_CERT)));
    }

    public static final String EXAMPLE_CERT = "308202063082016fa003020102020829b38eab36c17550300d06092a864886f70d01010505003015311330110603550403130a726f6f742e72696b6572301e170d3034303332343233303231315a170d3036303332343233313231315a3010310e300c0603550403130572696b657230819f300d06092a864886f70d010101050003818d0030818902818100ba38aabf7cc4009ec3792509d9f50a93062995a671900395807aec6a9132f788b2ef58c021772509b02bc0cdcd52d73678c953d992413d7a64cb788317b6df1a7bd51e8d60303ec560448478cc089c8980a48923bca1d721398c60a76b9165bce4e4ea447a04ced066df7648ad764dbce48f7235aac92c2f45cb0387dcaaba7d0203010001a3643062300f0603551d130101ff04053003010100300f0603551d0f0101ff0405030307a000301d0603551d0e041604147562fcfd17554344f77cc1c74c03ffcd24913ef0301f0603551d230418301680141fa3c0db54eb527fca4fbe5a876f4db42b06ec76300d06092a864886f70d0101050500038181001e7f5b2a886fb40ba2de576e1f8c225ad3c285ed8c7c27a913eb6fc624c4045d861e2107eeb7db230b0eff7445e660e359176752f177f961f0f68d1654857ab0a0ffab2dc77c486d4e1e4f7596f70b3ca212fb35a687816dd8aa8e1ee9aab12794fd3447d6e82404744df60197c3cfa1cbcd675c8f41a7aa9df2c4e49e9dfd93";
    public static final String KEY_ENCODED = "30820277020100300d06092a864886f70d0101010500048202613082025d02010002818100ba38aabf7cc4009ec3792509d9f50a93062995a671900395807aec6a9132f788b2ef58c021772509b02bc0cdcd52d73678c953d992413d7a64cb788317b6df1a7bd51e8d60303ec560448478cc089c8980a48923bca1d721398c60a76b9165bce4e4ea447a04ced066df7648ad764dbce48f7235aac92c2f45cb0387dcaaba7d02030100010281804ab6ad9b023dc959e967537aee5da80e70ec8244334fea8032fa1e9c6b011ddb549f3ee66706dc6f54a55947b8d741cd730ca37da9764f6a29c290e957bc612a19c9e3972473f956659d9037e644cedc1441fef16cc7ee4e943e13de3d7d04e67c0317272eb4f409686cf83d2d79ed20519921ca308198a682b62c9d7f404a81024100dce1d6618cd548cd5fb6697f3f115c95de12607f1649ca6de2e37e7b0346614b2d327b975133deefe3471c187ad2763199b743673fbcf6d75e577bf968055fd1024100d7d417f7b7c0a0768acebc269ab9c8e8f4149c3c3e319eda73e186dd972b6e06e276408b56751e6473e0a0584b2f00dc8fdbe870d01236c88481bb3321f826ed024100b60ad7020ced179453e6e5e9be93d3879cbfee91af4fdfab530c858862b995a43cbad78b6d9c5f87bbfc3656a29b64581ac524a32aafd58cc8af3778e5575091024005e36773feb366ad554426a5f6fab29c886c3786fd3b6556186b42beb0ed226755ae5c3c70f3690cdc8c78537059abf0588c6b5f088e36a869d5104268ded851024100d5ae95211764367ecabfb5fb0ec4a4e3527b14d1697aa643613cbad880e0d6172b8dea22e6e2d168099641fbe764c22d2745dc820c23dcbbf35bd010790f61ac";
    public static final String ENCODED_FORMAT = "PKCS#8";
    public static final String KEY_MODULUS = "130769082883256415220218729572806867212452634063428254105878307927530501228495400528599308231745918033415962177933256588503916078926080318123454063737198273465333737083531434970047904676465832036455535584135793333683990334843785383906065761524536597113590864967731493266420803704445384515950044052668002515581";
    public static final String KEY_PRIVATE_EXPONENT = "52465664667780706803796806008626939995486970109461563902222625824289897299266949379724281102972781310872321124045608568261012395734964308478235212962233407099665351810450969693299258191253569464257675482392194295806923755978670536307041904011677423671716680294679388235287677953431917875090301160075290495617";


    private static class XmlFileFilter extends FileFilter {
        public boolean accept(File file) {
            final String name = file.getName().toLowerCase();
            return file.isDirectory() || name.endsWith(".xml");
        }

        public String getDescription() {
            return "License files (*.xml)";
        }
    }
}
