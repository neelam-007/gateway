package com.l7tech.console.panels;

import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfig;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;
import java.util.ResourceBundle;

/*
 * This class provides a panel for users to view the configuration data of the internal identity provider.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class InternalIdentityProviderConfigPanel extends WizardStepPanel {

    /**
     * Constructor - create a new provider type panel.
     *
     * @param next  The panel for use in the next step.
     * @param showProviderType  show/hide the provider type component on the panel.
     */
    public InternalIdentityProviderConfigPanel(WizardStepPanel next, boolean showProviderType) {
        super(next);
        this.showProviderType = showProviderType;
        initResources();
        initComponents();
    }

   /**
     * Populate the configuration data from the wizard input object to the visual components of the panel.
     *
     * @param settings  The current value of configuration items in the wizard input object.
     *
     * @throws IllegalArgumentException   if the data provided by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        if (settings != null) {

            if (settings instanceof IdentityProviderConfig) {

                IdentityProviderConfig iProviderConfig = (IdentityProviderConfig) settings;

                if (iProviderConfig.getOid() != -1) {

                    getProviderNameTextField().setText(iProviderConfig.getName());
                }
            }
        }
        providerTypesCombo.setEnabled(false);
    }

    /**
     * Store the values of all fields on the panel to the wizard object which is a used for
     * keeping all the modified values. The wizard object will be used for providing the
     * updated values when updating the server.
     *
     * @param settings the object representing wizard panel state
     */
    public void storeSettings(Object settings) {

        if (settings instanceof IdentityProviderConfig) {
            ((IdentityProviderConfig) settings).setName(getProviderNameTextField().getText());
        }
    }

    /** @return the wizard step label    */
    public String getStepLabel() {
        return "Provider Configuration";
    }

    /**
     * Provide the description for the step being taken on this panel.
     *
     * @return  String  The descritpion of the step.
     */
    public String getDescription() {
        return "Modification of the Internal Identity Provider is not " +
                "allowed. The data shown above is for viewing purposes only.";
    }

    /**
     * Loads locale-specific resources: strings  etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();

        resources = ResourceBundle.getBundle("com.l7tech.console.resources.IdentityProviderDialog", locale);
    }

    private void initComponents() {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints;

        int rowIndex = 0;

        // provider types
        JLabel providerTypesLabel = new JLabel();
        providerTypesLabel.setToolTipText(resources.getString("providerTypeTextField.tooltip"));
        providerTypesLabel.setText(resources.getString("providerTypeTextField.label"));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = rowIndex;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 12, 0, 0);
        panel.add(providerTypesLabel, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = rowIndex++;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 7, 0, 0);
        panel.add(getProviderTypes(), constraints);

          if(!showProviderType) {
              providerTypesLabel.setVisible(false);
              getProviderTypes().setVisible(false);
          }

        // Provider ID label
        JLabel providerNameLabel = new JLabel();
        providerNameLabel.setToolTipText(resources.getString("providerNameTextField.tooltip"));
        providerNameLabel.setText(resources.getString("providerNameTextField.label"));

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = rowIndex;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 12, 0, 0);
        panel.add(providerNameLabel, constraints);

        // Provider ID text field
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = rowIndex++;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 7, 0, 11);
        panel.add(getProviderNameTextField(), constraints);

        add(panel);

    }

    /**
     * A method that returns a JCheckBox that indicates
     * wether the user wishes to define additional properties
     * of the entity
     *
     * @return the CheckBox component
     */
    private JComboBox getProviderTypes() {
        if (providerTypesCombo == null) {

            providerTypesCombo = new JComboBox();
            providerTypesCombo.addItem("Internal Provider");
            providerTypesCombo.setRenderer(providerTypeRenderer);
            providerTypesCombo.setToolTipText(resources.getString("providerTypeTextField.tooltip"));
        }
        return providerTypesCombo;
    }

    /**
     * A method that returns a JTextField containing provider information
     *
     * @return the ID textfield
     */
    public JTextField getProviderNameTextField() {
        if (providerNameTextField != null) return providerNameTextField;

        providerNameTextField = new JTextField();
        providerNameTextField.setPreferredSize(new Dimension(217, 20));
        providerNameTextField.setMinimumSize(new Dimension(217, 20));
        providerNameTextField.setToolTipText(resources.getString("providerNameTextField.tooltip"));
        providerNameTextField.setEditable(false);

        return providerNameTextField;
    }

    private ListCellRenderer
            providerTypeRenderer = new DefaultListCellRenderer() {
                /**
                 * Return a component that has been configured to display the identity provider
                 * type value.
                 *
                 * @param list The JList we're painting.
                 * @param value The value returned by list.getModel().getElementAt(index).
                 * @param index The cells index.
                 * @param isSelected True if the specified cell was selected.
                 * @param cellHasFocus True if the specified cell has the focus.
                 * @return A component whose paint() method will render the specified value.
                 *
                 * @see JList
                 * @see ListSelectionModel
                 * @see ListModel
                 */
                public Component getListCellRendererComponent(JList list,
                                                              Object value,
                                                              int index,
                                                              boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (!(value instanceof LdapIdentityProviderConfig)) {
                        setText(value.toString());
                    } else {
                        LdapIdentityProviderConfig type = (LdapIdentityProviderConfig) value;
                        setText(type.getName());
                    }

                    return this;
                }

            };

    private ResourceBundle resources = null;
    private boolean showProviderType;
    private JComboBox providerTypesCombo = null;
    private JTextField providerNameTextField = null;
}
