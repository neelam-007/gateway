package com.l7tech.console.panels;

import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.text.MaxLengthDocument;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UserPanel - edits the <CODE>User/CODE> instances.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class UserPanel extends EntityEditorPanel {
    static Logger log = Logger.getLogger(UserPanel.class.getName());
    final static String USER_ICON_RESOURCE = "com/l7tech/console/resources/user16.png";

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
    private UserGroupsPanel groupPanel; // membership
    private CertificatePanel certPanel; //certificate
    // Apply/Revert buttons
    private JButton okButton;
    private JButton cancelButton;

    // user
    private EntityHeader userHeader;
    private UserBean user;
    private Set userGroups;

    private boolean formModified;

    // Titles/Labels
    private static final String DETAILS_LABEL = "General";
    private static final String MEMBERSHIP_LABEL = "Membership";
    private static final String CERTIFICATE_LABEL = "Certificate";

    private static final String OK_BUTTON = "OK";
    private static final String CANCEL_BUTTON = "Cancel";
    private final static String CHANGE_PASSWORD_LABEL = "Change Password";
    private JLabel emailLabel;
    private JTextField emailTextField;
    private JButton changePassButton;
    private final String USER_DOES_NOT_EXIST_MSG = "This user no longer exists";

    private IdentityProvider idProvider;
    private final MainWindow mainWindow = TopComponents.getInstance().getMainWindow();

    private final ActionListener closeDlgListener = new ActionListener() {
                                                      public void actionPerformed(ActionEvent e) {
                                                          SwingUtilities.windowForComponent(UserPanel.this).dispose();
                                                      }
                                                    };


    /**
     * default constructor
     */
    public UserPanel() {
    }

    public void initialize() {
        try {
            // Initialize form components
            groupPanel = new UserGroupsPanel(this);
            certPanel = new CertificatePanel(this, passwordChangeListener);
            layoutComponents();
            this.addHierarchyListener(hierarchyListener);
        } catch (Exception e) {
            log.log(Level.SEVERE, "GroupPanel()", e);
            e.printStackTrace();
        }
    }

    /**
     * Enables or disables the buttons based
     * on whether or not data on the form has been changed
     */
    void setModified(boolean b) {
        formModified = b;
    }

    /**
     * Constructs the panel
     * 
     * @param userHeader 
     * @param idProvider 
     */
    public void edit(EntityHeader userHeader, IdentityProvider idProvider) {
        this.idProvider = idProvider;
        edit(userHeader);
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

            userHeader = (EntityHeader)object;

            if (!EntityType.USER.equals(userHeader.getType())) {
                throw new IllegalArgumentException("Invalid argument type: "
                  + "\nExpected: User "
                  + "\nReceived: " + userHeader.getType());
            }

            if (idProvider == null) {
                throw new RuntimeException("User edit operation without specified identity provider.");
            }

            boolean isNew = userHeader.getOid() == 0;
            if (isNew) {
                user = new UserBean();
                user.setName(userHeader.getName());
                userGroups = null;
            } else {
                User u = idProvider.getUserManager().findByPrimaryKey(userHeader.getStrId());
                if (u == null) {
                    JOptionPane.showMessageDialog(mainWindow, USER_DOES_NOT_EXIST_MSG, "Warning", JOptionPane.WARNING_MESSAGE);
                    throw new NoSuchElementException("User missing " + userHeader.getOid());
                }
                user = u.getUserBean();
                userGroups = idProvider.getGroupManager().getGroupHeaders(u.getUniqueIdentifier());
            }
            // Populate the form for insert/update
            initialize();
            setData(user);
        } catch (FindException e) {
            ErrorManager.getDefault().notify(Level.SEVERE, e, "Error while editing user " + userHeader.getName());
        }
    }

    /**
     * Retrieve the <code>USer</code> this panel is editing.
     * It is a convenience, and package private method, for
     * interested panels.
     * 
     * @return the user that this panel is currently editing
     */
    UserBean getUser() {
        return user;
    }

    Set getUserGroups() {
        if (userGroups == null) userGroups = new HashSet();
        return userGroups;
    }

    public IdentityProvider getProvider() {
        return idProvider;
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

        if (idProvider.isReadOnly()) firstNameTextField.setEnabled(false);

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

        if (idProvider.isReadOnly()) lastNameTextField.setEnabled(false);

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

        if (idProvider.isReadOnly()) emailTextField.setEnabled(false);

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
            if (idProvider.isReadOnly()) {
                okButton.addActionListener(closeDlgListener);
            } else {
                okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // Apply changes if possible
                    if (!collectAndSaveChanges()) {
                        // Error - just return
                        return;
                    }
                    Window dlg = SwingUtilities.windowForComponent(UserPanel.this);
                    dlg.setVisible(false);
                    dlg.dispose();
                }
            });
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
     * Create the Change password button
     */
    private JButton getChangePassButton() {
        final UserPanel userPanel = this;

        if (changePassButton == null) {
            changePassButton = new JButton(CHANGE_PASSWORD_LABEL);

            changePassButton.
              addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent e) {
                      new PasswordDialog(
                        mainWindow, userPanel,
                        user, passwordChangeListener).show();
                      // Refresh the panel (since the agent's cert might have been revoked)
                  }
              });

        }
        if (idProvider.isReadOnly()) changePassButton.setEnabled(false);
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
                    idProvider.getUserManager().findByPrimaryKey(userHeader.getStrId()).getUserBean();
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
    private void setData(UserBean user) {
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
     * @return User   the instance with changes applied
     */
    private UserBean collectChanges() {
        user.setLastName(this.getLastNameTextField().getText());
        user.setFirstName(this.getFirstNameTextField().getText());
        user.setEmail(getEmailTextField().getText());
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
                idProvider.getUserManager().update(user, userGroups);
            } else {
                id = idProvider.getUserManager().save(user, userGroups);
                userHeader.setStrId(id);
            }

            // Cleanup
            formModified = false;
        } catch (ObjectNotFoundException e) {
            JOptionPane.showMessageDialog(mainWindow, USER_DOES_NOT_EXIST_MSG, "Warning", JOptionPane.WARNING_MESSAGE);
            result = false;
        } catch (ObjectModelException e) {
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

        UserPanel panel = new UserPanel();
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
                  if (UserPanel.this.isDisplayable()) {
                      JDialog d = (JDialog)SwingUtilities.windowForComponent(UserPanel.this);
                      if (d != null) {
                          d.setTitle(userHeader.getName() + " Properties");
                      }
                  }
              }
          }
      };


}


