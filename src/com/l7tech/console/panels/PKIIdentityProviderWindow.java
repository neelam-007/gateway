package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.Locator;
import com.l7tech.console.table.TrustedCertsTable;
import com.l7tech.console.table.TrustedCertTableSorter;
import com.l7tech.console.event.*;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.logging.Logger;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class PKIIdentityProviderWindow extends JDialog {

    private JPanel mainPanel;
    private JScrollPane certScrollPane;
    private JButton addButton;
    private JButton removeButton;
    private JButton propertiesButton;
    private JButton saveButton;
    private JButton cancelButton;
    private TrustedCertsTable trustedCertTable = null;

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.PKIIdentityProviderDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(PKIIdentityProviderWindow.class.getName());
    private Object providerConfig;


    /**
     * Constructor
     *
     * @param owner The parent component.
     */
    public PKIIdentityProviderWindow(JFrame owner) {
        super(owner, resources.getString("new.provider.dialog.title"), true);
        initialize();
        pack();
        Utilities.centerOnScreen(this);
    }

    /**
     * Constructor
     *
     * @param owner The parent component.
     * @param providerConfig   The given identity config object.
     */
    public PKIIdentityProviderWindow(Dialog owner, Object providerConfig) {
        super(owner, resources.getString("edit.provider.dialog.title"), true);
        this.providerConfig = providerConfig;
        initialize();
        pack();
        Utilities.centerOnScreen(this);
    }

    private void initialize() {

        final JDialog thisDialog = this;
        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);

        if(trustedCertTable == null) {
            trustedCertTable = new TrustedCertsTable();
        }
        certScrollPane.setViewportView(trustedCertTable);
        certScrollPane.getViewport().setBackground(Color.white);

        // Hide the cert usage data column
        trustedCertTable.hideColumn(TrustedCertTableSorter.CERT_TABLE_CERT_USAGE_COLUMN_INDEX);

        // initialize the button states
        enableOrDisableButtons();

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


        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                CertSearchPanel sp = new CertSearchPanel(thisDialog);
                sp.addCertListener(certListener);
                sp.show();
                sp.setSize(400, 600);

            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int row = trustedCertTable.getSelectedRow();
                if (row >= 0) {
                     trustedCertTable.getTableSorter().deleteRow(row);
                }
            }
        });

        propertiesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {

                int row = trustedCertTable.getSelectedRow();
                if (row >= 0) {
                    CertPropertiesWindow cpw = new CertPropertiesWindow(thisDialog, (TrustedCert) trustedCertTable.getTableSorter().getData(row), false);
                    cpw.show();
                }
            }
        });

         saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {

                dispose();
            }
        });

    }

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

    private final CertListener certListener = new CertListenerAdapter() {
        public void certSelected(CertEvent e) {
             trustedCertTable.getTableSorter().addRow(e.getCert());
        }

    };

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
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(12, 0, 15, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JLabel _3;
        _3 = new JLabel();
        _3.setText("PKI Provider Name:");
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JTextField _4;
        _4 = new JTextField();
        _2.add(_4, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 8, 1, 6, 0, null, null, new Dimension(250, -1)));
        final JPanel _5;
        _5 = new JPanel();
        _5.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_5, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JPanel _6;
        _6 = new JPanel();
        _6.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        _5.add(_6, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JButton _7;
        _7 = new JButton();
        cancelButton = _7;
        _7.setText("Cancel");
        _6.add(_7, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _8;
        _8 = new JButton();
        saveButton = _8;
        _8.setText("Save");
        _6.add(_8, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _9;
        _9 = new com.intellij.uiDesigner.core.Spacer();
        _6.add(_9, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 1, 6, 1, null, null, null));
        final JPanel _10;
        _10 = new JPanel();
        _10.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(5, 0, 0, 0), -1, -1));
        _5.add(_10, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JPanel _11;
        _11 = new JPanel();
        _11.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        _10.add(_11, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 3, 3, 3, null, null, null));
        final JButton _12;
        _12 = new JButton();
        addButton = _12;
        _12.setText("Add");
        _11.add(_12, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _13;
        _13 = new JButton();
        removeButton = _13;
        _13.setText("Remove");
        _11.add(_13, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _14;
        _14 = new JButton();
        propertiesButton = _14;
        _14.setText("Properties");
        _11.add(_14, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _15;
        _15 = new com.intellij.uiDesigner.core.Spacer();
        _11.add(_15, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, 0, 2, 1, 6, null, null, null));
        final JScrollPane _16;
        _16 = new JScrollPane();
        certScrollPane = _16;
        _10.add(_16, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 7, 7, null, new Dimension(450, 300), null));
        final JLabel _17;
        _17 = new JLabel();
        _17.setText("Trusted Certificates");
        _1.add(_17, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 0, 0, 0, null, null, null));
    }


}
