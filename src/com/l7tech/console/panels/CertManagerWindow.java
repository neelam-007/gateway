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

                        CertImportMethodsPanel sp = new CertImportMethodsPanel(new CertDetailsPanel(new CertUsagePanel(null)));


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
                CertPropertiesWindow cpw = new CertPropertiesWindow(instance, updatedTrustedCert);

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
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        final JPanel _1;
        _1 = new JPanel();
        mainPanel = _1;
        _1.setLayout(new GridLayoutManager(4, 4, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new GridLayoutManager(7, 1, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_2, new GridConstraints(2, 2, 1, 1, 0, 3, 3, 3, null, null, null));
        final JButton _3;
        _3 = new JButton();
        addButton = _3;
        _3.setText("Add");
        _2.add(_3, new GridConstraints(1, 0, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _4;
        _4 = new JButton();
        removeButton = _4;
        _4.setText("Remove");
        _2.add(_4, new GridConstraints(2, 0, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _5;
        _5 = new JButton();
        propertiesButton = _5;
        _5.setText("Properties");
        _2.add(_5, new GridConstraints(3, 0, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _6;
        _6 = new JButton();
        closeButton = _6;
        _6.setText("Close");
        _2.add(_6, new GridConstraints(5, 0, 1, 1, 0, 1, 3, 0, null, null, null));
        final Spacer _7;
        _7 = new Spacer();
        _2.add(_7, new GridConstraints(4, 0, 1, 1, 0, 2, 1, 6, null, null, null));
        final JPanel _8;
        _8 = new JPanel();
        _8.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_8, new GridConstraints(1, 1, 1, 1, 0, 3, 3, 3, null, null, null));
        final JLabel _9;
        _9 = new JLabel();
        _9.setText("Trusted Certificates");
        _8.add(_9, new GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JScrollPane _10;
        _10 = new JScrollPane();
        certTableScrollPane = _10;
        _1.add(_10, new GridConstraints(2, 1, 1, 1, 0, 3, 7, 7, null, new Dimension(450, 400), null));
        final Spacer _11;
        _11 = new Spacer();
        _1.add(_11, new GridConstraints(2, 3, 1, 1, 0, 1, 6, 1, new Dimension(10, -1), null, null));
        final Spacer _12;
        _12 = new Spacer();
        _1.add(_12, new GridConstraints(2, 0, 1, 1, 0, 1, 6, 1, new Dimension(10, -1), null, null));
        final Spacer _13;
        _13 = new Spacer();
        _1.add(_13, new GridConstraints(3, 1, 1, 1, 0, 2, 1, 6, new Dimension(-1, 10), new Dimension(-1, 10), null));
        final Spacer _14;
        _14 = new Spacer();
        _1.add(_14, new GridConstraints(0, 1, 1, 1, 0, 2, 1, 6, new Dimension(-1, 10), new Dimension(-1, 10), null));
    }


}
