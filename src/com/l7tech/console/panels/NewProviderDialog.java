package com.l7tech.console.panels;

import com.l7tech.console.text.FilterDocument;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.ldap.LdapConfigSettings;
import com.l7tech.util.Locator;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.*;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.EventListener;

/**
 * This class is the New Provider dialog.
 */
public class NewProviderDialog extends JDialog {

    private IdentityProviderConfig iProvider = new IdentityProviderConfig();
    private EventListenerList listenerList = new EventListenerList();
    private JPanel providersPanel;
    private ProviderSettingsPanel providerSettingsPanel;
    private Dimension origDimension;

    /**
     * Create a new NewProviderDialog
     *
     * @param parent the parent Frame. May be <B>null</B>
     */
    public NewProviderDialog(JFrame parent) {
        super(parent, true);
        initResources();
        initComponents();
        pack();
        Utilities.centerOnScreen(this);
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
     * @param header
     */
    private void fireEventProviderAdded(EntityHeader header) {
        EntityEvent event = new EntityEvent(header);
        EventListener[] listeners = listenerList.getListeners(EntityListener.class);
        for (int i = 0; i < listeners.length; i++) {
            ((EntityListener)listeners[i]).entityAdded(event);
        }
    }


    /**
     * Loads locale-specific resources: strings  etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.NewProviderDialog", locale);
    }

    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private void initComponents() {

        GridBagConstraints constraints = null;

        Container contents = getContentPane();
        JPanel panel = new JPanel();
        panel.setDoubleBuffered(true);
        contents.add(panel);
        panel.setLayout(new GridBagLayout());
        setTitle(resources.getString("dialog.title"));

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                // user hit window manager close button
                windowAction(CMD_CANCEL);
            }
        });

        addComponentListener(new ComponentAdapter() {
            /** Invoked when the component has been made visible.*/
            public void componentShown(ComponentEvent e) {
                origDimension = NewProviderDialog.this.getSize();
            }
        });

        // Provider ID label
        JLabel providerNameLabel = new JLabel();
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
        panel.add(getProviderNameTextField(), constraints);

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

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(12, 7, 0, 0);
        panel.add(getProvidersPanel(), constraints);


        // Buttons
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(12, 0, 12, 21);
        JPanel buttonPanel = createButtonPanel();
        panel.add(buttonPanel, constraints);

        getRootPane().setDefaultButton(createButton);
    } // initComponents()

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

        providerNameTextField.
          setDocument(
            new FilterDocument(24,
              new FilterDocument.Filter() {
                  public boolean accept(String str) {
                      if (str == null) return false;
                      return true;
                  }
              }));

        providerNameTextField.getDocument().
          addDocumentListener(new DocumentListener() {
              public void insertUpdate(DocumentEvent e) {
                  createButton.
                    setEnabled(enableCreateButton(e));
              }

              public void removeUpdate(DocumentEvent e) {
                  createButton.setEnabled(enableCreateButton(e));
              }

              public void changedUpdate(DocumentEvent e) {
                  createButton.setEnabled(enableCreateButton(e));
              }

              private boolean enableCreateButton(DocumentEvent e) {
                  return e.getDocument().getLength() > 0 &&
                    providerTypesCombo.getSelectedIndex() != -1;
              }

          });

        return providerNameTextField;
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
            IdentityProviderType[] types =
              new IdentityProviderType[]{IdentityProviderType.LDAP};

            providerTypesCombo = new JComboBox(types);
            providerTypesCombo.setRenderer(providerTypeRenderer);
            providerTypesCombo.setToolTipText(resources.getString("providerTypeTextField.tooltip"));
            providerTypesCombo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    IdentityProviderType ipt =
                      (IdentityProviderType)providerTypesCombo.getSelectedItem();
                    if (ipt == null) return;
                    selectProvidersPanel(ipt);
                }
            });
        }
        return providerTypesCombo;
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
     * select the provider panel for the provider type
     */
    private void selectProvidersPanel(IdentityProviderType ip) {
        providersPanel.removeAll();
        providersPanel.setLayout(new BorderLayout());
        if (ip == IdentityProviderType.LDAP) {
            providerSettingsPanel = getLdapPanel();
            providersPanel.add(providerSettingsPanel);
        }
        Dimension size = origDimension;
        setSize((int)size.getWidth(), (int)(size.getHeight() * 1.5));
        validate();
        repaint();
    }

    /**
     * Creates the panel of buttons that goes along the bottom
     * of the dialog
     *
     * Sets the variable okButton
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, 0));

        // OK button (global variable)
        createButton = new JButton();
        createButton.setText(resources.getString("createButton.label"));
        createButton.setToolTipText(resources.getString("createButton.tooltip"));
        createButton.setEnabled(false);
        createButton.setActionCommand(CMD_OK);
        createButton.
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent event) {
                  windowAction(event.getActionCommand());
              }
          });
        panel.add(createButton);

        // space
        panel.add(Box.createRigidArea(new Dimension(5, 0)));

        // cancel button
        JButton cancelButton = new JButton();
        cancelButton.setText(resources.getString("cancelButton.label"));
        cancelButton.setActionCommand(CMD_CANCEL);
        cancelButton.
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent event) {
                  windowAction(event.getActionCommand());
              }
          });
        panel.add(cancelButton);

        // equalize buttons
        Utilities.equalizeButtonSizes(new JButton[]{createButton, cancelButton});

        return panel;
    } // createButtonPanel()

    /**
     * The user has selected an option. Here we close and dispose
     * the dialog.
     * If actionCommand is an ActionEvent, getCommandString() is
     * called, otherwise toString() is used to get the action command.
     *
     * @param actionCommand
     *               may be null
     */
    private void windowAction(String actionCommand) {
        if (actionCommand == null) {
            // do nothing
        } else if (actionCommand.equals(CMD_CANCEL)) {
            this.dispose();
        } else if (actionCommand.equals(CMD_OK)) {
            if (!validateInput()) {
                providerNameTextField.requestFocus();
                return;
            }
            insertProvider();
        }
    }

    /** insert the provider */
    private void insertProvider() {
        iProvider.setName(providerNameTextField.getText());
        providerSettingsPanel.readSettings(iProvider);
        // emil, i leave this commented so you can see what i did
        //IdentityProviderTypeImp ip = new IdentityProviderTypeImp();
        //ip.setClassName("bla");
        //iProvider.setType(ip);

        final EntityHeader header = new EntityHeader();

        SwingUtilities.invokeLater(
          new Runnable() {
              public void run() {
                  header.setName(iProvider.getName());
                  header.setType(EntityType.ID_PROVIDER_CONFIG);
                  try {
                      header.setOid(getProviderConfigManager().save(iProvider));
                      fireEventProviderAdded(header);
                  } catch (SaveException e) {
                      e.printStackTrace();
                  } catch (RuntimeException e) {
                      e.printStackTrace();
                  }
                  NewProviderDialog.this.dispose();
              }
          });
    }


    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private ProviderSettingsPanel getLdapPanel() {
        final JTextField ldapHostTextField = new JTextField();
        final JTextField ldapSearchBaseTextField = new JTextField();

        ProviderSettingsPanel panel = new ProviderSettingsPanel() {
            void readSettings(IdentityProviderConfig config) {
                config.setTypeVal(IdentityProviderType.LDAP.toVal());
                config.putProperty(LdapConfigSettings.LDAP_HOST_URL, ldapHostTextField.getText());
                config.putProperty(LdapConfigSettings.LDAP_SEARCH_BASE, ldapSearchBaseTextField.getText());
            }
        };

        panel.setLayout(new GridBagLayout());

        // LDAP host
        JLabel ldapHostLabel = new JLabel();
        ldapHostLabel.setToolTipText(resources.getString("ldapHostTextField.tooltip"));
        ldapHostLabel.setText(resources.getString("ldapHostTextField.label"));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 12, 0, 0);
        panel.add(ldapHostLabel, constraints);

        ldapHostTextField.setPreferredSize(new Dimension(217, 20));
        ldapHostTextField.setMinimumSize(new Dimension(217, 20));
        ldapHostTextField.setToolTipText(resources.getString("ldapHostTextField.tooltip"));

        // ldap host text field
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 7, 0, 11);
        panel.add(ldapHostTextField, constraints);

        // search base label
        JLabel ldapSearchBaseLabel = new JLabel();
        ldapSearchBaseLabel.setToolTipText(resources.getString("ldapSearchBaseTextField.tooltip"));
        ldapSearchBaseLabel.setText(resources.getString("ldapSearchBaseTextField.label"));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 12, 0, 0);
        panel.add(ldapSearchBaseLabel, constraints);


        ldapSearchBaseTextField.setPreferredSize(new Dimension(217, 20));
        ldapSearchBaseTextField.setMinimumSize(new Dimension(217, 20));
        ldapSearchBaseTextField.setToolTipText(resources.getString("ldapSearchBaseTextField.tooltip"));
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 7, 0, 11);
        panel.add(ldapSearchBaseTextField, constraints);

        return panel;
    }


    /**
     * validate the input
     *
     * @return true validated, false othwerwise
     */
    private boolean validateInput() {
        return true;
    }

    private IdentityProviderConfigManager getProviderConfigManager() throws RuntimeException {
        IdentityProviderConfigManager ipc =
          (IdentityProviderConfigManager)Locator.
          getDefault().lookup(IdentityProviderConfigManager.class);
        if (ipc == null) {
            throw new RuntimeException("Could not find registered " + IdentityProviderConfigManager.class);
        }

        return ipc;
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
              IdentityProviderType type = (IdentityProviderType)value;
              setText(type.description());
              return this;
          }

      };

    /**
     * the specific provider settigs panels exted this panel
     */
    abstract static class ProviderSettingsPanel extends JPanel {
        /**
         * read the identity prov ider config
         *
         * @param config the receving configuraiton
         */
        abstract void readSettings(IdentityProviderConfig config);

        /**
         * validate the config. The specific panel validates its
         * config, this is optional method that is typically invoked
         * by the caller panel before the record insert.
         *
         * @param config the identity configuration
         * @return boolean if valid config, false otherwise
         */
        boolean validate(IdentityProviderConfig config) {
            return true;
        }
    }

    /** Resource bundle with default locale */
    private ResourceBundle resources = null;

    private String CMD_CANCEL = "cmd.cancel";

    private String CMD_OK = "cmd.ok";

    private JButton createButton = null;

    /** provider ID text field */
    private JTextField providerNameTextField = null;
    private JComboBox providerTypesCombo = null;
}

