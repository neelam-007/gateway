package com.l7tech.console.panels;

import com.l7tech.console.action.NewFederatedIdentityProviderAction;
import com.l7tech.console.action.NewLdapProviderAction;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.policy.exporter.IdProviderReference;
import com.l7tech.console.policy.exporter.FederatedIdProviderReference;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.util.HexUtils;
import com.l7tech.gateway.common.admin.IdentityAdmin;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This wizard panel allows an administrator to resolve a missing identity provider
 * refered to in an exported policy during import. This is only invoked when the
 * missing identity provider cannot be resolved automatically based on the exported
 * properties.
 * 
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 23, 2004<br/>
 * $Id$
 */
public class ResolveForeignIdentityProviderPanel extends WizardStepPanel {
    public ResolveForeignIdentityProviderPanel(WizardStepPanel next, IdProviderReference unresolvedRef) {
        super(next);
        this.unresolvedRef = unresolvedRef;
        initialize();
    }

    public String getDescription() {
        return getStepLabel();
    }

    public String getStepLabel() {
        return "Unresolved provider " + getProviderNameForDisplay();
    }

    public boolean canFinish() {
        return !hasNextPanel();
    }

    public boolean onNextButton() {
        // collect actions details and store in the reference for resolution
        if (manualResolvRadio.isSelected()) {
            Long newProviderId = getProviderIdFromName(providerSelector.getSelectedItem().toString());
            if (newProviderId == null) {
                // this cannot happen
                logger.severe("could not get provider from name");
                return false;
            }
            unresolvedRef.setLocalizeReplace(newProviderId.longValue());
        } else if (removeRadio.isSelected()) {
            unresolvedRef.setLocalizeDelete();
        } else if (ignoreRadio.isSelected()) {
            unresolvedRef.setLocalizeIgnore();
        }
        return true;
    }

    private String getProviderNameForDisplay() {
        String name = unresolvedRef.getProviderName();

        if ( name == null ) {
            name = "Unknown";            
        }

        return name;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
        // show the details of the foreign provider
        foreignProviderName.setText(getProviderNameForDisplay());
        try {
            foreignProviderType.setText(IdentityProviderType.fromVal(unresolvedRef.getIdProviderTypeVal()).description());
        } catch (IllegalArgumentException iae) {
            foreignProviderType.setText("Unknown");
        }

        // make radio buttons sane
        ButtonGroup actionRadios = new ButtonGroup();
        actionRadios.add(manualResolvRadio);
        actionRadios.add(removeRadio);
        actionRadios.add(ignoreRadio);
        // default action will be to remove
        removeRadio.setSelected(true);
        providerSelector.setEnabled(false);

        // enable/disable provider selector as per action type selected
        manualResolvRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                providerSelector.setEnabled(true);
            }
        });
        removeRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                providerSelector.setEnabled(false);
            }
        });
        ignoreRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                providerSelector.setEnabled(false);
            }
        });

        createProviderButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCreateIdProvider();
            }

        });

        populateIdProviders(unresolvedRef.getIdProviderTypeVal());
        if (providerSelector.getModel().getSize() == 0)
            manualResolvRadio.setEnabled(false);
        populatePropsTable();
    }

    private void resetProvList() {
        if (providerSelector.getModel().getSize() == 0) {
            manualResolvRadio.setEnabled(false);
        } else {
            manualResolvRadio.setEnabled(true);
        }
    }

    /**
     * invoked when the user clicks the create id provider button
     */
    private void onCreateIdProvider() {
        // update the list once the provide is created
        final EntityListener updateProviderListCallback = new EntityListener() {
            public void entityAdded(EntityEvent ev) {
                populateIdProviders(unresolvedRef.getIdProviderTypeVal());
                resetProvList();
                if (providerSelector.getModel().getSize() > 0) {
                    manualResolvRadio.setSelected(true);
                    providerSelector.setEnabled(true);
                    providerSelector.setSelectedItem(((EntityHeader)ev.getEntity()).getName());
                }
            }
            public void entityUpdated(EntityEvent ev) {}
            public void entityRemoved(EntityEvent ev) {}
        };
        // start the process to create the type of id provider based on the saved info
        if (IdentityProviderType.LDAP.toVal() == unresolvedRef.getIdProviderTypeVal()) {
            LdapIdentityProviderConfig ldapConfig = new LdapIdentityProviderConfig();
            ldapConfig.setName(unresolvedRef.getProviderName());
            ldapConfig.setSerializedProps(unresolvedRef.getIdProviderConfProps());
            NewLdapProviderAction action = new NewLdapProviderAction(ldapConfig);
            action.addEntityListener(updateProviderListCallback);
            action.invoke();
        } else if (IdentityProviderType.FEDERATED.toVal() == unresolvedRef.getIdProviderTypeVal()) {
            //pre-populate the federated identity provider information
            FederatedIdentityProviderConfig fipConfig = new FederatedIdentityProviderConfig();
            fipConfig.setName(unresolvedRef.getProviderName());
            fipConfig.setSerializedProps(unresolvedRef.getIdProviderConfProps());
            HashMap<String, String> userUpdateMap = null;
            HashMap<String, String> groupUpdateMap = null;
            if(unresolvedRef instanceof FederatedIdProviderReference) {
                FederatedIdProviderReference fedIdRef = (FederatedIdProviderReference)unresolvedRef;
                fipConfig.setImportedGroups(fedIdRef.getImportedGroups());
                fipConfig.setImportedUsers(fedIdRef.getImportedUsers());
                fipConfig.setImportedGroupMembership(fedIdRef.getImportedGroupMembership());
                userUpdateMap = fedIdRef.getUserUpdateMap();
                groupUpdateMap = fedIdRef.getGroupUpdateMap();
            }
            NewFederatedIdentityProviderAction action = new NewFederatedIdentityProviderAction(null, fipConfig, userUpdateMap, groupUpdateMap);
            action.addEntityListener(updateProviderListCallback);
            action.invoke();
        } else {
            DialogDisplayer.showInputDialog(this,
                    "Select type:",
                    "Select Identity Provider Type",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[] { IdentityProviderType.FEDERATED.description(), IdentityProviderType.LDAP.description() },
                    "",
                    new DialogDisplayer.InputListener(){
                        public void reportResult(Object option) {
                            if ( option != null ) {
                                if ( IdentityProviderType.FEDERATED.description().equals(option) ) {
                                    NewFederatedIdentityProviderAction action = new NewFederatedIdentityProviderAction(null);
                                    action.addEntityListener(updateProviderListCallback);
                                    action.invoke();
                                } else {
                                    NewLdapProviderAction action = new NewLdapProviderAction();
                                    action.addEntityListener(updateProviderListCallback);
                                    action.invoke();
                                }
                            }
                        }
                    });
        }

    }

    private void populatePropsTable() {
        DefaultTableModel model = (DefaultTableModel)providerPropsTable.getModel();
        model.addColumn("Name");
        model.addColumn("Value");
        String szedProps = unresolvedRef.getIdProviderConfProps();
        if (szedProps != null) {
            ByteArrayInputStream in = new ByteArrayInputStream(HexUtils.encodeUtf8(szedProps));
            java.beans.XMLDecoder decoder = new java.beans.XMLDecoder(in);
            Map props = (Map)decoder.readObject();
            Set keys = props.keySet();
            for (Object key : keys) {
                if (key instanceof String) {
                    Object val = props.get(key);
                    if (val instanceof String) {
                        model.addRow(new Object[]{key, val});
                    }
                }
            }
        }
    }

    private void populateIdProviders(int requiredProviderType) {
        // populate provider selector
        IdentityAdmin admin = Registry.getDefault().getIdentityAdmin();
        if (admin == null) {
            logger.severe("Cannot get the IdentityAdmin");
            return;
        }
        providerHeaders = null;
        try {
            providerHeaders = admin.findAllIdentityProviderConfig();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot get the id provider headers.", e);
            return;
        }
        if (providerHeaders == null) {
            // this can't happen under normal circumpstences
            throw new RuntimeException("No providers at all?");
        }
        DefaultComboBoxModel idprovidermodel = new DefaultComboBoxModel();
        for (EntityHeader entityHeader : providerHeaders) {
            try {
                IdentityProviderConfig ipc = admin.findIdentityProviderConfigByID(entityHeader.getOid());
                if (ipc != null && (requiredProviderType == 0 || ipc.getTypeVal() == requiredProviderType) ) {
                    idprovidermodel.addElement(entityHeader.getName());
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Cannot get id provider config.", e);
                return;
            }
        }
        providerSelector.setModel(idprovidermodel);
    }

    private Long getProviderIdFromName(String name) {
        for (EntityHeader entityHeader : providerHeaders) {
            if (entityHeader.getName().equals(name)) {
                return new Long(entityHeader.getOid());
            }
        }
        return null;
    }

    private JPanel mainPanel;
    private JTextField foreignProviderName;
    private JTextField foreignProviderType;
    private JRadioButton manualResolvRadio;
    private JRadioButton removeRadio;
    private JRadioButton ignoreRadio;
    private JComboBox providerSelector;
    private JTable providerPropsTable;
    private JButton createProviderButton;

    private IdProviderReference unresolvedRef;
    private EntityHeader[] providerHeaders;
    private final Logger logger = Logger.getLogger(ResolveForeignIdentityProviderPanel.class.getName());

}
