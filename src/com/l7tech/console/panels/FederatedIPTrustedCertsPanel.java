package com.l7tech.console.panels;

import com.l7tech.console.table.TrustedCertsTable;
import com.l7tech.console.table.TrustedCertTableSorter;
import com.l7tech.console.event.CertListener;
import com.l7tech.console.event.CertListenerAdapter;
import com.l7tech.console.event.CertEvent;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.logging.Logger;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class FederatedIPTrustedCertsPanel extends IdentityProviderStepPanel{
    private JPanel mainPanel;
    private JScrollPane certScrollPane;
    private JButton addButton;
    private JButton removeButton;
    private JButton propertiesButton;

    private TrustedCertsTable trustedCertTable = null;
    private IdentityProviderConfig providerConfig;
    private EventListenerList listenerList = new EventListenerList();

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.FederatedIdentityProviderDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(FederatedIPTrustedCertsPanel.class.getName());

    public FederatedIPTrustedCertsPanel(WizardStepPanel next) {
        super(next);
        initComponents();
    }

    public String getDescription() {
        return  "Select the certificates that will be trusted by this identity provider from the SecureSpan Gateway's trusted certificate store.";
    }

    /** @return the wizard step label    */
    public String getStepLabel() {
        return "Select the Trusted Certificates";
    }

    /**
     * Populate the configuration data from the wizard input object to the visual components of the panel.
     *
     * @param settings The current value of configuration items in the wizard input object.
     * @throws IllegalArgumentException if the data provided by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        //todo:
    }


    /**
     * Store the values of all fields on the panel to the wizard object which is a used for
     * keeping all the modified values. The wizard object will be used for providing the
     * updated values when updating the server.
     *
     * @param settings the object representing wizard panel state
     */
    public void storeSettings(Object settings) {
        //todo:
    }
    
    private void initComponents() {

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

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

                Wizard w = (Wizard) TopComponents.getInstance().getComponent(CreateFederatedIPWizard.NAME);
                CertSearchPanel sp = new CertSearchPanel(w);
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
                     Wizard w = (Wizard) TopComponents.getInstance().getComponent(CreateFederatedIPWizard.NAME);

                    CertPropertiesWindow cpw = new CertPropertiesWindow(w, (TrustedCert) trustedCertTable.getTableSorter().getData(row), false);
                    cpw.show();
                }
            }
        });



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
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JPanel _3;
        _3 = new JPanel();
        _3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(5, 0, 0, 0), -1, -1));
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JPanel _4;
        _4 = new JPanel();
        _4.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        _3.add(_4, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 3, 3, 3, null, null, null));
        final JButton _5;
        _5 = new JButton();
        addButton = _5;
        _5.setText("Add");
        _4.add(_5, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _6;
        _6 = new JButton();
        removeButton = _6;
        _6.setText("Remove");
        _4.add(_6, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _7;
        _7 = new JButton();
        propertiesButton = _7;
        _7.setText("Properties");
        _4.add(_7, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _8;
        _8 = new com.intellij.uiDesigner.core.Spacer();
        _4.add(_8, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, 0, 2, 1, 6, null, null, null));
        final JScrollPane _9;
        _9 = new JScrollPane();
        certScrollPane = _9;
        _3.add(_9, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 7, 7, null, new Dimension(450, 300), null));
        final JLabel _10;
        _10 = new JLabel();
        _10.setText("Trusted Certificates:");
        _1.add(_10, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
    }


}
