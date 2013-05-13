package com.l7tech.console.panels;

import com.l7tech.common.io.KeyGenParams;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.PleaseWaitDialog;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.ExceptionUtils;

import javax.security.auth.x500.X500Principal;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.InvocationTargetException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog that offers ways of creating a new key pair and associated metadata.
 */
public class  NewPrivateKeyDialog extends JDialog {
    protected static final Logger logger = Logger.getLogger(NewPrivateKeyDialog.class.getName());

    private static final String TITLE = "Create Private Key";
    private static final String DEFAULT_EXPIRY = Long.toString(365 * 5);

    private static class KeyType {
        private final int size;
        private final String label;
        private final int hsmSec;
        private final int softSec;
        private final String curveName;

        protected KeyType(int size, String label, int hsmSec, int softSec) {
            this.size = size;
            this.label = label;
            this.hsmSec = hsmSec;
            this.softSec = softSec;
            this.curveName = null;
        }

        private KeyType(String curveName, String label) {
            this.size = 0;
            this.label = label;
            this.hsmSec = 30;
            this.softSec = 30;
            this.curveName = curveName;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static KeyType rsasize(int bits, int hsmSec, int softSec) { return new KeyType(bits, bits + " bit RSA", hsmSec, softSec); }
    private static KeyType curvename(String name) { return new KeyType(name, "Elliptic Curve - " + name); }

    private static class SigHash {
        private final String label;
        private final String algorithm;

        private SigHash(String label, String algorithm) {
            this.label = label;
            this.algorithm = algorithm;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static SigHash sighash(String label, String alg) {
        return new SigHash(label, alg);
    }

    private final KeystoreFileEntityHeader keystoreInfo;

    private JPanel rootPanel;
    private JTextField dnField;
    private JComboBox cbKeyType;
    private JTextField aliasField;
    private JButton createButton;
    private JButton cancelButton;
    private JTextField expiryDaysField;
    private JCheckBox caCheckBox;
    private JComboBox cbSigHash;
    private SecurityZoneWidget securityZoneWidget;

    private String defaultDn;
    private String lastDefaultDn;
    private boolean usingHsm;

    private boolean confirmed;
    private String newAlias;
    private AsyncAdminMethods.JobId<X509Certificate> keypairJobId;

    final InputValidator validator = new InputValidator(this, getTitle());


    /**
     * Create a NewPrivateKeyDialog.
     *
     * @param owner  owner frame
     * @param keystoreInfo describes a keystore on the current Gateway which is not read-only.  Required.
     * @throws HeadlessException if running headless
     */
    public NewPrivateKeyDialog(Frame owner, KeystoreFileEntityHeader keystoreInfo) throws HeadlessException {
        super(owner, TITLE, true);
        this.keystoreInfo = keystoreInfo;
        initialize();
    }

    /**
     * Create a NewPrivateKeyDialog.
     *
     * @param owner  owner dialog
     * @param keystoreInfo describes a keystore on the current Gateway which is not read-only.  Required.
     * @throws HeadlessException if running headless
     */
    public NewPrivateKeyDialog(Dialog owner, KeystoreFileEntityHeader keystoreInfo) throws HeadlessException {
        super(owner, TITLE, true);
        this.keystoreInfo = keystoreInfo;
        initialize();
    }

    private void initialize() {
        if (keystoreInfo == null) throw new IllegalArgumentException("keystoreInfo is required");
        final Container contentPane = getContentPane();
        contentPane.removeAll();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(rootPanel, BorderLayout.CENTER);

        confirmed = false;

        validator.attachToButton(createButton, new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (createKey()) {
                    confirmed = true;
                    dispose();
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        // Populate DN field if it's uncustomized
        aliasField.setDocument(new PlainDocument() { // force to lowercase (Bug #6167)
            @Override
            public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                super.insertString(offs, str.toLowerCase(), a);
            }
        });
        aliasField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { onChange(); }
            @Override
            public void removeUpdate(DocumentEvent e) { onChange(); }
            @Override
            public void changedUpdate(DocumentEvent e) { onChange(); }

            private void onChange() {
                String dn = dnField.getText();
                lastDefaultDn = defaultDn;
                defaultDn = makeDefaultDn();
                if (dn == null || dn.length() < 1 || dn.equals(defaultDn) || dn.equals(lastDefaultDn))
                    if (defaultDn != null) dnField.setText(defaultDn);
            }
        });
        validator.constrainTextFieldToBeNonEmpty("Alias", aliasField, null);

        // Have DN field select all on focus if the DN is not customized
        dnField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                String dn = dnField.getText();
                if (dn == null) return;
                if (dn.equals(defaultDn) || dn.equals(lastDefaultDn))
                    dnField.selectAll();
            }

            @Override
            public void focusLost(FocusEvent e) {}
        });
        validator.constrainTextFieldToBeNonEmpty("Subject DN", dnField, new InputValidator.ComponentValidationRule(dnField) {
            @Override
            public String getValidationError() {
                String dn = dnField.getText();
                try {
                    new X500Principal(dn);
                    return null;
                } catch (IllegalArgumentException e) {
                    return "Bad DN: " + ExceptionUtils.getMessage(e);
                }
            }
        });

        validator.constrainTextFieldToNumberRange("Days until expiry", expiryDaysField, 1, Integer.MAX_VALUE);

        expiryDaysField.setDocument(new NumberField(8));
        expiryDaysField.setText(DEFAULT_EXPIRY);

        // Some EC curve names commented out because they aren't supported by RSA BSAFE Crypto-J as of version 4.1.0.1
        final KeyType dfltk;
        Collection<KeyType> types = new ArrayList<KeyType>(Arrays.asList(
                rsasize(512, 20, 1),
                rsasize(768, 50, 2),
                rsasize(1024, 100, 5),
                rsasize(1280, 60 * 7, 10),
        dfltk = rsasize(2048, 60 * 20, 17),
                curvename("sect163k1"),
                //curvename("sect163r1"),
                curvename("sect163r2"),
                //curvename("sect193r1"),
                //curvename("sect193r2"),
                curvename("sect233k1"),
                curvename("sect233r1"),
                //curvename("sect239k1"),
                curvename("sect283k1"),
                curvename("sect283r1"),
                curvename("sect409k1"),
                curvename("sect409r1"),
                curvename("sect571k1"),
                curvename("sect571r1"),
                //curvename("secp160k1"),
                //curvename("secp160r1"),
                //curvename("secp160r2"),
                //curvename("secp192k1"),
                curvename("secp192r1"),
                //curvename("secp224k1"),
                curvename("secp224r1"),
                //curvename("secp256k1"),
                curvename("secp256r1"),
                curvename("secp384r1"),
                curvename("secp521r1"),
                curvename("prime192v1"),
                curvename("prime256v1"),
                curvename("K-163"),
                curvename("B-163"),
                curvename("K-233"),
                curvename("B-233"),
                curvename("K-283"),
                curvename("B-283"),
                curvename("K-409"),
                curvename("B-409"),
                curvename("K-571"),
                curvename("B-571"),
                curvename("P-192"),
                curvename("P-224"),
                curvename("P-256"),
                curvename("P-384"),
                curvename("P-521")
        ));

        if (keystoreInfo.getKeyStoreType() != null && keystoreInfo.getKeyStoreType().toLowerCase().contains("pkcs11"))
            usingHsm = true;

        cbKeyType.setModel(new DefaultComboBoxModel(types.toArray()));
        cbKeyType.setSelectedItem(dfltk);

        final SigHash dflth;
        Collection<SigHash> hashes = new ArrayList<SigHash>(Arrays.asList(
        dflth = sighash("Auto", null),
                sighash("SHA-1", "SHA1"),
                sighash("SHA-256", "SHA256"),
                sighash("SHA-384", "SHA384"),
                sighash("SHA-512", "SHA512")
        ));
        cbSigHash.setModel(new DefaultComboBoxModel(hashes.toArray()));
        cbSigHash.setSelectedItem(dflth);

        securityZoneWidget.configure(EntityType.SSG_KEY_ENTRY, OperationType.CREATE, null);
    }

    /** @return the default DN for the current alias, or null if there isn't one. */
    private String makeDefaultDn() {
        String alias = aliasField.getText();
        if (alias == null)
            return null;
        alias = alias.trim().toLowerCase().replaceAll("[^a-zA-Z0-9\\.\\-_]", "");
        return "CN=" + alias;
    }

    /**
     * Do the actual work and attempt to create the new key.
     * @return true if a key was generated successfully
     *         false if there was an error (in which case an error message has already been displayed)
     */
    private boolean createKey() {
        final String alias = aliasField.getText();
        final int expiryDays = Integer.parseInt(expiryDaysField.getText());
        final boolean makeCaCert = caCheckBox.isSelected();
        final KeyType keyType = getSelectedKeyType();
        final SsgKeyMetadata metadata = securityZoneWidget.getSelectedZone() == null ? null: new SsgKeyMetadata(keystoreInfo.getOid(), alias, securityZoneWidget.getSelectedZone());
        //noinspection UnusedAssignment
        Throwable ouch = null;
        try {
            final X500Principal dn = parseDn();
            final JProgressBar bar = new JProgressBar();
            bar.setIndeterminate(true);
            final PleaseWaitDialog waitDlg = new PleaseWaitDialog(this, "Generating key...", bar);
            waitDlg.pack();
            waitDlg.setModal(true);
            Utilities.centerOnScreen(waitDlg);

            Callable<Object> callable = new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    if (keyType.curveName != null) {
                        // Elliptic curve
                        keypairJobId = getCertAdmin().generateEcKeyPair(keystoreInfo.getOid(), alias, metadata, dn, keyType.curveName, expiryDays, makeCaCert, getSigAlg(true));
                    } else {
                        // RSA
                        keypairJobId = getCertAdmin().generateKeyPair(keystoreInfo.getOid(), alias, metadata, dn, keyType.size, expiryDays, makeCaCert, getSigAlg(false));
                    }
                    newAlias = alias;
                    return null;
                }
            };

            Utilities.doWithDelayedCancelDialog(callable, waitDlg, 500L);
            return true;
        } catch (InvocationTargetException e) {
            ouch = e;
        } catch (InterruptedException e) {
            // Thread interrupted or user cancelled.  Can't happen in this case
            logger.log(Level.WARNING, "Unexpected InterruptedException", e);
            return false;
        }
        Throwable ex = ExceptionUtils.unnestToRoot(ouch);
        final String mess = "Unable to generate key pair: " + ExceptionUtils.getMessage(ex);
        logger.log(Level.WARNING, mess, ouch);
        JOptionPane.showMessageDialog(this, mess, "Key Pair Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    private X500Principal parseDn() throws InvocationTargetException {
        try {
            return new X500Principal(dnField.getText());
        } catch (IllegalArgumentException e) {
            throw new InvocationTargetException(e);
        }
    }

    private String getSigAlg(boolean useEcc) {
        SigHash hash = getSelectedSigHash();
        return KeyGenParams.getSignatureAlgorithm(
                useEcc ? KeyGenParams.ALGORITHM_EC : KeyGenParams.ALGORITHM_RSA,
                hash.algorithm );
    }

    private SigHash getSelectedSigHash() {
        Object h = cbSigHash.getSelectedItem();
        if (h instanceof SigHash)
            return (SigHash) h;
        throw new IllegalStateException("Unrecognized sig hash: " + h);
    }

    private KeyType getSelectedKeyType() {
        Object s = cbKeyType.getSelectedItem();
        if (s instanceof KeyType)
            return (KeyType)s;
        throw new IllegalStateException("Unrecognized key type: " + s);
    }

    private TrustedCertAdmin getCertAdmin() {
        return Registry.getDefault().getTrustedCertManager();
    }

    /** @return true if this dialog has been dismissed with the Create button. */
    public boolean isConfirmed() {
        return confirmed;
    }

    /** @return the new alias chosen. */
    public String getNewAlias() {
        return newAlias;
    }

    /** @return the JobId of the "generate keypair" job that was started on the server, or null if the dialog was not confirmed. */
    public AsyncAdminMethods.JobId<X509Certificate> getKeypairJobId() {
        return keypairJobId;
    }

    /** @return recommended maximum number of minutes to wait for the "generate keypair" job to finish. */
    public int getSecondsToWaitForJobToFinish() {
        final KeyType ks = getSelectedKeyType();
        return usingHsm ? ks.hsmSec : ks.softSec;
    }

    private void createUIComponents() {
        aliasField = new SquigglyTextField();
        dnField = new SquigglyTextField();
        expiryDaysField = new SquigglyTextField();
    }
}
