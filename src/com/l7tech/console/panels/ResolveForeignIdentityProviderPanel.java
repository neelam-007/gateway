package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.exporter.IdProviderReference;

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

        populateIdProviders();
        populatePropsTable();
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
    private ButtonGroup actionRadios;

    private IdProviderReference unresolvedRef;
    private EntityHeader[] providerHeaders;
    private final Logger logger = Logger.getLogger(ResolveForeignIdentityProviderPanel.class.getName());

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
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 2, new Insets(5, 5, 5, 5), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        _2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Missing Identity Provider Details"));
        final JLabel _3;
        _3 = new JLabel();
        _3.setText("Name");
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _4;
        _4 = new JLabel();
        _4.setText("Type");
        _2.add(_4, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JTextField _5;
        _5 = new JTextField();
        foreignProviderName = _5;
        _5.setEditable(false);
        _5.setText("blah name");
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JTextField _6;
        _6 = new JTextField();
        foreignProviderType = _6;
        _6.setEditable(false);
        _6.setText("blah provider type");
        _2.add(_6, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JScrollPane _7;
        _7 = new JScrollPane();
        _2.add(_7, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 2, 0, 3, 7, 7, null, null, null));
        final JTable _8;
        _8 = new JTable();
        providerPropsTable = _8;
        _8.setPreferredScrollableViewportSize(new Dimension(450, 150));
        _8.setAutoResizeMode(4);
        _7.setViewportView(_8);
        final JPanel _9;
        _9 = new JPanel();
        _9.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 2, new Insets(5, 5, 5, 5), -1, -1));
        _1.add(_9, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        _9.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Action"));
        final JRadioButton _10;
        _10 = new JRadioButton();
        manualResolvRadio = _10;
        _10.setText("Change assertions to use this identity provider:");
        _9.add(_10, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JRadioButton _11;
        _11 = new JRadioButton();
        removeRadio = _11;
        _11.setText("Remove assertions that refer to the missing identity provider");
        _11.setLabel("Remove assertions that refer to the missing identity provider");
        _9.add(_11, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JRadioButton _12;
        _12 = new JRadioButton();
        ignoreRadio = _12;
        _12.setText("Import erroneous assertions as-is");
        _12.setRequestFocusEnabled(false);
        _12.setLabel("Import erroneous assertions as-is");
        _12.setFocusPainted(false);
        _9.add(_12, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JComboBox _13;
        _13 = new JComboBox();
        providerSelector = _13;
        _13.setToolTipText("Existing local identity provider");
        _9.add(_13, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 8, 1, 2, 0, null, null, null));
        final JButton _14;
        _14 = new JButton();
        _14.setText("Create new Identity Provider");
        _14.setToolTipText("Create a new identity provider so you can then associate those assertions with");
        _9.add(_14, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, 8, 0, 1, 0, null, null, null));
        final JLabel _15;
        _15 = new JLabel();
        _15.setText("Policy contains assertions that refer to an unknown identity provider.");
        _1.add(_15, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 2, 0, 6, null, null, null));
    }

}
