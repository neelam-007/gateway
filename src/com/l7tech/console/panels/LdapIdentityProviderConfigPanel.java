package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.ArrowIcon;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class is a panel for users to input the configuration data of a LDAP identity provider.
 *
 * <p> Copyright (C) 2003 Layer 7 Technologies Inc.</p>
 *
 * $Id$
 */

public class LdapIdentityProviderConfigPanel extends IdentityProviderStepPanel {

    /** Creates new form ServicePanel */
    public LdapIdentityProviderConfigPanel(WizardStepPanel next, boolean providerTypeSelectable) {
        super(next);
        this.providerTypeSelectable = providerTypeSelectable;
        initResources();
        setLayout(new BorderLayout());
        add(getTypePanel(), BorderLayout.NORTH);
    }

    /**
     * Loads locale-specific resources: strings  etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();

        resources = ResourceBundle.getBundle("com.l7tech.console.resources.IdentityProviderDialog", locale);
    }

    public String getDescription() {
        return  "This Wizard allow you to configure an LDAP Identity Provider. Fields " +
                "marked with an asterisk \"*\" are required. Click [Test] at any time to " +
                "test the LDAP configuration.";
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

    private JList getLdapHostList() {
        if (ldapUrlList == null) {
            DefaultComboBoxModel model = new DefaultComboBoxModel();
            ldapUrlList = new JList(model);
        }
        return ldapUrlList;
    }

    private JComponent getLdapHostListPanel() {
        JPanel output = new JPanel();
        output.setLayout(new BorderLayout());
        JScrollPane listpanel = new JScrollPane(getLdapHostList());
        listpanel.setMinimumSize(new Dimension(217, 40));
        output.add(listpanel, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        GridLayout btlyt = new GridLayout(2, 1);
        buttonPanel.setLayout(btlyt);
        buttonPanel.add(getUpButton());
        buttonPanel.add(getDownButton());
        output.add(buttonPanel, BorderLayout.EAST);
        return output;
    }

    private JButton getUpButton() {
        if (upbutton == null) {
            upbutton = new JButton(new ArrowIcon(ArrowIcon.UP)) {
                int fixedsize = ArrowIcon.DEFAULT_SIZE+6;
                public Dimension getSize() {
                    return new Dimension(fixedsize, fixedsize);
                }
                public void setSize(Dimension d) {
                    super.setSize(new Dimension(fixedsize, fixedsize));
                }
                public void setBounds(int x, int y, int width, int height) {
                    if (height > fixedsize && y > height) {
                        y += height - fixedsize;
                    }
                    super.setBounds(x, y, fixedsize, fixedsize);
                }
            };
            upbutton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // todo
                    System.out.println("TODO UP");
                }
            });
        }
        return upbutton;
    }

    private JButton getDownButton() {
        if (downbutton == null) {
            downbutton = new JButton(new ArrowIcon(ArrowIcon.DOWN)) {
                int fixedsize = ArrowIcon.DEFAULT_SIZE+6;
                public Dimension getSize() {
                    return new Dimension(fixedsize, fixedsize);
                }
                public void setSize(Dimension d) {
                    super.setSize(new Dimension(fixedsize, fixedsize));
                }
                public void setBounds(int x, int y, int width, int height) {
                    if (height > fixedsize && y > 0) {
                        y += height - fixedsize;
                    }
                    super.setBounds(x, y, fixedsize, fixedsize);
                }
            };
            downbutton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // todo
                    System.out.println("TODO DOWN");
                }
            });
        }
        return downbutton;
    }

    private JButton getAddButton() {
        if (addButt != null) return addButt;
        addButt = new JButton("Add");
        addButt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String newUrl = (String)JOptionPane.showInputDialog(null,
                                                            "Enter the LDAP URL:",
                                                            "Add LDAP Host URL  ",
                                                            JOptionPane.PLAIN_MESSAGE,
                                                            null, null,
                                                            "ldap://host:port");
                DefaultComboBoxModel model = (DefaultComboBoxModel)getLdapHostList().getModel();
                if (newUrl != null) {
                    if (model.getIndexOf(newUrl) < 0) {
                        model.addElement(newUrl);
                    }
                }
            }
        });
        return addButt;
    }

    private JButton getRemoveButton() {
        if (removeButt != null) return removeButt;
        removeButt = new JButton("Remove");
        removeButt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selected = getLdapHostList().getSelectedIndex();
                if (selected > -1) {
                    ((DefaultComboBoxModel)getLdapHostList().getModel()).removeElementAt(selected);
                }
            }
        });
        return removeButt;
    }

    /*private JTextField getLdapHostTextField() {
        if (ldapHostTextField != null) return ldapHostTextField;

        ldapHostTextField = new JTextField();
        ldapHostTextField.setPreferredSize(new Dimension(217, 20));
        ldapHostTextField.setMinimumSize(new Dimension(217, 20));
        ldapHostTextField.setToolTipText(resources.getString("ldapHostTextField.tooltip"));

        ldapHostTextField.setText("");
        ldapHostTextField.addKeyListener(keyListener);

        return ldapHostTextField;
    }*/

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
        ldapBindDNTextField.setPreferredSize(new Dimension(217, 20));
        ldapBindDNTextField.setMinimumSize(new Dimension(217, 20));
        ldapBindDNTextField.setToolTipText(resources.getString("ldapBindDNTextField.tooltip"));

        ldapBindDNTextField.setText("");
        return ldapBindDNTextField;
    }

    private JTextField getLdapBindPassTextField() {
        if (ldapBindPassTextField != null) return ldapBindPassTextField;

        ldapBindPassTextField = new JTextField();
        ldapBindPassTextField.setPreferredSize(new Dimension(217, 20));
        ldapBindPassTextField.setMinimumSize(new Dimension(217, 20));
        ldapBindPassTextField.setToolTipText(resources.getString("ldapBindPassTextField.tooltip"));

        ldapBindPassTextField.setText("");
        return ldapBindPassTextField;
    }

    private IdentityAdmin getIdentityAdmin()
            throws RuntimeException {
        return Registry.getDefault().getIdentityAdmin();
    }

    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private JPanel getTypePanel() {

        if(typePanel != null) return typePanel;

        typePanel = new JPanel();
        typePanel.setLayout(new GridBagLayout());
        typePanel.setPreferredSize(new Dimension(400, 80));
        typePanel.setMinimumSize(new Dimension(400, 50));

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
        constraints.insets = new Insets(12, 0, 0, 0);
        typePanel.add(providerTypesLabel, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = rowIndex++;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 20, 0, 0);
        typePanel.add(getProviderTypes(), constraints);

        getProviderTypes().setEnabled(providerTypeSelectable);

        return typePanel;
    }

    private JPanel getAddRemoveButtons() {
        JPanel output = new JPanel(new BorderLayout());
        output.add(getAddButton(), BorderLayout.WEST);
        output.add(getRemoveButton(), BorderLayout.EAST);
        return output;
    }

    private JPanel getConfigPanel() {
        if( configPanel != null)  return configPanel;

        configPanel = new JPanel();

        configPanel.setLayout(new GridBagLayout());
        configPanel.setPreferredSize(new Dimension(400, 400));
        configPanel.setMinimumSize(new Dimension(400, 400));
        GridBagConstraints constraints;

        int rowIndex = 0;

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
        configPanel.add(providerNameLabel, constraints);

        // Provider ID text field
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = rowIndex++;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 7, 0, 11);
        configPanel.add(getProviderNameTextField(), constraints);

        // LDAP host
        JLabel ldapHostLabel = new JLabel();
        ldapHostLabel.setToolTipText(resources.getString("ldapHostTextField.tooltip"));
        ldapHostLabel.setText(resources.getString("ldapHostTextField.label"));

        constraints.gridx = 0;
        constraints.gridy = rowIndex;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(12, 12, 0, 0);
        configPanel.add(ldapHostLabel, constraints);

        // ldap host text field
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = rowIndex++;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 7, 0, 11);
        configPanel.add(getLdapHostListPanel(), constraints);

        // add, remove buttons
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = rowIndex++;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.SOUTHWEST;
        constraints.insets = new Insets(0, 7, 0, 0);
        configPanel.add(getAddRemoveButtons(), constraints);

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
        configPanel.add(ldapSearchBaseLabel, constraints);

        // search base text field
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = rowIndex++;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 7, 0, 0);
        configPanel.add(getLdapSearchBaseTextField(), constraints);

        // Binding DN label
        JLabel ldapBindDNLabel = new JLabel();
        ldapBindDNLabel.setToolTipText(resources.getString("ldapBindDNTextField.tooltip"));
        ldapBindDNLabel.setText(resources.getString("ldapBindDNTextField.label"));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = rowIndex;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 12, 0, 0);
        configPanel.add(ldapBindDNLabel, constraints);

        // Binding DN textfield
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = rowIndex++;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 7, 0, 0);
        configPanel.add(getLdapBindDNTextField(), constraints);

        // Binding password label
        JLabel ldapBindPassLabel = new JLabel();
        ldapBindPassLabel.setToolTipText(resources.getString("ldapBindPassTextField.tooltip"));
        ldapBindPassLabel.setText(resources.getString("ldapBindPassTextField.label"));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = rowIndex;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 12, 0, 0);
        configPanel.add(ldapBindPassLabel, constraints);

        // Binding password textfield
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = rowIndex++;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 7, 0, 0);
        configPanel.add(getLdapBindPassTextField(), constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = rowIndex++;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 7, 0, 10);

        return configPanel;
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
                templates = getIdentityAdmin().getLdapTemplates();
            } catch (FindException e) {
                log.log(Level.WARNING, "cannot retrieve templates", e);
                templates = new LdapIdentityProviderConfig[0];
            } catch (Exception e) {
                log.log(Level.WARNING, "cannot retrieve templates", e);
                templates = new LdapIdentityProviderConfig[0];
            }
            Object[] items = new Object[1 + templates.length];
            items[0] = "Select the provider type";
            for (int i = 0; i < templates.length; i++) {
                items[i + 1] = templates[i];
            }
            providerTypesCombo = new JComboBox(items);
            providerTypesCombo.setRenderer(providerTypeRenderer);
            providerTypesCombo.setToolTipText(resources.getString("providerTypeTextField.tooltip"));
            providerTypesCombo.setPreferredSize(new Dimension(217, 20));
            providerTypesCombo.setMinimumSize(new Dimension(217, 20));

            providerTypesCombo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Object o = providerTypesCombo.getSelectedItem();

                    if (o instanceof LdapIdentityProviderConfig) {
                        if(configPanel == null) {
                            LdapIdentityProviderConfigPanel.this.add(getConfigPanel(), BorderLayout.CENTER);

                            JPanel dummyPanel = new JPanel();
                            dummyPanel.setPreferredSize(new Dimension(400, 60));
                            LdapIdentityProviderConfigPanel.this.add(dummyPanel, BorderLayout.SOUTH);
                        } else {
                            getConfigPanel().setVisible(true);
                            updateControlButtonState();
                        }
                    } else {
                        advanceAllowed = false;
                        finishAllowed = false;
                        if(configPanel != null) {
                            getConfigPanel().setVisible(false);
                        }
                    }

                    // notify the wizard to update the state of the control buttons
                    notifyListeners();
                }
            });
        }
        return providerTypesCombo;
    }

    private void updateControlButtonState() {
        if (getProviderNameTextField().getText().length() > 0 &&
                //getLdapHostTextField().getText().length() > 0 &&
                getLdapHostList().getModel().getSize() > 0 &&
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
                    String[] listdata = iProviderConfig.getLdapUrl();
                    for (int i = 0; i < listdata.length; i++) {
                        String s = listdata[i];
                        ((DefaultComboBoxModel)getLdapHostList().getModel()).addElement(s);
                    }
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

                updateControlButtonState();
            }
        }
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

            Object selectedType = providerTypesCombo.getSelectedItem();

            if (selectedType instanceof LdapIdentityProviderConfig) {
                LdapIdentityProviderConfig ldapType = (LdapIdentityProviderConfig) selectedType;

                // stores the default mappings only when the config is a new object or
                // when the selection of the template is changed
                if (((LdapIdentityProviderConfig) settings).getTemplateName() == null ||
                        (((LdapIdentityProviderConfig) settings).getTemplateName() != null &&
                        !((LdapIdentityProviderConfig) settings).getTemplateName().equals(ldapType.getTemplateName()))) {

                    ((LdapIdentityProviderConfig) settings).setGroupMappings(ldapType.getGroupMappings());
                    ((LdapIdentityProviderConfig) settings).setUserMappings(ldapType.getUserMappings());
                }

                ((LdapIdentityProviderConfig) settings).setTemplateName(ldapType.getTemplateName());
                DefaultComboBoxModel model = (DefaultComboBoxModel)getLdapHostList().getModel();
                String[] newlist = new String[model.getSize()];
                for (int i = 0; i < newlist.length; i++) {
                    newlist[i] = (String)model.getElementAt(i);
                }
                ((LdapIdentityProviderConfig) settings).setLdapUrl(newlist);
                ((LdapIdentityProviderConfig) settings).setName(getProviderNameTextField().getText());
                ((LdapIdentityProviderConfig) settings).setSearchBase(getLdapSearchBaseTextField().getText());
                ((LdapIdentityProviderConfig) settings).setBindDN(getLdapBindDNTextField().getText());
                ((LdapIdentityProviderConfig) settings).setBindPasswd(getLdapBindPassTextField().getText());

            }
        }
    }

    /**
     * Test whether the step panel allows testing the settings.
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean canTest() {
        if (advanceAllowed) return true;
        return false;
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
    private JTextField providerNameTextField = null;
    private JTextField ldapBindPassTextField = null;
    private JTextField ldapBindDNTextField = null;
    private JTextField ldapSearchBaseTextField = null;
    //private JTextField ldapHostTextField = null;
    private JComboBox providerTypesCombo = null;
    private LdapIdentityProviderConfig[] templates = null;
    static final Logger log = Logger.getLogger(LdapIdentityProviderConfigPanel.class.getName());
    private boolean providerTypeSelectable;
    private boolean finishAllowed = false;
    private boolean advanceAllowed = false;
    private JPanel configPanel = null;
    private JPanel typePanel = null;
    private JList ldapUrlList = null;
    private JButton addButt;
    private JButton removeButt;
    private JButton upbutton;
    private JButton downbutton;
}
