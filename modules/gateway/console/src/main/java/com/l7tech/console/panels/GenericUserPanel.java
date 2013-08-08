package com.l7tech.console.panels;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.jcalendar.JDateTimeChooser;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gateway.common.security.rbac.AttemptedCreateSpecific;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.objectmodel.EntityType.USER;

/**
 * GenericUserPanel - edits the <CODE>Generic User/CODE> instances. This includes internal users, LDAP users.
 *
 * @author Emil Marceta
 */
public class GenericUserPanel extends UserPanel {
    static Logger log = Logger.getLogger(GenericUserPanel.class.getName());

    private JTextArea nameLabel;
    private JCheckBox enabledCheckBox;
    private JScrollPane nameScrollPane;

    private JLabel firstNameLabel;
    private JTextField firstNameTextField;

    private JLabel lastNameLabel;
    private JTextField lastNameTextField;

    // Panels always present
    private JPanel mainPanel;
    private JPanel detailsPanel;
    private JPanel buttonPanel;

    private JTabbedPane tabbedPane;
    private IdentityRoleAssignmentsPanel rolesPanel;
    private UserGroupsPanel groupPanel; // membership
    private UserCertPanel certPanel; //certificate
    private UserSshPanel sshPanel;
    private UserStatusPanel statusPanel;

    // Apply/Revert buttons
    private JButton okButton;
    private JButton cancelButton;

    // Titles/Labels
    private static final String DETAILS_LABEL = "General";
    private static final String MEMBERSHIP_LABEL = "Groups";
    private static final String ROLES_LABEL = "Roles";
    private static final String CERTIFICATE_LABEL = "Certificate";
    private static final String SSH_LABEL = "SSH";

    private static final String CANCEL_BUTTON = "Cancel";
    private final static String CHANGE_PASSWORD_LABEL = "Reset Password";
    private JLabel emailLabel;
    private JTextField emailTextField;
    private JButton changePassButton;
    private final String USER_DOES_NOT_EXIST_MSG = "This user no longer exists";

    private JCheckBox accountNeverExpiresCheckbox;
    private JDateTimeChooser expireTimeChooser;
    private JLabel expireStateLabel;
    private JPanel expirationPanel;
    private boolean canUpdate;
    private JLabel expiresLabel;

    public GenericUserPanel() {
        super();
        expireTimeChooser = new JDateTimeChooser(null, new Date(System.currentTimeMillis()), null, null);
    }

    private void initialize() {
        try {
            // Initialize form components
            rolesPanel = new IdentityRoleAssignmentsPanel(user, userGroups, config.isAdminEnabled());
            groupPanel = new UserGroupsPanel(this, config, config.isWritable() && canUpdate);
            certPanel = new NonFederatedUserCertPanel(this, config.isWritable() ? passwordChangeListener : null, canUpdate);
            if (config.type().equals(IdentityProviderType.INTERNAL) && user instanceof InternalUser) {
                sshPanel = new UserSshPanel((InternalUser) user, config.isWritable(), canUpdate);
            }
            statusPanel = new UserStatusPanel();
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
        enabledCheckBox.setEnabled(enabledCheckBox.isEnabled() && config.isWritable() && canUpdate);
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

            userHeader = (IdentityHeader) object;

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
                    InternalUser iu = new InternalUser();
                    iu.setName(userHeader.getName());
                    user = iu;
                } else if (config.type().equals(IdentityProviderType.LDAP)) {
                    LdapUser lu = new LdapUser();
                    lu.setName(userHeader.getName());
                    user = lu;
                }
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
            if (e instanceof NoSuchElementException) throw (NoSuchElementException) e;
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
        this.setLayout(new BorderLayout());
        this.setMaximumSize(new Dimension(600, 450));
        this.setPreferredSize(new Dimension(600, 450));

        // Add the main panel
        add(getMainPanel(), BorderLayout.CENTER);
    }

    /**
     * Returns the mainPanel
     */
    private JPanel getMainPanel() {
        // If panel not already created
        if (null != mainPanel) return mainPanel;

        // Create panel
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Add GroupTabbedPane
        mainPanel.add(getTabbedPane(), BorderLayout.CENTER);

        // Add buttonPanel
        mainPanel.add(getButtonPanel(), BorderLayout.SOUTH);

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
        tabbedPane.add(rolesPanel, ROLES_LABEL);  tabbedPane.setEnabledAt(1,rolesPanel.isEnabled());
        tabbedPane.add(groupPanel, MEMBERSHIP_LABEL);
        tabbedPane.add(certPanel, CERTIFICATE_LABEL);
        if (config.type().equals(IdentityProviderType.INTERNAL) && sshPanel != null) {
            tabbedPane.add(sshPanel, SSH_LABEL);
        }

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

        Box headerPanel = Box.createHorizontalBox();

        JLabel imageLabel = new JLabel(new ImageIcon(ImageCache.getInstance().getIcon(USER_ICON_RESOURCE)));
        imageLabel.setBorder(BorderFactory.createEmptyBorder(16, 8, 8, 8));
        headerPanel.add(imageLabel);

        headerPanel.add(getNameScrollPane());
        headerPanel.add(getEnabledCheckBox());
        headerPanel.add(Box.createHorizontalStrut(10));

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
                            GridBagConstraints.HORIZONTAL,
                            new Insets(0, 10, 0, 10), 0, 0));
        }

        detailsPanel.add(Box.createVerticalGlue(),
                new GridBagConstraints(0, 14, 2, 1, 1.0, 1.0,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.VERTICAL,
                        new Insets(10, 0, 0, 0), 0, 0));

        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusPanel.getStatusButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Registry.getDefault().getIdentityAdmin().activateUser(user);
                    populateStatus(user);
                } catch (FindException e1) {
                    JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(), e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    log.log(Level.SEVERE, "Error updating User: " + e.toString(), e);
                } catch (UpdateException e1) {
                    JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(), e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    log.log(Level.SEVERE, "Error updating User: " + e.toString(), e);
                }
            }
        });

        final JPanel headerStatusPanel = new JPanel();
        headerStatusPanel.setLayout(new BorderLayout());
        headerStatusPanel.add(headerPanel, BorderLayout.NORTH);
        headerStatusPanel.add(statusPanel, BorderLayout.SOUTH);

        JPanel outerDetailsPanel = new JPanel();
        outerDetailsPanel.setLayout(new BorderLayout());
        outerDetailsPanel.add(headerStatusPanel, BorderLayout.NORTH);
        outerDetailsPanel.add(detailsPanel, BorderLayout.CENTER);
        detailsPanel = outerDetailsPanel;

        // Return panel
        return detailsPanel;
    }


    /**
     * Returns name
     */
    private JTextArea getNameLabel() {
        // If label not already created
        if (nameLabel != null) return nameLabel;
        // Create label
        nameLabel = new JTextArea(1, 0);
        nameLabel.setBorder(BorderFactory.createEmptyBorder());
        nameLabel.setOpaque(false);
        nameLabel.setEditable(false);

        // Return label
        return nameLabel;
    }

    /**
     * Returns name scroll pane
     */
    private JScrollPane getNameScrollPane() {
        // If scroll pane not already created
        if (nameScrollPane != null) return nameScrollPane;

        // create
        nameScrollPane = new JScrollPane(getNameLabel(),
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        nameScrollPane.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        return nameScrollPane;
    }

    private JCheckBox getEnabledCheckBox() {
        // If scroll pane not already created
        if (enabledCheckBox != null) return enabledCheckBox;

        // create
        enabledCheckBox = new JCheckBox("Enabled");

        if (user instanceof InternalUser) {
            InternalUser iu = (InternalUser) user;

            enabledCheckBox.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
            enabledCheckBox.setEnabled(System.currentTimeMillis() < iu.getExpiration() || iu.getExpiration() == -1);
            enabledCheckBox.setSelected(iu.isEnabled());
            enabledCheckBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    setModified(true);
                    enableDisableComponents();
                }
            });

            return enabledCheckBox;
        }
        return enabledCheckBox;
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
            firstNameTextField.setDocument(new MaxLengthDocument(32));
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
            lastNameTextField.setDocument(new MaxLengthDocument(32));
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
            emailTextField.setDocument(new MaxLengthDocument(128));
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
            InternalUser iu = (InternalUser) user;
            expirationPanel.setLayout(new BoxLayout(expirationPanel, BoxLayout.Y_AXIS));
            Box topPanel = Box.createHorizontalBox();
            Box botPanel = Box.createHorizontalBox();
            Box upperTopPanel = Box.createHorizontalBox();

            expirationPanel.add(upperTopPanel);
            expirationPanel.add(topPanel);
            expirationPanel.add(botPanel);

            expiresLabel = new JLabel("Expires on:");
            accountNeverExpiresCheckbox = new JCheckBox("Account Never Expires");
            accountNeverExpiresCheckbox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    enableDisableComponents();
                }
            });

            topPanel.add(accountNeverExpiresCheckbox);
            botPanel.add(expiresLabel);
            botPanel.add(Box.createHorizontalStrut(8));
            expireTimeChooser.getJCalendar().setDecorationBackgroundVisible(true);
            expireTimeChooser.getJCalendar().setDecorationBordersVisible(false);
            expireTimeChooser.getJCalendar().setWeekOfYearVisible(false);
            expireTimeChooser.setPreferredSize(new Dimension(170, 20));
            expireTimeChooser.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    setModified(true);
                    enableDisableComponents();
                }
            });

            expireStateLabel = new JLabel("Expired");

            JPanel datePanel = new JPanel();
            datePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
            datePanel.add(expireTimeChooser);
            datePanel.add(expireStateLabel);
            botPanel.add(datePanel);
            boolean expired = false;
            final boolean neverExpires = iu.getExpiration() == -1;
            if (!neverExpires) {
                expireTimeChooser.setDate(new Date(iu.getExpiration()));
                long now = System.currentTimeMillis();
                if (now > iu.getExpiration()) {
                    expired = true;
                    expireTimeChooser.getDateEditor().getUiComponent().setBackground(Color.RED);
                }
            }
            expireStateLabel.setVisible(expired);
            accountNeverExpiresCheckbox.setSelected(neverExpires);

            accountNeverExpiresCheckbox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setModified(true);
                }
            });

            topPanel.add(Box.createHorizontalGlue());
            botPanel.add(Box.createHorizontalGlue());
            upperTopPanel.add(Box.createHorizontalGlue());
            enableDisableComponents();
        }
        return expirationPanel;
    }

    private void enableDisableComponents() {
        final boolean neverExpire = accountNeverExpiresCheckbox.isSelected();
        long now = System.currentTimeMillis();
        boolean notExpired = expireTimeChooser.getDate() == null? false : expireTimeChooser.getDate().after(new Date(now));
        expireTimeChooser.setEnabled(!neverExpire);
        expireTimeChooser.getDateEditor().getUiComponent().setBackground(notExpired || neverExpire ? Color.WHITE : Color.RED);
        expiresLabel.setEnabled(neverExpire);
        expireStateLabel.setVisible(!notExpired && !neverExpire);
        enabledCheckBox.setEnabled(neverExpire || notExpired);
        enableDisableStatus(enabledCheckBox.isSelected() && (neverExpire || notExpired));
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
                            if (user instanceof InternalUser) {
                                InternalUser iu = (InternalUser) user;
                                DialogDisplayer.display(new PasswordDialog(TopComponents.getInstance().getTopParent(), userPanel,
                                        iu, passwordChangeListener, CHANGE_PASSWORD_LABEL), new Runnable(){
                                    @Override
                                    public void run() {
                                        populateStatus(user);
                                        enableDisableComponents();
                                    }
                                });
                            } else {
                                throw new IllegalStateException("Password can only be changed for Internal users");
                            }
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
        // Set tabbed panels (add/remove extranet tab)   \
        if (user instanceof InternalUser) {
            enabledCheckBox.setSelected(((InternalUser) user).isEnabled());
        } else {
            enabledCheckBox.setVisible(false);
        }
        getNameLabel().setText(user.getName());
        getNameLabel().setCaretPosition(0);
        getFirstNameTextField().setText(user.getFirstName());
        getLastNameTextField().setText(user.getLastName());
        getEmailTextField().setText(user.getEmail());
        populateStatus(user);
        setModified(false);
    }


    /**
     * Collect changes from the form into the user instance.
     *
     * @return User the instance with changes applied
     */
    private User collectChanges() {
        if (user instanceof InternalUser) {
            InternalUser iu = (InternalUser) user;
            iu.setLastName(this.getLastNameTextField().getText());
            iu.setFirstName(this.getFirstNameTextField().getText());
            iu.setEmail(getEmailTextField().getText());
            iu.setEnabled(this.getEnabledCheckBox().isSelected());
            if (!expireTimeChooser.isEnabled()) {
                iu.setExpiration(-1);
            } else {
                iu.setExpiration(expireTimeChooser.getDate().getTime());
            }
            if (sshPanel != null) {
                // TODO store public key as one long line of Base64, without PEM headers
                iu.setProperty(InternalUser.PROPERTIES_KEY_SSH_USER_PUBLIC_KEY, sshPanel.getInternalUserPublicKey());
            }
        } else if (user instanceof LdapUser) {
            LdapUser lu = (LdapUser) user;
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
            JOptionPane.showMessageDialog(this, USER_DOES_NOT_EXIST_MSG, "Warning", JOptionPane.WARNING_MESSAGE);
            result = false;
        } catch (RuleViolationUpdateException e) {
            JOptionPane.showMessageDialog(this, ExceptionUtils.getMessage( e ), "Warning", JOptionPane.WARNING_MESSAGE);
            result = false;
        } catch (Exception e) {  // todo rethrow as runtime and handle with ErrorHandler em
            StringBuffer msg = new StringBuffer();
            msg.append("There was an error updating ");
            msg.append("User ").append(userHeader.getName()).append(".\n");
            JOptionPane.showMessageDialog(this, msg.toString(), "Error", JOptionPane.ERROR_MESSAGE);
            log.log(Level.SEVERE, "Error updating User: " + e.toString(), ExceptionUtils.getDebugException(e));
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
        if (expireTimeChooser.isEnabled() && expireTimeChooser.getDate() == null) {
            DialogDisplayer.showMessageDialog(
                    this,
                    "Please enter a valid date for the account expiry.",
                    "Invalid Account Expiry",
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    null);
            return false;
        }

        return true;
    }

    private void populateStatus(final User user) {
        if (user instanceof LdapUser ||
                user instanceof InternalUser) {

            LogonInfo.State userState = null;
            try {
                userState = Registry.getDefault().getIdentityAdmin().getLogonState(user);
            } catch (final FindException e) {
                log.log(Level.WARNING, "Unable to determine logon state for user: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
            if (userState == null) {
                userState = LogonInfo.State.ACTIVE;
            }

            statusPanel.getDynamicStatusLabel().setText(getStateString(userState));

            statusPanel.getStatusButton().setVisible(canUpdate && userState != LogonInfo.State.ACTIVE);

            boolean isUserEnabledAndNotExpired = true;
            if (user instanceof InternalUser) {
                isUserEnabledAndNotExpired = ((InternalUser) user).isEnabled();
                long expiry = ((InternalUser) user).getExpiration();

                isUserEnabledAndNotExpired = isUserEnabledAndNotExpired && (expiry < 0 || expiry > System.currentTimeMillis());
            }
            enableDisableStatus(config.isAdminEnabled() && isUserEnabledAndNotExpired);

            String statusButtonText = null;
            if (userState == LogonInfo.State.INACTIVE)
                statusButtonText = "Activate";
            else if (userState == LogonInfo.State.EXCEED_ATTEMPT)
                statusButtonText = "Unlock";
            statusPanel.getStatusButton().setText(statusButtonText);
        } else {
            statusPanel.getStatusLabel().setVisible(false);
            statusPanel.getDynamicStatusLabel().setVisible(false);
            statusPanel.getStatusButton().setVisible(false);
        }

    }

    private void enableDisableStatus(boolean isUserEnabledAndNotExpired) {
        if (statusPanel.getStatusLabel().isVisible()) {
            statusPanel.getDynamicStatusLabel().setEnabled(isUserEnabledAndNotExpired);
            statusPanel.getStatusLabel().setEnabled(isUserEnabledAndNotExpired);
            statusPanel.getStatusButton().setEnabled(isUserEnabledAndNotExpired);
        }
    }

    private String getStateString(LogonInfo.State state) {
        switch (state) {
            case ACTIVE:
                return "Active";
            case INACTIVE:
                return "Inactive";
            case EXCEED_ATTEMPT:
                return "Locked";
            default:
                return null;
        }
    }

    // debug
    public static void main(String[] args) {

        GenericUserPanel panel = new GenericUserPanel();
        EntityHeader eh = new EntityHeader();
        eh.setOid(0);
        eh.setName("Test user");
        eh.setType(EntityType.USER);
        IdentityHeader ih = new IdentityHeader(-2, eh);
        panel.edit(ih, new IdentityProviderConfig(IdentityProviderType.INTERNAL));

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


