package com.l7tech.console.panels;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.fed.VirtualGroup;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.text.FilterDocument;
import com.l7tech.console.util.Registry;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.action.Actions;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.EventListener;
import java.util.logging.Level;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class NewVirtualGroupDialog extends JDialog {
    /** Resource bundle with default locale */
    private ResourceBundle resources = null;
    private String CMD_CANCEL = "cmd.cancel";
    private String CMD_OK = "cmd.ok";

    private JPanel mainPanel;
    private JTextField groupNameTextField;
    private JTextField x509DNPatternTextField;
    private JTextField emailPatternTextField;
    private JButton createButton;
    private JButton cancelButton;
    private JTextField groupDescriptionTextField;

    private JFrame parent;
    private IdentityProviderConfig ipc;
    private EventListenerList listenerList = new EventListenerList();
    GroupBean group = new GroupBean();
    private JTextArea textArea1;
    private JTextField textField1;

    /**
     * Create a new NewGroupDialog fdialog for a given Company
     *
     * @param parent  the parent Frame. May be <B>null</B>
     */
    public NewVirtualGroupDialog(JFrame parent, IdentityProviderConfig ipc) {
        super(parent, true);
        this.parent = parent;
        this.ipc = ipc;
        initResources();
        initComponents();
        pack();
        Utilities.centerOnScreen(this);
    }

    /**
     * Loads locale-specific resources: strings  etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.NewVirtualGroupDialog", locale);
    }

    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private void initComponents() {

        Container contents = getContentPane();
        JPanel panel = new JPanel();
        panel.setDoubleBuffered(true);
        contents.add(mainPanel);
        setTitle(resources.getString("dialog.title"));

        addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent event) {
            // user hit window manager close button
            windowAction(CMD_CANCEL);
        }
        });

        Actions.setEscKeyStrokeDisposes(this);

        groupNameTextField.setDocument(new FilterDocument(24,
                        new FilterDocument.Filter() {
                            public boolean accept(String str) {
                                if (str == null) return false;
                                return true;
                            }
                        }));

        createButton.setActionCommand(CMD_OK);
        createButton.addActionListener(new ActionListener() {
                   public void actionPerformed(ActionEvent event) {
                       windowAction(event.getActionCommand());
                   }
               });

        cancelButton.setActionCommand(CMD_CANCEL);
        cancelButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        windowAction(event.getActionCommand());
                    }
                });

        // equalize buttons
        Utilities.equalizeButtonSizes(new JButton[]{createButton, cancelButton});

    }

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
                groupNameTextField.requestFocus();
                return;
            }

            insertGroup();
        }
    }


    /** insert the Group */
    private void insertGroup() {
        group.setName(groupNameTextField.getText());
        group.setDescription(groupDescriptionTextField.getText());
        final VirtualGroup vGroup = new VirtualGroup(group);
        vGroup.setX509SubjectDnPattern(x509DNPatternTextField.getText());
        vGroup.setSamlEmailPattern(emailPatternTextField.getText());
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        try {
                            EntityHeader header = new EntityHeader();
                            header.setType(EntityType.GROUP);
                            header.setName(group.getName());
                            group.setUniqueIdentifier(Registry.getDefault().getIdentityAdmin().saveGroup(ipc.getOid(), vGroup, null ));
                            header.setStrId(group.getUniqueIdentifier());
                            NewVirtualGroupDialog.this.fireEventGroupAdded(header);
                        } catch (Exception e) {
                            ErrorManager.getDefault().
                              notify(Level.WARNING, e, "Error encountered while adding a group\n"+
                                     "The Group has not been created.");
                        }
                        NewVirtualGroupDialog.this.dispose();
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
     * validate the username and context
     *
     * @return true validated, false othwerwise
     */
    private boolean validateInput() {
         if(groupNameTextField.getText().length() < 3) {
                   JOptionPane.showMessageDialog(this, resources.getString("groupIdTextField.error.empty"),
                                   resources.getString("groupIdTextField.error.title"),
                                   JOptionPane.ERROR_MESSAGE);
                   return false;
               }
         return true;
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


}
