package com.l7tech.console.panels;

import com.l7tech.console.text.FilterDocument;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.imp.IdentityProviderTypeImp;
import com.l7tech.util.Locator;
import org.apache.log4j.Category;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This class is the New Provider dialog.
 */
public class NewProviderDialog extends JDialog {

    private PanelListener listener;
    private IdentityProviderConfig iProvider = new com.l7tech.identity.imp.IdentityProviderConfigImp();

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
     * set the PanelListener
     *
     * @param l the PanelListener
     */
    public void setPanelListener(PanelListener l) {
        listener = l;
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

        // additional properties
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 12, 0, 0);
        panel.add(getAdditionalProperties(), constraints);

        // Buttons
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(12, 0, 12, 21);
        JPanel buttonPanel = createButtonPanel();
        panel.add(buttonPanel, constraints);

        // bottom filler
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(0, 0, 0, 0);
        Component filler = Box.createHorizontalStrut(8);
        panel.add(filler, constraints);

        // side filler
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 3;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(0, 0, 0, 0);
        Component filler2 = Box.createHorizontalStrut(8);
        panel.add(filler2, constraints);

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

        return providerNameTextField;
    }

    /**
     * A method that returns a JCheckBox that indicates
     * wether the user wishes to define additional properties
     * of the entity
     *
     * @return the CheckBox component
     */
    private JCheckBox getAdditionalProperties() {
        if (additionalPropertiesCheckBox == null) {
            additionalPropertiesCheckBox = new JCheckBox(resources.getString("additionalProperties.label"));
            additionalPropertiesCheckBox.setHorizontalTextPosition(SwingConstants.LEADING);

            additionalPropertiesCheckBox.addItemListener(new ItemListener() {
                /**
                 * Invoked when an item has been selected or deselected.
                 * The code written for this method performs the operations
                 * that need to occur when an item is selected (or deselected).
                 */
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == e.SELECTED) {
                        createThenEdit = true;
                    }
                }
            });
        }

        return additionalPropertiesCheckBox;
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
        createButton.setActionCommand(CMD_OK);
        createButton.
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        windowAction(event);
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
                        windowAction(event);
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
    private void windowAction(Object actionCommand) {
        String cmd = null;

        if (actionCommand != null) {
            if (actionCommand instanceof ActionEvent) {
                cmd = ((ActionEvent)actionCommand).getActionCommand();
            } else {
                cmd = actionCommand.toString();
            }
        }
        if (cmd == null) {
            // do nothing
        } else if (cmd.equals(CMD_CANCEL)) {
            this.dispose();
        } else if (cmd.equals(CMD_OK)) {
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
        IdentityProviderTypeImp ip = new IdentityProviderTypeImp();
        ip.setClassName("bla");
        iProvider.setType(ip);

        final EntityHeader header = new EntityHeader();

        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        header.setName(iProvider.getName());
                        header.setType(EntityType.ID_PROVIDER_CONFIG);
                        try {
                            getProviderConfigManager().save(iProvider);
                            listener.onInsert(header);
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
     * override the Dialogue method so tha we can open and editor
     * panel if requested
     */
    public void dispose() {
        super.dispose();

        // maybe cancel was pressed, this ensures that
        // an object has been inserted
        if (insertSuccess) {
            if (createThenEdit) {
                // yes the additional properties are to be defined
                createThenEdit = false;
                SwingUtilities.invokeLater(
                        new Runnable() {
                            public void run() {
                            }
                        });
            }
        }
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
            throw new RuntimeException("Could not find registered "+IdentityProviderConfigManager.class);
        }

        return ipc;
    }

    /**
     * main for testing
     *
     * @param args
     */
    public static void main(String[] args) {
        try {

            JFrame frame = new JFrame() {
                public Dimension getPreferredSize() {
                    return new Dimension(200, 100);
                }
            };
            new NewProviderDialog(frame).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Resource bundle with default locale */
    private ResourceBundle resources = null;

    private String CMD_CANCEL = "cmd.cancel";

    private String CMD_OK = "cmd.ok";

    private JButton createButton = null;
    private JButton editButton = null;

    private boolean insertSuccess = false;
    private boolean createThenEdit = false;

    /** provider ID text field */
    private JTextField providerNameTextField = null;
    private JCheckBox additionalPropertiesCheckBox = null;
}

