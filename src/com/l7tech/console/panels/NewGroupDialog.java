package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.Actions;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.text.FilterDocument;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.*;
import java.util.EventListener;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;

/**
 * New Group dialog.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class NewGroupDialog extends JDialog {

    /** Resource bundle with default locale */
    private ResourceBundle resources = null;

    private String CMD_CANCEL = "cmd.cancel";

    private String CMD_OK = "cmd.ok";

    private JButton createButton = null;

    /** ID text field */
    private JTextField groupIdTextField = null;
    private JCheckBox additionalPropertiesCheckBox = null;
    private JFrame parent;

    private boolean insertSuccess = false;
    private boolean createThenEdit = false;

    GroupBean group = new GroupBean();
    private EventListenerList listenerList = new EventListenerList();
    IdentityProviderConfig ipc;

    /**
     * Create a new NewGroupDialog fdialog for a given Company
     *
     * @param parent  the parent Frame. May be <B>null</B>
     */
    public NewGroupDialog(JFrame parent, IdentityProviderConfig ipc) {
        super(parent, true);
        this.parent = parent;
        this.ipc = ipc;
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
     * Loads locale-specific resources: strings  etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.NewGroupDialog", locale);
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

        // If ipc is null, the action is invoked either from the Task menu or Home page.
        // So this must be the Internal group.
        if (ipc == null) {
            setTitle(resources.getString("dialog.internal.title"));
        } else {            
            if (ipc.type() == IdentityProviderType.FEDERATED) {
                setTitle(resources.getString("dialog.federated.title"));
            } else if (ipc.type() == IdentityProviderType.INTERNAL) {
                setTitle(resources.getString("dialog.internal.title"));
            } else {
                setTitle(resources.getString("dialog.title"));
            }
        }

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                // user hit window manager close button
                windowAction(CMD_CANCEL);
            }
        });
        Actions.setEscKeyStrokeDisposes(this);
        

        // user name label
        JLabel groupIdLabel = new JLabel();
        groupIdLabel.setToolTipText(resources.getString("groupIdTextField.tooltip"));
        groupIdLabel.setText(resources.getString("groupIdTextField.label"));

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 16, 0, 0);
        panel.add(groupIdLabel, constraints);

        // group name text field
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 7, 0, 11);
        panel.add(getGroupIdTextField(), constraints);



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

        // button panel
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
        constraints.gridy = 6;
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
        constraints.gridheight = 7;
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
     * A method that returns a JTextField containing userId information
     *
     * @return the user ID textfield
     */
    private JTextField getGroupIdTextField() {
        if (groupIdTextField != null) return groupIdTextField;

        groupIdTextField = new JTextField();
        groupIdTextField.setPreferredSize(new Dimension(200, 20));
        groupIdTextField.setMinimumSize(new Dimension(200, 20));
        groupIdTextField.setToolTipText(resources.getString("groupIdTextField.tooltip"));

        groupIdTextField.
                setDocument(
                        new FilterDocument(24,
                                new FilterDocument.Filter() {
                                    public boolean accept(String str) {
                                        if (str == null) return false;
                                        return true;
                                    }
                                }));
        return groupIdTextField;
    }

    /**
     * A method that returns a JCheckBox that indicates
     * wether the user wishes to define additional properties
     * of the entity
     *
     * @return the CheckBox component
     */
    private JCheckBox getAdditionalProperties() {
        if (additionalPropertiesCheckBox != null)
            return additionalPropertiesCheckBox;

        additionalPropertiesCheckBox = new JCheckBox(resources.getString("additionalProperties.label"));
        additionalPropertiesCheckBox.setHorizontalTextPosition(SwingConstants.LEADING);

        additionalPropertiesCheckBox.addItemListener(new ItemListener() {
            /**
             * Invoked when an item has been selected or deselected.
             * The code written for this method performs the operations
             * that need to occur when an item is selected (or deselected).
             */
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    createThenEdit = true;
                }
            }
        });

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
                groupIdTextField.requestFocus();
                return;
            }

            insertGroup();
        }
    }


    /** insert the Group */
    private void insertGroup() {
        group.setName(groupIdTextField.getText());
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        try {
                            EntityHeader header = new EntityHeader();
                            header.setType(EntityType.GROUP);
                            header.setName(group.getName());
                            long providerid;
                            if (ipc == null) {
                                providerid = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID;
                            } else providerid = ipc.getOid();
                            group.setUniqueIdentifier(Registry.getDefault().getIdentityAdmin().saveGroup(providerid, group, null ));
                            header.setStrId(group.getUniqueIdentifier());
                            NewGroupDialog.this.fireEventGroupAdded(header);
                            insertSuccess = true;
                        } catch (Exception e) {
                            ErrorManager.getDefault().
                              notify(Level.WARNING, e, "Error encountered while adding a group\n"+
                                     "The Group has not been created.");
                        }
                        NewGroupDialog.this.dispose();
                    }
                });

    }

    private void fireEventGroupAdded(EntityHeader header) {
       EntityEvent event = new EntityEvent(this, header);
       EventListener[] listeners = listenerList.getListeners(EntityListener.class);
        for (int i = 0; i< listeners.length; i++) {
            ((EntityListener)listeners[i]).entityAdded(event);
        }
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
                                EntityHeader header = new EntityHeader();
                                header.setType(EntityType.GROUP);
                                header.setName(group.getName());
                                header.setStrId(group.getUniqueIdentifier());

                                GroupPanel panel = GroupPanel.newInstance(ipc, header);
                                if (panel == null) return;
                                try {
                                    panel.edit(header, ipc);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                EditorDialog dialog = new EditorDialog(parent, panel);
                                dialog.pack();
                                Utilities.centerOnScreen(dialog);
                                dialog.show();
                                insertSuccess = false;
                            }
                        });
            }
        }
    }

    /**
     * validate the username and context
     *
     * @return true validated, false othwerwise
     */
    private boolean validateInput() {
        if (groupIdTextField.getText().length() < 3) {
                   JOptionPane.showMessageDialog(this, resources.getString("groupIdTextField.error.empty"),
                                   resources.getString("groupIdTextField.error.title"),
                                   JOptionPane.ERROR_MESSAGE);
                   return false;
               }
         return true;
    }

}
