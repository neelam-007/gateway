package com.l7tech.console.panels;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.l7tech.console.action.Actions;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.event.*;
import com.l7tech.console.table.TrustedCertTableSorter;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.*;


import javax.swing.table.DefaultTableModel;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.Vector;
import java.rmi.RemoteException;


/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p/>
 * $Id$
 */
public class CertManagerWindow extends JDialog {

    public static final int CERT_TABLE_CERT_NAME_COLUMN_INDEX = 0;
    public static final int CERT_TABLE_CERT_SUBJECT_COLUMN_INDEX = 1;
    public static final int CERT_TABLE_CERT_USAGE_COLUMN_INDEX = 2;
    private JPanel mainPanel;
    private JButton addButton;
    private JButton removeButton;
    private JButton propertiesButton;
    private JButton closeButton;
    private static CertManagerWindow instance = null;
    private JTable trustedCertTable = null;
    private JScrollPane certTableScrollPane;
    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());
    private TrustedCertTableSorter trustedCertTableSorter;

    private CertManagerWindow(Frame owner) {
        super(owner, resources.getString("dialog.title"), true);
    }

    private CertManagerWindow(Dialog owner) {
        super(owner, resources.getString("dialog.title"), true);
    }

    public static CertManagerWindow getInstance(Window owner) {

        if (instance == null) {
            if (owner instanceof Dialog)
                instance = new CertManagerWindow((Dialog) owner);
            else if (owner instanceof Frame)
                instance = new CertManagerWindow((Frame) owner);
            else
                throw new IllegalArgumentException("Owner must be derived from either Frame or Window");

            instance.initialize();
            instance.loadTrustedCerts();
        }

        return instance;
    }

    private void initialize() {

        Container p = instance.getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);
        certTableScrollPane.setViewportView(getTrustedCertTable());


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
                //todo:
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                //todo:
            }
        });

        instance.pack();
        enableOrDisableButtons();
        Actions.setEscKeyStrokeDisposes(instance);

    }

    private void loadTrustedCerts() {

        java.util.List certList = null;
        try {
            certList = getTrustedCertAdmin().findAllCerts();

            Vector certs = new Vector();
            for (int i = 0; i < certList.size(); i++) {
                Object o = (Object) certList.get(i);
                certs.add(o);
            }
            getTrustedCertTableModel().setData(certs);
            getTrustedCertTableModel().getRealModel().setRowCount(certs.size());
            getTrustedCertTableModel().fireTableDataChanged();

        } catch (RemoteException re) {

        } catch (FindException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void enableOrDisableButtons() {
        boolean propsEnabled = false;
        boolean removeEnabled = false;
/*        int row = getJmsQueueTable().getSelectedRow();
        if (row >= 0) {
            JmsAdmin.JmsTuple i = (JmsAdmin.JmsTuple)getJmsQueueTableModel().getJmsQueues().get(row);
            if (i != null) {
                removeEnabled = true;
                propsEnabled = true;
            }
        }*/
        removeButton.setEnabled(removeEnabled);
        propertiesButton.setEnabled(propsEnabled);
    }

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

            if (o instanceof CertInfo) {

                final CertInfo ci = (CertInfo) o;

                if (ci != null && ci.getTrustedCert() != null) {

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {

                            try {
                                getTrustedCertAdmin().saveCert(ci.getTrustedCert());
                            } catch (SaveException e) {
                                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            } catch (RemoteException e) {
                                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            } catch (VersionException e) {
                                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            } catch (UpdateException e) {
                                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            }
                        }
                    });
                }
            }
        }

    };

    /**
     * Return trustedCertTable property value
     *
     * @return  JTable
     */
    private JTable getTrustedCertTable() {

        if (trustedCertTable != null) return trustedCertTable;

        trustedCertTable = new javax.swing.JTable();
        trustedCertTable.setModel(getTrustedCertTableModel());

        return trustedCertTable;
    }

    /**
     * Return TrustedCertTableSorter property value
     *
     * @return  TrustedCertTableSorter
     */
    private TrustedCertTableSorter getTrustedCertTableModel() {

        if (trustedCertTableSorter != null) {
            return trustedCertTableSorter;
        }

        Object[][] rows = new Object[][]{};

        String[] cols = new String[]{
            "Name", "Subject", "Usage"
        };

        trustedCertTableSorter = new TrustedCertTableSorter(new DefaultTableModel(rows, cols) {
            public boolean isCellEditable(int row, int col) {
                // the table cells are not editable
                return false;
            }
        });

        return trustedCertTableSorter;

    }

    private TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        TrustedCertAdmin tca =
                (TrustedCertAdmin) Locator.
                getDefault().lookup(TrustedCertAdmin.class);
        if (tca == null) {
            throw new RuntimeException("Could not find registered " + TrustedCertAdmin.class);
        }

        return tca;
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
