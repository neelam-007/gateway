package com.l7tech.console.panels;

import com.l7tech.objectmodel.FindException;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.common.util.Locator;

import javax.swing.*;
import java.util.Locale;
import java.util.ResourceBundle;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/*
 * This class provides a panel for users to select the type of LDAP identity provider.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class IdentityProviderTypePanel extends WizardStepPanel {

    /**
     * Constructor - create a new provider type panel.
     *
     * @param next  The panel for use in the next step.
     */
    public IdentityProviderTypePanel(WizardStepPanel next) {
        super(next);
        initResources();
        initComponents();
    }

    /**
     * Loads locale-specific resources: strings  etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.IdentityProviderDialog", locale);
    }

    /**
     * Provide the description for the step being taken on this panel.
     *
     * @return
     */
    public String getDescription() {
        return "Select the type of LDAP identity provider.";
    }

    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private void initComponents() {

        GridBagConstraints constraints = null;

        JPanel panel = new JPanel();
        panel.setDoubleBuffered(true);
        add(panel);
        panel.setLayout(new GridBagLayout());

        // provider types
        JLabel providerTypesLabel = new JLabel();
        providerTypesLabel.setToolTipText(resources.getString("providerTypeTextField.tooltip"));
        providerTypesLabel.setText(resources.getString("providerTypeTextField.label"));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 12, 0, 0);
        panel.add(providerTypesLabel, constraints);


        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 7, 0, 0);
        panel.add(getProviderTypes(), constraints);

    } // initComponents()


    /** @return the wizard step label  */
    public String getStepLabel() {
        return "Provider Type";
    }

    /**
     * A method that returns a JCheckBox that indicates
     * the type of identity provider selected by the user.
     *
     * @return the CheckBox component
     */
    private JComboBox getProviderTypes() {
        if (providerTypesCombo == null) {
            try {
                templates = getProviderConfigManager().getLdapTemplates();
            } catch (FindException e) {
                log.log(Level.WARNING, "cannot retrieve templates", e);
                templates = new LdapIdentityProviderConfig[0];
            } catch (RuntimeException e) {
                log.log(Level.WARNING, "cannot retrieve templates", e);
                templates = new LdapIdentityProviderConfig[0];
            }
            Object[] items = new Object[1+templates.length];
            items[0] = "Select the provider type";
            for (int i = 0; i < templates.length; i++) {
                items[i+1] = templates[i];
            }
            providerTypesCombo = new JComboBox(items);
            providerTypesCombo.setRenderer(providerTypeRenderer);
            providerTypesCombo.setToolTipText(resources.getString("providerTypeTextField.tooltip"));
            providerTypesCombo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Object o = providerTypesCombo.getSelectedItem();

                    if (o instanceof LdapIdentityProviderConfig) {
                        // a type is selected, can advance to next panel
                        advanceAllowed = true;
                    } else {
                        advanceAllowed = false;
                    }

                    // notify the wizard to update the state of the control buttons
                    notifyListeners();
                }
            });
        }
        return providerTypesCombo;
    }

    /**
     * Get the ProviderConfigManager object reference.
     *
     * @return  the object reference to the ProviderConfigManager.
     * @throws RuntimeException  when the ProviderConfigManager cannot be located.
     */
    private IdentityProviderConfigManager getProviderConfigManager()
      throws RuntimeException {
        IdentityProviderConfigManager ipc =
          (IdentityProviderConfigManager)Locator.
          getDefault().lookup(IdentityProviderConfigManager.class);
        if (ipc == null) {
            throw new RuntimeException("Could not find registered " + IdentityProviderConfigManager.class);
        }

        return ipc;
    }

    /**
     * Store the values of all fields on the panel to the wizard object which is a used for
     * keeping all the modified values. The wizard object will be used for providing the
     * updated values when updating the server.
     *
     * @param settings the object representing wizard panel state
     */
    public void storeSettings(Object settings) {

        if (settings != null) {
            LdapIdentityProviderConfig ldapType = (LdapIdentityProviderConfig )providerTypesCombo.getSelectedItem();
            ((LdapIdentityProviderConfig) settings).setTemplateName(ldapType.getTemplateName());
        }
    }

    /**
     * Indicate this panel is not the last one. The user must go to the panel.
     *
     * @return false
     */
    public boolean canFinish() {
        return false;
    }

    /**
     * Test whether the step is finished and it is safe to advance to the next one.  This method
     * should return quickly.
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean canAdvance() {
        return advanceAllowed;
    }

    /**
     *  A cell renderer for displaying the name of the identity provider type in JBoxCombo component.
     *
     **/
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
           * @see javax.swing.JList
           * @see javax.swing.ListSelectionModel
           * @see javax.swing.ListModel
           */
          public Component getListCellRendererComponent(JList list,
                                                        Object value,
                                                        int index,
                                                        boolean isSelected, boolean cellHasFocus) {
              super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
              if (!(value instanceof LdapIdentityProviderConfig)) {
                  setText(value.toString());
              } else {
                  LdapIdentityProviderConfig type = (LdapIdentityProviderConfig)value;
                  setText(type.getName());
              }

              return this;
          }

      };

    private ResourceBundle resources = null;
    private JComboBox providerTypesCombo = null;
    private LdapIdentityProviderConfig[] templates = null;
    static final Logger log = Logger.getLogger(IdentityProviderTypePanel.class.getName());
    boolean advanceAllowed = false;

}
