package com.l7tech.console.panels;

import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.security.rbac.AttemptedCreate;
import com.l7tech.common.security.rbac.AttemptedCreateSpecific;
import com.l7tech.common.security.rbac.AttemptedOperation;
import com.l7tech.common.security.rbac.AttemptedUpdate;
import static com.l7tech.common.security.rbac.EntityType.USER;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.text.MaxLengthDocument;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.ObjectNotFoundException;
import net.sf.nachocalendar.components.DateField;
import net.sf.nachocalendar.components.DefaultDayRenderer;
import net.sf.nachocalendar.components.DefaultHeaderRenderer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DateFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.text.DateFormat;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GenericUserPanel - edits the <CODE>Generic User/CODE> instances. This includes internal users, LDAP users.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class GenericUserPanel extends UserPanel {
    static Logger log = Logger.getLogger(GenericUserPanel.class.getName());

    private JLabel nameLabel;

    private JLabel firstNameLabel;
    private JTextField firstNameTextField;

    private JLabel lastNameLabel;
    private JTextField lastNameTextField;

    // Panels always present
    private JPanel mainPanel;
    private JPanel detailsPanel;
    private JPanel buttonPanel;

    private JTabbedPane tabbedPane;
    private UserRoleAssignmentsPanel rolesPanel;
    private UserGroupsPanel groupPanel; // membership
    private UserCertPanel certPanel; //certificate
    // Apply/Revert buttons
    private JButton okButton;
    private JButton cancelButton;

    // Titles/Labels
    private static final String DETAILS_LABEL = "General";
    private static final String MEMBERSHIP_LABEL = "Groups";
    private static final String ROLES_LABEL = "Assigned Roles";
    private static final String CERTIFICATE_LABEL = "Certificate";

    private static final String CANCEL_BUTTON = "Cancel";
    private final static String CHANGE_PASSWORD_LABEL = "Change Password";
    private JLabel emailLabel;
    private JTextField emailTextField;
    private JButton changePassButton;
    private final String USER_DOES_NOT_EXIST_MSG = "This user no longer exists";

    private JCheckBox accountNeverExpiresCheckbox;
    private DateField expireDateField;
    private JPanel expirationPanel;
    private boolean canUpdate;

    public GenericUserPanel() {
        super();
    }

    private void initialize() {
        try {
            // Initialize form components
            rolesPanel = new UserRoleAssignmentsPanel(user);
            groupPanel = new UserGroupsPanel(this, config, config.isWritable() && canUpdate);
            certPanel = new NonFederatedUserCertPanel(this, config.isWritable() ? passwordChangeListener : null, canUpdate);
            layoutComponents();
            this.addHierarchyListener(hierarchyListener);
            applyFormSecurity();
        } catch (Exception e) {
            log.log(Level.SEVERE, "GroupPanel()", e);
            e.printStackTrace();
        }
    }

    protected void applyFormSecurity() {
        // list components that are subject to security (they require the full admin role)
        firstNameTextField.setEditable(config.isWritable() && canUpdate);
        lastNameTextField.setEditable(config.isWritable() && canUpdate);
        emailTextField.setEditable(config.isWritable() && canUpdate);
        
        changePassButton.setEnabled(config.isWritable() && canUpdate);
        if (expirationPanel != null) {
            expirationPanel.setEnabled(config.isWritable() && canUpdate);
            accountNeverExpiresCheckbox.setEnabled(config.isWritable() && canUpdate);
        }
    }


    /**
     * Retrieves the Group and constructs the Panel
     *
     * @param object
     * @throws java.util.NoSuchElementException
     *          if the user cannot be retrieved
     */
    public void edit(Object object) throws NoSuchElementException {
        try {
            // Here is where we would use the node context to retrieve Panel content
            if (!(object instanceof EntityHeader)) {
                throw new IllegalArgumentException("Invalid argument type: "
                    + "\nExpected: EntityHeader"
                    + "\nReceived: " + object.getClass().getName());
            }

            userHeader = (IdentityHeader)object;

            if (!EntityType.USER.equals(userHeader.getType())) {
                throw new IllegalArgumentException("Invalid argument type: "
                    + "\nExpected: User "
                    + "\nReceived: " + userHeader.getType());
            }

            if (config == null) {
                throw new RuntimeException("User edit operation without specified identity provider.");
            }

            boolean isNew = userHeader.getOid() == 0;
            AttemptedOperation ao;
            if (isNew) {
                if (config.type().equals(IdentityProviderType.INTERNAL)) {
                    user = new InternalUser();
                } else if (config.type().equals(IdentityProviderType.LDAP)) {
                    user = new LdapUser();
                }
                user.getUserBean().setName(userHeader.getName());
                userGroups = null;
                ao = new AttemptedCreateSpecific(USER, new UserBean(config.getOid(), "<new user>"));
            } else {
                User u = getIdentityAdmin().findUserByID(config.getOid(), userHeader.getStrId());
                if (u == null) {
                    JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(),
                                                  USER_DOES_NOT_EXIST_MSG, "Warning", JOptionPane.WARNING_MESSAGE);
                    throw new NoSuchElementException("User missing " + userHeader.getOid());
                } else {
                    ao = new AttemptedUpdate(USER, u);
                }
                user = u;
                userGroups = getIdentityAdmin().getGroupHeaders(config.getOid(), u.getId());
            }
            canUpdate = Registry.getDefault().getSecurityProvider().hasPermission(ao);
            // Populate the form for insert/update
            initialize();
            setData(user);
        } catch (Exception e) {
            // fla bugfix bugzilla #1783 this is supposed to passthrough.
            if (e instanceof NoSuchElementException) throw (NoSuchElementException)e;
            else ErrorManager.getDefault().notify(Level.SEVERE, e, "Error while editing user " + userHeader.getName());
        }
    }

    private IdentityAdmin getIdentityAdmin() {
        return Registry.getDefault().getIdentityAdmin();
    }

    /**
     * Initialize all form panel components
     */
    private void layoutComponents() {
        // Set layout
        if (user != null)
            this.setName(user.getName());
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
        mainPanel.add(getTabbedPane(),
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
    private JTabbedPane getTabbedPane() {
        // If tabbed pane not already created
        if (null != tabbedPane) return tabbedPane;

        // Create tabbed pane
        tabbedPane = new JTabbedPane();

        // Add all tabs
        tabbedPane.add(getDetailsPanel(), DETAILS_LABEL);
        tabbedPane.add(rolesPanel, ROLES_LABEL);
        tabbedPane.add(groupPanel, MEMBERSHIP_LABEL);
        tabbedPane.add(certPanel, CERTIFICATE_LABEL);

        // Return tabbed pane
        return tabbedPane;
    }

    /**
     * Returns detailsPanel
     */
    private JPanel getDetailsPanel() {
        // If panel not already created
        if (detailsPanel != null) return detailsPanel;

        detailsPanel = new JPanel();
        detailsPanel.setLayout(new GridBagLayout());

        detailsPanel.add(new JLabel(new ImageIcon(ImageCache.getInstance().getIcon(USER_ICON_RESOURCE))),
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

        detailsPanel.add(getFirstNameLabel(),
          new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(10, 10, 0, 0), 0, 0));

        detailsPanel.add(getFirstNameTextField(),
          new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(10, 15, 0, 10), 0, 0));

        detailsPanel.add(getLastNameLabel(),
          new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(10, 10, 0, 0), 0, 0));

        detailsPanel.add(getLastNameTextField(),
          new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(10, 15, 0, 10), 0, 0));

        detailsPanel.add(getEmailLabel(),
          new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(10, 10, 0, 0), 0, 0));

        detailsPanel.add(getEmailTextField(),
          new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(10, 15, 0, 10), 0, 0));

        detailsPanel.add(new JSeparator(JSeparator.HORIZONTAL),
          new GridBagConstraints(0, 11, 2, 1, 0.0, 0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.BOTH,
            new Insets(15, 10, 0, 10), 0, 0));

        detailsPanel.add(getChangePassButton(),
          new GridBagConstraints(1, 12, 1, 1, 0.0, 0.0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(15, 10, 0, 10), 0, 0));

        if (IdentityProviderType.INTERNAL.equals(config.type())) {
            // add account expiration here
            detailsPanel.add(getExpirationPanel(),
              new GridBagConstraints(0, 13, 2, 1, 1.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(0, 10, 0, 10), 0, 0));
        }

        Component strut = Box.createGlue();

        detailsPanel.add(strut,
          new GridBagConstraints(0, 14, 2, 1, 1.0, 1.0,
            GridBagConstraints.CENTER,
            GridBagConstraints.BOTH,
            new Insets(10, 0, 0, 0), 0, 0));

        Utilities.equalizeLabelSizes(new JLabel[]{
                getLastNameLabel(),
              });

        // Return panel
        return detailsPanel;
    }


    /**
     * Returns lastNameLabel
     */
    private JLabel getNameLabel() {
        // If label not already created
        if (nameLabel != null) return nameLabel;
        // Create label
        nameLabel = new JLabel();

        // Return label
        return nameLabel;
    }


    /**
     * Returns firstNameLabel
     */
    private JLabel getFirstNameLabel() {
        // If label not already created
        if (firstNameLabel != null) return firstNameLabel;

        // Create label
        firstNameLabel = new JLabel("First Name:");

        // Return label
        return firstNameLabel;
    }

    /**
     * Returns lastNameTextField
     */
    private JTextField getFirstNameTextField() {
        // If text field not already created
        if (firstNameTextField == null) {
            // Create text field
            firstNameTextField = new JTextField();
            firstNameTextField.setMinimumSize(new Dimension(200, 20));
            firstNameTextField.setPreferredSize(new Dimension(200, 20));
            firstNameTextField.setEditable(true);
            firstNameTextField.setDocument(new MaxLengthDocument(50));
            // Register listeners
            firstNameTextField.getDocument().addDocumentListener(documentListener);
        }

        firstNameTextField.setEnabled(config.isWritable());

        // Return text field
        return firstNameTextField;
    }


    /**
     * Returns lastNameLabel
     */
    private JLabel getLastNameLabel() {
        // If label not already created
        if (lastNameLabel != null) return lastNameLabel;

        // Create label
        lastNameLabel = new JLabel("Last Name:");


        // Return label
        return lastNameLabel;
    }

    /**
     * Returns lastNameTextField
     */
    private JTextField getLastNameTextField() {
        // If text field not already created
        if (lastNameTextField == null) {
            // Create text field
            lastNameTextField = new JTextField();
            lastNameTextField.setMinimumSize(new Dimension(200, 20));
            lastNameTextField.setPreferredSize(new Dimension(200, 20));
            lastNameTextField.setEditable(true);
            lastNameTextField.setDocument(new MaxLengthDocument(50));
            // Register listeners
            lastNameTextField.getDocument().addDocumentListener(documentListener);
        }

        lastNameTextField.setEnabled(config.isWritable());

        // Return text field
        return lastNameTextField;
    }


    /**
     * Returns email Label
     */
    private JLabel getEmailLabel() {
        // If label not already created
        if (emailLabel != null) return emailLabel;

        // Create label
        emailLabel = new JLabel("Email:");


        // Return label
        return emailLabel;
    }

    /**
     * Returns email TextField
     */
    private JTextField getEmailTextField() {
        // If text field not already created
        if (emailTextField == null) {
            // Create text field
            emailTextField = new JTextField();
            emailTextField.setMinimumSize(new Dimension(200, 20));
            emailTextField.setPreferredSize(new Dimension(200, 20));
            emailTextField.setEditable(true);
            emailTextField.setDocument(new MaxLengthDocument(50));
            // Register listeners
            emailTextField.getDocument().addDocumentListener(documentListener);
        }

        emailTextField.setEnabled(config.isWritable());

        // Return text field
        return emailTextField;
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

    private JPanel getExpirationPanel() {
        expirationPanel = new JPanel();
        if (IdentityProviderType.INTERNAL.equals(config.type())) {
            InternalUser iu = (InternalUser)user;
            expirationPanel.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.WEST;
            final JLabel expiresLabel = new JLabel("Expires on:");
            accountNeverExpiresCheckbox = new JCheckBox("Account Never Expires");
            accountNeverExpiresCheckbox.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    final boolean enable = !accountNeverExpiresCheckbox.isSelected();
                    expireDateField.setEnabled(enable);
                    expiresLabel.setEnabled(enable);
                }
            });
            expirationPanel.add(accountNeverExpiresCheckbox, c);
            JPanel expireOndatePanel = new JPanel();
            expireOndatePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
            expireOndatePanel.add(expiresLabel);
            expireDateField = new DateField(new DateFormatter(DateFormat.getDateInstance(DateFormat.MEDIUM)));
            expireDateField.setRenderer(new DefaultDayRenderer());
            expireDateField.setHeaderRenderer(new DefaultHeaderRenderer());
            expireOndatePanel.add(expireDateField);
            c.gridx++;
            expirationPanel.add(expireOndatePanel, c);
            final boolean neverExpires = iu.getExpiration() == -1;
            if (!neverExpires) {
                expireDateField.setValue(new Date(iu.getExpiration()));
            }
            accountNeverExpiresCheckbox.setSelected(neverExpires);

            accountNeverExpiresCheckbox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setModified(true);
                }
            });

            expireDateField.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    setModified(true);
                }
            });
        }
        return expirationPanel;
    }

    /**
     * Create the Change password button
     */
    private JButton getChangePassButton() {
        final GenericUserPanel userPanel = this;

        if (changePassButton == null) {
            changePassButton = new JButton(CHANGE_PASSWORD_LABEL);

            changePassButton.
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DialogDisplayer.display(new PasswordDialog(TopComponents.getInstance().getTopParent(), userPanel,
                      user.getUserBean(), passwordChangeListener));
                    // Refresh the panel (since the Bridge's cert might have been revoked)
                }
            });

        }
        changePassButton.setEnabled(config.isWritable());
        return changePassButton;
    }

    private EntityListener
    passwordChangeListener = new EntityListenerAdapter() {
        /**
         * Fired when an set of children is updated.
         *
         * @param ev event describing the action
         */
        public void entityUpdated(EntityEvent ev) {
            try {
                user =
                getIdentityAdmin().findUserByID(config.getOid(), userHeader.getStrId());
                user = collectChanges();
                boolean b = formModified;
                setData(user);
                setModified(b);
            } catch (Exception ex) {
                ErrorManager.getDefault().notify(Level.WARNING, ex, "Error retrieving the user " + userHeader.getStrId());
            }

        }
    };

    public boolean certExist() {
        return certPanel.certExist();
    }

    /**
     * Populates the form from the user bean
     *
     * @param user
     */
    private void setData(User user) {
        // Set tabbed panels (add/remove extranet tab)
        nameLabel.setText(user.getName());
        getFirstNameTextField().setText(user.getFirstName());
        getLastNameTextField().setText(user.getLastName());
        getEmailTextField().setText(user.getEmail());
        setModified(false);
    }


    /**
     * Collect changes from the form into the user instance.
     *
     * @return User the instance with changes applied
     */
    private User collectChanges() {
        if (user instanceof InternalUser) {
            InternalUser iu = (InternalUser)user;
            iu.setLastName(this.getLastNameTextField().getText());
            iu.setFirstName(this.getFirstNameTextField().getText());
            iu.setEmail(getEmailTextField().getText());
            if (!expireDateField.isEnabled()) {
                iu.setExpiration(-1);
            } else {
                iu.setExpiration(((Date)expireDateField.getValue()).getTime());
            }
        } else if (user instanceof LdapUser) {
            LdapUser lu = (LdapUser)user;
            lu.setLastName(this.getLastNameTextField().getText());
            lu.setFirstName(this.getFirstNameTextField().getText());
            lu.setEmail(getEmailTextField().getText());
        } else {
            throw new RuntimeException("Unsupported user type: " + user.getClass().getName()); // should not happen
        }
        return user;
    }


    /**
     * Applies the changes on the form to the user bean and update the database;
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

        // Try adding/updating the User
        try {
            String id;
            if (userHeader.getStrId() != null) {
                getIdentityAdmin().saveUser(config.getOid(), user, userGroups);
            } else {
                id = getIdentityAdmin().saveUser(config.getOid(), user, userGroups);
                userHeader.setStrId(id);
            }

            // Cleanup
            formModified = false;
        } catch (ObjectNotFoundException e) {
            JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(), USER_DOES_NOT_EXIST_MSG, "Warning", JOptionPane.WARNING_MESSAGE);
            result = false;
        } catch (Exception e) {  // todo rethrow as runtime and handle with ErrorHandler em
            StringBuffer msg = new StringBuffer();
            msg.append("There was an error updating ");
            msg.append("User ").append(userHeader.getName()).append(".\n");
            JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(), msg.toString(), "Error", JOptionPane.ERROR_MESSAGE);
            log.log(Level.SEVERE, "Error updating User: " + e.toString(), e);
            result = false;
        }
        return result;
    }


    private final ActionListener closeDlgListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            Utilities.dispose(Utilities.getRootPaneContainerAncestor(GenericUserPanel.this));
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
            if (user instanceof PersistentUser) {
                PersistentUser puser = (PersistentUser) user;
                AttemptedOperation ao;
                if (puser.getOid() == PersistentUser.DEFAULT_OID) {
                    ao = new AttemptedCreate(USER);
                } else {
                    ao = new AttemptedUpdate(USER, puser);
                }
                if (config.isWritable() && Registry.getDefault().getSecurityProvider().hasPermission(ao)) {
                    // Apply changes if possible
                    if (!collectAndSaveChanges()) {
                        // Error - just return
                        return;
                    }
                }
            }
            Utilities.dispose(Utilities.getRootPaneContainerAncestor(GenericUserPanel.this));
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

    /**
     * Validates form data and returns if user Id and description form fields
     * are valid or not.
     *
     * @return boolean indicating if the form fields are valid or not.
     */
    private boolean validateForm() {
        return true;
    }

    // debug
    public static void main(String[] args) {

        GenericUserPanel panel = new GenericUserPanel();
        EntityHeader eh = new EntityHeader();
        eh.setName("Test user");
        eh.setType(EntityType.USER);
        panel.edit(eh);

        panel.setPreferredSize(new Dimension(600, 300));
        JFrame frame = new JFrame("Group panel Test");
        frame.setContentPane(panel);
        frame.pack();
        frame.setVisible(true);
    }


    // hierarchy listener
    private final HierarchyListener hierarchyListener =
      new HierarchyListener() {
        /**
         * Called when the hierarchy has been changed.
         */
        public void hierarchyChanged(HierarchyEvent e) {
            int eID = e.getID();
            long flags = e.getChangeFlags();

            if (eID == HierarchyEvent.HIERARCHY_CHANGED &&
                ((flags & HierarchyEvent.DISPLAYABILITY_CHANGED) == HierarchyEvent.DISPLAYABILITY_CHANGED)) {
                if (GenericUserPanel.this.isDisplayable()) {
                    Utilities.setTitle(Utilities.getRootPaneContainerAncestor(GenericUserPanel.this),
                                       userHeader.getName() + " Properties");
                }
            }
        }
      };


}


