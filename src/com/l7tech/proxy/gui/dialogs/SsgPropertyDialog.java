package com.l7tech.proxy.gui.dialogs;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.CertificatePanel;
import com.l7tech.common.gui.widgets.ContextMenuTextField;
import com.l7tech.common.gui.widgets.WrappingLabel;
import com.l7tech.proxy.ClientProxy;
import com.l7tech.proxy.datamodel.*;
import com.l7tech.proxy.gui.Gui;
import com.l7tech.proxy.gui.util.IconManager;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panel for editing properties of an SSG object.
 * User: mike
 * Date: May 26, 2003
 * Time: 11:14:36 AM
 */
public class SsgPropertyDialog extends PropertyDialog implements SsgListener {
    private static final Logger log = Logger.getLogger(SsgPropertyDialog.class.getName());
    private static final Ssg referenceSsg = new Ssg(); // SSG bean with default values for all

    // Model
    private Ssg ssg; // The real Ssg instance, to which changes may be committed.
    private ClientProxy clientProxy;  // The Client Proxy we're attached to, so we can display the bind port.

    // View
    private int gridY = 0; // Used for layout

    //   View for General pane
    private JComponent generalPane;
    private JTextField fieldServerAddress;

    //   View for Identity pane
    private SsgIdentityPanel ssgIdentityPane;

    //   View for Network pane
    private SsgNetworkPanel networkPane;

    //   View for Bridge Policy pane
    private JComponent bridgePolicyPane;
    private JCheckBox cbUseSslByDefault;

    //   View for Service Policies pane
    private SsgPoliciesPanel policiesPane;

    /** Create an SsgPropertyDialog ready to edit an Ssg instance. */
    private SsgPropertyDialog(ClientProxy clientProxy, final Ssg ssg) {
        super("Gateway Properties");
        this.clientProxy = clientProxy;
        tabbedPane.add("General", getGeneralPane());
        tabbedPane.add("Identity", getIdentityPane(ssg));
        tabbedPane.add("Network", getNetworkPane());
        tabbedPane.add("Bridge Policy", getBridgePolicyPane());
        tabbedPane.add("Service Policies", getPoliciesPane());
        ssg.addSsgListener(this);
        setSsg(ssg);
        pack();
    }

    protected void finalize() throws Throwable {
        ssg.removeSsgListener(this);
        super.finalize();
    }

    /**
     * Attempt to build an "edit properties" dialog box for the given Ssg.
     * @param ssg The ssg whose properties we intend to edit
     * @return The property dialog that will edit said properties.  Call show() on it to run it.
     */
    public static SsgPropertyDialog makeSsgPropertyDialog(ClientProxy clientProxy, final Ssg ssg) {
        return new SsgPropertyDialog(clientProxy, ssg);
    }

    private JComponent getBridgePolicyPane() {
        if (bridgePolicyPane == null) {
            int y = 0;
            JPanel outerPane = new JPanel(new GridBagLayout());
            bridgePolicyPane = new JScrollPane(outerPane);
            bridgePolicyPane.setBorder(BorderFactory.createEmptyBorder());
            JPanel pane = new JPanel(new GridBagLayout());
            pane.setBorder(BorderFactory.createTitledBorder("  Client-Side Policy  "));
            outerPane.add(pane,
                          new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                                                 GridBagConstraints.NORTHWEST,
                                                 GridBagConstraints.HORIZONTAL,
                                                 new Insets(14, 5, 0, 5), 0, 0));
            outerPane.add(Box.createGlue(),
                          new GridBagConstraints(0, 99, 1, 1, 1.0, 1.0,
                                                 GridBagConstraints.CENTER,
                                                 GridBagConstraints.BOTH,
                                                 new Insets(0, 0, 0, 0), 0, 0));
            cbUseSslByDefault = new JCheckBox("Use SSL By Default");
            pane.add(cbUseSslByDefault,
                     new GridBagConstraints(0, y++, 1, 1, 1.0, 0.0,
                                            GridBagConstraints.WEST,
                                            GridBagConstraints.HORIZONTAL,
                                            new Insets(5, 15, 5, 0), 0, 0));
        }
        return bridgePolicyPane;
    }

    private SsgPoliciesPanel getPoliciesPane() {
        if (policiesPane == null) {
            policiesPane = new SsgPoliciesPanel();
        }
        return policiesPane;
    }

    private SsgIdentityPanel getIdentityPane(final Ssg ssg) {
        if (ssgIdentityPane != null) return ssgIdentityPane;

        if (ssg.isFederatedGateway()) {
            ssgIdentityPane = new FederatedSsgIdentityPanel(ssg);
        } else {
            TrustedSsgIdentityPanel tp = new TrustedSsgIdentityPanel(ssg);
            ssgIdentityPane = tp;
            tp.getUseClientCredentialCheckBox().addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateIdentityEnableState();
                }
            });

        }

        ssgIdentityPane.getClientCertButton().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    X509Certificate cert = ssg.getClientCertificate();
                    if (cert == null) {
                        final String close = "   Close   ";
                        int r = JOptionPane.showOptionDialog(Gui.getInstance().getFrame(),
                                                             "A client certificate for the SecureSpan Gateway " + ssgName() + "\n" +
                                                             "was not found.  Import a client certificate below.",
                                                             "Client Certificate Not Found",
                                                             JOptionPane.YES_NO_OPTION,
                                                             JOptionPane.INFORMATION_MESSAGE,
                                                             null,
                                                             new String[] {"Import Client Certificate", close},
                                                             close);
                        if (r == 0)
                            importClientCertificate();
                        return;
                    }
                    new CertDialog(cert, "Client Certificate", "Client Certificate for Gateway " + ssgName()).show();
                } catch (Exception e1) {
                    log.log(Level.SEVERE, "Unable to access client certificate", e1);
                    e1.printStackTrace();
                    Gui.errorMessage("Unable to Access Client Certificate",
                                         "Unable to access client certificate for the SecureSpan Gateway " + ssgName() + ".",
                                         e1);
                    }
                }
            });

        final ActionListener ssgCertButtonAction = new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            X509Certificate cert = ssg.getServerCertificate();
                            if (cert == null) {
                                JOptionPane.showMessageDialog(Gui.getInstance().getFrame(),
                                        "A certificate for the SecureSpan gateway " + ssgName() + "\n" +
                                        "was not found.",
                                        "Gateway Certificate Not Found",
                                        JOptionPane.INFORMATION_MESSAGE);
                                return;
                            }
                            new CertDialog(cert, "Server Certificate", "Server Certificate for Gateway " + ssgName()).show();
                        } catch (Exception e1) {
                            log.log(Level.SEVERE, "Unable to access server certificate", e1);
                            Gui.errorMessage("Unable to access server certificate",
                                    "Unable to access server certificate for Gateway " + ssgName(),
                                    e1);
                        }
                    }
                };
        ssgIdentityPane.getSsgCertButton().addActionListener(ssgCertButtonAction);

        return ssgIdentityPane;
    }

    private void importClientCertificate() {
        if (!(ssgIdentityPane instanceof TrustedSsgIdentityPanel))
            throw new IllegalStateException("Not supported for federated SSG");
        TrustedSsgIdentityPanel trustPane = (TrustedSsgIdentityPanel)ssgIdentityPane;
        char[] ssgPass = trustPane.getUserPasswordField().getPassword();
        if (ssgPass == null || ssgPass.length < 1) {
            ssgPass = PasswordDialog.getPassword(Gui.getInstance().getFrame(),
                                                 "Enter new password for Gateway " + ssgName());
            if (ssgPass == null)
                return;
            trustPane.getUserPasswordField().setText(new String(ssgPass));
        }

        JFileChooser fc = Utilities.createJFileChooser();
        fc.setDialogTitle("Select client certificate");
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        FileFilter fileFilter = new FileFilter() {
            public boolean accept(File f) {
                return (f.isDirectory() || f.getName().endsWith(".p12") || f.getName().endsWith(".P12"));
            }

            public String getDescription() {
                return "(*.p12) PKCS#12 Personal Digital Certificate";
            }
        };
        fc.setFileFilter(fileFilter);
        fc.setMultiSelectionEnabled(false);
        int r = fc.showDialog(Gui.getInstance().getFrame(), "Import certificate");
        if (r != JFileChooser.APPROVE_OPTION)
            return;
        File certFile = fc.getSelectedFile();
        if (certFile == null)
            return;
        char[] pass = PasswordDialog.getPassword(Gui.getInstance().getFrame(),
                                                 "Enter pass phrase for this PKCS#12 file",
                                                 true);
        if (pass == null)
            return;
        try {
            SsgKeyStoreManager.importClientCertificate(ssg, certFile, pass, null, ssgPass);
        } catch (ClassCastException e) {
            // translate this one into a friendlier error message
            log.log(Level.WARNING, "Unable to import certificate", e);
            Gui.errorMessage("Unable to import certificate\n\nThis is not a valid PKCS#12 keystore file.");
            return;
        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to import certificate", e);
            Gui.errorMessage("Unable to import certificate\n\n" + (e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
            return;
        }

        String clientCertUsername = SsgKeyStoreManager.lookupClientCertUsername(ssg);
        if (clientCertUsername != null)
            trustPane.getUsernameTextField().setText(clientCertUsername);
        updateIdentityEnableState();

        JOptionPane.showMessageDialog(Gui.getInstance().getFrame(),
                                      "Client certificate imported successfully.");
    }

    private void updateIdentityEnableState() {
        if (!(ssgIdentityPane instanceof TrustedSsgIdentityPanel))
            return;
        TrustedSsgIdentityPanel tp = (TrustedSsgIdentityPanel)ssgIdentityPane;
        if (tp.getUseClientCredentialCheckBox().isSelected()) {
            tp.getUserPasswordField().setEnabled(false);
            tp.getUsernameTextField().setEditable(false);
            tp.getUserPasswordField().setEditable(false);
        } else {
            tp.getSavePasswordCheckBox().setEnabled(true);
            tp.getUserPasswordField().setEnabled(true);
            tp.getUserPasswordField().setEditable(true);
            tp.getUsernameTextField().setEditable(SsgKeyStoreManager.lookupClientCertUsername(ssg) == null);
        }
    }

    private SsgNetworkPanel getNetworkPane() {
        if (networkPane == null)
            networkPane = new SsgNetworkPanel();
        return networkPane;
    }

    /** Create panel controls.  Should be called only from a constructor. */
    private JComponent getGeneralPane() {
        if (generalPane == null) {
            gridY = 0;
            JPanel pane = new JPanel(new GridBagLayout());
            generalPane = new JScrollPane(pane);
            generalPane.setBorder(BorderFactory.createEmptyBorder());

            JLabel image = new JLabel(IconManager.getSplashImageIcon());
            image.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            pane.add(image,
                     new GridBagConstraints(0, gridY++, 2, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.NONE,
                                            new Insets(25, 5, 0, 0), 0, 0));

            String splaintext = "Enter the host name or Internet address of the SecureSpan " +
                    "Gateway that will process Web service requests.";
            WrappingLabel splain01 = new WrappingLabel(splaintext, 3);
            pane.add(splain01,
                     new GridBagConstraints(0, gridY++, 2, 1, 1000.0, 0.0,
                                            GridBagConstraints.WEST,
                                            GridBagConstraints.HORIZONTAL,
                                            new Insets(25, 25, 0, 25), 0, 0));

            pane.add(new JLabel("Gateway Hostname:"),
                     new GridBagConstraints(0, gridY, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.EAST,
                                            GridBagConstraints.NONE,
                                            new Insets(5, 25, 0, 0), 0, 0));
            pane.add(getFieldServerAddress(),
                     new GridBagConstraints(1, gridY++, 1, 1, 1000.0, 0.0,
                                            GridBagConstraints.WEST,
                                            GridBagConstraints.HORIZONTAL,
                                            new Insets(5, 5, 0, 25), 0, 0));

            // Have a spacer eat any leftover space
            pane.add(new JPanel(),
                     new GridBagConstraints(0, gridY++, 2, 1, 1.0, 1.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(0, 0, 0, 0), 0, 0));
        }

        return generalPane;
    }

    private class CertDialog extends JDialog {
        CertDialog(X509Certificate cert, String title, String mess) throws CertificateEncodingException, NoSuchAlgorithmException {
            super(SsgPropertyDialog.this, title, true);
            Container c = this.getContentPane();
            c.setLayout(new GridBagLayout());
            c.setSize(new Dimension(300, 200));
            c.add(new JLabel(mess),
                  new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0,
                                         GridBagConstraints.CENTER,
                                         GridBagConstraints.BOTH,
                                         new Insets(5, 5, 5, 5), 0, 0));
            CertificatePanel cpan = new CertificatePanel(cert);
            c.add(cpan,
                  new GridBagConstraints(0, 1, 3, 1, 1000.0, 1000.0,
                                         GridBagConstraints.CENTER,
                                         GridBagConstraints.BOTH,
                                         new Insets(5, 5, 5, 5), 0, 0));
            c.add(getCloseButton(),
                  new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                                         GridBagConstraints.CENTER,
                                         GridBagConstraints.NONE,
                                         new Insets(5, 5, 5, 5), 0, 0));
            pack();
            Utilities.centerOnScreen(this);
        }

        JButton getCloseButton() {
            JButton cb = new JButton("OK");
            cb.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    CertDialog.this.hide();
                }
            });
            return cb;
        }
    }

    /** Enable or disable the Ok button, depending on whether all input is acceptable. */
    private void checkOk() {
        boolean ok = true;
        if (fieldServerAddress.getText().length() < 2)
            ok = false;
        getOkButton().setEnabled(ok);
    }

    /** Get the Server URL text field. */
    private JTextField getFieldServerAddress() {
        if (fieldServerAddress == null) {
            fieldServerAddress = new ContextMenuTextField();
            fieldServerAddress.setPreferredSize(new Dimension(220, 20));
            fieldServerAddress.setToolTipText("<HTML>Gateway hostname or address, for example<br><address>gateway.example.com");
            fieldServerAddress.getDocument().addDocumentListener(new DocumentListener() {
                public void  insertUpdate(DocumentEvent e) { checkOk(); }
                public void  removeUpdate(DocumentEvent e) { checkOk(); }
                public void changedUpdate(DocumentEvent e) { checkOk(); }
            });
        }
        return fieldServerAddress;
    }

    private boolean isPortsCustom(Ssg ssg) {
        return referenceSsg.getSsgPort() != ssg.getSsgPort() || referenceSsg.getSslPort() != ssg.getSslPort();
    }

    /** Set the Ssg object being edited by this panel. */
    private void setSsg(final Ssg ssg) {
        this.ssg = ssg;
        synchronized (ssg) {

            // override the default (trusted SSG) if the ssg is a federated SSG
            if (ssg.isFederatedGateway()) {
                FederatedSsgIdentityPanel fp = (FederatedSsgIdentityPanel)ssgIdentityPane;
                WsTrustSamlTokenStrategy strat = ssg.getWsTrustSamlTokenStrategy();
                if (strat != null) {
                    // Federated ssg using third-party WS-Trust service
                    fp.getWspAppliesToField().setText(strat.getAppliesTo());
                    fp.getWstUsernameField().setText(strat.getUsername());
                    fp.getWstPasswordField().setText(new String(strat.getPassword()));
                    fp.getWsTrustUrlTextField().setText(strat.getWsTrustUrl());
                    // TODO fp.getWstSavePasswordCheckBox();
                }
            } else {
                TrustedSsgIdentityPanel tp = (TrustedSsgIdentityPanel)ssgIdentityPane;
                tp.getUsernameTextField().setText(ssg.getUsername());
                char[] pass = ssg.getRuntime().getCachedPassword();
                boolean hasPassword = pass != null;
                tp.getUserPasswordField().setText(new String(hasPassword ? pass : "".toCharArray()));
                boolean customPorts = isPortsCustom(ssg);
                getNetworkPane().setCustomPorts(customPorts);
                tp.getSavePasswordCheckBox().setSelected(ssg.isSavePasswordToDisk());
                tp.getUseClientCredentialCheckBox().setSelected(ssg.isChainCredentialsFromClient());
                String clientCertUsername = SsgKeyStoreManager.lookupClientCertUsername(ssg);
                if (clientCertUsername != null) {
                    tp.getUsernameTextField().setText(clientCertUsername);
                    tp.getUsernameTextField().setEditable(false);
                }
                updateIdentityEnableState();
            }

            getNetworkPane().setLocalEndpoint("http://localhost:" + clientProxy.getBindPort() + "/" +
                                       ssg.getLocalEndpoint());
            getNetworkPane().setWsdlEndpoint("http://localhost:" + clientProxy.getBindPort() + "/" +
                                      ssg.getLocalEndpoint() + ClientProxy.WSIL_SUFFIX);
            fieldServerAddress.setText(ssg.getSsgAddress());

            getNetworkPane().setSsgPort(ssg.getSsgPort());
            getNetworkPane().setSslPort(ssg.getSslPort());
            getNetworkPane().setUseOverrideIpAddresses(ssg.isUseOverrideIpAddresses());
            getNetworkPane().setCustomIpAddresses(ssg.getOverrideIpAddresses());
            cbUseSslByDefault.setSelected(ssg.isUseSslByDefault());
            getNetworkPane().updateCustomPortsEnableState();
        }
        getPoliciesPane().setPolicyCache(ssg.getRuntime().rootPolicyManager());

        checkOk();
    }

    /**
     * Called when the Ok button is pressed.
     * Should copy any updated properties into the target object and return normally.
     * Caller is responsible for hiding and disposing of the property dialog.
     */
    protected void commitChanges() {
        synchronized (ssg) {

            if (!ssg.isFederatedGateway()) {
                TrustedSsgIdentityPanel tp = (TrustedSsgIdentityPanel)ssgIdentityPane;
                ssg.setUsername(tp.getUsernameTextField().getText().trim());
                ssg.setSavePasswordToDisk(tp.getSavePasswordCheckBox().isSelected());
                ssg.setChainCredentialsFromClient(tp.getUseClientCredentialCheckBox().isSelected());

                // We'll treat a blank password as though it's unconfigured.  If the user really needs to use
                // a blank password to access a service, he can leave the password field blank in the logon
                // dialog when it eventually appears.
                char[] pass = tp.getUserPasswordField().getPassword();

                // Make sure prompting is enabled
                ssg.getRuntime().promptForUsernameAndPassword(true);
                ssg.getRuntime().setCachedPassword(pass.length > 0 ? tp.getUserPasswordField().getPassword() : null);
            } else {
                FederatedSsgIdentityPanel fp = (FederatedSsgIdentityPanel)ssgIdentityPane;

                WsTrustSamlTokenStrategy strat = ssg.getWsTrustSamlTokenStrategy();
                if (strat != null) {
                    // This is a federated Ssg using a third-party Ws-Trust service
                    strat.setPassword(fp.getWstPasswordField().getPassword());
                    strat.setUsername(fp.getWstUsernameField().getText());
                    strat.setWsTrustUrl(fp.getWsTrustUrlTextField().getText());
                    strat.setAppliesTo(fp.getWspAppliesToField().getText());
                    // TODO fp.getWstSavePasswordCheckBox();
                }

                // Force chain credentials to be off if this is a fed ssg
                ssg.setChainCredentialsFromClient(false);
            }

            // applicable to both trusted and federated SSG
            ssg.setSsgAddress(fieldServerAddress.getText().trim().toLowerCase());
            ssg.setUseSslByDefault(cbUseSslByDefault.isSelected());
            ssg.setUseOverrideIpAddresses(getNetworkPane().isUseOverrideIpAddresses());
            ssg.setOverrideIpAddresses(getNetworkPane().getCustomIpAddresses());

            if (getNetworkPane().isCustomPorts()) {
                ssg.setSsgPort(getNetworkPane().getSsgPort());
                ssg.setSslPort(getNetworkPane().getSslPort());
            } else {
                ssg.setSsgPort(referenceSsg.getSsgPort());
                ssg.setSslPort(referenceSsg.getSslPort());
            }
            ssg.getRuntime().resetSslContext();
        }
        setSsg(ssg);
    }

    /**
     * Find the SSG name to display.  Uses the name from the SSG, if any; otherwise, the typed
     * in name; otherwise the string "&lt;New Gateway&gt;".
     * @return
     */
    private String ssgName() {
        String result =  ssg.getSsgAddress();
        if (result != null && result.length() > 0)
            return result;
        result = getFieldServerAddress().getText();
        if (result != null && result.length() > 0)
            return result;
        return "<New Gateway>";
    }

    /**
     * This event is fired when a policy is attached to an Ssg with a PolicyAttachmentKey, either new
     * or updated.
     *
     * @param evt
     */
    public void policyAttached(SsgEvent evt) {
        getPoliciesPane().updatePolicyPanel();
    }

    public void dataChanged(SsgEvent evt) {
        // Take no action; we don't override the user's data.
    }

    public void show() {
        getFieldServerAddress().requestFocus();
        super.show();
    }
}
