package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.console.action.NewLdapProviderAction;
import com.l7tech.console.action.NewFederatedIdentityProviderAction;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.exporter.IdProviderReference;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.io.ByteArrayInputStream;

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
        return "Unresolved provider " + unresolvedRef.getProviderName();
    }

    public boolean canFinish() {
        if (hasNextPanel()) return false;
        return true;
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

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
        // show the details of the foreign provider
        foreignProviderName.setText(unresolvedRef.getProviderName());
        foreignProviderType.setText(IdentityProviderType.fromVal(unresolvedRef.getIdProviderTypeVal()).description());

        // make radio buttons sane
        actionRadios = new ButtonGroup();
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

        populateIdProviders();
        populatePropsTable();
    }

    /**
     * invoked when the user clicks the create id provider button
     */
    private void onCreateIdProvider() {
        // update the list once the provide is created
        EntityListener updateProviderListCallback = new EntityListener() {
            public void entityAdded(EntityEvent ev) {
                populateIdProviders();
            }
            public void entityUpdated(EntityEvent ev) {}
            public void entityRemoved(EntityEvent ev) {}
        };
        // start the process to create the type of id provider based on the saved info
        if (IdentityProviderType.LDAP.toVal() == unresolvedRef.getIdProviderTypeVal()) {
            NewLdapProviderAction action = new NewLdapProviderAction(null);
            action.addEntityListener(updateProviderListCallback);
            action.invoke();
        } else if (IdentityProviderType.FEDERATED.toVal() == unresolvedRef.getIdProviderTypeVal()) {
            NewFederatedIdentityProviderAction action = new NewFederatedIdentityProviderAction(null);
            action.addEntityListener(updateProviderListCallback);
            action.invoke();
        }

    }

    private void populatePropsTable() {
        DefaultTableModel model = (DefaultTableModel)providerPropsTable.getModel();
        model.addColumn("Name");
        model.addColumn("Value");
        String szedProps = unresolvedRef.getIdProviderConfProps();
        if (szedProps != null) {
            ByteArrayInputStream in = new ByteArrayInputStream(szedProps.getBytes());
            java.beans.XMLDecoder decoder = new java.beans.XMLDecoder(in);
            Map props = (Map)decoder.readObject();
            Set keys = props.keySet();
            for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
                Object o = (Object) iterator.next();
                if (o instanceof String) {
                    Object val = props.get(o);
                    if (val instanceof String) {
                        model.addRow(new Object[] {o, val});
                    }
                }
            }
        }
    }

    private void populateIdProviders() {
        // populate provider selector
        IdentityAdmin admin = null;
        admin = Registry.getDefault().getIdentityAdmin();
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
        for ( int i = 0; i < providerHeaders.length; i++ ) {
            EntityHeader entityHeader = providerHeaders[i];
            idprovidermodel.addElement(entityHeader.getName());
        }
        providerSelector.setModel(idprovidermodel);
    }

    private Long getProviderIdFromName(String name) {
        for ( int i = 0; i < providerHeaders.length; i++ ) {
            EntityHeader entityHeader = providerHeaders[i];
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
    private ButtonGroup actionRadios;

    private IdProviderReference unresolvedRef;
    private EntityHeader[] providerHeaders;
    private final Logger logger = Logger.getLogger(ResolveForeignIdentityProviderPanel.class.getName());

}
