package com.l7tech.console.panels;

import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.PleaseWaitDialog;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.console.util.Registry;

import javax.security.auth.x500.X500Principal;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.Arrays;
import java.util.ArrayList;
import java.security.cert.X509Certificate;

/**
 * Dialog that offers ways of creating a new key pair and associated metadata.
 */
public class NewPrivateKeyDialog extends JDialog {
    protected static final Logger logger = Logger.getLogger(NewPrivateKeyDialog.class.getName());

    private static final String TITLE = "Create Private Key";
    private static final String DEFAULT_EXPIRY = Long.toString(365 * 5);

    private static class KeySize {
        private final int size;
        private final String label;
        private final int hsmSec;
        private final int softSec;

        protected KeySize(int size, String label, int hsmSec, int softSec) {
            this.size = size;
            this.label = label;
            this.hsmSec = hsmSec;
            this.softSec = softSec;
        }

        public String toString() {
            return label;
        }
    }

    private static final KeySize RSA512 = new KeySize(512, "512 bit RSA", 20, 1);
    private static final KeySize RSA768 = new KeySize(768, "768 bit RSA", 50, 2);
    private static final KeySize RSA1024 = new KeySize(1024, "1024 bit RSA", 100, 5);
    private static final KeySize RSA1280 = new KeySize(1280, "1280 bit RSA", 60 * 7, 10);
    private static final KeySize RSA2048 = new KeySize(2048, "2048 bit RSA", 60 * 20, 17);

    private final TrustedCertAdmin.KeystoreInfo keystoreInfo;

    private JPanel rootPanel;
    private JTextField dnField;
    private JComboBox cbKeyType;
    private JTextField aliasField;
    private JButton createButton;
    private JButton cancelButton;
    private JTextField expiryDaysField;

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
    public NewPrivateKeyDialog(Frame owner, TrustedCertAdmin.KeystoreInfo keystoreInfo) throws HeadlessException {
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
    public NewPrivateKeyDialog(Dialog owner, TrustedCertAdmin.KeystoreInfo keystoreInfo) throws HeadlessException {
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
            public void actionPerformed(final ActionEvent e) {
                if (createKey()) {
                    confirmed = true;
                    dispose();
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        // Populate DN field if it's uncustomized
        aliasField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { onChange(); }
            public void removeUpdate(DocumentEvent e) { onChange(); }
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
            public void focusGained(FocusEvent e) {
                String dn = dnField.getText();
                if (dn == null) return;
                if (dn.equals(defaultDn) || dn.equals(lastDefaultDn))
                    dnField.selectAll();
            }

            public void focusLost(FocusEvent e) {}
        });
        validator.constrainTextFieldToBeNonEmpty("Subject DN", dnField, new InputValidator.ComponentValidationRule(dnField) {
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

        Collection<KeySize> sizes = new ArrayList<KeySize>(Arrays.asList(
                RSA512,
                RSA768,
                RSA1024,
                RSA1280,
                RSA2048
        ));

        if (keystoreInfo.type != null && keystoreInfo.type.toLowerCase().contains("pkcs11"))
            usingHsm = true;

        cbKeyType.setModel(new DefaultComboBoxModel(sizes.toArray()));
        cbKeyType.setSelectedItem(RSA1024);
    }

    /** @return the default DN for the current alias, or null if there isn't one. */
    private String makeDefaultDn() {
        String alias = aliasField.getText();
        if (alias == null)
            return null;
        alias = alias.trim().toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
        return "CN=" + alias;
    }

    /**
     * Do the actual work and attempt to create the new key.
     * @return true if a key was generated successfully
     *         false if there was an error (in which case an error message has already been displayed)
     */
    private boolean createKey() {
        final String alias = aliasField.getText();
        final String dn = dnField.getText();
        final int expiryDays = Integer.parseInt(expiryDaysField.getText());
        final int keybits = getKeyBits();
        //noinspection UnusedAssignment
        Throwable ouch = null;
        try {
            final JProgressBar bar = new JProgressBar();
            bar.setIndeterminate(true);
            final PleaseWaitDialog waitDlg = new PleaseWaitDialog(this, "Generating key...", bar);
            waitDlg.pack();
            waitDlg.setModal(true);
            Utilities.centerOnScreen(waitDlg);

            Callable<Object> callable = new Callable<Object>() {
                public Object call() throws Exception {
                    keypairJobId = getCertAdmin().generateKeyPair(keystoreInfo.id, alias, dn, keybits, expiryDays);
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
        final String mess = "Unable to generate key pair: " + ExceptionUtils.getMessage(ouch);
        logger.log(Level.WARNING, mess, ouch);
        JOptionPane.showMessageDialog(this, mess, "Key Pair Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    private int getKeyBits() {
        return getSelectedKeySize().size;
    }

    private KeySize getSelectedKeySize() {
        Object s = cbKeyType.getSelectedItem();
        if (s instanceof KeySize)
            return (KeySize)s;
        throw new IllegalStateException("Unrecognized key size: " + s);
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
        final KeySize ks = getSelectedKeySize();
        return usingHsm ? ks.hsmSec : ks.softSec;
    }

    private void createUIComponents() {
        aliasField = new SquigglyTextField();
        dnField = new SquigglyTextField();
        expiryDaysField = new SquigglyTextField();
    }
}
