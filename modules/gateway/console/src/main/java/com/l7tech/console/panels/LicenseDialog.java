/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.panels;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.action.ActionVetoException;
import com.l7tech.console.util.ClusterPropertyCrud;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.gateway.common.License;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.*;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SignatureException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog for viewing the current license and possibly installing a new one.
 */
public class LicenseDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(LicenseDialog.class.getName());

    final LicensePanel licensePanel;
    private JPanel rootPanel;
    private JPanel leftPanel;
    private JButton closeButton;
    private JButton installButton;
    private JButton removeButton;

    private boolean showingLicenseOrError = false;
    private ClusterStatusAdmin admin = Registry.getDefault().getClusterStatusAdmin();

    private boolean disconnectManagerOnClose;

    public LicenseDialog(Frame owner, String gatewayName) throws HeadlessException {
        super(owner);
        this.licensePanel = new LicensePanel(gatewayName, false);
        init();
    }

    public LicenseDialog(Dialog owner, String gatewayName) throws HeadlessException {
        super(owner);
        this.licensePanel = new LicensePanel(gatewayName, false);
        init();
    }

    /**
     * Find a new license InputStream from the user.
     * @param result callback that will be informed asynchronously of either the resulting inputstream or error.
     */
    private void findLicenseStream(final Functions.BinaryVoid<InputStream, Throwable> result) {
        try {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public InputStream run() {
                    try {
                        result.call(getLicenseStreamFromFile(), null);
                    } catch (IOException e) {
                        result.call(null, e);
                    }
                    return null;
                }
            });
        } catch (AccessControlException e) {
            getLicenseStreamFromTextBox(result);
        }
    }

    private void getLicenseStreamFromTextBox(final Functions.BinaryVoid<InputStream, Throwable> result) {
        JDialog textDlg = new JDialog(this, "License XML", true);
        textDlg.setLayout(new BorderLayout());
        textDlg.add(new JLabel("Paste the new license XML below:"), BorderLayout.NORTH);
        final TextArea licenseTextArea = new TextArea(15, 80);
        textDlg.add(licenseTextArea, BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
        buttons.add(Box.createGlue());
        buttons.add(okButton);
        buttons.add(cancelButton);
        textDlg.add(buttons, BorderLayout.SOUTH);

        textDlg.pack();
        Utilities.centerOnScreen(textDlg);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                result.call(new ByteArrayInputStream(HexUtils.encodeUtf8(licenseTextArea.getText())), null);
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        textDlg.getRootPane().setDefaultButton(okButton);
        DialogDisplayer.display(textDlg);
    }

    /**
     * Try to get the license stream from a file on disk, if we have permission to do so.
     *
     * @return  the license stream, or null to cancel.
     * @throws AccessControlException  if our privileges are insufficient
     * @throws java.io.IOException if the selected file can't be opened or read
     */
    private InputStream getLicenseStreamFromFile() throws AccessControlException, IOException {
        JFileChooser fc = FileChooserUtil.createJFileChooser();
        fc.setFileFilter(new FileFilter() {
            @Override
            public String getDescription() {
                return "License files (*.xml)";
            }

            @Override
            public boolean accept(File f) {
                final String name = f.getName().toLowerCase();
                return f.isDirectory() || name.endsWith(".xml");
            }
        });
        fc.setDialogTitle("Select license file to install");
        int result = fc.showOpenDialog(LicenseDialog.this);
        if (result != JFileChooser.APPROVE_OPTION)
            return null;

        File file = fc.getSelectedFile();
        if (file == null)
            return null;

        return new FileInputStream(file);        
    }

    private void onClose() {
        dispose();

        if (disconnectManagerOnClose) {
            // disconnect from SSG
            TopComponents.getInstance().setConnectionLost(true);
            TopComponents.getInstance().disconnectFromGateway();
        }
    }

    private void init() {
        DialogDisplayer.suppressSheetDisplay(this); // Dialogs that resize themselves are incompatible with sheet display (Bug #3969)
        setTitle("Gateway Cluster License");
        Container cp = getContentPane();
        cp.setLayout(new BoxLayout(cp, BoxLayout.X_AXIS));
        this.add(rootPanel);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.add(licensePanel);
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                String confirmationDialogTitle = "Confirm License Removal";
                String confirmationDialogMessage =
                    "<html>This will irrevocably destroy the existing gateway license and cannot<p>" +
                        "be undone.  Also this will cause the Policy Manager to disconnect.<p>" +
                        "<center>Really remove the existing gateway license?</center></html>";

                DialogDisplayer.showSafeConfirmDialog(
                    LicenseDialog.this,
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

                            try {
                                ClusterProperty licProp = admin.findPropertyByName("license");
                                if (licProp != null) {
                                    admin.deleteProperty(licProp);
                                }
                            } catch (ObjectModelException e2) {
                                JOptionPane.showMessageDialog(LicenseDialog.this,
                                    "Unable to forcibly remove this license file: " +
                                        ExceptionUtils.getMessage(e2),
                                    "Unable to remove license file",
                                    JOptionPane.ERROR_MESSAGE);
                                return;
                            }

                            disconnectManagerOnClose = true;
                            onClose();
                        }
                    }
                );
            }
        });

        setRemoveButtonVisibleOrInvisible();

        // create the add action listener
        installButton.addActionListener(createInstallButtonAction());

        try {
            License license = admin.getCurrentLicense();
            Registry.getDefault().getLicenseManager().setLicense(license);
            licensePanel.setLicense(license);
            showingLicenseOrError = license != null;
            installButton.setText(showingLicenseOrError ? "Change License" : "Install License");
            pack();
        } catch (InvalidLicenseException e) {
            licensePanel.setLicenseError(ExceptionUtils.getMessage(e));
            showingLicenseOrError = true;
            pack();
        }

        getRootPane().setDefaultButton( closeButton );
        closeButton.requestFocusInWindow();
        Utilities.setFocusable( rootPanel, false, installButton, closeButton, removeButton );
    }

    /**
     * Set the remove button to be visible or invisible depending on if a license has been installed or not.
     */
    private void setRemoveButtonVisibleOrInvisible() {
        try {
            ClusterProperty licProp = admin.findPropertyByName("license");
            if (licProp == null) {
                removeButton.setVisible(false);
            } else {
                removeButton.setVisible(true);
            }
        } catch (ObjectModelException e2) {
            removeButton.setVisible(false);
        }
    }

    /**
     * Show the click-wrap EULA dialog, if there's a EULA in this license.
     *
     * @param licenseXml the license XML that is about to be installed.  Required.
     * @return true if the user clicked "I agree"
     * @throws IOException if there is a problem reading the license text.
     * @throws InvalidLicenseException if the license XML is not valid.
     */
    private boolean eulaConfirmed(String licenseXml) throws IOException, InvalidLicenseException {
        final License license;
        try {
            // Parse to extract custom eula, but don't bother checking signature or feature sets yet
            // (that'll be done on the server when we try to install the license)
            license = new License(licenseXml, null, null);
        } catch (TooManyChildElementsException e) {
            throw new InvalidLicenseException(e);
        } catch (SignatureException e) {
            throw new InvalidLicenseException(e); // can't happen
        } catch (SAXException e) {
            throw new InvalidLicenseException(e);
        } catch (ParseException e) {
            throw new InvalidLicenseException(e);
        }

        EulaDialog clickWrap = new EulaDialog(LicenseDialog.this, license);

        if (!clickWrap.isEulaPresent())
            throw new InvalidLicenseException("The specified license does not contain any EULA text.");

        clickWrap.pack();
        Utilities.centerOnScreen(clickWrap);
        clickWrap.setVisible(true);
        return clickWrap.isConfirmed();
    }


    private ActionListener createInstallButtonAction() {

        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                // if the user chooses change the license, perform clear the workspace
                if ("Change License".equals(installButton.getText())) {
                    doChangeLicense();
                } else {
                    doFindLicenses();
                }
            }
        };
    }

    private void doChangeLicense() {

        try {
            // display warning to user
            int resp = JOptionPane.showConfirmDialog(this,
                    "Changing the gateway license will disconnect the Policy Manager.\nDo you wish to continue? ",
                    "Change Gateway License", JOptionPane.YES_NO_OPTION);

            if (JOptionPane.YES_OPTION == resp) {
                // Clear off the workspace so users can save any un-saved changes
                TopComponents.getInstance().getCurrentWorkspace().clearWorkspace();
                
                // perform the license change -- setting the flag to disconnect the manager when complete
                disconnectManagerOnClose = doFindLicenses();
            }

        } catch (ActionVetoException veto) {
            // do nothing
        }

    }

    private boolean doFindLicenses() {

        final List<Boolean> success = new ArrayList<Boolean>(2);
        success.add(Boolean.FALSE);

        findLicenseStream(new Functions.BinaryVoid<InputStream, Throwable>() {
            @Override
            public void call(InputStream is, Throwable ioe) {
                try {
                    if (ioe != null) throw new CausedIOException(ioe);
                    if (is == null) return;
                    String licenseXml = XmlUtil.nodeToString(XmlUtil.parse(is));

                    //check if the license is valid
                    try {
                        admin.validateLicense(licenseXml);
                    } catch (InvalidLicenseException ile) {
                        JOptionPane.showMessageDialog(LicenseDialog.this,
                                "That license is invalid and cannot be installed:\n " + ExceptionUtils.getMessage(ile),
                                "Invalid license", JOptionPane.ERROR_MESSAGE);
                            return;
                    }
                    if (showingLicenseOrError) {
                        // Last chance to confirm installation over top of an existing license
                        final String cancel = "    Cancel    ";
                        final String destroyIt = " Destroy Existing License ";
                        String options[] = { destroyIt, cancel };
                        String valid = licensePanel.isValidLicense() ? "valid" : "invalid";

                        int confResult = JOptionPane.showOptionDialog(
                                LicenseDialog.this,
                                "Are you sure you want to REPLACE the existing " + valid + " license with the\nnew license?",
                                "Destroy Existing License",
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.WARNING_MESSAGE,
                                null,
                                options,
                                cancel);
                        if (confResult != 0)
                            return;
                    }

                    try {
                        // Show click wrap license
                        if (!eulaConfirmed(licenseXml))
                            return;

                        admin.installNewLicense(licenseXml);
                    } catch (InvalidLicenseException e1) {
                        final String msg = ExceptionUtils.getMessage(e1);

                        String postDated = "is not yet valid: becomes valid on";
                        if (msg.indexOf(postDated) < 1) {
                            // Not post-dated; something else is wrong with it
                            JOptionPane.showMessageDialog(LicenseDialog.this,
                                                          "That license is invalid and cannot be installed:\n" +
                                                                  ExceptionUtils.getMessage(e1),
                                                          "Unable to install license",
                                                          JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        final String cancel = "    Cancel    ";
                        final String force = "  Forcibly Install Invalid License  ";
                        String options[] = { force, cancel };

                        int confResult = JOptionPane.showOptionDialog(
                                LicenseDialog.this,
                                "That license is not valid, but might become valid in the future:\n\n\t" +
                                msg +
                                "\n\n" +
                                "Do you want to force the invalid license to be installed?",
                                "Invalid License File",
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.WARNING_MESSAGE,
                                null,
                                options,
                                cancel);
                        if (confResult != 0)
                            return;

                        try {
                            // Go behind the license manager's back and forcibly install the invalid license
                            ClusterPropertyCrud.putClusterProperty("license", licenseXml);
                            // Fallthrough and update the license panel
                        } catch (ObjectModelException e2) {
                            JOptionPane.showMessageDialog(LicenseDialog.this,
                                                          "Unable to forcibly install this license file: " +
                                                                  ExceptionUtils.getMessage(e2),
                                                          "Unable to install license file",
                                                          JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }

                    try {
                        License license = admin.getCurrentLicense();
                        showingLicenseOrError = license != null;
                        licensePanel.setLicense(license);
                        TopComponents.getInstance().getAssertionRegistry().updateModularAssertions();
                        TopComponents.getInstance().getEncapsulatedAssertionRegistry().updateEncapsulatedAssertions();
                        Registry.getDefault().getLicenseManager().setLicense(license);
                        installButton.setText(showingLicenseOrError ? "Change License" : "Install License");
                        pack();
                        success.add(0, Boolean.TRUE);
                    } catch (InvalidLicenseException e1) {
                        licensePanel.setLicenseError(ExceptionUtils.getMessage(e1));
                        showingLicenseOrError = true;
                        pack();
                    }

                    closeButton.requestFocusInWindow();

                    setRemoveButtonVisibleOrInvisible();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(LicenseDialog.this,
                                                  "Unable to read this license file: " + ExceptionUtils.getMessage(ex),
                                                  "Unable to read license file",
                                                  JOptionPane.ERROR_MESSAGE);
                } catch (SAXException e1) {
                    JOptionPane.showMessageDialog(LicenseDialog.this,
                                                  "The specified license file isn't well-formed XML: " + ExceptionUtils.getMessage(e1),
                                                  "Unable to read license file",
                                                  JOptionPane.ERROR_MESSAGE);
                } catch (UpdateException e1) {
                    logger.log(Level.SEVERE, "Unable to install license: " + ExceptionUtils.getMessage(e1), e1);
                } catch (FindException ex) {
                    logger.log(Level.SEVERE, "Unable to install license: " + ExceptionUtils.getMessage(ex), ex);
                } finally {
                    if (is != null) //noinspection EmptyCatchBlock
                        try { is.close(); } catch (IOException ex) {}
                }
            }
        });

        return success.get(0);
    }
}
