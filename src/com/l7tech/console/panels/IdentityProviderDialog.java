package com.l7tech.console.panels;

import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.InvalidIdProviderCfgException;
import com.l7tech.identity.ldap.LdapConfigSettings;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.common.util.Locator;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.*;
import java.util.EventListener;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.MessageFormat;

/**
 * This class is the Identity Provider dialog.
 */
public class IdentityProviderDialog extends JDialog {
    static final Logger log = Logger.getLogger(IdentityProviderDialog.class.getName());

    private EntityHeader header = new EntityHeader();
    private IdentityProviderConfig iProvider = new IdentityProviderConfig();
    private EventListenerList listenerList = new EventListenerList();
    private JPanel providersPanel;
    private ProviderSettingsPanel providerSettingsPanel;
    private Dimension origDimension;

    /**
     * Create a new IdentityProviderDialog
     *
     * @param parent the parent Frame. May be <B>null</B>
     */
    public IdentityProviderDialog(JFrame parent, EntityHeader h) {
        super(parent, true);
        addHierarchyListener(hierarchyListener);
        header = h;
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
     * notfy the listeners that the entity has been added
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
     * notfy the listeners that the entity has been updated
     * @param header
     */
    private void fireEventProviderUpdated(EntityHeader header) {
        EntityEvent event = new EntityEvent(header);
        EventListener[] listeners = listenerList.getListeners(EntityListener.class);
        for (int i = 0; i < listeners.length; i++) {
            ((EntityListener)listeners[i]).entityUpdated(event);
        }
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
        constraints.insets = new Insets(12, 7, 0, 11);
        panel.add(getProvidersPanel(), constraints);

        // Buttons
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(12, 7, 12, 11);
        JPanel buttonPanel = createButtonPanel();
        panel.add(buttonPanel, constraints);

        getRootPane().setDefaultButton(saveButton);
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

        providerNameTextField.getDocument().
          addDocumentListener(new DocumentListener() {
              public void insertUpdate(DocumentEvent e) {
                  saveButton.
                    setEnabled(enableSaveButton(e));
              }

              public void removeUpdate(DocumentEvent e) {
                  saveButton.setEnabled(enableSaveButton(e));
              }

              public void changedUpdate(DocumentEvent e) {
                  saveButton.setEnabled(enableSaveButton(e));
              }

              private boolean enableSaveButton(DocumentEvent e) {
                  boolean enable =
                    e.getDocument().getLength() > 0 &&
                    providerTypesCombo.getSelectedIndex() != -1;
                  enable = enable &&
                    providerSettingsPanel != null;
                  enable = enable &&
                    iProvider.getTypeVal() != IdentityProviderType.INTERNAL.toVal();

                  return enable;
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
            Object[] items =
              new Object[]{
                  "Select the provider type",
                  IdentityProviderType.LDAP
              };

            providerTypesCombo = new JComboBox(items);
            providerTypesCombo.setRenderer(providerTypeRenderer);
            providerTypesCombo.setToolTipText(resources.getString("providerTypeTextField.tooltip"));
            providerTypesCombo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Object o = providerTypesCombo.getSelectedItem();
                    IdentityProviderType ipt = null;
                    if (o instanceof IdentityProviderType) {
                        ipt = (IdentityProviderType)o;
                    }
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
     * select the provider panel for the provider type, if null
     * reset the form
     */
    private void selectProvidersPanel(IdentityProviderType ip) {
        providersPanel.removeAll();
        providersPanel.setLayout(new BorderLayout());
        boolean found = false;
        providerSettingsPanel = null;
        if (ip == IdentityProviderType.LDAP) {
            providerSettingsPanel = getLdapPanel(iProvider);
            providersPanel.add(providerSettingsPanel);
            found = true;
        }

        saveButton.setEnabled(
          providerSettingsPanel != null && providerNameTextField.getText().length() > 0);
        Dimension size = origDimension;
        // todo: fix the hardcoded multiply
        setSize((int)size.getWidth(), (int)(size.getHeight() * (found ? 1.5 : 1.0)));
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
        saveButton = new JButton();
        saveButton.setText(resources.getString("saveButton.label"));
        saveButton.setToolTipText(resources.getString("saveButton.tooltip"));
        saveButton.setEnabled(false);
        saveButton.setActionCommand(CMD_OK);
        saveButton.
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent event) {
                  windowAction(event.getActionCommand());
              }
          });
        panel.add(saveButton);

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
        Utilities.equalizeButtonSizes(new JButton[]{saveButton, cancelButton});

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
            addOrUpdateProvider();
        } else if (actionCommand.equals(CMD_TEST)) {
            testSettings();
        }
    }


    /** populate the form from the provider beans */
    private void populateForm() {
        if (iProvider.getOid() != -1) {
            providerNameTextField.setText(iProvider.getName());
            // kludge, we add the internal provider, as itmay show only in
            // edit dsabled mode
            providerTypesCombo.addItem(IdentityProviderType.INTERNAL);
            for (int i = providerTypesCombo.getModel().getSize() - 1; i >= 0; i--) {
                IdentityProviderType type =
                  (IdentityProviderType)providerTypesCombo.getModel().getElementAt(i);
                if (iProvider.getTypeVal() == type.toVal()) {
                    providerTypesCombo.setSelectedIndex(i);
                    break;
                }
            }
            providerTypesCombo.setEnabled(false);
        }
    }

    private void testSettings() {
        IdentityProviderConfig tmp = new IdentityProviderConfig();
        tmp.setName(providerNameTextField.getText());
        providerSettingsPanel.readSettings(tmp);
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


    /** add or pudfate the provider */
    private void addOrUpdateProvider() {
        iProvider.setName(providerNameTextField.getText());
        providerSettingsPanel.readSettings(iProvider);

        SwingUtilities.invokeLater(
          new Runnable() {
              public void run() {
                  header.setName(iProvider.getName());
                  header.setType(EntityType.ID_PROVIDER_CONFIG);
                  try {
                      if (header.getOid() == -1) {
                          header.setOid(getProviderConfigManager().save(iProvider));
                          fireEventProviderAdded(header);
                          IdentityProviderDialog.this.dispose();
                      } else {
                          getProviderConfigManager().update(iProvider);
                          fireEventProviderUpdated(header);
                          IdentityProviderDialog.this.dispose();
                      }
                  } catch (Exception e) {
                      handleThrowable(e);
                  }
              }
          });
    }


    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private ProviderSettingsPanel getLdapPanel(IdentityProviderConfig config) {
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
        ldapHostTextField.setText(config.getProperty(LdapConfigSettings.LDAP_HOST_URL));


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
        ldapSearchBaseTextField.setText(config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE));
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 7, 0, 0);
        panel.add(ldapSearchBaseTextField, constraints);

        // test ldap
        JButton testButton = new JButton();
        testButton.setText(resources.getString("testLdapButton.label"));
        testButton.setToolTipText(resources.getString("testLdapButton.tooltip"));
        testButton.setActionCommand(CMD_TEST);
        testButton.
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent event) {
                  windowAction(event.getActionCommand());
              }
          });

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 7, 0, 0);

        panel.add(testButton, constraints);


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


    private void handleThrowable(Throwable e) {
        Throwable cause = ExceptionUtils.unnestToRoot(e);
        if (cause instanceof SaveException) {
            String msg =
              MessageFormat.format(resources.getString("provider.save.error"), new Object[]{header.getName()});
              ErrorManager.getDefault().
                notify(Level.WARNING, cause, msg, log);
        } else {
            ErrorManager.getDefault().
              notify(Level.WARNING, e, "Error updating the identity provider.");
        }
    }


    // hierarchy listener
    private final HierarchyListener
      hierarchyListener = new HierarchyListener() {
          /** Called when the hierarchy has been changed.*/
          public void hierarchyChanged(HierarchyEvent e) {
              long flags = e.getChangeFlags();
              if ((flags & HierarchyEvent.SHOWING_CHANGED) == HierarchyEvent.SHOWING_CHANGED) {
                  if (IdentityProviderDialog.this.isShowing()) {
                      origDimension = IdentityProviderDialog.this.getSize();
                      if (header.getOid() != -1) {
                          try {
                              iProvider =
                                getProviderConfigManager().findByPrimaryKey(header.getOid());

                          } catch (Exception e1) {
                              ErrorManager.getDefault().
                                notify(Level.WARNING, e1, "Error retrieving provider.");
                          }
                          populateForm();
                      }
                  }
              }
          }
      };


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
              if (!(value instanceof IdentityProviderType)) {
                  setText(value.toString());
              } else {
                  IdentityProviderType type = (IdentityProviderType)value;
                  setText(type.description());
              }

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
    private String CMD_TEST = "cmd.test";

    private JButton saveButton = null;

    /** provider ID text field */
    private JTextField providerNameTextField = null;
    private JComboBox providerTypesCombo = null;
}

