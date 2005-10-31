package com.l7tech.proxy.gui.dialogs;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.CertificatePanel;
import com.l7tech.common.gui.widgets.ContextMenuTextField;
import com.l7tech.common.gui.widgets.WrappingLabel;
import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.xml.WsTrustRequestType;
import com.l7tech.proxy.ClientProxy;
import com.l7tech.proxy.datamodel.*;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.CertificateAlreadyIssuedException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.gui.Gui;
import com.l7tech.proxy.ssl.CurrentSslPeer;

import javax.crypto.BadPaddingException;
import javax.net.ssl.SSLException;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.security.GeneralSecurityException;
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
    private JLabel imageLabel;

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
        super("Gateway Account Properties");
        this.clientProxy = clientProxy;
        tabbedPane.add("General", getGeneralPane(ssg));
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
     * @return The property dialog that will edit said properties.  Call setVisible(true) on it to run it.
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
            cbUseSslByDefault = new JCheckBox("Use SSL by Default");
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

    private ActionListener getCertActionListener(final FederatedSamlTokenStrategy strat, final String type) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    X509Certificate cert = strat.getTokenServerCert();
                    if (cert == null) {
                        JOptionPane.showMessageDialog(Gui.getInstance().getFrame(),
                                                      "A certificate for the " + type + " server\n" +
                                                      "was not found.",
                                                      type + " Server Certificate Not Found",
                                                      JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    new CertDialog(cert, type + " Server Certificate", type + " Server Certificate").setVisible(true);
                } catch (GeneralSecurityException e1) {
                    log.log(Level.SEVERE, "Unable to access " + type + " server certificate", e1);
                    Gui.criticalErrorMessage("Unable to access " + type + " server certificate",
                                     "Unable to access " + type + " server certificate",
                                     e1);
                }
            }
        };
    }

    private ActionListener getTestTokenRequestActionListener(final FederatedSsgIdentityPanel fp, final FederatedSamlTokenStrategy strat, final String type) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                try {
                    FederatedSamlTokenStrategy stratCopy = (FederatedSamlTokenStrategy) strat.clone();
                    updateStrategyFromView(stratCopy, fp);
                    try {
                        SecurityToken token = null;
                        for (;;) {
                            try {
                                stratCopy.clearCachedToken();
                                token = stratCopy.getOrCreate();
                                if (token == null) throw new NullPointerException("No token was returned by the server"); // can't happen
                                break;
                            } catch (SSLException e) {
                                stratCopy.handleSslException(CurrentSslPeer.get(), e);
                                strat.storeTokenServerCert(stratCopy.getTokenServerCert()); // copy it back out
                                /* FALLTHROUGH and try again now that cert was imported */
                            }
                        }

                        JOptionPane.showMessageDialog(Gui.getInstance().getFrame(),
                                                      "A " + token.getType().getName() +
                                                      " was successfully obtained using these " + type + " settings.",
                                                      "Success: Token Obtained",
                                                      JOptionPane.INFORMATION_MESSAGE );
                    } catch (Exception e) {
                        log.log(Level.INFO, "Unable to obtain token from " + type + " server", e);
                        JOptionPane.showMessageDialog(Gui.getInstance().getFrame(),
                                                      "A security token could not be obtained using these " + type + " settings.\n\n" +
                                                      "The error was: \n    " + HexUtils.wrapString(ExceptionUtils.getMessage(e), 80, 25, "\n    "),
                                                      "Unable to Obtain Token",
                                                      JOptionPane.ERROR_MESSAGE);
                    }
                }
                catch(CloneNotSupportedException cnse) {
                    log.log(Level.SEVERE,"Saml strategy is not correctly implemented! (MUST support clone)", cnse);
                }
            }
        };
    }

    private SsgIdentityPanel getIdentityPane(final Ssg ssg) {
        if (ssgIdentityPane != null) return ssgIdentityPane;

        if (ssg.isFederatedGateway()) {
            final FederatedSsgIdentityPanel fp = new FederatedSsgIdentityPanel(ssg);
            ssgIdentityPane = fp;
            AbstractSamlTokenStrategy astrat = ssg.getWsTrustSamlTokenStrategy();
            if(astrat instanceof WsTrustSamlTokenStrategy) {
                final WsTrustSamlTokenStrategy strat = (WsTrustSamlTokenStrategy) astrat;
                if (strat != null) {
                    fp.getWsTrustCertButton().addActionListener(getCertActionListener(strat, "WS-Trust"));

                    fp.getRequestTypeCombo().setModel(new DefaultComboBoxModel(WsTrustRequestType.getValues()));

                    fp.getWsTrustTestButton().addActionListener(getTestTokenRequestActionListener(fp, strat, "WS-Trust"));
                }
            }
            else if(astrat instanceof WsFederationPRPSamlTokenStrategy) {
                final WsFederationPRPSamlTokenStrategy strat = (WsFederationPRPSamlTokenStrategy) astrat;
                if (strat != null) {
                    fp.getWsFedCertButton().addActionListener(getCertActionListener(strat, "WS-Federation"));

                    fp.getWsFedTestButton().addActionListener(getTestTokenRequestActionListener(fp, strat, "WS-Federation"));
                }
            }

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
                        NoClientCert dlg = new NoClientCert(SsgPropertyDialog.this, ssgName());
                        dlg.pack();
                        Utilities.centerOnScreen(dlg);
                        dlg.setVisible(true);
                        dlg.dispose();
                        int r = dlg.getExitCondition();
                        if (r == NoClientCert.REQUESTED_IMPORT) {
                            importClientCertificate();
                        } else if (r == NoClientCert.REQUESTED_CSR) {
                            try {
                                for (;;) {
                                    try {
                                        manualCSR();
                                        cert = ssg.getClientCertificate();
                                        break;
                                    } catch (KeyStoreCorruptException e1) {
                                        ssg.getRuntime().handleKeyStoreCorrupt();
                                        /* FALLTHROUGH and retry */
                                    }
                                }
                                /* FALLTHROUGH and display newly-acquired client certificate */
                            } catch (CertificateAlreadyIssuedException csrex) {
                                final String msg = "Unable to obtain certificate from the SecureSpan Gateway " +
                                        ssgName() + " because this account already has a valid " +
                                        "certificate. Contact the gateway administrator for more information";
                                log.log(Level.WARNING, msg, csrex);
                                Gui.errorMessage(msg);
                                return;
                            } catch (BadCredentialsException csrex) {
                                final String msg = "Unable to obtain certificate from the SecureSpan Gateway " +
                                        ssgName() + " because of credentials provided. Contact the " +
                                        "gateway administrator for more information.";
                                log.log(Level.WARNING, msg, csrex);
                                ssg.getRuntime().setCachedPassword(null); // Bug #1592
                                Gui.errorMessage(msg);
                                return;
                            }
                        } else
                            return;
                        /* FALLTHROUGH and display newly-acquired client certificate */
                    }
                    if (cert != null)
                        new CertDialog(cert, "Client Certificate", "Client Certificate for Gateway " + ssgName()).setVisible(true);
                } catch (OperationCanceledException e1) {
                    return;
                } catch (Exception e1) {
                    String mess = ExceptionUtils.unnestToRoot(e1).getMessage();
                    if (mess == null) mess = "An exception occurred.";
                    log.log(Level.SEVERE, "Unable to access client certificate: " + mess, e1);
                    Gui.errorMessage("Unable to Access Client Certificate",
                                         "Unable to access client certificate for the SecureSpan Gateway " + ssgName() + ".",
                                         mess,
                                         e1);
                    return;
                }
            }
        });

        final ActionListener ssgCertButtonAction = new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            X509Certificate cert = ssg.getServerCertificate();
                            if (cert == null) {
                                JOptionPane.showMessageDialog(Gui.getInstance().getFrame(),
                                        "A certificate for the SecureSpan Gateway " + ssgName() + "\n" +
                                        "was not found.",
                                        "Gateway Server Certificate Not Found",
                                        JOptionPane.INFORMATION_MESSAGE);
                                return;
                            }
                            new CertDialog(cert, "View Server Certificate", "Server Certificate for the SecureSpan Gateway " + ssgName()).setVisible(true);
                        } catch (Exception e1) {
                            log.log(Level.SEVERE, "Unable to access server certificate", e1);
                            Gui.criticalErrorMessage("Unable to access server certificate",
                                    "Unable to access server certificate for Gateway " + ssgName(),
                                    e1);
                        }
                    }
                };
        ssgIdentityPane.getSsgCertButton().addActionListener(ssgCertButtonAction);

        return ssgIdentityPane;
    }

    private void manualCSR() throws OperationCanceledException, GeneralSecurityException,
                                    IOException, KeyStoreCorruptException, BadCredentialsException,
                                    CertificateAlreadyIssuedException {
        if (!(ssgIdentityPane instanceof TrustedSsgIdentityPanel) || ssg.isFederatedGateway())
            throw new IllegalStateException("Not supported for Federated Gateway");

        PasswordAuthentication creds = ssg.getRuntime().getCredentialManager().getCredentials(ssg);

        // make sure that the host name is set
        String newHost = fieldServerAddress.getText();
        if (newHost == null || newHost.length() < 1)
            newHost = ssg.getSsgAddress();
        if (newHost == null || newHost.length() < 1) {
            newHost = JOptionPane.showInputDialog(this, "Please enter a Gateway host name.");
            if (newHost == null) throw new OperationCanceledException();
        }
        if (newHost == null || newHost.length() < 1) {
            JOptionPane.showMessageDialog(this,
                                          "You must set a gateway host name before you can apply for a client " +
                                          "certificate.", "Cannot apply for client certificate",
                                          JOptionPane.ERROR_MESSAGE);
            throw new OperationCanceledException();
        }

        // Save the host name we're about to use in the bean and in the GUI
        ssg.setSsgAddress(newHost);
        fieldServerAddress.setText(newHost);
        checkOk();

        if (ssg.getServerCertificate() == null) {
            ssg.getRuntime().getSsgKeyStoreManager().installSsgServerCertificate(ssg, creds);
        }

        // PasswordAuthentication credentials
        ssg.getRuntime().getSsgKeyStoreManager().obtainClientCertificate(creds);

        // Save the creds we used in the bean and in the GUI
        ssg.setUsername(creds.getUserName());
        ssg.getRuntime().setCachedPassword(creds.getPassword());
        SsgIdentityPanel idp = getIdentityPane(ssg);
        if (idp instanceof TrustedSsgIdentityPanel) {
            TrustedSsgIdentityPanel trusted = (TrustedSsgIdentityPanel)idp;
            trusted.getUsernameTextField().setText(creds.getUserName());
            trusted.getUserPasswordField().setText(new String(creds.getPassword()));
        }
    }

    private void importClientCertificate() throws NoSuchAlgorithmException, CertificateEncodingException {
        if (!(ssgIdentityPane instanceof TrustedSsgIdentityPanel))
            throw new IllegalStateException("Not supported for federated SSG");
        TrustedSsgIdentityPanel trustPane = (TrustedSsgIdentityPanel)ssgIdentityPane;

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

        char[] ssgPass = trustPane.getUserPasswordField().getPassword();
        if (ssgPass == null || ssgPass.length < 1) {
            ssgPass = PasswordDialog.getPassword(Gui.getInstance().getFrame(),
                                                 "Enter new password for Gateway " + ssgName());
            if (ssgPass == null)
                return;
            trustPane.getUserPasswordField().setText(new String(ssgPass));
        }

        if (pass == null)
            return;
        try {
            ssg.getRuntime().getSsgKeyStoreManager().importClientCertificate(certFile, pass, new CertAliasPicker(this), ssgPass);
        } catch (ClassCastException e) {
            // translate this one into a friendlier error message
            log.log(Level.WARNING, "Unable to import certificate", e);
            Gui.errorMessage("Unable to import certificate\n\nThis is not a valid PKCS#12 keystore file.");
            return;
        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to import certificate", e);
            String msg = "Unable to import certificate";
            if (e instanceof IOException) {
                if (ExceptionUtils.causedBy(e, BadPaddingException.class)) {
                    msg += " (likely due to an incorrect password)";
                }
            }
            msg += "\n\n" + (e.getMessage() != null ? e.getMessage() : e.getClass().getName());
            Gui.errorMessage(msg);
            return;
        }

        String clientCertUsername = ssg.getRuntime().getSsgKeyStoreManager().lookupClientCertUsername();
        if (clientCertUsername != null)
            trustPane.getUsernameTextField().setText(clientCertUsername);
        updateIdentityEnableState();

        X509Certificate clientcert = ssg.getClientCertificate();
        new CertDialog(clientcert, "Client Certificate Imported Successfully", "New Client Certificate").setVisible(true);
        /*JOptionPane.showMessageDialog(Gui.getInstance().getFrame(),
                                      "Client certificate imported successfully.");*/
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
            tp.getUsernameTextField().setEditable(ssg.getRuntime().getSsgKeyStoreManager().lookupClientCertUsername() == null);
        }
    }

    private SsgNetworkPanel getNetworkPane() {
        if (networkPane == null)
            networkPane = new SsgNetworkPanel();
        return networkPane;
    }

    private JLabel getImageLabel(Ssg ssg) {
        if (imageLabel == null) {
            imageLabel = new JLabel(getIdentityPane(ssg).getGeneralPaneImageIcon());
            final Border white = BorderFactory.createLineBorder(Color.WHITE, 12);
            final Border lowered = BorderFactory.createLineBorder(Color.BLACK);
            final Border border = BorderFactory.createCompoundBorder(lowered, white);
            imageLabel.setBorder(border);
        }
        return imageLabel;
    }

    /** Create panel controls.  Should be called only from a constructor. */
    private JComponent getGeneralPane(Ssg ssg) {
        if (generalPane == null) {
            gridY = 0;
            JPanel pane = new JPanel(new GridBagLayout());
            generalPane = new JScrollPane(pane);
            generalPane.setBorder(BorderFactory.createEmptyBorder());

            pane.add(getImageLabel(ssg),
                     new GridBagConstraints(0, gridY++, 2, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.NONE,
                                            new Insets(25, 5, 0, 0), 0, 0));

            String splaintext = "Enter the host name or Internet address of the SecureSpan " +
                    "Gateway that will process service requests.";
            WrappingLabel splain01 = new WrappingLabel(splaintext, 3);
            pane.add(splain01,
                     new GridBagConstraints(0, gridY++, 2, 1, 1000.0, 0.0,
                                            GridBagConstraints.WEST,
                                            GridBagConstraints.HORIZONTAL,
                                            new Insets(25, 25, 0, 25), 0, 0));

            pane.add(new JLabel("Gateway Host Name:"),
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
                    CertDialog.this.setVisible(false);
                }
            });
            return cb;
        }
    }

    class CertAliasPicker extends JDialog implements SsgKeyStoreManager.AliasPicker {

        public CertAliasPicker(Dialog parent) {
            super(parent);
        }

        public String selectAlias(String[] options) throws SsgKeyStoreManager.AliasNotFoundException {
            Object selectedOption = JOptionPane.showInputDialog(this, "Select the alias for the certificate you want to import.",
                    "Select an Alias.", JOptionPane.QUESTION_MESSAGE,
				  null,options , null);
            return (String)selectedOption;

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
            fieldServerAddress.setToolTipText("<HTML>Gateway host name or address, for example<br><address>gateway.example.com");
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

    /**
     * Set the Ssg object being edited by this panel.
     */
    private void setSsg(final Ssg ssg) {
        this.ssg = ssg;
        synchronized (ssg) {

            // override the default (trusted SSG) if the ssg is a federated SSG
            if (ssg.isFederatedGateway()) {
                FederatedSsgIdentityPanel fp = (FederatedSsgIdentityPanel) ssgIdentityPane;
                AbstractSamlTokenStrategy astrat = ssg.getWsTrustSamlTokenStrategy();
                if (astrat instanceof WsTrustSamlTokenStrategy) {
                    WsTrustSamlTokenStrategy strat = (WsTrustSamlTokenStrategy) astrat;
                    // Federated ssg using third-party WS-Trust service
                    fp.getWspAppliesToField().setText(strat.getAppliesTo());
                    fp.getWstIssuerField().setText(strat.getWstIssuer());
                    if (strat.getRequestType() != null) fp.getRequestTypeCombo().setSelectedItem(WsTrustRequestType.fromString(strat.getRequestType()));
                    fp.getWstUsernameField().setText(strat.getUsername());
                    char[] pass = strat.getPassword();
                    if (pass == null) pass = new char[0];
                    fp.getWstPasswordField().setText(new String(pass));
                    fp.getWsTrustUrlTextField().setText(strat.getWsTrustUrl());
                    // TODO fp.getWstSavePasswordCheckBox();
                }
                else if (astrat instanceof WsFederationPRPSamlTokenStrategy) {
                    WsFederationPRPSamlTokenStrategy strat = (WsFederationPRPSamlTokenStrategy) astrat;
                    // Federated ssg using third-party WS-Federation service
                    fp.getWsFedUrlTextField().setText(strat.getIpStsUrl());
                    fp.getWsFedRealmTextField().setText(strat.getRealm());
                    fp.getWsFedReplyUrlTextField().setText(strat.getReplyUrl());
                    fp.getWsFedContextTextField().setText(strat.getContext());
                    fp.getWsFedTimestampCheckBox().setSelected(strat.isTimestamp());
                    fp.getWsFedUsernameField().setText(strat.getUsername());
                    char[] pass = strat.getPassword();
                    if (pass == null) pass = new char[0];
                    fp.getWsFedPasswordField().setText(new String(pass));
                }
            } else {
                TrustedSsgIdentityPanel tp = (TrustedSsgIdentityPanel)ssgIdentityPane;
                tp.getUsernameTextField().setText(ssg.getUsername());
                char[] pass = ssg.getRuntime().getCachedPassword();
                boolean hasPassword = pass != null;
                tp.getUserPasswordField().setText(new String(hasPassword ? pass : "".toCharArray()));
                tp.getSavePasswordCheckBox().setSelected(ssg.isSavePasswordToDisk());
                tp.getUseClientCredentialCheckBox().setSelected(ssg.isChainCredentialsFromClient());
                String clientCertUsername = ssg.getRuntime().getSsgKeyStoreManager().lookupClientCertUsername();
                if (clientCertUsername != null) {
                    tp.getUsernameTextField().setText(clientCertUsername);
                    tp.getUsernameTextField().setEditable(false);
                }
            }

            updateIdentityEnableState();
            boolean customPorts = isPortsCustom(ssg);
            getNetworkPane().setCustomPorts(customPorts);
            getNetworkPane().setLocalEndpoint("http://localhost:" + clientProxy.getBindPort() + "/" +
                                       ssg.getLocalEndpoint());
            getNetworkPane().setWsdlEndpoint("http://localhost:" + clientProxy.getBindPort() + "/" +
                                      ssg.getLocalEndpoint() + ClientProxy.WSIL_SUFFIX);
            fieldServerAddress.setText(ssg.getSsgAddress());

            getNetworkPane().setSsgPort(ssg.getSsgPort());
            getNetworkPane().setSslPort(ssg.getSslPort());
            getNetworkPane().setUseOverrideIpAddresses(ssg.isUseOverrideIpAddresses());
            getNetworkPane().setCustomIpAddresses(ssg.getOverrideIpAddresses());
            getNetworkPane().setFailoverStrategyName(ssg.getFailoverStrategyName());
            cbUseSslByDefault.setSelected(ssg.isUseSslByDefault());
            getNetworkPane().updateCustomPortsEnableState();
        }
        getPoliciesPane().setPolicyCache(ssg.getRuntime().getPolicyManager());

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

                AbstractSamlTokenStrategy astrat = ssg.getWsTrustSamlTokenStrategy();
                if(astrat instanceof WsTrustSamlTokenStrategy) {
                    WsTrustSamlTokenStrategy strat = (WsTrustSamlTokenStrategy) astrat;
                    if (strat != null)
                        updateWsTrustStrategyFromView(strat, fp);
                }
                else if(astrat instanceof WsFederationPRPSamlTokenStrategy) {
                    WsFederationPRPSamlTokenStrategy strat = (WsFederationPRPSamlTokenStrategy) astrat;
                    if (strat != null)
                        updateWsFederationPRPStrategyFromView(strat, fp);
                }

                // Force chain credentials to be off if this is a fed ssg
                ssg.setChainCredentialsFromClient(false);
            }

            // applicable to both trusted and federated SSG
            ssg.setSsgAddress(fieldServerAddress.getText().trim().toLowerCase());
            ssg.setUseSslByDefault(cbUseSslByDefault.isSelected());
            ssg.setUseOverrideIpAddresses(getNetworkPane().isUseOverrideIpAddresses());
            ssg.setOverrideIpAddresses(getNetworkPane().getCustomIpAddresses());
            ssg.setFailoverStrategyName(getNetworkPane().getFailoverStrategyName());

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
     * Dispatch the update to the relevant method. 
     */
    private void updateStrategyFromView(AbstractSamlTokenStrategy strat, FederatedSsgIdentityPanel fp) {
        if(strat instanceof WsTrustSamlTokenStrategy) {
            updateWsTrustStrategyFromView((WsTrustSamlTokenStrategy)strat, fp);
        }
        else if(strat instanceof WsFederationPRPSamlTokenStrategy) {
            updateWsFederationPRPStrategyFromView((WsFederationPRPSamlTokenStrategy)strat, fp);
        }
        else {
            throw new IllegalArgumentException("Unsupported strategy type");
        }
    }


    /**
     * Copy the information from the specified panel into the specified WS-Trust token strategy.
     */
    private void updateWsTrustStrategyFromView(WsTrustSamlTokenStrategy strat, FederatedSsgIdentityPanel fp) {
        // This is a federated Ssg using a third-party Ws-Trust service
        strat.setPassword(fp.getWstPasswordField().getPassword());
        strat.setUsername(fp.getWstUsernameField().getText());
        strat.setWsTrustUrl(fp.getWsTrustUrlTextField().getText());
        strat.setAppliesTo(fp.getWspAppliesToField().getText());
        strat.setWstIssuer(fp.getWstIssuerField().getText());
        WsTrustRequestType requestType = (WsTrustRequestType) fp.getRequestTypeCombo().getSelectedItem();
        if (requestType != null) 
            strat.setRequestType(requestType.getUri());
        // TODO fp.getWstSavePasswordCheckBox();
    }

    /**
     * Copy the information from the specified panel into the specified WS-Federation token strategy.
     */
    private void updateWsFederationPRPStrategyFromView(WsFederationPRPSamlTokenStrategy strat, FederatedSsgIdentityPanel fp) {
        // This is a federated Ssg using a third-party Ws-Federation service
        strat.setPassword(fp.getWsFedPasswordField().getPassword());
        strat.setUsername(fp.getWsFedUsernameField().getText());
        strat.setIpStsUrl(fp.getWsFedUrlTextField().getText());
        strat.setRealm(fp.getWsFedRealmTextField().getText());
        strat.setReplyUrl(fp.getWsFedReplyUrlTextField().getText());
        strat.setContext(fp.getWsFedContextTextField().getText());
        strat.setTimestamp(fp.getWsFedTimestampCheckBox().isSelected());
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

    public void setVisible(boolean b) {
        if (b)
            getFieldServerAddress().requestFocus();
        super.setVisible(b);
    }
}
