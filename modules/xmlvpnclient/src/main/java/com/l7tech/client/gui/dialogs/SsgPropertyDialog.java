package com.l7tech.client.gui.dialogs;

import com.l7tech.client.ClientProxy;
import com.l7tech.client.gui.Gui;
import com.l7tech.common.io.AliasNotFoundException;
import com.l7tech.common.io.CertUtils;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.*;
import com.l7tech.kerberos.KerberosClient;
import com.l7tech.kerberos.KerberosUtils;
import com.l7tech.proxy.Constants;
import com.l7tech.proxy.datamodel.*;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.ssl.CurrentSslPeer;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TextUtils;
import com.l7tech.util.ValidationUtils;
import com.l7tech.xml.WsTrustRequestType;

import javax.crypto.BadPaddingException;
import javax.net.ssl.SSLException;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
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
    private final Ssg ssg; // The real Ssg instance, to which changes may be committed.
    private final SsgFinder ssgFinder; // Existing SSGs, for global validation (like catching duplicate endpoint labels)
    private final int bindPort; // the local port the client proxy is bound to

    //   View for General pane
    private JComponent generalPane;
    private JTextField fieldServerAddress;
    private JTextField fieldKerberosName;
    private JLabel imageLabel;

    //   View for Identity pane
    private SsgIdentityPanel ssgIdentityPane;

    //   View for Network pane
    private SsgNetworkPanel networkPane;

    //   View for Bridge Policy pane
    private BridgePolicyPanel bridgePolicyPane;

    //   View for Service Policies pane
    private SsgPoliciesPanel policiesPane;

    /** Create an SsgPropertyDialog ready to edit an Ssg instance. */
    private SsgPropertyDialog(final Ssg ssg, final SsgFinder ssgFinder, final int bindPort) {
        super("Gateway Account Properties");
        this.ssg = ssg;
        this.ssgFinder = ssgFinder;
        this.bindPort = bindPort;
        if (ssg == null || ssgFinder == null) throw new IllegalArgumentException("ssg and ssgFinder must not be null");
        tabbedPane.add("General", getGeneralPane(ssg));
        tabbedPane.add("Identity", getIdentityPane(ssg));
        tabbedPane.add("Network", getNetworkPane());
        tabbedPane.add(Constants.APP_NAME +" Policy", getBridgePolicyPane());
        tabbedPane.add("Service Policies", getPoliciesPane());
        ssg.addSsgListener(this);
        modelToView();
        pack();
    }

    protected void finalize() throws Throwable {
        ssg.removeSsgListener(this);
        super.finalize();
    }

    /**
     * Attempt to build an "edit properties" dialog box for the given Ssg.
     * @param ssg The ssg whose properties we intend to edit.  Required
     * @param ssgFinder Finds the existing SSGs, for global validation.  Required
     * @param bindPort the port the client proxy is listening on on localhost
     * @return The property dialog that will edit said properties.  Call setVisible(true) on it to run it.
     */
    public static SsgPropertyDialog makeSsgPropertyDialog(final Ssg ssg, SsgFinder ssgFinder, int bindPort) {
        return new SsgPropertyDialog(ssg, ssgFinder, bindPort);
    }

    private BridgePolicyPanel getBridgePolicyPane() {
        if (bridgePolicyPane == null) {
            bridgePolicyPane = new BridgePolicyPanel(this, !ssg.isGeneric());
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
                        //noinspection UnusedAssignment
                        SecurityToken token = null;
                        for (;;) {
                            try {
                                stratCopy.clearCachedToken();
                                token = stratCopy.getOrCreate(ssg);
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

                        updateViewCredentialsFromStrategy(fp, stratCopy);
                    } catch (Exception e) {
                        log.log(Level.INFO, "Unable to obtain token from " + type + " server", e);
                        JOptionPane.showMessageDialog(Gui.getInstance().getFrame(),
                                                      "A security token could not be obtained using these " + type + " settings.\n\n" +
                                                      "The error was: \n    " + TextUtils.wrapString(ExceptionUtils.getMessage(e), 80, 25, "\n    "),
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
                fp.getWsTrustCertButton().addActionListener(getCertActionListener(strat, "WS-Trust"));
                fp.getRequestTypeCombo().setModel(new DefaultComboBoxModel(WsTrustRequestType.getValues()));
                fp.getWsTrustTestButton().addActionListener(getTestTokenRequestActionListener(fp, strat, "WS-Trust"));
            }
            else if (astrat instanceof WsFederationPRPSamlTokenStrategy) {
                final WsFederationPRPSamlTokenStrategy strat = (WsFederationPRPSamlTokenStrategy)astrat;
                fp.getWsFedCertButton().addActionListener(getCertActionListener(strat, "WS-Federation"));
                fp.getWsFedTestButton().addActionListener(getTestTokenRequestActionListener(fp, strat, "WS-Federation"));
            }

        } else {
            TrustedSsgIdentityPanel tp = new TrustedSsgIdentityPanel(ssg);
            ssgIdentityPane = tp;
            tp.getUseKerberosCredentialCheckbox().setEnabled(KerberosUtils.isEnabled());
            tp.getUseKerberosCredentialCheckbox().addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    updateIdentityEnableState();
                }
            });

            tp.getUseClientCredentialCheckBox().addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateIdentityEnableState();
                }
            });

        }

        ssgIdentityPane.getClientCertButton().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() { public void run() {
                try {
                    X509Certificate cert = ssg.getClientCertificate();
                    if (cert == null) {
                        NoClientCert dlg = new NoClientCert(SsgPropertyDialog.this, serverName(), !ssg.isGeneric());
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
                                final String msg = "Unable to obtain certificate from the " +
                                        serverName() + " because this account already has a valid " +
                                        "certificate. Contact the server administrator for more information";
                                log.log(Level.WARNING, msg, csrex);
                                Gui.errorMessage(msg);
                                return;
                            } catch (ServerFeatureUnavailableException sfue) {
                                final String msg = "Unable to obtain certificate from the " +
                                        serverName() + " because the certificate signing service is not available " +
                                        "on this server.";
                                log.log(Level.WARNING, msg, sfue);
                                Gui.errorMessage(msg);
                                return;
                            } catch (BadCredentialsException csrex) {
                                final String msg = "Unable to obtain certificate from the " +
                                        serverName() + " because of credentials provided. Contact the " +
                                        "server administrator for more information.";
                                log.log(Level.WARNING, msg, csrex);
                                ssg.getRuntime().setCachedPassword(null); // Bug #1592
                                Gui.errorMessage(msg);
                                return;
                            }
                        } else
                            return;
                        /* FALLTHROUGH and display newly-acquired client certificate */
                    }
                    if (cert != null) {
                        if (ssgIdentityPane instanceof TrustedSsgIdentityPanel) {
                            JCheckBox kcb = ((TrustedSsgIdentityPanel)ssgIdentityPane).getUseKerberosCredentialCheckbox();
                            kcb.setSelected(false);
                            kcb.setEnabled(false);

                            JTextField utf = ((TrustedSsgIdentityPanel)ssgIdentityPane).getUsernameTextField();
                            String nameFromCert = ssg.getRuntime().getSsgKeyStoreManager().lookupClientCertUsername();
                            if (nameFromCert != null) {
                                utf.setText(nameFromCert);
                                utf.setEditable(false);
                            }
                        }
                        new CertDialog(cert, "Client Certificate", "Client Certificate for " + serverName()).setVisible(true);
                    }
                } catch (OperationCanceledException e1) {
                    return;
                } catch (Exception e1) {
                    String mess = ExceptionUtils.unnestToRoot(e1).getMessage();
                    if (mess == null) mess = "An exception occurred.";
                    log.log(Level.SEVERE, "Unable to access client certificate: " + mess, e1);
                    Gui.errorMessage("Unable to Access Client Certificate",
                                         "Unable to access client certificate for the " + serverName() + '.',
                                         mess,
                                         e1);
                    return;
                }
                }}).start();
            }
        });

        final ActionListener ssgCertButtonAction = new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            X509Certificate cert = ssg.getServerCertificate();
                            if (cert == null) {
                                NoServerCertDialog dlg = new NoServerCertDialog(SsgPropertyDialog.this, ssg, serverName());
                                dlg.pack();
                                Utilities.centerOnScreen(dlg);
                                dlg.setModal(true);
                                dlg.setVisible(true);
                                if (!dlg.wasNewCertSaved())
                                    return;
                                cert = ssg.getServerCertificate();
                                if (cert == null)
                                    return;
                            }
                            new CertDialog(cert, "View Server Certificate", "Server Certificate for " + serverName()).setVisible(true);
                        } catch (Exception e1) {
                            log.log(Level.SEVERE, "Unable to access server certificate", e1);
                            Gui.criticalErrorMessage("Unable to access server certificate",
                                    "Unable to access server certificate for  " + serverName(),
                                    e1);
                        }
                    }
                };
        ssgIdentityPane.getSsgCertButton().addActionListener(ssgCertButtonAction);

        return ssgIdentityPane;
    }

    private void manualCSR() throws OperationCanceledException, GeneralSecurityException,
            IOException, KeyStoreCorruptException, BadCredentialsException,
            CertificateAlreadyIssuedException, ServerFeatureUnavailableException {
        if (!(ssgIdentityPane instanceof TrustedSsgIdentityPanel) || ssg.isFederatedGateway())
            throw new IllegalStateException("Not supported for Federated Gateway");

        final PasswordAuthentication creds;
        try {
            creds = Managers.getCredentialManager().getCredentials(ssg);
        } catch (HttpChallengeRequiredException e) {
            throw new RuntimeException(e); // can't happen, it's a GuiCredentialManager (even if ssg has pass-through auth enabled)
        }

        // make sure that the host name is set
        String newHost = fieldServerAddress.getText();
        if (newHost == null || newHost.length() < 1)
            newHost = ssg.getSsgAddress();
        if (newHost == null || newHost.length() < 1) {
            newHost = JOptionPane.showInputDialog(this, "Please enter a Gateway host name.");
            if (newHost == null) throw new OperationCanceledException();
        }
        if (newHost.length() < 1) {
            JOptionPane.showMessageDialog(this,
                                          "You must set a gateway host name before you can apply for a client " +
                                          "certificate.", "Cannot apply for client certificate",
                                                          JOptionPane.ERROR_MESSAGE);
            throw new OperationCanceledException();
        }

        // Save the host name we're about to use in the bean and in the GUI
        ssg.setSsgAddress(newHost);
        fieldServerAddress.setText(newHost);
        validator.validate();

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

        JFileChooser fc = FileChooserUtil.createJFileChooser();
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
        char[] pass = PasswordDoubleEntryDialog.getPassword(Gui.getInstance().getFrame(),
                                                 "Enter pass phrase for this PKCS#12 file",
                                                 true);

        char[] ssgPass = trustPane.getUserPasswordField().getPassword();
        if (ssgPass == null || ssgPass.length < 1) {
            ssgPass = PasswordDoubleEntryDialog.getPassword(Gui.getInstance().getFrame(),
                                                 "Enter new password for " + serverName());
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

        boolean kerbPrincFromCache = false;
        String kprinc = null;
        try {
            kprinc = new KerberosClient().getKerberosInitPrincipal();
            if (kprinc != null) {
                kerbPrincFromCache = true;
            }
        }
        catch(Exception e) {
            // ignore
        }

        if (tp.getUseKerberosCredentialCheckbox().isSelected() &&
                kerbPrincFromCache) {
            // When using kerberos creds from ticket cache (SSO) it is not valid to use
            // client creds or to save the password
            tp.getUseClientCredentialCheckBox().setEnabled(false);
            tp.getSavePasswordCheckBox().setEnabled(false);
            tp.getUsernameTextField().setText(kprinc);
            tp.getUsernameTextField().setEditable(false);
            tp.getUserPasswordField().setEnabled(false);
            tp.getUserPasswordField().setEditable(false);
        }
        else if(tp.getUseKerberosCredentialCheckbox().isSelected()) {
            // Kerberos creds gained from usename / password, client creds not allowed
            tp.getUseClientCredentialCheckBox().setEnabled(false);
            tp.getSavePasswordCheckBox().setEnabled(true);
            tp.getUserPasswordField().setEnabled(true);
            tp.getUserPasswordField().setEditable(true);
        }
        else if (tp.getUseClientCredentialCheckBox().isSelected()) {
            tp.getUseClientCredentialCheckBox().setEnabled(true);
            tp.getSavePasswordCheckBox().setEnabled(false);
            tp.getUserPasswordField().setEnabled(false);
            tp.getUserPasswordField().setEditable(false);
            tp.getUsernameTextField().setEditable(false);
        }
        else {
            tp.getUseClientCredentialCheckBox().setEnabled(true);
            tp.getSavePasswordCheckBox().setEnabled(true);
            tp.getUserPasswordField().setEnabled(true);
            tp.getUserPasswordField().setEditable(true);
            tp.getUsernameTextField().setEditable(ssg.getRuntime().getSsgKeyStoreManager().lookupClientCertUsername() == null);
        }
    }

    private SsgNetworkPanel getNetworkPane() {
        if (networkPane == null) {
            final boolean wsdlAndPorts = !ssg.isGeneric();
            networkPane = new SsgNetworkPanel(validator, ssg, ssgFinder, wsdlAndPorts, wsdlAndPorts);
        }
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
            int gridY = 0;
            JPanel pane = new JPanel(new GridBagLayout());
            generalPane = new JScrollPane(pane);
            generalPane.setBorder(BorderFactory.createEmptyBorder());

            pane.add(getImageLabel(ssg),
                     new GridBagConstraints(0, gridY++, 2, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.NONE,
                                            new Insets(25, 5, 0, 0), 0, 0));

            String splaintext = "Enter the host name or IP address of the " + serverType() + " that will process service requests.";
            WrappingLabel splain01 = new WrappingLabel(splaintext, 3);
            pane.add(splain01,
                     new GridBagConstraints(0, gridY++, 2, 1, 1000.0, 0.0,
                                            GridBagConstraints.WEST,
                                            GridBagConstraints.HORIZONTAL,
                                            new Insets(25, 25, 0, 25), 0, 0));

            pane.add(new JLabel(ssg.isGeneric() ? "Service URL:" : "Gateway Host Name:"),
                     new GridBagConstraints(0, gridY, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.EAST,
                                            GridBagConstraints.NONE,
                                            new Insets(5, 25, 0, 0), 0, 0));
            pane.add(getFieldServerAddress(),
                     new GridBagConstraints(1, gridY++, 1, 1, 1000.0, 0.0,
                                            GridBagConstraints.WEST,
                                            GridBagConstraints.HORIZONTAL,
                                            new Insets(5, 5, 0, 25), 0, 0));

            JTextField fkn = getFieldKerberosName(); // ensure created
            if(KerberosUtils.isEnabled() && !ssg.isFederatedGateway()) {
                pane.add(new JLabel("Kerberos Name:"),
                         new GridBagConstraints(0, gridY, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.EAST,
                                                GridBagConstraints.NONE,
                                                new Insets(5, 25, 0, 0), 0, 0));
                pane.add(fkn,
                         new GridBagConstraints(1, gridY++, 1, 1, 1000.0, 0.0,
                                                GridBagConstraints.WEST,
                                                GridBagConstraints.HORIZONTAL,
                                                new Insets(5, 5, 0, 25), 0, 0));
            }

            // Have a spacer eat any leftover space
            pane.add(new JPanel(),
                     new GridBagConstraints(0, gridY, 2, 1, 1.0, 1.0,
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

    class CertAliasPicker extends JDialog implements CertUtils.AliasPicker {

        public CertAliasPicker(Dialog parent) {
            super(parent);
        }

        public String selectAlias(String[] options) throws AliasNotFoundException {
            Object selectedOption = JOptionPane.showInputDialog(this, "Select the alias for the certificate you want to import.",
                    "Select an Alias.", JOptionPane.QUESTION_MESSAGE,
                    null,options , null);
            return (String)selectedOption;

        }
    }

    /** Get the Server URL text field. */
    private JTextField getFieldServerAddress() {
        if (fieldServerAddress == null) {
            fieldServerAddress = new SquigglyTextField();
            Utilities.attachDefaultContextMenu(fieldServerAddress);
            fieldServerAddress.setPreferredSize(new Dimension(220, 20));
            if (ssg.isGeneric()) {
                fieldServerAddress.setToolTipText("<HTML>Web service POST URL, for example <br><address>http://service.example.com/soap");
                validator.constrainTextFieldToBeNonEmpty("Service URL", fieldServerAddress,
                                                         new InputValidator.ComponentValidationRule(fieldServerAddress) {
                                                             public String getValidationError() {
                                                                 if (!ValidationUtils.isValidUrl(fieldServerAddress.getText()))
                                                                     return "Service URL must be a valid URL.";
                                                                 return null;
                                                             }
                                                         });
            } else {
                fieldServerAddress.setToolTipText("<HTML>Gateway host name or address, for example<br><address>gateway.example.com");
                validator.constrainTextFieldToBeNonEmpty("Gateway server address", fieldServerAddress,
                                                         new InputValidator.ComponentValidationRule(fieldServerAddress) {
                                                             public String getValidationError() {
                                                                 if (!ValidationUtils.isValidDomain(fieldServerAddress.getText()))
                                                                     return "Gateway server address must be a valid host name or IP address.";
                                                                 return null;
                                                             }
                                                         });
            }
        }
        return fieldServerAddress;
    }

    /** Get the Kerberos service/host name */
    private JTextField getFieldKerberosName() {
        if (fieldKerberosName == null) {
            fieldKerberosName = new ContextMenuTextField();
            fieldKerberosName.setPreferredSize(new Dimension(220, 20));
            fieldKerberosName.setToolTipText("<HTML>Optional Gateway service/host name, for example<br><address>http/gateway.example.com");
            validator.constrainTextField(fieldKerberosName, new InputValidator.ComponentValidationRule(fieldKerberosName) {
                public String getValidationError() {
                    if (!ValidationUtils.isValidCharacters(fieldKerberosName.getText(), ValidationUtils.ALPHA_NUMERIC+"/.-_"))
                        return "Optional gateway service field must contain only letters, numbers, slashes, and periods.";
                    return null;
                }
            });
        }
        return fieldKerberosName;
    }

    private boolean isPortsCustom(Ssg ssg) {
        return referenceSsg.getSsgPort() != ssg.getSsgPort() || referenceSsg.getSslPort() != ssg.getSslPort();
    }

    /**
     * Set the Ssg object being edited by this panel.
     */
    private void modelToView() {
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
                    char[] pass = strat.password();
                    if (pass == null) pass = new char[0];
                    fp.getWstPasswordField().setText(new String(pass));
                    fp.getWsTrustUrlTextField().setText(strat.getWsTrustUrl());
                    // TODO fp.getWstSavePasswordCheckBox() + for wsFed
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
                    char[] pass = strat.password();
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
                    tp.getUseKerberosCredentialCheckbox().setEnabled(false);
                    ssg.setEnableKerberosCredentials(false);
                }
                tp.getUseKerberosCredentialCheckbox().setSelected(ssg.isEnableKerberosCredentials());
            }

            updateIdentityEnableState();
            boolean customPorts = isPortsCustom(ssg);
            getNetworkPane().setCustomPorts(customPorts);
            String endpointBase = "http://localhost:" + bindPort + "/";
            getNetworkPane().setLocalEndpointBase(endpointBase);
            getNetworkPane().setDefaultLocalEndpoint(ssg.makeDefaultLocalEndpoint());
            getNetworkPane().setCurrentLocalEndpoint(ssg.getLocalEndpoint());
            getNetworkPane().setWsdlEndpointSuffix(ClientProxy.WSIL_SUFFIX);

            if (ssg.isGeneric()) {
                getFieldServerAddress().setText(ssg.getServerUrl());
            }
            else
                getFieldServerAddress().setText(ssg.getSsgAddress());
            getFieldKerberosName().setText(ssg.getKerberosName());

            getNetworkPane().setSsgPort(ssg.getSsgPort());
            getNetworkPane().setSslPort(ssg.getSslPort());
            getNetworkPane().setUseOverrideIpAddresses(ssg.isUseOverrideIpAddresses());
            getNetworkPane().setCustomIpAddresses(ssg.getOverrideIpAddresses());
            getNetworkPane().setFailoverStrategyName(ssg.getFailoverStrategyName());
            getBridgePolicyPane().setUseSslByDefault(ssg.isUseSslByDefault());
            getBridgePolicyPane().setHeaderPassthrough(ssg.isHttpHeaderPassthrough());
            getBridgePolicyPane().setProperties(ssg.getProperties());
            getNetworkPane().updateCustomPortsEnableState();
        }
        getPoliciesPane().setPolicyCache(ssg.getRuntime().getPolicyManager());

        validator.validate();
    }

    /**
     * Called when the Ok button is pressed.
     * Should copy any updated properties into the target object and return normally.
     * Caller is responsible for hiding and disposing of the property dialog.
     */
    protected void commitChanges() {
        try {
            synchronized (ssg) {
                // Try to set server URL first since it might throw
                if (ssg.isGeneric()) {
                    URL url = new URL(getFieldServerAddress().getText().trim());
                    ssg.setServerUrl(url.toExternalForm());
                    ssg.setSsgPort(url.getPort());
                    ssg.setSsgFile(url.getFile());
                    ssg.setSslPort(url.getPort());
                    ssg.setSsgAddress(url.getHost());
                    ssg.setUseSslByDefault("https".equalsIgnoreCase(url.getProtocol()));
                } else {
                    ssg.setSsgAddress(getFieldServerAddress().getText().trim().toLowerCase());
                    commitPorts();
                }

                if (!ssg.isFederatedGateway()) {
                    TrustedSsgIdentityPanel tp = (TrustedSsgIdentityPanel)ssgIdentityPane;
                    ssg.setKerberosName(getFieldKerberosName().getText());
                    ssg.setUsername(tp.getUsernameTextField().getText().trim());
                    ssg.setSavePasswordToDisk(tp.getSavePasswordCheckBox().isEnabled() &&
                                              tp.getSavePasswordCheckBox().isSelected());
                    ssg.setChainCredentialsFromClient(tp.getUseClientCredentialCheckBox().isEnabled() &&
                                                      tp.getUseClientCredentialCheckBox().isSelected());
                    ssg.setEnableKerberosCredentials(tp.getUseKerberosCredentialCheckbox().isEnabled() &&
                                                     tp.getUseKerberosCredentialCheckbox().isSelected());

                    // We'll treat a blank password as though it's unconfigured.  If the user really needs to use
                    // a blank password to access a service, he can leave the password field blank in the logon
                    // dialog when it eventually appears.
                    char[] pass = tp.getUserPasswordField().getPassword();

                    // Make sure prompting is enabled
                    ssg.getRuntime().setCachedPassword(pass.length > 0 ? tp.getUserPasswordField().getPassword() : null);
                } else {
                    FederatedSsgIdentityPanel fp = (FederatedSsgIdentityPanel)ssgIdentityPane;

                    AbstractSamlTokenStrategy astrat = ssg.getWsTrustSamlTokenStrategy();
                    if(astrat!=null) {
                        updateStrategyFromView(astrat, fp);
                        astrat.clearCachedToken();
                    }

                    // Force chain credentials to be off if this is a fed ssg
                    ssg.setChainCredentialsFromClient(false);
                }

                // applicable to both trusted and federated SSG
                if (!ssg.isGeneric())
                    ssg.setUseSslByDefault(getBridgePolicyPane().isUseSslByDefault());
                ssg.setProperties(getBridgePolicyPane().getProperties());
                ssg.setHttpHeaderPassthrough(getBridgePolicyPane().isHeaderPassthrough());
                ssg.setUseOverrideIpAddresses(getNetworkPane().isUseOverrideIpAddresses());
                ssg.setOverrideIpAddresses(getNetworkPane().getCustomIpAddresses());
                ssg.setFailoverStrategyName(getNetworkPane().getFailoverStrategyName());

                if (getNetworkPane().isCustomLabel()) {
                    ssg.setLocalEndpoint(getNetworkPane().getCustomLabel());
                } else {
                    ssg.setLocalEndpoint(ssg.makeDefaultLocalEndpoint());
                }

                // Reset the runtime, but preserve the existing cached password and policy cache
                {
                    char[] cached = ssg.getRuntime().getCachedPassword();
                    PolicyManager policyCache = ssg.getRuntime().getPolicyManager();
                    ssg.resetRuntime();
                    ssg.getRuntime().setCachedPassword(cached);
                    ssg.getRuntime().setPolicyManager(policyCache);
                }
            }
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(Gui.getInstance().getFrame(),
                                          "The specified server URL is not valid.",
                                          "Invalid server URL",
                                          JOptionPane.ERROR_MESSAGE);
        }
        modelToView();
    }

    private void commitPorts() {
        if (getNetworkPane().isCustomPorts()) {
            ssg.setSsgPort(getNetworkPane().getSsgPort());
            ssg.setSslPort(getNetworkPane().getSslPort());
        } else {
            ssg.setSsgPort(referenceSsg.getSsgPort());
            ssg.setSslPort(referenceSsg.getSslPort());
        }

        if (!ssg.isGeneric()) {
            try {
                ssg.setServerUrl(null);
            } catch(MalformedURLException mue) {
                log.log(Level.WARNING, "Error resetting serverUrl.", mue);
            }
        }
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
     * Called after a successful token request to update the username/password (if changed).
     */
    private void updateViewCredentialsFromStrategy(FederatedSsgIdentityPanel fp, AbstractSamlTokenStrategy strat) {
        final String username;
        char[] password;

        if(strat instanceof WsTrustSamlTokenStrategy) {
            username = ((WsTrustSamlTokenStrategy)strat).getUsername();
            password = ((WsTrustSamlTokenStrategy)strat).password();
        }
        else if(strat instanceof WsFederationPRPSamlTokenStrategy) {
            username = ((WsFederationPRPSamlTokenStrategy)strat).getUsername();
            password = ((WsFederationPRPSamlTokenStrategy)strat).password();
        }
        else {
            throw new IllegalArgumentException("Unsupported strategy type");
        }

        if (password == null) password = new char[0];

        fp.getWstUsernameField().setText(username);
        fp.getWstPasswordField().setText(new String(password));
    }

    /**
     * Copy the information from the specified panel into the specified WS-Trust token strategy.
     */
    private void updateWsTrustStrategyFromView(WsTrustSamlTokenStrategy strat, FederatedSsgIdentityPanel fp) {
        // This is a federated Ssg using a third-party Ws-Trust service
        strat.storePassword(fp.getWstPasswordField().getPassword());
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
        strat.storePassword(fp.getWsFedPasswordField().getPassword());
        strat.setUsername(fp.getWsFedUsernameField().getText());
        strat.setIpStsUrl(fp.getWsFedUrlTextField().getText());
        strat.setRealm(fp.getWsFedRealmTextField().getText());
        strat.setReplyUrl(fp.getWsFedReplyUrlTextField().getText());
        strat.setContext(fp.getWsFedContextTextField().getText());
        strat.setTimestamp(fp.getWsFedTimestampCheckBox().isSelected());
    }

    /** @return serverType followed by a space and ssgName. */
    private String serverName() {
        return serverType() + ' ' + ssgName();
    }

    /** @return either "SecureSpan Gateway" or "Web service" depending on whether Ssg is generic. */
    private String serverType() {
        return ssg.isGeneric() ? "Web service" : "SecureSpan Gateway";
    }

    /**
     * Find the SSG name to display.  Uses the name from the SSG, if any; otherwise, the typed
     * in name; otherwise the string "&lt;New Gateway&gt;" or the string "&lt;New Service&gt;".
     */
    private String ssgName() {
        String result =  ssg.getSsgAddress();
        if (result != null && result.length() > 0)
            return result;
        result = getFieldServerAddress().getText();
        if (result != null && result.length() > 0)
            return result;
        return ssg.isGeneric() ? "<New Web Service>" : "<New Gateway>";
    }

    public void policyAttached(SsgEvent evt) {
        getPoliciesPane().updatePolicyPanel();
    }

    public void dataChanged(SsgEvent evt) {
        // Take no action; we don't override the user's data.
    }

    public void sslReset(SsgEvent evt) {
        // Don't care
    }

    public void setVisible(boolean b) {
        if (b)
            getFieldServerAddress().requestFocus();
        super.setVisible(b);
    }
}
