package com.l7tech.console.panels;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.CertUtils;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.util.*;
import com.l7tech.gateway.common.security.SpecialKeyType;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteSpecific;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.PasswordDoubleEntryDialog;
import com.l7tech.objectmodel.*;
import com.l7tech.security.cert.KeyUsageUtils;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.FileUtils;
import org.apache.commons.lang.ObjectUtils;

import javax.security.auth.x500.X500Principal;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.console.panels.PrivateKeyPropertiesDialog.SpecialKeyTypeRequirement.*;

/**
 *
 */
public class PrivateKeyPropertiesDialog extends JDialog {
    private static final String PROP_ALLOW_EC_FOR_DEFAULT_SSL = "com.l7tech.allowEcKeyForDefaultSsl";
    private static final boolean ALLOW_EC_FOR_DEFAULT_SSL = ConfigFactory.getBooleanProperty( PROP_ALLOW_EC_FOR_DEFAULT_SSL, false );

    private JList certList;
    private JButton destroyPrivateKeyButton;
    private JButton viewCertificateButton;
    private JButton generateCSRButton;
    private JButton cancelButton;
    private JButton replaceCertificateChainButton;
    private JPanel mainPanel;
    private JTextField locationField;
    private JTextField aliasField;
    private JTextField typeField;
    private JButton markAsSpecialPurposeButton;
    private JLabel caCapableLabel;
    private JButton exportKeyButton;
    private JPanel specialKeyTypeLabelsPanel;
    private SecurityZoneWidget zoneControl;
    private JButton okButton;
    private PrivateKeyManagerWindow.KeyTableRow subject;

    static enum SpecialKeyTypeRequirement {
        showFinalConfirmation,
        requireNotToBeAuditViewerKey,
        requireNotToHaveAnyOtherSpecialKeyType,
        requireCaCapableCert,
        requireTlsCapableCertIfRsa,
        requireRsaForEncryption,
        disallowEcc,
        warnOnAssignmentIfAuditViewerAlreadyExists
    }

    private static Map<SpecialKeyType, EnumSet<SpecialKeyTypeRequirement>> requirementsByType = new LinkedHashMap<SpecialKeyType, EnumSet<SpecialKeyTypeRequirement>>();
    static {
        requirementsByType.put(SpecialKeyType.SSL, EnumSet.of(
                showFinalConfirmation,
                requireNotToBeAuditViewerKey,
                requireTlsCapableCertIfRsa,
                disallowEcc
        ));

        requirementsByType.put(SpecialKeyType.CA, EnumSet.of(
                showFinalConfirmation,
                requireNotToBeAuditViewerKey,
                requireCaCapableCert
        ));

        requirementsByType.put(SpecialKeyType.AUDIT_SIGNING, EnumSet.of(
                showFinalConfirmation,
                requireNotToBeAuditViewerKey
        ));

        requirementsByType.put(SpecialKeyType.AUDIT_VIEWER, EnumSet.of(
                showFinalConfirmation,
                requireNotToHaveAnyOtherSpecialKeyType,
                warnOnAssignmentIfAuditViewerAlreadyExists
        ));
    }

    private class MakeSpecialKeyTypeAction extends AbstractAction {
        final SpecialKeyType type;
        final EnumSet<SpecialKeyTypeRequirement> requirements;

        private MakeSpecialKeyTypeAction(SpecialKeyType type, EnumSet<SpecialKeyTypeRequirement> requirements) {
            super("Make " + PrivateKeyManagerWindow.getLabelForSpecialKeyType(type), PrivateKeyManagerWindow.getIconForSpecialKeyType(type));
            this.type = type;
            this.requirements = requirements;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            makeSpecialKeyType(type, requirements);
        }
    }

    private Map<SpecialKeyType, MakeSpecialKeyTypeAction> makeSpecialKeyTypeActions = new LinkedHashMap<SpecialKeyType, MakeSpecialKeyTypeAction>();
    {
        for (Map.Entry<SpecialKeyType, EnumSet<SpecialKeyTypeRequirement>> entry : requirementsByType.entrySet()) {
            SpecialKeyType type = entry.getKey();
            EnumSet<SpecialKeyTypeRequirement> reqs = entry.getValue();
            makeSpecialKeyTypeActions.put(type, new MakeSpecialKeyTypeAction(type, reqs));
        }
    }

    private Logger logger = Logger.getLogger(PrivateKeyPropertiesDialog.class.getName());
    private boolean deleted;
    private boolean defaultKeyChanged;
    private boolean certificateChainChanged;
    private boolean securityZoneChanged;
    private final DefaultAliasTracker defaultAliasTracker;
    private final PermissionFlags flags;

    public PrivateKeyPropertiesDialog(JDialog owner, PrivateKeyManagerWindow.KeyTableRow subject, PermissionFlags flags, DefaultAliasTracker defaultAliasTracker)
    {
        super(owner, true);
        this.defaultAliasTracker = defaultAliasTracker;
        this.subject = subject;
        this.flags = flags;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Private Key Properties");

        AttemptedOperation deleteOperation = new AttemptedDeleteSpecific(EntityType.SSG_KEY_ENTRY, subject.getKeyEntry());
        AttemptedOperation updateOperation = new AttemptedUpdate(EntityType.SSG_KEY_ENTRY, subject.getKeyEntry());

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                close();
            }
        });

        destroyPrivateKeyButton.addActionListener(new SecureAction(deleteOperation) {
            @Override
            protected void performAction() {
                delete();
            }
        });

        replaceCertificateChainButton.addActionListener(new SecureAction(updateOperation) {
            @Override
            public void performAction() {
                assignCert();
            }
        });

        generateCSRButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                getCSR();
            }
        });

        viewCertificateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                viewCert();
            }
        });

        markAsSpecialPurposeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                markAsSpecialPurpose();
            }
        });

        exportKeyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportKey();
            }
        });

        Utilities.setEnterAction(this, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        Utilities.setEscAction(this, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        aliasField.setText(subject.getAlias());

        zoneControl.setEntityType(EntityType.SSG_KEY_ENTRY);
        zoneControl.setSelectedZone(subject.getKeyEntry().getSecurityZone());

        String location = subject.getKeystore().getName();
        if (subject.getKeystore().isReadonly())
            location = location + "  (Read-Only)";
        locationField.setText(location);
        typeField.setText(subject.getKeyType());
        populateList();

        caCapableLabel.setVisible(isCertChainCaCapable(subject));

        specialKeyTypeLabelsPanel.setLayout(new BoxLayout(specialKeyTypeLabelsPanel, BoxLayout.Y_AXIS));
        boolean atLeastOneActionEnabled = false;
        boolean atLeastOneLabelAdded = false;
        for (SpecialKeyType type : SpecialKeyType.values()) {
            if (subject.isDesignatedAs(type)) {
                String name = PrivateKeyManagerWindow.getLabelForSpecialKeyType(type);
                if (name != null) {
                    JLabel label = new JLabel("This is the " + name + ".");
                    label.setIcon(PrivateKeyManagerWindow.getIconForSpecialKeyType(type));
                    if (atLeastOneLabelAdded)
                        specialKeyTypeLabelsPanel.add(Box.createVerticalStrut(6));
                    specialKeyTypeLabelsPanel.add(label);
                    atLeastOneLabelAdded = true;
                }
            }

            MakeSpecialKeyTypeAction action = makeSpecialKeyTypeActions.get(type);
            boolean enable = true;
            if (subject.isDesignatedAs(type))
                enable = false;
            if (!defaultAliasTracker.isSpecialKeyMutable(type))
                enable = false;

            action.setEnabled(enable);
            if (enable) {
                atLeastOneActionEnabled = true;
            }
        }

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!ObjectUtils.equals(subject.getKeyEntry().getSecurityZone(), zoneControl.getSelectedZone())) {
                    subject.getKeyEntry().setSecurityZone(zoneControl.getSelectedZone());
                    try {
                        Registry.getDefault().getTrustedCertManager().updateKeyEntry(subject.getKeyEntry());
                        securityZoneChanged = true;
                    } catch (final UpdateException ex) {
                        showErrorMessage("Unable to Set Security Zone", "Error: " + ExceptionUtils.getMessage(ex), ex);
                    }
                }
                close();
            }
        });

        markAsSpecialPurposeButton.setEnabled(atLeastOneActionEnabled);
        if (!markAsSpecialPurposeButton.isEnabled())
            markAsSpecialPurposeButton.setToolTipText("Special-purpose key roles cannot be changed.");

        certList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                Object seled = certList.getSelectedValue();
                if (seled != null) {
                    viewCertificateButton.setEnabled(true);
                } else {
                    viewCertificateButton.setEnabled(false);
                }
            }
        });
        certList.setSelectedIndex(0);
        viewCertificateButton.setEnabled(true);

        KeystoreFileEntityHeader keystore = subject.getKeystore();
        if (keystore.isReadonly()) {
            destroyPrivateKeyButton.setEnabled(false);
            replaceCertificateChainButton.setEnabled(false);
            generateCSRButton.setEnabled(false);
        }

        if (!flags.canDeleteSome()) {
            destroyPrivateKeyButton.setEnabled(false);
            exportKeyButton.setEnabled(false);
            generateCSRButton.setEnabled(false);
        }
        if (!flags.canUpdateSome())
            replaceCertificateChainButton.setEnabled(false);

        Utilities.equalizeButtonSizes(new JButton[] {
                markAsSpecialPurposeButton,
                generateCSRButton,
                replaceCertificateChainButton,
                exportKeyButton,
        });
    }

    private void markAsSpecialPurpose() {
        JPopupMenu pop = new JPopupMenu();
        for (MakeSpecialKeyTypeAction action : makeSpecialKeyTypeActions.values()) {
            pop.add(action);
        }
        pop.show(markAsSpecialPurposeButton, 0, 0);
    }

    private boolean isCertChainCaCapable(PrivateKeyManagerWindow.KeyTableRow subject) {
        X509Certificate[] chain = subject.getKeyEntry().getCertificateChain();
        int pathLen = chain.length;
        for (X509Certificate cert : chain) {
            if (!CertUtils.isCertCaCapable(cert))
                return false;
            if (pathLen > cert.getBasicConstraints())
                return false;
            pathLen--;
        }
        return true;
    }

    public boolean isDefaultKeyChanged() {
        return defaultKeyChanged;
    }

    public boolean isCertificateChainChanged() {
        return certificateChainChanged;
    }

    public boolean isSecurityZoneChanged() {
        return securityZoneChanged;
    }

    class ListEntry {
        ListEntry(String subjectDn, X509Certificate cert) {
            this.subjectDn = subjectDn;
            this.cert = cert;
        }
        public X509Certificate cert;
        public String subjectDn;

        public X509Certificate getCert() {
            return cert;
        }

        public String getSubjectDn() {
            return subjectDn;
        }

        @Override
        public String toString() {
            return getSubjectDn();
        }
    }

    private void populateList() {
        X509Certificate[] data = subject.getKeyEntry().getCertificateChain();
        String[] dns = subject.getKeyEntry().getCertificateChainSubjectDns();
        ListEntry[] listData = new ListEntry[data.length];
        for (int i = 0; i < data.length; i++) {
            listData[i] = new ListEntry(dns[i], data[i]);
        }
        certList.setListData(listData);
    }

    private void viewCert() {
        ListEntry seled = (ListEntry)certList.getSelectedValue();
        if (seled == null) {
            return;
        }
        X509Certificate cert = seled.getCert();
        if (cert == null) {
            DialogDisplayer.showMessageDialog(this, "The Policy Manager is unable to read or display this certificate.", "Unable to Parse Certificate", JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        TrustedCert tc = new TrustedCert();
        tc.setCertificate(cert);
        tc.setName(cert.getSubjectDN().toString());
        tc.setSubjectDn(cert.getSubjectDN().toString());
        CertPropertiesWindow dlg = new CertPropertiesWindow(this, tc, false, false);
        dlg.setModal(true);
        dlg.setTitle("Certificate Properties");
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg);
    }

    private TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        return Registry.getDefault().getTrustedCertManager();
    }

    private void makeSpecialKeyType(final SpecialKeyType type, final EnumSet<SpecialKeyTypeRequirement> requirements) {
        if (requirements.contains(requireNotToBeAuditViewerKey) && subject.isDesignatedAs(SpecialKeyType.AUDIT_VIEWER)) {
            DialogDisplayer.showMessageDialog(
                    markAsSpecialPurposeButton,
                    "This key is already designated as the audit viewer private key.\n\n" +
                            "The Gateway is unable to permit the audit viewer key to be used for any other purpose.\n",
                    "Key Already Designated For Audit Viewing",
                    JOptionPane.WARNING_MESSAGE,
                    null);
            return;
        }

        if (requirements.contains(requireNotToHaveAnyOtherSpecialKeyType) && subject.getSpecialKeyTypeDesignations().size() > 0) {
            DialogDisplayer.showMessageDialog(
                    markAsSpecialPurposeButton,
                    "This key is already designated for another special purpose and cannot be used as the audit viewer key.\n\n" +
                    "The Gateway is unable to permit the audit viewer key to be used for any other purpose.\n",
                    "Key Already Designated For Conflicting Special Purpose",
                    JOptionPane.WARNING_MESSAGE,
                    null);
            return;
        }

        final boolean isRsa = subject.getKeyType().toUpperCase().startsWith("RSA");
        if (requirements.contains(requireTlsCapableCertIfRsa) && isRsa && !isCertChainSslCapable(subject)) {
            DialogDisplayer.showMessageDialog(
                    markAsSpecialPurposeButton,
                    "This key's certificate chain has a key usage disallowing use as an SSL server cert.\n" +
                    "Many SSL clients -- including the Policy Manager, and web browsers -- will refuse\n" +
                    "to connect to an SSL server that uses this key for its SSL server cert.",
                    "Unsuitable SSL Certificate",
                    JOptionPane.WARNING_MESSAGE,
                    null);
            return;
        }

        final boolean isEcc = subject.getKeyType().toUpperCase().startsWith("EC");
        if (requirements.contains(disallowEcc) && isEcc) {
            final String title = "Unsuitable Default SSL Key";
            final String mess = "This is an elliptic curve private key.\n\n" +
                    "Many SSL clients -- including the Gateway's browser-based admin applet when run\n" +
                    "with a standard Java install, and many web browsers -- will be unable to connect\n" +
                    "to an SSL server that uses this key as its SSL server certificate.";

            if (ALLOW_EC_FOR_DEFAULT_SSL) {
                DialogDisplayer.showConfirmDialog(markAsSpecialPurposeButton,
                        mess + "\n\nAre you sure you wish the cluster to use this as the default SSL private key?",
                        title,
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        new DialogDisplayer.OptionListener() {
                            @Override
                            public void reportResult(int option) {
                                if (option == JOptionPane.OK_OPTION)
                                    makeSpecialKeyType(type, satisfiedRequirements(requirements, disallowEcc));
                            }
                        });
            } else {
                DialogDisplayer.showMessageDialog(markAsSpecialPurposeButton, mess, title, JOptionPane.WARNING_MESSAGE, null);
            }
            return;
        }

        if (requirements.contains(requireCaCapableCert) && !isCertChainCaCapable(subject)) {
            DialogDisplayer.showConfirmDialog(
                    markAsSpecialPurposeButton,
                    "This certificate chain does not specifically enable use as a CA cert.\n" +
                    "Some software will reject client certificates signed by this key." +
                    "\n\nAre you sure you want the cluster to use this as the default CA private key?",
                    "Unsuitable CA Certificate",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    new DialogDisplayer.OptionListener() {
                        @Override
                        public void reportResult(int option) {
                            if (option == JOptionPane.YES_OPTION)
                                makeSpecialKeyType(type, satisfiedRequirements(requirements, requireCaCapableCert));
                        }
                    });
            return;
        }

        if (requirements.contains(requireRsaForEncryption) && !isRsa) {
            DialogDisplayer.showConfirmDialog(
                    markAsSpecialPurposeButton,
                    "This private key is an elliptic curve key.\n" +
                    "The Gateway currently cannot use elliptic curve keys for message-level decryption.\n" +
                    "An audit viewer policy will not be able to use this key to decrypt encrypted audit messages.\n" +
                    "\nAre you sure you want the cluster to use this as the default audit viewer private key?",
                    "Unsuitable Audit Viewer Certificate",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    new DialogDisplayer.OptionListener() {
                        @Override
                        public void reportResult(int option) {
                            if (option == JOptionPane.YES_OPTION)
                                makeSpecialKeyType(type, satisfiedRequirements(requirements, requireRsaForEncryption));
                        }
                    });
            return;
        }

        String what = PrivateKeyManagerWindow.getLabelForSpecialKeyType(type);
        if (requirements.contains(showFinalConfirmation)) {
            final String extraWarningText;
            if (requirements.contains(warnOnAssignmentIfAuditViewerAlreadyExists) && defaultAliasTracker.getSpecialKey(SpecialKeyType.AUDIT_VIEWER) != null) {
                extraWarningText = "<p>The current Audit Viewer Key will become available for use elsewhere in the Gateway.<br>" +
                        "Delete the existing key to ensure it cannot be used to decrypt any audits encrypted for it.<br>" +
                        "The new Audit Viewer key will no longer be available for any other usage in the Gateway.<br>" +
                        "Ensure this will not break any existing policies or configuration before continuing.<br>";
            } else {
                extraWarningText = "";
            }

            DialogDisplayer.showSafeConfirmDialog(
                    TopComponents.getInstance().getTopParent(),
                    "<html>Are you sure you wish to change the cluster " + what + "?<br>" +
                            extraWarningText +
                            "<p><br></p>All cluster nodes will need to be restarted before the change will fully take effect.",
                    "Confirm New Cluster " + what,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    new DialogDisplayer.OptionListener() {
                        @Override
                        public void reportResult(int option) {
                            if (option == JOptionPane.YES_OPTION)
                                makeSpecialKeyType(type, satisfiedRequirements(requirements, showFinalConfirmation, warnOnAssignmentIfAuditViewerAlreadyExists));
                        }
                    }
            );
            return;
        }

        try {
            defaultAliasTracker.assignSpecialKeyRole(subject.getKeyEntry(), type);
        } catch (Exception e) {
            showErrorMessage("Update Failed", "Failed to change default " + what + " key: " + ExceptionUtils.getMessage(e), e);
            return;
        }

        DialogDisplayer.showMessageDialog(this,
                "The " + what + " has been changed.\n\nThe change will not fully take effect until all cluster nodes have been restarted.",
                "Default " + what + " Updated",
                JOptionPane.INFORMATION_MESSAGE, null);
        defaultKeyChanged = true;
        close();
    }

    private EnumSet<SpecialKeyTypeRequirement> satisfiedRequirements(EnumSet<SpecialKeyTypeRequirement> requirements, SpecialKeyTypeRequirement... satisfied) {
        EnumSet<SpecialKeyTypeRequirement> ret = requirements.clone();
        for (SpecialKeyTypeRequirement requirement : satisfied) {
            ret.remove(requirement);
        }
        return ret;
    }

    private boolean isCertChainSslCapable(PrivateKeyManagerWindow.KeyTableRow subject) {
        return KeyUsageUtils.isCertSslCapable( subject.getCertificate() );
    }

    private void getCSR() {
        final TrustedCertAdmin admin = getTrustedCertAdmin();
        final GenerateCSRDialog dlg = new GenerateCSRDialog(this, subject.getSubjectDN());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if(!dlg.isOkD()){
                    return;
                }

                final byte[] csr;
                try {
                    CertGenParams params = new CertGenParams(new X500Principal(dlg.getCsrSubjectDN()), 365 * 2, false, null);
                    params.setHashAlgorithm(dlg.getSelectedHash());
                    csr = admin.generateCSR(subject.getKeystore().getOid(), subject.getAlias(), params);
                } catch (FindException e) {
                    logger.log(Level.WARNING, "cannot get csr from ssg", e);
                    DialogDisplayer.showMessageDialog(generateCSRButton, "Error getting CSR " + e.getMessage(),
                            "CSR Error", JOptionPane.ERROR_MESSAGE, null);
                    return;
                }
                // save CSR to file
                SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
                    @Override
                    public void useFileChooser(JFileChooser chooser) {
                        chooser.setDialogTitle("Save CSR to File");
                        chooser.setMultiSelectionEnabled(false);
                        FileFilter p10Filter = FileChooserUtil.buildFilter(".p10", "(*.p10) PKCS #10 Files");
                        FileFilter pemFilter = FileChooserUtil.buildFilter(".pem", "(*.pem) BASE64 PEM Files");
                        chooser.setFileFilter(p10Filter);
                        chooser.setFileFilter(pemFilter);

                        int ret = chooser.showSaveDialog(TopComponents.getInstance().getTopParent());
                        if (JFileChooser.APPROVE_OPTION == ret) {
                            String name = chooser.getSelectedFile().getPath();
                            // add an extension if not presented.
                            if (name.indexOf('.') < 0 ||
                                    (!name.endsWith(".p10") && !name.endsWith(".pem"))) {
                                if (chooser.getFileFilter() == pemFilter) {
                                    name = name + ".pem";
                                } else {
                                    name = name + ".p10";
                                }
                            }

                            byte[] bytes;
                            if (chooser.getFileFilter() == pemFilter) {
                                try {
                                    bytes = CertUtils.encodeCsrAsPEM(csr).getBytes();
                                } catch (IOException e) {
                                    logger.log(Level.WARNING, "error encoding as PEM", e);
                                    DialogDisplayer.showMessageDialog(generateCSRButton, "Error Encoding As PEM " + e.getMessage(),
                                            "Error", JOptionPane.ERROR_MESSAGE, null);
                                    return;
                                }
                            } else {
                                bytes = csr;
                            }

                            // save the file
                            try {
                                File newFile = new File(name);
                                //if file already exists, we need to ask for confirmation to overwrite. (Bug 6026)
                                if (newFile.exists()) {
                                    int result = JOptionPane.showOptionDialog(chooser, "The file '" + newFile.getName() + "' already exists.  Overwrite?",
                                            "Warning",JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                                    if (result != JOptionPane.YES_OPTION)
                                        return;
                                }
                                FileUtils.save(new ByteArrayInputStream(bytes), newFile);
                            } catch (IOException e) {
                                logger.log(Level.WARNING, "error saving CSR", e);
                                DialogDisplayer.showMessageDialog(generateCSRButton, "Error Saving CSR " + e.getMessage(),
                                        "Error", JOptionPane.ERROR_MESSAGE, null);
                            }
                        }
                    }
                });
            }
        });
    }

    private void assignCert() {
        final CertImportMethodsPanel sp = new CertImportMethodsPanel(
                            new CertDetailsPanel(null) {
                                @Override
                                public boolean canFinish() {
                                    return true;
                                }
                            }, false);

        final AddCertificateWizard w = new AddCertificateWizard(this, sp);
        w.setTitle("Assign Certificate to Private Key");
        w.addWizardListener(new WizardAdapter() {
            @Override
            public void wizardFinished(WizardEvent we) {
                Object o = w.getWizardInput();

                if (o == null) return;
                if (!(o instanceof TrustedCert)) {
                    // shouldn't happen
                    throw new IllegalStateException("Wizard returned a " + o.getClass().getName() + ", was expecting a " + TrustedCert.class.getName());
                }

                final TrustedCertAdmin admin = getTrustedCertAdmin();
                try {
                    X509Certificate[] certChain = sp.getCertChain();
                    if (certChain != null) {
                        subject.getKeyEntry().setCertificateChain(certChain);
                        admin.updateKeyEntry(subject.getKeyEntry());
                        //re-get the entry from the ssg after assigning (weird but see bzilla #3852)
                        List<SsgKeyEntry> tmp = admin.findAllKeys(subject.getKeystore().getOid(), true);
                        for (SsgKeyEntry ske : tmp) {
                            if (ske.getAlias().equalsIgnoreCase(subject.getAlias())) {
                                subject.setKeyEntry(ske);
                                break;
                            }
                        }
                        populateList();
                        certificateChainChanged = true;
                    }
                } catch (GeneralSecurityException e) {
                    showErrorMessage("Error Assigning Certificate",
                            "Error Assigning new Cert.  Make sure the " +
                            "cert you choose is related to the public " +
                            "key it is being assigned for.",
                            ExceptionUtils.getDebugException(e));
                } catch (ObjectModelException e) {
                    showErrorMessage("Error Assigning Certificate",
                            "Error Assigning new Cert.  Make sure the " +
                            "cert you choose is related to the public " +
                            "key it is being assigned for.",
                            ExceptionUtils.getDebugException(e));
                } catch (IOException e) {
                    showErrorMessage("Error Assigning Certificate",
                            "Error Assigning new Cert.  Make sure the " +
                            "cert you choose is related to the public " +
                            "key it is being assigned for.",
                            ExceptionUtils.getDebugException(e));
                }
            }
        });

        w.pack();
        Utilities.centerOnScreen(w);
        DialogDisplayer.display(w);
    }

    private void exportKey() {
        final SsgKeyEntry entry = subject.getKeyEntry();
        final String kstype = subject.getKeystore().getKeyStoreType();
        final boolean hardwareHint = kstype != null && kstype.contains("HARDWARE");

        final PasswordDoubleEntryDialog passDlg = new PasswordDoubleEntryDialog(this, "Enter Export Passphrase");
        DialogDisplayer.display(passDlg, new Runnable() {
            @Override
            public void run() {
                if (!passDlg.isConfirmed())
                    return;

                char[] passphrase = passDlg.getPassword();

                try {
                    byte[] p12bytes = getTrustedCertAdmin().exportKey(entry.getKeystoreId(), entry.getAlias(), entry.getAlias(), passphrase);
                    saveKeystoreBytes(p12bytes);
                } catch (UnrecoverableKeyException e) {
                    String hardwaremsg = hardwareHint ? " because it is stored in a hardware keystore" : "";
                    showErrorMessage("Unable to Export Key", "This private key cannot be exported" + hardwaremsg + ".", e);
                } catch (GeneralSecurityException e) {
                    showErrorMessage("Unable to Export Key", "Unable to export key: " + ExceptionUtils.getMessage(e), e);
                } catch (ObjectModelException e) {
                    showErrorMessage("Unable to Export Key", "Unable to export key: " + ExceptionUtils.getMessage(e), e);
                }
            }
        });
    }

    private void saveKeystoreBytes(final byte[] p12bytes) {
        FileChooserUtil.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser chooser) {
                chooser.setDialogTitle("Save As PKCS#12 File");
                chooser.setMultiSelectionEnabled(false);
                FileFilter p12Fil = FileChooserUtil.buildFilter(".p12", "(*.p12) PKCS #12 (PFX) Keystore Files");
                chooser.setFileFilter(p12Fil);

                int ret = chooser.showSaveDialog(TopComponents.getInstance().getTopParent());
                if (JFileChooser.APPROVE_OPTION == ret) {
                    try {
                        String name = chooser.getSelectedFile().getPath();
                        if (!name.endsWith(".p12")) {
                            name = name + ".p12";
                        }

                        File newFile = new File(name);

                        //if file already exists, we need to ask for confirmation to overwrite.
                        if (newFile.exists()) {
                            int result = JOptionPane.showOptionDialog(chooser, "The file '" + newFile.getName() + "' already exists.  Overwrite?",
                                    "Warning",JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                            if (result != JOptionPane.YES_OPTION) {
                                return;
                            }
                        }
                        FileUtils.save(new ByteArrayInputStream(p12bytes), newFile);
                    } catch (IOException e) {
                        showErrorMessage("Unable to Save", "Unable to save PKCS#12 file: " + ExceptionUtils.getMessage(e), e);
                    }
                }
            }
        });
    }

    private void close() {
        dispose();
    }

    private void delete() {

        final KeystoreFileEntityHeader keystore = subject.getKeystore();
        if (keystore.isReadonly()) {
            JOptionPane.showMessageDialog(this, "This keystore is read-only.", "Unable to Remove Key", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String confirmationDialogTitle = "Confirm Private Key Deletion";
        String alias = subject.getAlias();
        if (alias.length() > 50) {
            alias = alias.substring(0, 49) + "...";
        }

        String subjectDn = subject.getKeyEntry().getSubjectDN();
        if (subjectDn.length() > 50) {
            subjectDn = subjectDn.substring(0,42) + "...";
        }

        boolean isAuditViewer = subject.isDesignatedAs(SpecialKeyType.AUDIT_VIEWER);
        String confirmationDialogMessage =
            "<html><center>This will delete this key and cannot be undone. " +
                    (isAuditViewer ? "Encrypted audit records will no longer be viewable. " : "") +
                    "The change will not fully take effect until all cluster nodes have been restarted.</center><p>" +
                "<center>Really delete the private key " + alias + " (" + subjectDn + ")?</center></html>";

        DialogDisplayer.showSafeConfirmDialog(
            this,
            confirmationDialogMessage,
            confirmationDialogTitle,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE,
            new DialogDisplayer.OptionListener() {
                @Override
                public void reportResult(int option) {
                    if (option == JOptionPane.CANCEL_OPTION) {
                        return;
                    }
                    deleted = true;
                    dispose();
                }
            }
        );
    }

    public boolean isDeleted() {
        return deleted;
    }

    private void showErrorMessage(String title, String msg, Throwable e) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, null);
    }
}