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


    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// !!! IMPORTANT !!!
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * !!! IMPORTANT !!!
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        createButton = new JButton();
        createButton.setText("Create");
        panel1.add(createButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        panel1.add(cancelButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 2, new Insets(0, 5, 10, 10), -1, -1));
        mainPanel.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        groupNameTextField = new JTextField();
        groupNameTextField.setText("");
        panel2.add(groupNameTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), new Dimension(200, -1)));
        final JLabel label1 = new JLabel();
        label1.setText("Group Name:");
        panel2.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label2 = new JLabel();
        label2.setText("Description:");
        panel2.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        groupDescriptionTextField = new JTextField();
        panel2.add(groupDescriptionTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), new Dimension(300, -1)));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 2, new Insets(5, 10, 10, 10), -1, -1));
        mainPanel.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "  Group Pattern  "));
        final JLabel label3 = new JLabel();
        label3.setText("X509 Subject DN:");
        panel3.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label4 = new JLabel();
        label4.setText("Email:");
        panel3.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        x509DNPatternTextField = new JTextField();
        x509DNPatternTextField.setText("");
        panel3.add(x509DNPatternTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        emailPatternTextField = new JTextField();
        panel3.add(emailPatternTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
    }
}
