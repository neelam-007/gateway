package com.l7tech.console.panels;

import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.InvalidIdProviderCfgException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.common.util.Locator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class LdapIdentityProviderConfigPanel extends WizardStepPanel {

    /** Creates new form ServicePanel */
    public LdapIdentityProviderConfigPanel(WizardStepPanel next, boolean showProviderType) {
        super(next);
        this.showProviderType = showProviderType;
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

    public String getDescription() {
        return "Enter the configuration data of the LDAP Identity Provider. The fields marked with '*' are mandatory. You are allowed to go to next step only when all the mandatory fields are filled. You can test the LDAP configuration by pressing the Test button.";
    }

    /** @return the wizard step label    */
    public String getStepLabel() {
        return "Provider Configuration";
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
        providerNameTextField.addKeyListener(keyListener);

        return providerNameTextField;
    }

    private JTextField getLdapHostTextField() {
        if (ldapHostTextField != null) return ldapHostTextField;

        ldapHostTextField = new JTextField();
        ldapHostTextField.setPreferredSize(new Dimension(217, 20));
        ldapHostTextField.setMinimumSize(new Dimension(217, 20));
        ldapHostTextField.setToolTipText(resources.getString("ldapHostTextField.tooltip"));

        ldapHostTextField.setText("");
        ldapHostTextField.addKeyListener(keyListener);

        return ldapHostTextField;
    }

    private JTextField getLdapSearchBaseTextField() {
        if (ldapSearchBaseTextField != null) return ldapSearchBaseTextField;

        ldapSearchBaseTextField = new JTextField();
        ldapSearchBaseTextField.setPreferredSize(new Dimension(217, 20));
        ldapSearchBaseTextField.setMinimumSize(new Dimension(217, 20));
        ldapSearchBaseTextField.setToolTipText(resources.getString("ldapSearchBaseTextField.tooltip"));

        ldapSearchBaseTextField.setText("");
        ldapSearchBaseTextField.addKeyListener(keyListener);

        return ldapSearchBaseTextField;
    }

    private JTextField getLdapBindDNTextField() {
        if (ldapBindDNTextField != null) return ldapBindDNTextField;

        ldapBindDNTextField = new JTextField();
        ldapBindDNTextField.setPreferredSize( new Dimension( 217, 20 ) );
        ldapBindDNTextField.setMinimumSize( new Dimension( 217, 20 ) );
        ldapBindDNTextField.setToolTipText( resources.getString( "ldapBindDNTextField.tooltip" ) );

        ldapBindDNTextField.setText("");
        return ldapBindDNTextField;
    }

    private JTextField getLdapBindPassTextField() {
        if (ldapBindPassTextField != null) return ldapBindPassTextField;

        ldapBindPassTextField = new JTextField();
        ldapBindPassTextField.setPreferredSize( new Dimension( 217, 20 ) );
        ldapBindPassTextField.setMinimumSize( new Dimension( 217, 20 ) );
        ldapBindPassTextField.setToolTipText( resources.getString( "ldapBindPassTextField.tooltip" ) );

        ldapBindPassTextField.setText("");
        return ldapBindPassTextField;
    }

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
     * This method is called from within the constructor to
     * initialize the dialog.
     */



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

        // LDAP host
        JLabel ldapHostLabel = new JLabel();
        ldapHostLabel.setToolTipText(resources.getString("ldapHostTextField.tooltip"));
        ldapHostLabel.setText(resources.getString("ldapHostTextField.label"));

        constraints.gridx = 0;
        constraints.gridy = rowIndex;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 12, 0, 0);
        panel.add(ldapHostLabel, constraints);

        // ldap host text field
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = rowIndex++;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 7, 0, 11);
        panel.add(getLdapHostTextField(), constraints);

        // search base label
        JLabel ldapSearchBaseLabel = new JLabel();
        ldapSearchBaseLabel.setToolTipText(resources.getString("ldapSearchBaseTextField.tooltip"));
        ldapSearchBaseLabel.setText(resources.getString("ldapSearchBaseTextField.label"));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = rowIndex;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 12, 0, 0);
        panel.add(ldapSearchBaseLabel, constraints);

        // search base text field
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = rowIndex++;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 7, 0, 0);
        panel.add(getLdapSearchBaseTextField(), constraints);

        // Binding DN label
        JLabel ldapBindDNLabel = new JLabel();
        ldapBindDNLabel.setToolTipText( resources.getString( "ldapBindDNTextField.tooltip" ) );
        ldapBindDNLabel.setText( resources.getString( "ldapBindDNTextField.label" ) );
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = rowIndex;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets( 12, 12, 0, 0 );
        panel.add( ldapBindDNLabel, constraints );

        // Binding DN textfield
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = rowIndex++;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets( 12, 7, 0, 0 );
        panel.add( getLdapBindDNTextField(), constraints );

        // Binding password label
        JLabel ldapBindPassLabel = new JLabel();
        ldapBindPassLabel.setToolTipText( resources.getString( "ldapBindPassTextField.tooltip" ) );
        ldapBindPassLabel.setText( resources.getString( "ldapBindPassTextField.label" ) );
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = rowIndex;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets( 12, 12, 0, 0 );
        panel.add( ldapBindPassLabel, constraints );

        // Binding password textfield
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = rowIndex++;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets( 12, 7, 0, 0 );
        panel.add( getLdapBindPassTextField(), constraints );

        // test ldap
        JButton testButton = new JButton();
        testButton.setText(resources.getString("testLdapButton.label"));
        testButton.setToolTipText(resources.getString("testLdapButton.tooltip"));
        testButton.setActionCommand(CMD_TEST);

        testButton.
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent event) {
                  testSettings();
              }
          });

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = rowIndex++;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 7, 0, 10);

        panel.add(testButton, constraints);

        add(panel);
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
                  LdapIdentityProviderConfig type = (LdapIdentityProviderConfig)value;
                  setText(type.getName());
              }

              return this;
          }

      };

    private void getNewSettings(IdentityProviderConfig config) {

        if (config == null || !(config instanceof LdapIdentityProviderConfig)) {
            throw new RuntimeException("unhandled provider config type");
        }
        IdentityProviderType type = config.type();
        config.setTypeVal(type.toVal());
        LdapIdentityProviderConfig convertedcfg = (LdapIdentityProviderConfig) config;
        convertedcfg.setBindDN(ldapBindDNTextField.getText());
        convertedcfg.setBindPasswd(ldapBindPassTextField.getText());
        convertedcfg.setLdapUrl(ldapHostTextField.getText());
        convertedcfg.setSearchBase(ldapSearchBaseTextField.getText());
    }

    /**
      * Indicate this panel is not the last one. The user must go to the panel.
      *
      * @return false
      */
     public boolean canFinish() {
         return finishAllowed;
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

    private void testSettings() {
        Object type = providerTypesCombo.getSelectedItem();
        IdentityProviderConfig tmp = null;
        if (type instanceof LdapIdentityProviderConfig) {
            try {
                tmp = new LdapIdentityProviderConfig((LdapIdentityProviderConfig)type);
            } catch (IOException e) {
                log.log(Level.SEVERE, "cannot instantiate new provider config based on template", e);
                return;
            }
        } else {
            log.severe("unhandled provider type");
            return;
        }

        tmp.setName(providerNameTextField.getText());
        getNewSettings(tmp);
        String errorMsg = null;
        try {
            getProviderConfigManager().test(tmp);
        } catch (InvalidIdProviderCfgException e) {
            errorMsg = e.getMessage();
        } catch (RuntimeException e) {
            errorMsg = resources.getString("test.error.runtime") + "\n" + e.getMessage();
        }
        if (errorMsg == null) {
            JOptionPane.showMessageDialog(this, resources.getString("test.res.ok"),
                    resources.getString("test.res.title"),
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, errorMsg,
                    resources.getString("test.res.title"),
                    JOptionPane.ERROR_MESSAGE);
        }
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
        }
        return providerTypesCombo;
    }

    private void updateControlButtonState() {
        if (getProviderNameTextField().getText().length() > 0 &&
                getLdapHostTextField().getText().length() > 0 &&
                getLdapSearchBaseTextField().getText().length() > 0) {

            // can advance to next panel only when the above three fields are not empty
            advanceAllowed = true;
            finishAllowed = true;
        } else {
            advanceAllowed = false;
            finishAllowed = false;
        }

        // notify the wizard to update the state of the control buttons
        notifyListeners();
    }

    /** populate the form from the provider beans */
    public void readSettings(Object settings) throws IllegalArgumentException {
        if (settings != null) {

            if (settings instanceof LdapIdentityProviderConfig) {

                LdapIdentityProviderConfig iProviderConfig = (LdapIdentityProviderConfig) settings;

                if (iProviderConfig.getOid() != -1) {

                    getProviderNameTextField().setText(iProviderConfig.getName());

                    getLdapBindPassTextField().setText(iProviderConfig.getBindPasswd());
                    getLdapBindDNTextField().setText(iProviderConfig.getBindDN());
                    getLdapSearchBaseTextField().setText(iProviderConfig.getSearchBase());
                    getLdapHostTextField().setText(iProviderConfig.getLdapUrl());
                }
                for (int i = providerTypesCombo.getModel().getSize() - 1; i >= 0; i--) {
                    Object toto = providerTypesCombo.getModel().getElementAt(i);
                    if (toto instanceof LdapIdentityProviderConfig) {
                        if (((LdapIdentityProviderConfig) toto).getName().equals(iProviderConfig.getTemplateName())) {
                            providerTypesCombo.setSelectedIndex(i);
                            break;
                        }
                    }
                }

                providerTypesCombo.setEnabled(false);
                updateControlButtonState();
            }
        }
    }

    public void storeSettings(Object settings) {

        if (settings instanceof LdapIdentityProviderConfig) {

            ((LdapIdentityProviderConfig) settings).setLdapUrl(getLdapHostTextField().getText());
            ((LdapIdentityProviderConfig) settings).setName(getProviderNameTextField().getText());
            ((LdapIdentityProviderConfig) settings).setSearchBase(getLdapSearchBaseTextField().getText());
            ((LdapIdentityProviderConfig) settings).setBindDN(getLdapBindDNTextField().getText());
            ((LdapIdentityProviderConfig) settings).setBindPasswd(getLdapBindPassTextField().getText());

        }
    }

    private KeyListener keyListener = new KeyListener() {
        public void keyPressed(KeyEvent ke) {
            // don't care
        }

        public void keyReleased(KeyEvent ke) {
            updateControlButtonState();
        }

        public void keyTyped(KeyEvent ke) {
            // don't care
        }
    };

    private ResourceBundle resources = null;
    private String CMD_TEST = "cmd.test";
    private JTextField providerNameTextField = null;
    private JTextField ldapBindPassTextField = null;
    private JTextField ldapBindDNTextField = null;
    private JTextField ldapSearchBaseTextField = null;
    private JTextField ldapHostTextField = null;
    private JComboBox providerTypesCombo = null;
    private LdapIdentityProviderConfig[] templates = null;
    static final Logger log = Logger.getLogger(LdapIdentityProviderConfigPanel.class.getName());
    private boolean showProviderType;
    private boolean finishAllowed = false;
    private boolean advanceAllowed = false;

}
