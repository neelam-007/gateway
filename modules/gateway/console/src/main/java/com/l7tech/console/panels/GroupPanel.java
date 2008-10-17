package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gateway.common.security.rbac.AttemptedCreateSpecific;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import static com.l7tech.objectmodel.EntityType.GROUP;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.Group;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.PersistentGroup;
import com.l7tech.identity.fed.VirtualGroup;
import com.l7tech.identity.ldap.LdapGroup;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GroupPanel is the main entry point panel for the <CODE>Group</CODE>.
 */
public abstract class GroupPanel<GT extends Group> extends EntityEditorPanel {
    static Logger log = Logger.getLogger(GroupPanel.class.getName());
    public final static String GROUP_ICON_RESOURCE = "com/l7tech/console/resources/group16.png";

    private JLabel nameLabel;
    private JLabel descriptionLabel;
    private JTextField descriptionTextField;

    // Panels always present
    private JPanel mainPanel;
    private JPanel buttonPanel;

    protected JTabbedPane tabbedPane;

    // Apply/Revert buttons
    private JButton okButton;
    private JButton cancelButton;

    // group
    private EntityHeader groupHeader;
    protected GT group;

    // Titles/Labels
    protected static final String DETAILS_LABEL = "General";
    protected static final String MEMBERSHIP_LABEL = "Membership";
    private static final String GROUP_DESCRIPTION_TITLE = "Description:";

    private static final String CANCEL_BUTTON = "Cancel";
    private static final int MAX_DESC_LENGTH = 256;
    private boolean formModified;
    private boolean wasoked = false;

    protected IdentityProviderConfig config;
    private final String GROUP_DOES_NOT_EXIST_MSG = "This group no longer exists";
    private final Frame topParent = TopComponents.getInstance().getTopParent();

    protected boolean canUpdate;

    private final ActionListener closeDlgListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            Utilities.dispose(Utilities.getRootPaneContainerAncestor(GroupPanel.this));
        }
    };


    protected GroupPanel(IdentityProviderConfig config) {
        this.config = config;
        final SecurityProvider provider = Registry.getDefault().getSecurityProvider();
        if (provider == null) {
            throw new IllegalStateException("Could not instantiate security provider");
        }
    }

    public static GroupPanel newInstance(IdentityProviderConfig config, EntityHeader header) {
        Group g;
        try {
            g = getIdentityAdmin().findGroupByID(config.getOid(), header.getStrId());

            if (g instanceof VirtualGroup) {
                return newVirtualGroupPanel(config);
            } else if (g instanceof PersistentGroup) {
                return newPhysicalGroupPanel(config);
            } else if (g instanceof LdapGroup) {
                return newPhysicalGroupPanel(config);
            } else {
                throw new RuntimeException("Can't create a GroupPanel implementation for " + g.getClass().getName());
            }
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }

    static PhysicalGroupPanel newPhysicalGroupPanel(IdentityProviderConfig config) {
        return new PhysicalGroupPanel(config);
    }

    static VirtualGroupPanel newVirtualGroupPanel(IdentityProviderConfig config) {
        return new VirtualGroupPanel(config);
    }

    abstract Set<IdentityHeader> getGroupMembers();

    /**
     * Enables or disables the buttons based
     * on whether or not data on the form has been changed
     */
    void setModified(boolean b) {
        // If entity not already changed
        formModified = true;
    }

    /**
     * Constructs the panel
     *
     * @param grpHeader
     * @param config
     */
    public void edit(EntityHeader grpHeader, IdentityProviderConfig config) {
        this.config = config;
        edit(grpHeader);
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

            if (!EntityType.GROUP.equals(groupHeader.getType())) {
                throw new IllegalArgumentException("Invalid argument type: "
                  + "\nExpected: Group "
                  + "\nReceived: " + groupHeader.getType());
            }

            if (config == null) {
                throw new RuntimeException("Group edit operation without specified identity provider.");
            }

            boolean isNew = groupHeader.getOid() == 0;
            AttemptedOperation ao;
            if (isNew) {
                group = newGroup(groupHeader);
                ao = new AttemptedCreateSpecific(GROUP, group);
            } else {
                final IdentityAdmin admin = getIdentityAdmin();
                //noinspection unchecked
                GT g = (GT) admin.findGroupByID(config.getOid(), groupHeader.getStrId());
                if (g == null) {
                    JOptionPane.showMessageDialog(topParent, GROUP_DOES_NOT_EXIST_MSG, "Warning", JOptionPane.WARNING_MESSAGE);
                    throw new NoSuchElementException("User missing " + groupHeader.getOid());
                } else {
                    ao = new AttemptedUpdate(GROUP, g);
                }
                group = g;
                loadedGroup(g);
            }
            canUpdate = Registry.getDefault().getSecurityProvider().hasPermission(ao);
            initialize();
            // Populate the form for insert/update
            setData(group);
        } catch (FindException e) {
            ErrorManager.getDefault().notify(Level.SEVERE, e, "Error while editing user " + groupHeader.getName());
        }
    }

    protected static IdentityAdmin getIdentityAdmin() {
        return Registry.getDefault().getIdentityAdmin();
    }

    protected abstract void loadedGroup(Group g) throws FindException;

    protected abstract GT newGroup(EntityHeader groupHeader);

    protected void initialize() {
        layoutComponents();
        applyFormSecurity();
        this.addHierarchyListener(hierarchyListener);
    }

    /**
     * Retrieve the <code>Group</code> this panel is editing.
     * It is a convenience, and package private method, for
     * interested panels.
     *
     * @return the group that this panel is currently editing
     */
    public Group getGroup() {
        return group;
    }

    IdentityProviderConfig getIdentityProviderConfig() {
        return config;
    }


    /**
     * Initialize all form panel components
     */
    protected void layoutComponents() {
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

    /**
     * Returns the mainPanel
     */
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


    /**
     * Returns tabbedPane
     */
    private JTabbedPane getGroupTabbedPane() {
        // If tabbed pane not already created
        if (null == tabbedPane) {
            // Create tabbed pane
            tabbedPane = new JTabbedPane();

            // Add all tabs
            tabbedPane.add(getDetailsPanel(), DETAILS_LABEL);
            tabbedPane.add(getMembershipPanel(), MEMBERSHIP_LABEL);
        }

        // Return tabbed pane
        return tabbedPane;
    }

    protected abstract JPanel getDetailsPanel();

    protected abstract JPanel getMembershipPanel();

    /**
     * Returns descriptionLabel
     */
    protected JLabel getNameLabel() {
        // If label not already created
        if (nameLabel != null) return nameLabel;
        // Create label
        nameLabel = new JLabel();

        // Return label
        return nameLabel;

    }


    /**
     * Returns descriptionLabel
     */
    protected JLabel getDescriptionLabel() {
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

    /**
     * Returns descriptionTextField
     */
    protected JTextField getDescriptionTextField() {
        // If text field not already created
        if (descriptionTextField == null) {
            // Create text field
            descriptionTextField = new JTextField();
            descriptionTextField.setMinimumSize(new Dimension(200, 20));
            descriptionTextField.setPreferredSize(new Dimension(200, 20));
            descriptionTextField.setEditable(true);
            descriptionTextField.setDocument(new MaxLengthDocument(MAX_DESC_LENGTH));
            // Register listeners
            descriptionTextField.getDocument().addDocumentListener(documentListener);
        }

        // Return text field
        return descriptionTextField;
    }

    /**
     * Returns buttonPanel
     */
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


    /**
     * Returns okButton
     */
    private JButton getOKButton() {
        // If button not already created
        if (null == okButton) {
            // Create button
            okButton = new JButton(new OkAction());
        }

        // Return button
        return okButton;
    }

    /**
     * Returns cancelButton
     */
    private JButton getCancelButton() {
        // If button not already created
        if (null == cancelButton) {

            // Create button
            cancelButton = new JButton(CANCEL_BUTTON);

            // Register listener
            cancelButton.addActionListener(closeDlgListener);
        }

        // Return button
        return cancelButton;
    }


    /**
     * Populates the form from the group bean
     *
     * @param group
     */
    protected void setData(Group group) {
        // Set tabbed panels (add/remove extranet tab)
        nameLabel.setText(group.getName());
        String desc = group.getDescription();
        if (desc != null && desc.length() > MAX_DESC_LENGTH) {
            desc = desc.substring(0, MAX_DESC_LENGTH - 1);
        }
        getDescriptionTextField().setText(desc);
        setModified(false);
    }


    /**
     * Collect changes from the form into the group instance.
     *
     * @return Group   the instance with changes applied
     */
    protected abstract Group collectChanges();

    protected void applyFormSecurity() {
        getDescriptionTextField().setEnabled(config.isWritable() && canUpdate);
    }

    /**
     * Applies the changes on the form to the group bean and update the database;
     * Returns indication if the changes were applied successfully.
     *
     * @return boolean - the indication if the changes were applied successfully
     */
    private boolean collectAndSaveChanges() {
        boolean result = true;
        if (!formModified) return true;

        // Perform final validations
        if (!validateForm()) {
            // Error message has already been displayed - just return
            return false;
        }

        collectChanges();

        // Try adding/updating the Group
        try {
            String id = save();
            if (groupHeader.getStrId() == null) {
                groupHeader.setStrId(id);
            }
        } catch (ObjectNotFoundException e) {
            JOptionPane.showMessageDialog(topParent, GROUP_DOES_NOT_EXIST_MSG, "Warning", JOptionPane.WARNING_MESSAGE);
            result = true;
        } catch (ObjectModelException e) { 
            StringBuffer msg = new StringBuffer();
            msg.append("There was an error updating ");
            msg.append("Group ").append(groupHeader.getName()).append(".\n").append(ExceptionUtils.getMessage(e)).append("\n");
            JOptionPane.showMessageDialog(topParent, msg.toString(), "Error", JOptionPane.ERROR_MESSAGE);
            log.log(Level.SEVERE, "Error updating Group: " + e.toString(), e);
            result = false;
        }
        return result;
    }

    protected abstract String save() throws SaveException, UpdateException, ObjectNotFoundException;


    /**
     * Validates form data and returns if group Id and description form fields
     * are valid or not.
     *
     * @return boolean indicating if the form fields are valid or not.
     */
    protected boolean validateForm() {
        return true;
    }

    /**
     * A listener to detect when Document components have changed. Once this is
     * done a flag is set to ensure that the apply changes/ revert buttons are
     * enabled.
     */
    private final DocumentListener documentListener = new DocumentListener() {
        /**
         * Gives notification that there was an insert into the document.
         */
        public void insertUpdate(DocumentEvent e) {
            setModified(true);
        }

        /**
         * Gives notification that a portion of the document has been
         */
        public void removeUpdate(DocumentEvent e) {
            setModified(true);
        }

        /**
         * Gives notification that an attribute or set of attributes changed.
         */
        public void changedUpdate(DocumentEvent e) {
            setModified(true);
        }
    };

    class OkAction extends SecureAction {
        protected OkAction() {
            super(null);
        }

        /**
         * Actually perform the action.
         */
        protected void performAction() {
            if (group instanceof PersistentGroup) {
                PersistentGroup pgroup = (PersistentGroup) group;
                AttemptedOperation ao;
                if (PersistentGroup.DEFAULT_OID == pgroup.getOid()) {
                    ao = new AttemptedCreate(GROUP);
                } else {
                    ao = new AttemptedUpdate(GROUP, pgroup);
                }

                if (config.isWritable() && canAttemptOperation(ao)) {
                    // Apply changes if possible
                    if (!collectAndSaveChanges()) {
                        // Error - just return
                        return;
                    }
                }
            }
            Utilities.dispose(Utilities.getRootPaneContainerAncestor(GroupPanel.this));
            wasoked = true;
        }

        /**
         * @return the action name
         */
        public String getName() {
            return "OK";
        }

        /**
         * subclasses override this method specifying the resource name
         */
        protected String iconResource() {
            return null;
        }

        /**
         * @return the aciton description
         */
        public String getDescription() {
            return null;
        }
    }


    // hierarchy listener
    protected final
    HierarchyListener hierarchyListener =
      new HierarchyListener() {
          /**
           * Called when the hierarchy has been changed.
           */
          public void hierarchyChanged(HierarchyEvent e) {
              int eID = e.getID();
              long flags = e.getChangeFlags();

              if (eID == HierarchyEvent.HIERARCHY_CHANGED &&
                ((flags & HierarchyEvent.DISPLAYABILITY_CHANGED) == HierarchyEvent.DISPLAYABILITY_CHANGED)) {
                  if (GroupPanel.this.isDisplayable()) {
                      Utilities.setTitle(Utilities.getRootPaneContainerAncestor(GroupPanel.this),
                                         groupHeader.getName() + " Properties");
                  }
              }
          }
      };

    public boolean wasOKed() {
        return wasoked;
    }
}


