package com.l7tech.console.panels;

import com.l7tech.console.text.MaxLengthDocument;
import com.l7tech.console.util.IconManager;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.Group;
import com.l7tech.identity.internal.imp.GroupImp;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.imp.EntityHeaderImp;
import org.apache.log4j.Category;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.rmi.RemoteException;

/**
 * GroupPanel is the main entry point panel for the <CODE>Group</CODE>.
 */
public class GroupPanel extends EntityEditorPanel {
    private static final Category log = Category.getInstance(GroupPanel.class.getName());
    private JLabel nameLabel;
    private JLabel descriptionLabel;
    private JTextField descriptionTextField;

    // Panels always present
    private JPanel mainPanel;
    private JPanel detailsPanel;
    private JPanel buttonPanel;

    private JTabbedPane tabbedPane;
    private final GroupUsersPanel usersPanel = new GroupUsersPanel(this); // membership

    // Apply/Revert buttons
    private JButton okButton;
    private JButton cancelButton;


    // group
    private EntityHeader groupHeader;
    private Group group;

    // Titles/Labels
    private static final String DETAILS_LABEL = "General";
    private static final String MEMBERSHIP_LABEL = "Memebership";
    private static final String GROUP_DESCRIPTION_TITLE = "Description:";

    private static final String OK_BUTTON = "OK";
    private static final String CANCEL_BUTTON = "Cancel";


    /**
     * default constructor
     */
    public GroupPanel() {
        try {
            // Initialize form components
            layoutComponents();
            this.addHierarchyListener(hierarchyListener);
        } catch (Exception e) {
            log.error("GroupPanel()", e);
            e.printStackTrace();
        }
    }

    /**
     * Enables or disables the buttons based
     * on whether or not data on the form has been changed
     */
    void setModified(boolean b) {
        // If entity not already changed
        getOKButton().setEnabled(b);

    }


    /**
     * Retrieves the Group and constructs the Panel
     *
     * @param object
     */
    public void edit(Object object) {
        try {
            // Here is where we would use the node context to retrieve Panel content
            if (!(object instanceof EntityHeader)) {
                throw new IllegalArgumentException("Invalid argument type: "
                        + "\nExpected: EntityHeader"
                        + "\nReceived: " + object.getClass().getName());
            }

            groupHeader = (EntityHeader)object;

            if (!Group.class.equals(groupHeader.getType())) {
                throw new IllegalArgumentException("Invalid argument type: "
                        + "\nExpected: Group "
                        + "\nReceived: " + groupHeader.getType());
            }

            boolean isNew = groupHeader.getOid() == 0;
            if (isNew) {
                group = new GroupImp();
                group.setName(groupHeader.getName());
            } else {
                group =
                        Registry.getDefault().getInternalGroupManager().findByPrimaryKey(groupHeader.getOid());
                if (group == null) {
                    throw new RuntimeException("Group missing " + groupHeader.getOid());
                }
            }

            // Populate the form for insert/update
            setData(group);
        } catch (Exception e) {
            log.error("GroupPanel Edit Exception: " + e.toString());
        }
    }

    /**
     * Retrieve the <code>Group</code> this panel is editing.
     * It is a convenience, and package private method, for
     * interested panels.
     *
     *
     * @return the group that this panel is currently editing
     */
    Group getGroup() {
        return group;
    }


    /**
     * Initialize all form panel components
     */
    private void layoutComponents() {
        // Set layout
        this.setName("Group");
        this.setLayout(new GridBagLayout());
        this.setMaximumSize(new Dimension(380, 450));
        this.setPreferredSize(new Dimension(380, 450));

        // Add the main panel
        add(getMainPanel(),
                new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.BOTH,
                        new Insets(8, 8, 8, 8), 0, 0));
    }

    /** Returns the mainPanel */
    private JPanel getMainPanel() {
        // If panel not already created
        if (null != mainPanel) return mainPanel;

        // Create panel
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());

        // Add GroupTabbedPane
        mainPanel.add(getGroupTabbedPane(),
                new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));

        // Add buttonPanel
        mainPanel.add(getButtonPanel(),
                new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));

        // Return panel
        return mainPanel;
    }

    /** Returns tabbedPane */
    private JTabbedPane getGroupTabbedPane() {
        // If tabbed pane not already created
        if (null == tabbedPane) {
            // Create tabbed pane
            tabbedPane = new JTabbedPane();

            // Add all tabs
            tabbedPane.add(getDetailsPanel(), DETAILS_LABEL);
            tabbedPane.add(usersPanel, MEMBERSHIP_LABEL);
        }

        // Return tabbed pane
        return tabbedPane;
    }

    /** Returns detailsPanel */
    private JPanel getDetailsPanel() {
        // If panel not already created
        if (detailsPanel == null) {
            detailsPanel = new JPanel();
            detailsPanel.setLayout(new GridBagLayout());

            detailsPanel.add(new JLabel(IconManager.getInstance().getIcon(EntityType.GROUP)),
                    new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.WEST,
                            GridBagConstraints.NONE,
                            new Insets(5, 10, 0, 0), 0, 0));

            detailsPanel.add(getNameLabel(),
                    new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.WEST,
                            GridBagConstraints.NONE,
                            new Insets(10, 15, 0, 0), 0, 0));

            detailsPanel.add(new JSeparator(JSeparator.HORIZONTAL),
                    new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0,
                            GridBagConstraints.WEST,
                            GridBagConstraints.BOTH,
                            new Insets(10, 10, 0, 10), 0, 0));


            detailsPanel.add(getDescriptionLabel(),
                    new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                            GridBagConstraints.WEST,
                            GridBagConstraints.NONE,
                            new Insets(10, 10, 0, 0), 0, 0));

            detailsPanel.add(getDescriptionTextField(),
                    new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
                            GridBagConstraints.WEST,
                            GridBagConstraints.HORIZONTAL,
                            new Insets(10, 15, 0, 10), 0, 0));


            detailsPanel.add(new JSeparator(JSeparator.HORIZONTAL),
                    new GridBagConstraints(0, 11, 2, 1, 0.0, 0.0,
                            GridBagConstraints.WEST,
                            GridBagConstraints.BOTH,
                            new Insets(15, 10, 0, 10), 0, 0));

            Component strut = Box.createVerticalStrut(8);

            detailsPanel.add(strut,
                    new GridBagConstraints(0, 12, 2, 1, 1.0, 1.0,
                            GridBagConstraints.CENTER,
                            GridBagConstraints.BOTH,
                            new Insets(10, 0, 0, 0), 0, 0));

            Utilities.equalizeLabelSizes(new JLabel[]{
                getDescriptionLabel(),
            });
        }
        // Return panel
        return detailsPanel;
    }


    /** Returns descriptionLabel */
    private JLabel getNameLabel() {
        // If label not already created
        if (nameLabel != null) return nameLabel;
        // Create label
        nameLabel = new JLabel();

        // Return label
        return nameLabel;

    }


    /** Returns descriptionLabel */
    private JLabel getDescriptionLabel() {
        // If label not already created
        if (descriptionLabel == null) {
            // Create label
            descriptionLabel = new JLabel(GROUP_DESCRIPTION_TITLE);
        }

        // Return label
        return descriptionLabel;

        /*
        new JLabel(groupHeader.getName()
        */
    }

    /** Returns descriptionTextField */
    private JTextField getDescriptionTextField() {
        // If text field not already created
        if (descriptionTextField == null) {
            // Create text field
            descriptionTextField = new JTextField();
            descriptionTextField.setMinimumSize(new Dimension(200, 20));
            descriptionTextField.setPreferredSize(new Dimension(200, 20));
            descriptionTextField.setEditable(true);
            descriptionTextField.setDocument(new MaxLengthDocument(50));
            // Register listeners
            descriptionTextField.getDocument().addDocumentListener(documentListener);
        }

        // Return text field
        return descriptionTextField;
    }

    /** Returns buttonPanel */
    private JPanel getButtonPanel() {
        if (buttonPanel == null) {
            buttonPanel = new JPanel();
            buttonPanel.setLayout(new GridBagLayout());

            Component hStrut = Box.createHorizontalStrut(8);

            // add components
            buttonPanel.add(hStrut,
                    new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                            GridBagConstraints.CENTER,
                            GridBagConstraints.BOTH,
                            new Insets(0, 0, 0, 0), 0, 0));

            buttonPanel.add(getOKButton(),
                    new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER,
                            GridBagConstraints.NONE,
                            new Insets(5, 5, 5, 5), 0, 0));

            buttonPanel.add(getCancelButton(),
                    new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER,
                            GridBagConstraints.NONE,
                            new Insets(5, 5, 5, 5), 0, 0));

            JButton buttons[] = new JButton[]
            {
                getOKButton(),
                getCancelButton()
            };
            Utilities.equalizeButtonSizes(buttons);
        }
        return buttonPanel;
    }


    /** Returns okButton */
    private JButton getOKButton() {
        // If button not already created
        if (null == okButton) {
            // Create button
            okButton = new JButton(OK_BUTTON);

            // Register listener
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        // Apply changes if possible
                        if (!collectAndSaveChanges()) {
                            // Error - just return
                            return;
                        }
                        Window dlg = SwingUtilities.windowForComponent(GroupPanel.this);
                        dlg.setVisible(false);
                        dlg.dispose();
                    } catch (Exception ex) {
                        // Popup dialog with error
                    }
                }
            });
        }

        // Return button
        return okButton;
    }

    /** Returns cancelButton */
    private JButton getCancelButton() {
        // If button not already created
        if (null == cancelButton) {

            // Create button
            cancelButton = new JButton(CANCEL_BUTTON);

            // Register listener
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    SwingUtilities.windowForComponent(GroupPanel.this).dispose();
                }
            });
        }

        // Return button
        return cancelButton;
    }


    /**
     * Populates the form from the group bean
     *
     * @param group
     */
    private void setData(Group group) throws Exception {
        // Set tabbed panels (add/remove extranet tab)
        nameLabel.setText(group.getName());
        getDescriptionTextField().setText(group.getDescription());
        setModified(false);
    }


    /**
     * Collect changes from the form into the group instance.
     *
     * @return Group   the instance with changes applied
     */
    private Group collectChanges() {
        group.setDescription(this.getDescriptionTextField().getText());
        group.setMemberHeaders(usersPanel.getCurrentUsers());
        return group;
    }


    /**
     * Applies the changes on the form to the group bean and update the database;
     * Returns indication if the changes were applied successfully.
     *
     * @return boolean - the indication if the changes were applied successfully
     */
    private boolean collectAndSaveChanges() {
        boolean result = true;

        // Perform final validations
        if (!validateForm()) {
            // Error message has already been displayed - just return
            return false;
        }

        collectChanges();

        // Try adding/updating the Group
        try {
            if (groupHeader.getOid() != 0) {
                Registry.getDefault().getInternalGroupManager().update(group);
            } else {
                long id =
                        Registry.getDefault().getInternalGroupManager().save(group);
                groupHeader.setOid(id);
            }

            // Notify listener of this insert/update
            if (null != panelListener) {
                panelListener.onUpdate(groupHeader);
            }

            // Cleanup
        } catch (Exception e) {
            StringBuffer msg = new StringBuffer();
            msg.append("There was an error updating ");
            msg.append("Group ").append(groupHeader.getName()).append(".\n");
            JOptionPane.showMessageDialog(null,
                    msg.toString(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            log.error("Error updating Group: " + e.toString());
            e.printStackTrace();
            result = false;
        }
        return result;
    }


    /**
     * Validates form data and returns if group Id and description form fields
     * are valid or not.
     *
     * @return boolean indicating if the form fields are valid or not.
     */
    private boolean validateForm() {
        return true;
    }

    // debug
    public static void main(String[] args) {

        GroupPanel panel = new GroupPanel();
        EntityHeader eh = new EntityHeader();
        eh.setName("Test group");
        eh.setType(EntityType.GROUP);
        panel.edit(eh);

        panel.setPreferredSize(new java.awt.Dimension(600, 300));
        JFrame frame = new JFrame("Group panel Test");
        frame.setContentPane(panel);
        frame.pack();
        frame.setVisible(true);
    }


    /**
     * A listener to detect when Document components have changed. Once this is
     * done a flag is set to ensure that the apply changes/ revert buttons are
     * enabled.
     */
    private final DocumentListener documentListener = new DocumentListener() {
        /** Gives notification that there was an insert into the document.*/
        public void insertUpdate(DocumentEvent e) {
            setModified(true);
        }

        /** Gives notification that a portion of the document has been */
        public void removeUpdate(DocumentEvent e) {
            setModified(true);
        }

        /** Gives notification that an attribute or set of attributes changed. */
        public void changedUpdate(DocumentEvent e) {
            setModified(true);
        }
    };

    // hierarchy listener
    private final
            HierarchyListener hierarchyListener =
            new HierarchyListener() {
                /** Called when the hierarchy has been changed.*/
                public void hierarchyChanged(HierarchyEvent e) {
                    int eID = e.getID();
                    long flags = e.getChangeFlags();

                    if (eID == HierarchyEvent.HIERARCHY_CHANGED &&
                            ((flags & HierarchyEvent.DISPLAYABILITY_CHANGED) == HierarchyEvent.DISPLAYABILITY_CHANGED)) {
                        if (GroupPanel.this.isDisplayable()) {
                            JDialog d = (JDialog)SwingUtilities.windowForComponent(GroupPanel.this);
                            if (d != null) {
                                d.setTitle(groupHeader.getName() + " Properties");
                            }
                        }
                    }
                }
            };


}


