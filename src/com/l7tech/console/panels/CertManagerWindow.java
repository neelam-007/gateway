package com.l7tech.console.panels;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.l7tech.console.action.Actions;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.event.*;
import com.l7tech.console.table.TrustedCertTableSorter;
import com.l7tech.console.table.TrustedCertsTable;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.*;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.Vector;
import java.util.logging.Logger;
import java.rmi.RemoteException;


/**
 * This class is the main window of the trusted certificate manager
 *
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p/>
 * $Id$
 */
public class CertManagerWindow extends JDialog {

    private JPanel mainPanel;
    private JButton addButton;
    private JButton removeButton;
    private JButton propertiesButton;
    private JButton closeButton;
    private static CertManagerWindow instance = null;
    private TrustedCertsTable trustedCertTable = null;
    private JScrollPane certTableScrollPane;
    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(CertManagerWindow.class.getName());

    /**
     * Constructor
     *
     * @param owner  The parent component
     */
    private CertManagerWindow(Frame owner) {
        super(owner, resources.getString("dialog.title"), true);
        initialize();
        loadTrustedCerts();
    }

    /**
     *  Create a instance of CertManagerWindow if it does not exist
     * @param owner The parent component
     * @return The object reference of the instance
     */
    public static CertManagerWindow getInstance(Window owner) {

        if (instance == null) {
          instance = new CertManagerWindow((Frame) owner);
        }

        return instance;
    }

    /**
     * Initialization of the cert manager window
     */
    private void initialize() {

        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);

        if(trustedCertTable == null) {
            trustedCertTable = new TrustedCertsTable();
        }
        certTableScrollPane.setViewportView(trustedCertTable);
        certTableScrollPane.getViewport().setBackground(Color.white);

        // Hide the cert usage data column
        trustedCertTable.hideColumn(TrustedCertTableSorter.CERT_TABLE_ISSUER_NAME_COLUMN_INDEX);

        trustedCertTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            /**
             * Called whenever the value of the selection changes.
             *
             * @param e the event that characterizes the change.
             */
            public void valueChanged(ListSelectionEvent e) {

                enableOrDisableButtons();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                hide();
                dispose();
            }
        });

        addButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {

                        CertImportMethodsPanel sp = new CertImportMethodsPanel(
                                             new CertDetailsPanel(
                                             new CertUsagePanel(null)), true);

                        JFrame f = TopComponents.getInstance().getMainWindow();
                        Wizard w = new AddCertificateWizard(f, sp);
                        w.addWizardListener(wizardListener);

                        // register itself to listen to the addEvent
                        //addEntityListener(listener);

                        w.pack();
                        w.setSize(780, 560);
                        Utilities.centerOnScreen(w);
                        w.setVisible(true);

                    }
                });
            }
        });

        propertiesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int sr = trustedCertTable.getSelectedRow();
                TrustedCert tc = (TrustedCert) trustedCertTable.getTableSorter().getData(sr);
                TrustedCert updatedTrustedCert = null;

                // retrieve the latest version
                try {
                    updatedTrustedCert = getTrustedCertAdmin().findCertByPrimaryKey(tc.getOid());
                } catch (FindException e) {
                    JOptionPane.showMessageDialog(mainPanel, resources.getString("cert.find.error"),
                            resources.getString("view.error.title"),
                            JOptionPane.ERROR_MESSAGE);
                } catch (RemoteException e) {
                    JOptionPane.showMessageDialog(mainPanel, resources.getString("cert.remote.exception"),
                            resources.getString("view.error.title"),
                            JOptionPane.ERROR_MESSAGE);
                }

                trustedCertTable.getTableSorter().updateData(sr, updatedTrustedCert);
                CertPropertiesWindow cpw = new CertPropertiesWindow(instance, updatedTrustedCert, true);

                cpw.show();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int sr = trustedCertTable.getSelectedRow();

                String certName = (String) trustedCertTable.getValueAt(sr, TrustedCertTableSorter.CERT_TABLE_CERT_NAME_COLUMN_INDEX);
                TrustedCert tc = (TrustedCert) trustedCertTable.getTableSorter().getData(sr);

                Object[] options = { "Remove", "Cancel" };
                int result = JOptionPane.showOptionDialog(null,
                                                          "<html>Are you sure you want to remove the certificate:  " +
                                                          certName + "?<br>" +
                                                          "<center>This action cannot be undone." +
                                                          "</center></html>",
                                                          "Remove the certificate?",
                                                          0, JOptionPane.WARNING_MESSAGE,
                                                          null, options, options[1]);
                if (result == 0) {
                    try {
                        getTrustedCertAdmin().deleteCert(tc.getOid());

                        // reload all certs from server
                        loadTrustedCerts();

                    } catch (FindException e) {
                        JOptionPane.showMessageDialog(instance, resources.getString("cert.find.error"),
                                        resources.getString("save.error.title"),
                                        JOptionPane.ERROR_MESSAGE);
                    } catch (DeleteException e) {
                        JOptionPane.showMessageDialog(instance, resources.getString("cert.delete.error"),
                                        resources.getString("save.error.title"),
                                        JOptionPane.ERROR_MESSAGE);
                    } catch (RemoteException e) {
                         JOptionPane.showMessageDialog(instance, resources.getString("cert.remote.exception"),
                                        resources.getString("save.error.title"),
                                        JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        pack();
        enableOrDisableButtons();
        Actions.setEscKeyStrokeDisposes(this);

    }

    /**
     * Load the certs from the database
     */
    private void loadTrustedCerts() {

        java.util.List certList = null;
        try {
            certList = getTrustedCertAdmin().findAllCerts();

            Vector certs = new Vector();
            for (int i = 0; i < certList.size(); i++) {
                Object o = (Object) certList.get(i);
                certs.add(o);
            }

            trustedCertTable.getTableSorter().setData(certs);
            trustedCertTable.getTableSorter().getRealModel().setRowCount(certs.size());
            trustedCertTable.getTableSorter().fireTableDataChanged();


        } catch (RemoteException re) {
            JOptionPane.showMessageDialog(instance, resources.getString("cert.remote.exception"),
                                        resources.getString("load.error.title"),
                                        JOptionPane.ERROR_MESSAGE);
        } catch (FindException e) {
            JOptionPane.showMessageDialog(instance, resources.getString("cert.find.error"),
                                        resources.getString("load.error.title"),
                                        JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Enable or disable the fields based on the current selections.
     */
    private void enableOrDisableButtons() {
        boolean propsEnabled = false;
        boolean removeEnabled = false;
        int row = trustedCertTable.getSelectedRow();
        if (row >= 0) {
            removeEnabled = true;
            propsEnabled = true;
        }
        removeButton.setEnabled(removeEnabled);
        propertiesButton.setEnabled(propsEnabled);
    }

    /**
     *  The callback for saving the new cert to the database
     */
    private WizardListener wizardListener = new WizardAdapter() {
        /**
         * Invoked when the wizard has finished.
         *
         * @param we the event describing the wizard finish
         */
        public void wizardFinished(WizardEvent we) {

            // update the provider
            Wizard w = (Wizard) we.getSource();

            Object o = w.getCollectedInformation();

            if (o instanceof TrustedCert) {

                final TrustedCert tc = (TrustedCert) o;

                if (tc != null) {

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {

                            try {
                                getTrustedCertAdmin().saveCert(tc);

                                // reload all certs from server
                                loadTrustedCerts();

                            } catch (SaveException e) {
                                JOptionPane.showMessageDialog(instance, resources.getString("cert.save.error"),
                                        resources.getString("save.error.title"),
                                        JOptionPane.ERROR_MESSAGE);
                            } catch (RemoteException e) {
                                JOptionPane.showMessageDialog(instance, resources.getString("cert.remote.exception"),
                                        resources.getString("save.error.title"),
                                        JOptionPane.ERROR_MESSAGE);
                            } catch (VersionException e) {
                                JOptionPane.showMessageDialog(instance, resources.getString("cert.version.error"),
                                        resources.getString("save.error.title"),
                                        JOptionPane.ERROR_MESSAGE);
                            } catch (UpdateException e) {
                                 JOptionPane.showMessageDialog(instance, resources.getString("cert.update.error"),
                                        resources.getString("save.error.title"),
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    });
                }
            }
        }

    };

    /**
     * Retrieve the object reference of the Trusted Cert Admin service
     *
     * @return TrustedCertAdmin  - The object reference.
     * @throws RuntimeException  if the object reference of the Trusted Cert Admin service is not found.
     */
    private TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        TrustedCertAdmin tca =
                (TrustedCertAdmin) Locator.
                getDefault().lookup(TrustedCertAdmin.class);
        if (tca == null) {
            throw new RuntimeException("Could not find registered " + TrustedCertAdmin.class);
        }

        return tca;
    }

    /**
     * clean up when the window is closed.
     */
    public void dispose() {
        instance = null;
        super.dispose();
    }


    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// !!! IMPORTANT !!!
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * !!! IMPORTANT !!!
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(4, 4, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(7, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        addButton = new JButton();
        addButton.setText("Add");
        panel1.add(addButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        removeButton = new JButton();
        removeButton.setText("Remove");
        panel1.add(removeButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        propertiesButton = new JButton();
        propertiesButton.setText("Properties");
        panel1.add(propertiesButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        closeButton = new JButton();
        closeButton.setText("Close");
        panel1.add(closeButton, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel2, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JLabel label1 = new JLabel();
        label1.setText("Trusted Certificates");
        panel2.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        certTableScrollPane = new JScrollPane();
        mainPanel.add(certTableScrollPane, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(450, 400), null));
        final Spacer spacer2 = new Spacer();
        mainPanel.add(spacer2, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, new Dimension(10, -1), null, null));
        final Spacer spacer3 = new Spacer();
        mainPanel.add(spacer3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, new Dimension(10, -1), null, null));
        final Spacer spacer4 = new Spacer();
        mainPanel.add(spacer4, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 10), new Dimension(-1, 10), null));
        final Spacer spacer5 = new Spacer();
        mainPanel.add(spacer5, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 10), new Dimension(-1, 10), null));
    }
}
