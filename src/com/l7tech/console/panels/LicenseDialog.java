/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.panels;

import com.l7tech.cluster.ClusterProperty;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.InvalidLicenseException;
import com.l7tech.common.License;
import com.l7tech.common.gui.widgets.LicensePanel;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.UpdateException;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.*;

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

    private boolean showingLicenseOrError = false;

    public LicenseDialog(Frame owner, String gatewayName) throws HeadlessException {
        super(owner);
        this.licensePanel = new LicensePanel(gatewayName);
        init();
    }

    public LicenseDialog(Dialog owner, String gatewayName) throws HeadlessException {
        super(owner);
        this.licensePanel = new LicensePanel(gatewayName);
        init();
    }

    /**
     * Get a new license InputStream from the user.
     * @return  a license stream, or null to cancel.
     * @throws java.io.IOException if a file is designated but couldn't be opened or read
     */
    private InputStream getLicenseStream() throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<InputStream>() {
                public InputStream run() throws IOException {
                    return getLicenseStreamFromFile();
                }
            });
        } catch (AccessControlException e) {
            /* FALLTHROUGH and try text box */
        } catch (PrivilegedActionException e) {
            if (ExceptionUtils.causedBy(e, IOException.class))
                throw new CausedIOException(ExceptionUtils.getMessage(e), e);
            /* FALLTHROUGH and try text box */
        }
        return getLicenseStreamFromTextBox();
    }

    private InputStream getLicenseStreamFromTextBox() {
        JDialog textDlg = new JDialog(this, "License XML", true);
        textDlg.setLayout(new BorderLayout());
        textDlg.add(new JLabel("Paste the new license XML below:"), BorderLayout.NORTH);
        final JTextArea licenseTextArea = new JTextArea(15, 80);
        Utilities.attachDefaultContextMenu(licenseTextArea);
        textDlg.add(new JScrollPane(licenseTextArea,
                                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS),
                    BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));

        JButton okButton = new JButton("Ok");
        JButton cancelButton = new JButton("Cancel");
        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
        buttons.add(Box.createGlue());
        buttons.add(okButton);
        buttons.add(cancelButton);
        textDlg.add(buttons, BorderLayout.SOUTH);

        textDlg.pack();
        Utilities.centerOnScreen(textDlg);

        final InputStream[] result = new InputStream[1];

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                result[0] = new ByteArrayInputStream(licenseTextArea.getText().getBytes());
                setVisible(false);
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        textDlg.getRootPane().setDefaultButton(okButton);
        textDlg.setVisible(true);
        textDlg.dispose();
        return result[0];
    }

    /**
     * Try to get the license stream from a file on disk, if we have permission to do so.
     *
     * @return  the license stream, or null to cancel.
     * @throws AccessControlException  if our privileges are insufficient
     * @throws java.io.IOException if the selected file can't be opened or read
     */
    private InputStream getLicenseStreamFromFile() throws AccessControlException, IOException {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileFilter() {
            public String getDescription() {
                return "License files (*.xml)";
            }

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

    private void init() {
        setTitle("Gateway Cluster License");
        Container cp = getContentPane();
        cp.setLayout(new BoxLayout(cp, BoxLayout.X_AXIS));
        this.add(rootPanel);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.add(licensePanel);
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        installButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                InputStream is = null;
                try {
                    is = getLicenseStream();
                    if (is == null) return;
                    String licenseXml = XmlUtil.nodeToString(XmlUtil.parse(is));
                    Registry reg = Registry.getDefault();
                    ClusterStatusAdmin admin = reg.getClusterStatusAdmin();

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
                            ClusterProperty licProp = admin.findPropertyByName("license");
                            if (licProp == null) licProp = new ClusterProperty("license", licenseXml);
                            admin.saveProperty(licProp);
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
                        reg.getLicenseManager().setLicense(license);
                        installButton.setVisible(false);
                        pack();
                        return;
                    } catch (InvalidLicenseException e1) {
                        licensePanel.setLicenseError(ExceptionUtils.getMessage(e1));
                        showingLicenseOrError = true;
                        pack();
                        return;
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(LicenseDialog.this,
                                                  "Unable to read this license file: " + ExceptionUtils.getMessage(ex),
                                                  "Unable to read license file",
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (SAXException e1) {
                    JOptionPane.showMessageDialog(LicenseDialog.this,
                                                  "The specified license file isn't well-formed XML: " + ExceptionUtils.getMessage(e1),
                                                  "Unable to read license file",
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (UpdateException e1) {
                    logger.log(Level.SEVERE, "Unable to install license: " + ExceptionUtils.getMessage(e1), e1);
                } finally {
                    if (is != null) //noinspection EmptyCatchBlock
                        try { is.close(); } catch (IOException ex) {}
                }
            }
        });

        try {
            Registry reg = Registry.getDefault();
            ClusterStatusAdmin admin = reg.getClusterStatusAdmin();
            License license = admin.getCurrentLicense();
            reg.getLicenseManager().setLicense(license);
            licensePanel.setLicense(license);
            showingLicenseOrError = license != null;
            installButton.setText(showingLicenseOrError ? "Change License" : "Install License");
            pack();
        } catch (InvalidLicenseException e) {
            licensePanel.setLicenseError(ExceptionUtils.getMessage(e));
            showingLicenseOrError = true;
            pack();
        } catch (RemoteException e) {
            logger.log(Level.SEVERE, "Unable to get current license: " + ExceptionUtils.getMessage(e), e);
        }
    }
}
