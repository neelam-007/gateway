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
        //manager = (IdentityProviderConfigManager)Locator.getDefault().lookup(IdentityProviderConfigManager.class);
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
        mainPanel.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 2, new Insets(5, 5, 5, 5), -1, -1));
        mainPanel.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Missing Identity Provider Details"));
        final JLabel label1 = new JLabel();
        label1.setText("Name");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label2 = new JLabel();
        label2.setText("Type");
        panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        foreignProviderName = new JTextField();
        foreignProviderName.setEditable(false);
        foreignProviderName.setText("blah name");
        panel1.add(foreignProviderName, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        foreignProviderType = new JTextField();
        foreignProviderType.setEditable(false);
        foreignProviderType.setText("blah provider type");
        panel1.add(foreignProviderType, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null));
        providerPropsTable = new JTable();
        providerPropsTable.setAutoResizeMode(4);
        providerPropsTable.setPreferredScrollableViewportSize(new Dimension(450, 150));
        scrollPane1.setViewportView(providerPropsTable);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(4, 2, new Insets(5, 5, 5, 5), -1, -1));
        mainPanel.add(panel2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Action"));
        manualResolvRadio = new JRadioButton();
        manualResolvRadio.setText("Change assertions to use this identity provider:");
        panel2.add(manualResolvRadio, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        removeRadio = new JRadioButton();
        removeRadio.setLabel("Remove assertions that refer to the missing identity provider");
        removeRadio.setText("Remove assertions that refer to the missing identity provider");
        panel2.add(removeRadio, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        ignoreRadio = new JRadioButton();
        ignoreRadio.setFocusPainted(false);
        ignoreRadio.setLabel("Import erroneous assertions as-is");
        ignoreRadio.setRequestFocusEnabled(false);
        ignoreRadio.setText("Import erroneous assertions as-is");
        panel2.add(ignoreRadio, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        providerSelector = new JComboBox();
        providerSelector.setToolTipText("Existing local identity provider");
        panel2.add(providerSelector, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        createProviderButton = new JButton();
        createProviderButton.setText("Create new Identity Provider");
        createProviderButton.setToolTipText("Create a new identity provider so you can then associate those assertions with");
        panel2.add(createProviderButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label3 = new JLabel();
        label3.setText("Policy contains assertions that refer to an unknown identity provider.");
        mainPanel.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null));
    }
}
