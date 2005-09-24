/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.internal.license.gui;

import com.l7tech.common.License;
import com.l7tech.common.gui.widgets.LicensePanel;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.Background;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.proxy.gui.util.IconManager;
import com.l7tech.internal.license.LicenseSpec;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.TimerTask;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;

import sun.security.rsa.RSAKeyFactory;
import org.bouncycastle.crypto.util.PrivateKeyFactory;

/**
 * @author mike
 */
public class LicenseGeneratorTopWindow extends JFrame {
    private LicensePanel licensePanel = new LicensePanel("Building License");
    private LicenseSpecPanel specPanel = new LicenseSpecPanel();
    private JPanel rootPanel;
    private JPanel displayPanel;
    private JPanel leftPanel;
    private JTextPane xmlField;
    private JButton issueLicenseButton;

    public boolean xmlChanged = false;

    public LicenseGeneratorTopWindow() throws HeadlessException {
        setContentPane(rootPanel);
        init();
    }

    private void init() {
        setIconImage( IconManager.getAppImage() );
        addWindowListener( new WindowAdapter() {
            public void windowClosing( final WindowEvent e ) {
                System.exit(0);
            }
        } );
        getContentPane().setMinimumSize(new Dimension(1020, 760));
        setTitle("Layer 7 License Generator");
        setJMenuBar(makeMenuBar());
        displayPanel.add(licensePanel);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        leftPanel.add(specPanel);
        xmlField.setText(SAMPLE_UNSIGNED);
        xmlField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { xmlChanged = true; }
            public void removeUpdate(DocumentEvent e) { xmlChanged = true; }
            public void changedUpdate(DocumentEvent e) { xmlChanged = true; }
        });
        Background.schedule(new TimerTask() {
            public void run() {
                if (!xmlChanged) return;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (!xmlChanged) return;
                        updateAllFromXml();
                        xmlChanged = false;
                    }
                });
            }
        }, 2000, 2000);

        issueLicenseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // TODO
            }
        });

        updateAllFromXml();
        Utilities.attachDefaultContextMenu(xmlField);
        pack();
    }

    private JMenuBar makeMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(makeFileMenu());
        return menuBar;
    }

    private JMenu makeFileMenu() {
        JMenu menu = new JMenu("File");

        menu.add(new JMenuItem(makeQuitAction()));

        return menu;
    }

    private Action makeQuitAction() {
        AbstractAction action = new AbstractAction("Exit") {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        };
        action.putValue(Action.SHORT_DESCRIPTION, "Exit the License Generator");
        action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_X));
        return action;
    }

    /** Parse the XML, and update the license panel and, if possible, the spec editor panel as well. */
    private void updateAllFromXml() {
        String xml = xmlField.getText();

        if (xml == null || xml.trim().length() < 1) {
            licensePanel.setLicense(null);
            return;
        }

        License license = null;
        try {
            license = new License(xml, null);
            licensePanel.setLicense(license);

            specPanel.setSpec(makeSpecFromLicense(license));

        } catch (Exception e) {
            licensePanel.setLicenseError(pad(e.getClass().getName() + ": " + ExceptionUtils.getMessage(e)));
            pack();
        }
    }

    private String pad(String s) {
        while (s.length() < (60 * 6)) {
            s += "\n                                                            ";
        }
        s += "\n";
        return s;
    }

    private LicenseSpec makeSpecFromLicense(License license) {
        // TODO - have to add special hack/hook to License so we can get at its private members
        return null;
    }

    private void getSignerKey() {
        RSAPrivateKey privKey = new RSAPrivateKey() {
            public BigInteger getPrivateExponent() {
                return new BigInteger(KEY_PRIVATE_EXPONENT);
            }

            public String getAlgorithm() {
                return "RSA";
            }

            public String getFormat() {
                return ENCODED_FORMAT;
            }

            public byte[] getEncoded() {
                try {
                    return HexUtils.unHexDump(KEY_ENCODED);
                } catch (IOException e) {
                    throw new RuntimeException(e); // can't happen
                }
            }

            public BigInteger getModulus() {
                return new BigInteger(KEY_MODULUS);
            }
        };
    }

    private void getSignerCert() throws CertificateException, IOException {
        CertificateFactory cfac = CertificateFactory.getInstance("X.509");
        X509Certificate signerCert = (X509Certificate)cfac.generateCertificate(
                new ByteArrayInputStream(HexUtils.unHexDump(EXAMPLE_CERT)));
    }


    public static final String SAMPLE_UNSIGNED =
            "<license Id=\"1001\" xmlns=\"http://l7tech.com/license\">\n" +
                    "    <description>Layer 7 Internal Developer License</description>\n" +
                    "    <valid>2005-09-24T06:54:01.718Z</valid>\n" +
                    "    <expires>2101-01-02T07:54:01.718Z</expires>\n" +
                    "    <host name=\"*\"/>\n" +
                    "    <ip address=\"*\"/>\n" +
                    "    <product name=\"Layer 7 SecureSpan Suite\">\n" +
                    "        <version major=\"3\" minor=\"4\"/>\n" +
                    "    </product>\n" +
                    "    <licensee contactEmail=\"developers@layer7tech.com\" name=\"Layer 7 Developer\"/>\n" +
                    "</license>";

    public static final String EXAMPLE_CERT = "308202063082016fa003020102020829b38eab36c17550300d06092a864886f70d01010505003015311330110603550403130a726f6f742e72696b6572301e170d3034303332343233303231315a170d3036303332343233313231315a3010310e300c0603550403130572696b657230819f300d06092a864886f70d010101050003818d0030818902818100ba38aabf7cc4009ec3792509d9f50a93062995a671900395807aec6a9132f788b2ef58c021772509b02bc0cdcd52d73678c953d992413d7a64cb788317b6df1a7bd51e8d60303ec560448478cc089c8980a48923bca1d721398c60a76b9165bce4e4ea447a04ced066df7648ad764dbce48f7235aac92c2f45cb0387dcaaba7d0203010001a3643062300f0603551d130101ff04053003010100300f0603551d0f0101ff0405030307a000301d0603551d0e041604147562fcfd17554344f77cc1c74c03ffcd24913ef0301f0603551d230418301680141fa3c0db54eb527fca4fbe5a876f4db42b06ec76300d06092a864886f70d0101050500038181001e7f5b2a886fb40ba2de576e1f8c225ad3c285ed8c7c27a913eb6fc624c4045d861e2107eeb7db230b0eff7445e660e359176752f177f961f0f68d1654857ab0a0ffab2dc77c486d4e1e4f7596f70b3ca212fb35a687816dd8aa8e1ee9aab12794fd3447d6e82404744df60197c3cfa1cbcd675c8f41a7aa9df2c4e49e9dfd93";
    public static final String KEY_ENCODED = "30820277020100300d06092a864886f70d0101010500048202613082025d02010002818100ba38aabf7cc4009ec3792509d9f50a93062995a671900395807aec6a9132f788b2ef58c021772509b02bc0cdcd52d73678c953d992413d7a64cb788317b6df1a7bd51e8d60303ec560448478cc089c8980a48923bca1d721398c60a76b9165bce4e4ea447a04ced066df7648ad764dbce48f7235aac92c2f45cb0387dcaaba7d02030100010281804ab6ad9b023dc959e967537aee5da80e70ec8244334fea8032fa1e9c6b011ddb549f3ee66706dc6f54a55947b8d741cd730ca37da9764f6a29c290e957bc612a19c9e3972473f956659d9037e644cedc1441fef16cc7ee4e943e13de3d7d04e67c0317272eb4f409686cf83d2d79ed20519921ca308198a682b62c9d7f404a81024100dce1d6618cd548cd5fb6697f3f115c95de12607f1649ca6de2e37e7b0346614b2d327b975133deefe3471c187ad2763199b743673fbcf6d75e577bf968055fd1024100d7d417f7b7c0a0768acebc269ab9c8e8f4149c3c3e319eda73e186dd972b6e06e276408b56751e6473e0a0584b2f00dc8fdbe870d01236c88481bb3321f826ed024100b60ad7020ced179453e6e5e9be93d3879cbfee91af4fdfab530c858862b995a43cbad78b6d9c5f87bbfc3656a29b64581ac524a32aafd58cc8af3778e5575091024005e36773feb366ad554426a5f6fab29c886c3786fd3b6556186b42beb0ed226755ae5c3c70f3690cdc8c78537059abf0588c6b5f088e36a869d5104268ded851024100d5ae95211764367ecabfb5fb0ec4a4e3527b14d1697aa643613cbad880e0d6172b8dea22e6e2d168099641fbe764c22d2745dc820c23dcbbf35bd010790f61ac";
    public static final String ENCODED_FORMAT = "PKCS#8";
    public static final String KEY_MODULUS = "130769082883256415220218729572806867212452634063428254105878307927530501228495400528599308231745918033415962177933256588503916078926080318123454063737198273465333737083531434970047904676465832036455535584135793333683990334843785383906065761524536597113590864967731493266420803704445384515950044052668002515581";
    public static final String KEY_PRIVATE_EXPONENT = "52465664667780706803796806008626939995486970109461563902222625824289897299266949379724281102972781310872321124045608568261012395734964308478235212962233407099665351810450969693299258191253569464257675482392194295806923755978670536307041904011677423671716680294679388235287677953431917875090301160075290495617";


}
