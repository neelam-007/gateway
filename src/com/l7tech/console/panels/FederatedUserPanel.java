package com.l7tech.console.panels;

import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.text.MaxLengthDocument;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ObjectNotFoundException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UserPanel - edits the <CODE>User/CODE> instances.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class FederatedUserPanel extends UserPanel {
    static Logger log = Logger.getLogger(FederatedUserPanel.class.getName());
    final static String USER_ICON_RESOURCE = "com/l7tech/console/resources/user16.png";

    private JLabel nameLabel;

    private JLabel firstNameLabel;
    private JTextField firstNameTextField;

    private JLabel lastNameLabel;
    private JTextField lastNameTextField;

    private JLabel loginLabel;
    private JTextField loginTextField;

    private JLabel x509SubjectNameLabel;
    private JTextField x509SubjectNameTextField;

    // Panels always present
    private JPanel mainPanel;
    private JPanel detailsPanel;
    private JPanel buttonPanel;

    private JTabbedPane tabbedPane;
    private UserGroupsPanel groupPanel; // membership
    private UserCertPanel certPanel; //certificate
    // Apply/Revert buttons
    private JButton okButton;
    private JButton cancelButton;

    // Titles/Labels
    private static final String DETAILS_LABEL = "General";
    private static final String MEMBERSHIP_LABEL = "Membership";
    private static final String CERTIFICATE_LABEL = "Certificate";

    private static final String OK_BUTTON = "OK";
    private static final String CANCEL_BUTTON = "Cancel";
    private JLabel emailLabel;
    private JTextField emailTextField;
    private final String USER_DOES_NOT_EXIST_MSG = "This user no longer exists";

    private final MainWindow mainWindow = TopComponents.getInstance().getMainWindow();

    private final ActionListener closeDlgListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            SwingUtilities.windowForComponent(FederatedUserPanel.this).dispose();
        }
    };

    public void initialize() {
        try {
            // Initialize form components
            groupPanel = new UserGroupsPanel(this, config);

            certPanel = new UserCertPanel(this);
            layoutComponents();
            this.addHierarchyListener(hierarchyListener);
        } catch (Exception e) {
            log.log(Level.SEVERE, "GroupPanel()", e);
            e.printStackTrace();
        }
    }

    public boolean certExist() {
        return certPanel.certExist();
    }

    /**
     * Retrieves the Group and constructs the Panel
     *
     * @param object
     * @throws NoSuchElementException if the user cannot be retrieved
     */
    public void edit(Object object) throws NoSuchElementException {
        try {
            // Here is where we would use the node context to retrieve Panel content
            if (!(object instanceof EntityHeader)) {
                throw new IllegalArgumentException("Invalid argument type: "
                        + "\nExpected: EntityHeader"
                        + "\nReceived: " + object.getClass().getName());
            }

            userHeader = (EntityHeader) object;

            if (!EntityType.USER.equals(userHeader.getType())) {
                throw new IllegalArgumentException("Invalid argument type: "
                        + "\nExpected: User "
                        + "\nReceived: " + userHeader.getType());
            }

            if (config == null) {
                throw new RuntimeException("User edit operation without specified identity provider.");
            }

            boolean isNew = userHeader.getOid() == 0;
            if (isNew) {
                user = new UserBean();
                user.setName(userHeader.getName());
                userGroups = null;
            } else {
                IdentityAdmin admin = getIdentityAdmin();
                User u = admin.findUserByPrimaryKey(config.getOid(), userHeader.getStrId());
                if (u == null) {
                    JOptionPane.showMessageDialog(mainWindow, USER_DOES_NOT_EXIST_MSG, "Warning", JOptionPane.WARNING_MESSAGE);
                    throw new NoSuchElementException("User missing " + userHeader.getOid());
                }
                user = u.getUserBean();
                userGroups = admin.getGroupHeaders(config.getOid(), u.getUniqueIdentifier());
            }
            // Populate the form for insert/update
            initialize();
            setData(user);
        } catch (Exception e) {
            ErrorManager.getDefault().notify(Level.SEVERE, e, "Error while editing user " + userHeader.getName());
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

        detailsPanel.add(getX509SubjectNameLabel(),
                new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST,
                        GridBagConstraints.NONE,
                        new Insets(10, 10, 0, 0), 0, 0));

        detailsPanel.add(getX509SubjectNameTextField(),
                new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
                        GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL,
                        new Insets(10, 15, 0, 10), 0, 0));

        detailsPanel.add(getLoginLabel(),
                new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST,
                        GridBagConstraints.NONE,
                        new Insets(10, 10, 0, 0), 0, 0));

        detailsPanel.add(getLoginTextField(),
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

        detailsPanel.add(getFirstNameLabel(),
                new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST,
                        GridBagConstraints.NONE,
                        new Insets(10, 10, 0, 0), 0, 0));

        detailsPanel.add(getFirstNameTextField(),
                new GridBagConstraints(1, 5, 1, 1, 1.0, 0.0,
                        GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL,
                        new Insets(10, 15, 0, 10), 0, 0));

        detailsPanel.add(getLastNameLabel(),
                new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST,
                        GridBagConstraints.NONE,
                        new Insets(10, 10, 0, 0), 0, 0));

        detailsPanel.add(getLastNameTextField(),
                new GridBagConstraints(1, 6, 1, 1, 1.0, 0.0,
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
           new GridBagConstraints(0, 13, 2, 1, 1.0, 1.0,
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
     * Returns loginLabel
     */
    private JLabel getLoginLabel() {
        // If label not already created
        if (loginLabel != null) return loginLabel;

        // Create label
        loginLabel = new JLabel("Login:");


        // Return label
        return loginLabel;
    }

    /**
     * Returns lastNameTextField
     */
    private JTextField getLoginTextField() {
        // If text field not already created
        if (loginTextField == null) {
            // Create text field
            loginTextField = new JTextField();
            loginTextField.setMinimumSize(new Dimension(200, 20));
            loginTextField.setPreferredSize(new Dimension(200, 20));
            loginTextField.setEditable(true);
            loginTextField.setDocument(new MaxLengthDocument(50));
            // Register listeners
            loginTextField.getDocument().addDocumentListener(documentListener);
        }

        loginTextField.setEnabled(config.isWritable());

        // Return text field
        return loginTextField;
    }

    /**
     * Returns x509SubjectNameLabel
     */
    private JLabel getX509SubjectNameLabel() {
        // If label not already created
        if (x509SubjectNameLabel != null) return x509SubjectNameLabel;

        // Create label
        x509SubjectNameLabel = new JLabel("X509 Subject Name:");


        // Return label
        return x509SubjectNameLabel;
    }

    /**
     * Returns x509SubjectNameTextField
     */
    private JTextField getX509SubjectNameTextField() {
        // If text field not already created
        if (x509SubjectNameTextField == null) {
            // Create text field
            x509SubjectNameTextField = new JTextField();
            x509SubjectNameTextField.setMinimumSize(new Dimension(200, 20));
            x509SubjectNameTextField.setPreferredSize(new Dimension(200, 20));
            x509SubjectNameTextField.setEditable(true);
            x509SubjectNameTextField.setDocument(new MaxLengthDocument(255));
            // Register listeners
            x509SubjectNameTextField.getDocument().addDocumentListener(documentListener);
        }

        x509SubjectNameTextField.setEnabled(config.isWritable());

        // Return text field
        return x509SubjectNameTextField;
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
            okButton = new JButton(OK_BUTTON);

            // Register listener
            if (config.isWritable()) {
                okButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        // Apply changes if possible
                        if (!collectAndSaveChanges()) {
                            // Error - just return
                            return;
                        }
                        Window dlg = SwingUtilities.windowForComponent(FederatedUserPanel.this);
                        dlg.setVisible(false);
                        dlg.dispose();
                    }
                });
            } else {
                okButton.addActionListener(closeDlgListener);
            }
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

    /**
     * Populates the form from the user bean
     *
     * @param user
     */
    private void setData(UserBean user) {
        // Set tabbed panels (add/remove extranet tab)
        nameLabel.setText(user.getName());
        getFirstNameTextField().setText(user.getFirstName());
        getLastNameTextField().setText(user.getLastName());
        getEmailTextField().setText(user.getEmail());
        getLoginTextField().setText(user.getLogin());
        getX509SubjectNameTextField().setText(user.getSubjectDn());
        setModified(false);
    }


    /**
     * Collect changes from the form into the user instance.
     *
     * @return User   the instance with changes applied
     */
    private UserBean collectChanges() {
        user.setLastName(this.getLastNameTextField().getText());
        user.setFirstName(this.getFirstNameTextField().getText());
        user.setEmail(getEmailTextField().getText());
        user.setLogin(getLoginTextField().getText());
        user.setSubjectDn(getX509SubjectNameTextField().getText());
        // user.setGroupHeaders(groupPanel.getCurrentGroups());
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
                id = user.getUniqueIdentifier();
                getIdentityAdmin().saveUser(config.getOid(), user, userGroups);
            } else {
                id = getIdentityAdmin().saveUser(config.getOid(), user, userGroups);
                userHeader.setStrId(id);
            }

            // Cleanup
            formModified = false;
        } catch (ObjectNotFoundException e) {
            JOptionPane.showMessageDialog(mainWindow, USER_DOES_NOT_EXIST_MSG, "Warning", JOptionPane.WARNING_MESSAGE);
            result = false;
        } catch (Exception e) {
            StringBuffer msg = new StringBuffer();
            msg.append("There was an error updating ");
            msg.append("User ").append(userHeader.getName()).append(".\n");
            JOptionPane.showMessageDialog(mainWindow, msg.toString(), "Error", JOptionPane.ERROR_MESSAGE);
            log.log(Level.SEVERE, "Error updating User: " + e.toString());
            result = false;
        }
        return result;
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

        FederatedUserPanel panel = new FederatedUserPanel();
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
    private final
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
                        if (FederatedUserPanel.this.isDisplayable()) {
                            JDialog d = (JDialog) SwingUtilities.windowForComponent(FederatedUserPanel.this);
                            if (d != null) {
                                d.setTitle(userHeader.getName() + " Properties");
                            }
                        }
                    }
                }
            };


}


