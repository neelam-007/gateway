package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.console.event.*;
import com.l7tech.console.table.TrustedCertTableSorter;
import com.l7tech.console.table.TrustedCertsTable;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.EventListener;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class FederatedIdentityProviderWindow extends JDialog {

    private JPanel mainPanel;
    private JScrollPane certScrollPane;
    private JButton addButton;
    private JButton removeButton;
    private JButton propertiesButton;
    private JButton saveButton;
    private JButton cancelButton;
    private JTextField providerNameTextField;
    private TrustedCertsTable trustedCertTable = null;
    private IdentityProviderConfig providerConfig;
    private EventListenerList listenerList = new EventListenerList();

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.FederatedIdentityProviderDialog", Locale.getDefault());


    /**
     * Constructor
     *
     * @param owner The parent component.
     */
    public FederatedIdentityProviderWindow(JFrame owner) {
        super(owner, resources.getString("new.provider.dialog.title"), true);
        this.providerConfig = new IdentityProviderConfig();
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
    public FederatedIdentityProviderWindow(JFrame owner, IdentityProviderConfig providerConfig) {
        super(owner, resources.getString("edit.provider.dialog.title"), true);
        this.providerConfig = providerConfig;
        initialize();
        pack();
        Utilities.centerOnScreen(this);
    }

    private void initialize() {

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
        initializeSaveButtonState();

        providerNameTextField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                initializeSaveButtonState();
            }
        });

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
                CertSearchPanel sp = new CertSearchPanel(FederatedIdentityProviderWindow.this);
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
                    CertPropertiesWindow cpw = new CertPropertiesWindow(FederatedIdentityProviderWindow.this, (TrustedCert) trustedCertTable.getTableSorter().getData(row), false);
                    cpw.show();
                }
            }
        });

         saveButton.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent event) {

                 if (providerConfig != null) {
                     saveSettings();
                     EntityHeader header = new EntityHeader();
                     header.setName(providerConfig.getName());
                     header.setType(EntityType.ID_PROVIDER_CONFIG);

                     //todo: comment out this until the server side is ready
/*                     try {
                         header.setOid(getProviderConfigManager().save(providerConfig));
                     } catch (SaveException e) {
                         ErrorManager.getDefault().notify(Level.WARNING, e, "Error saving the new identity provider: " + header.getName());
                         header = null;
                     }*/
                     fireEventEntityAdded(header);

                 }

                 dispose();
             }
         });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {

                dispose();
            }
        });

    }

    //todo: temp
    private void saveSettings() {
        if(providerConfig != null) {
            providerConfig.setName(providerNameTextField.getText().trim());
        }
    }

    /**
     * Initialize the saveButton state based on its length
     */
    private void initializeSaveButtonState() {
        if (providerNameTextField.getText().length() > 0) {
            saveButton.setEnabled(true);
        } else {
            saveButton.setEnabled(false);
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

    private final CertListener certListener = new CertListenerAdapter() {
        public void certSelected(CertEvent e) {
             trustedCertTable.getTableSorter().addRow(e.getCert());
        }

    };

    public Object getCollectedInformation() {
        return providerConfig;
    }

    /**
     * add the EntityListener
     *
     * @param listener the EntityListener
     */
    public void addEntityListener(EntityListener listener) {
        listenerList.add(EntityListener.class, listener);
    }

    /**
     * remove the the EntityListener
     *
     * @param listener the EntityListener
     */
    public void removeEntityListener(EntityListener listener) {
        listenerList.remove(EntityListener.class, listener);
    }

    /**
     * notfy the listeners
     *
     * @param eh the entity header
     */
    private void fireEventEntityAdded(final EntityHeader eh) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                EntityEvent event = new EntityEvent(this, eh);
                EventListener[] listeners = listenerList.getListeners(EntityListener.class);
                for (int i = 0; i < listeners.length; i++) {
                    ((EntityListener) listeners[i]).entityAdded(event);
                }
            }

        });
    }


}
