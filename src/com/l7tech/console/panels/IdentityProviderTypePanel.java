package com.l7tech.console.panels;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.common.util.Locator;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.EventListenerList;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class IdentityProviderTypePanel extends WizardStepPanel {

    /** Creates new form ServicePanel */
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
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private void initComponents() {

        GridBagConstraints constraints = null;

        JPanel panel = new JPanel();
        panel.setDoubleBuffered(true);
        add(panel);
        panel.setLayout(new GridBagLayout());

        // Provider ID label
/*        JLabel providerNameLabel = new JLabel();
        providerNameLabel.setToolTipText(resources.getString("providerNameTextField.tooltip"));
        providerNameLabel.setText(resources.getString("providerNameTextField.label"));

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 12, 0, 0);
        panel.add(providerNameLabel, constraints);

        // Provider ID text field
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 7, 0, 11);
        panel.add(getProviderNameTextField(), constraints);*/

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


    /** @return the wizard step label    */
    public String getStepLabel() {
        return "Provider Type";
    }


    /**
     * A method that returns a JTextField containing provider information
     *
     * @return the ID textfield
     */
/*

    public JTextField getProviderNameTextField() {
        if (providerNameTextField != null) return providerNameTextField;

        providerNameTextField = new JTextField();
        providerNameTextField.setPreferredSize(new Dimension(217, 20));
        providerNameTextField.setMinimumSize(new Dimension(217, 20));
        providerNameTextField.setToolTipText(resources.getString("providerNameTextField.tooltip"));

        providerNameTextField.getDocument().
          addDocumentListener(new DocumentListener() {
              public void insertUpdate(DocumentEvent e) {
                  //saveButton.setEnabled(enableSaveButton(e));
              }

              public void removeUpdate(DocumentEvent e) {
                  //saveButton.setEnabled(enableSaveButton(e));
              }

              public void changedUpdate(DocumentEvent e) {
                  //saveButton.setEnabled(enableSaveButton(e));
              }

              private boolean enableSaveButton(DocumentEvent e) {
                  boolean enable =
                    e.getDocument().getLength() > 0 &&
                    providerTypesCombo.getSelectedIndex() != -1;
                  //enable = enable &&
                  //  providerSettingsPanel != null;

                  return enable;
              }
          });

        return providerNameTextField;
    }
*/


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
            providerTypesCombo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Object o = providerTypesCombo.getSelectedItem();
                    LdapIdentityProviderConfig ipt = null;
                    if (o instanceof LdapIdentityProviderConfig) {
                        ipt = (LdapIdentityProviderConfig)o;
                    }
                    // todo: set the provider type selected
                    //selectProvidersPanel(ipt);
                }
            });
        }
        return providerTypesCombo;
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
     * return the provider details
     *
     * @return the panel for the selected provide
     */
    private JPanel getProvidersPanel() {
        if (providersPanel != null) return providersPanel;
        providersPanel = new JPanel();
        return providersPanel;
    }

    /**
     * select the provider panel for the provider type, if null
     * reset the form
     */
/*    private void selectProvidersPanel(LdapIdentityProviderConfig ip) {
        providersPanel.removeAll();
        providersPanel.setLayout(new BorderLayout());
        boolean found = false;
        providerSettingsPanel = null;
        if (ip != null) {
            //todo: go to next step in the wizard
            //providerSettingsPanel = getLdapPanel(iProvider);
            //providersPanel.add(providerSettingsPanel);
            //found = true;
        }

        //todo: enable the finish button
        //saveButton.setEnabled(providerSettingsPanel != null && providerNameTextField.getText().length() > 0);
        Dimension size = origDimension;
        // todo: fix the hardcoded multiply
//        setSize((int)size.getWidth(), (int)(size.getHeight() * (found ? 2.0 : 1.0)));
        validate();
        repaint();

    }*/

    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */


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

    /** Resource bundle with default locale */
    private ResourceBundle resources = null;

    private String CMD_CANCEL = "cmd.cancel";

    private String CMD_OK = "cmd.ok";
    private String CMD_TEST = "cmd.test";

    private JButton saveButton = null;
    private EntityHeader header = new EntityHeader();
    private IdentityProviderConfig iProvider;
    private EventListenerList listenerList = new EventListenerList();
    private JPanel providersPanel;
   // private IdentityProviderTypePanel.ProviderSettingsPanel providerSettingsPanel;
    private Dimension origDimension;

    /** provider ID text field */
    private JTextField providerNameTextField = null;
    private JComboBox providerTypesCombo = null;
    private LdapIdentityProviderConfig[] templates = null;
    static final Logger log = Logger.getLogger(IdentityProviderTypePanel.class.getName());

}
