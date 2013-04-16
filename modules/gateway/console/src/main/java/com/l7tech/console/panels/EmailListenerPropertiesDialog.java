package com.l7tech.console.panels;

import com.l7tech.console.util.PasswordGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.Authorizer;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.gateway.common.transport.email.EmailListenerAdmin;
import com.l7tech.gateway.common.transport.email.EmailServerType;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 26-Jun-2008
 * Time: 4:27:22 PM
 */
public class EmailListenerPropertiesDialog extends JDialog {
    public static final String TITLE = "Email Listener Properties";
    private static final EntityHeader[] EMPTY_ENTITY_HEADER = new EntityHeader[0];
    
    /** Resource bundle with default locale */
    private ResourceBundle resources = null;
    private Logger logger = Logger.getLogger(EmailListenerPropertiesDialog.class.getName());
    

    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;
    private JTextField name;
    private JTextField hostname;
    private JSpinner port;
    private JComboBox serverType;
    private JCheckBox useSSLCheckbox;
    private JTextField folderName;
    private JButton folderBrowseButton;
    private JSpinner checkInterval;
    private JTextField username;
    private JPasswordField password;
    private JCheckBox showPasswordCheckBox;
    private JLabel plaintextPasswordWarningLabel;
    private JButton testButton;
    private JCheckBox deleteOnReceiveCheckbox;
    private JCheckBox activeCheckbox;
    private JCheckBox associateWithPublishedService;
    private JLabel serviceNameLabel;
    private JComboBox serviceNameCombo;
    private ByteLimitPanel byteLimit;
    private SecurityZoneWidget zoneControl;

    private final EmailListener emailListener;
    private InputValidator inputValidator;
    private boolean confirmed = false;


    private static class ComboItem {
        ComboItem(String name, long id) {
            serviceName = name;
            serviceID = id;
        }

        @Override
        public String toString() {
            return serviceName;
        }

        String serviceName;
        long serviceID;

        @Override
        @SuppressWarnings({ "RedundantIfStatement" })
        public boolean equals(Object o) {
           if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ComboItem comboItem = (ComboItem) o;

            if (serviceID != comboItem.serviceID) return false;
            if (serviceName != null ? !serviceName.equals(comboItem.serviceName) : comboItem.serviceName != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = (serviceName != null ? serviceName.hashCode() : 0);
            result = 31 * result + (int) (serviceID ^ (serviceID >>> 32));
            return result;
        }
    }

        private RunOnChangeListener formPreener = new RunOnChangeListener(new Runnable() {
           @Override
           public void run() {
               enableOrDisableComponents();
           }
       });

       private void enableOrDisableComponents() {
           serviceNameLabel.setEnabled(associateWithPublishedService.isSelected());
           serviceNameCombo.setEnabled(associateWithPublishedService.isSelected());
       }
    
    /**
     * Creates a new instance of EmailListenerPropertiesDialog. The fields in the dialog
     * will be set from the values in the provided EmailListener and if the dialog is
     * dismissed with the OK button, then the provided EmailListener will be updated with
     * the values from the fields in this dialog.
     *
     * @param owner The owner of this dialog window
     * @param emailListener The EmailListener to read values from and possibly update
     */
    public EmailListenerPropertiesDialog(Dialog owner, EmailListener emailListener) throws HeadlessException {
        super(owner, TITLE, true);
        this.emailListener = emailListener;
        initialize();
    }

    /**
     * Creates a new instance of EmailListenerPropertiesDialog. The fields in the dialog
     * will be set from the values in the provided EmailListener and if the dialog is
     * dismissed with the OK button, then the provided EmailListener will be updated with
     * the values from the fields in this dialog.
     *
     * @param owner The owner of this dialog window
     * @param emailListener The EmailListener to read values from and possibly update
     */
    public EmailListenerPropertiesDialog(Frame owner, EmailListener emailListener) throws HeadlessException {
        super(owner, TITLE, true);
        this.emailListener = emailListener;
        initialize();
    }

    /**
     * Selects the text of the name field
     */
    public void selectNameField() {
        name.selectAll();
    }
    /**
     * Loads locale-specific resources: strings, images, etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.EmailListenerPropertiesDialog", locale);
    }

    /**
     * Initializes this dialog window and sets all of the fields based on the provided EmailListener
     * object.
     */
    private void initialize() {
        setContentPane(mainPanel);
        initializeFields();

        // Update all of the fields using the values from EmailListener
        modelToView();

        Utilities.equalizeButtonSizes(new AbstractButton[] { okButton, cancelButton });

        final Authorizer authorizer = Registry.getDefault().getSecurityProvider();
        okButton.setEnabled(emailListener == null || emailListener.getOid() == EmailListener.DEFAULT_OID ?
            authorizer.hasPermission(new AttemptedCreate(EntityType.EMAIL_LISTENER)) :
            authorizer.hasPermission(new AttemptedUpdate(EntityType.EMAIL_LISTENER, emailListener)));

        pack();
        enableOrDisableComponents();
        Utilities.centerOnScreen(this);
    }

    /**
     * Initializes the base settings fields.
     */
    private void initializeFields() {
        initResources();

        setTitle(resources.getString("emailListenerProperties.window.title"));
        inputValidator = new InputValidator(this, resources.getString("emailListenerProperties.window.title"));

        setContentPane(mainPanel);
        pack();
        setModal(true);
        getRootPane().setDefaultButton(okButton);

        // Attach the validator to the OK button
        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        inputValidator.attachToButton(testButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onTest();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        associateWithPublishedService.addActionListener(formPreener);

        // When the type field is changed, enable or disable the tabs that aren't associated
        // with the new type value.
        serverType.setModel(new DefaultComboBoxModel(EmailServerType.values()));
        serverType.setSelectedItem(EmailServerType.POP3);
        serverType.setRenderer(new Renderers.KeyedResourceRenderer(resources, "settings.serverType.{0}.text"));
        serverType.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updatePortAndFolder();
            }
        });

        useSSLCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if(useSSLCheckbox.isSelected()) {
                    port.setValue(((EmailServerType)serverType.getSelectedItem()).getDefaultSslPort());
                } else {
                    port.setValue(((EmailServerType)serverType.getSelectedItem()).getDefaultClearPort());
                }
            }
        });

        // Name field must not be empty and must not be longer than 128 characters
        ((AbstractDocument)name.getDocument()).setDocumentFilter(new DocumentSizeFilter(128));
        inputValidator.constrainTextFieldToBeNonEmpty("Name", name, new InputValidator.ComponentValidationRule(name) {
            @Override
            public String getValidationError() {
                if( name.getText().trim().length()==0 ) {
                    return resources.getString("settings.name.errors.empty");
                }

                return null;
            }
        });
        inputValidator.validateWhenDocumentChanges(name);

        // Hostname field must not be empty and must not be longer than 128 characters
        ((AbstractDocument)hostname.getDocument()).setDocumentFilter(new DocumentSizeFilter(128));
        inputValidator.constrainTextFieldToBeNonEmpty("Hostname", hostname, new InputValidator.ComponentValidationRule(hostname) {
            @Override
            public String getValidationError() {
                if( hostname.getText().trim().length()==0 ) {
                    return resources.getString("settings.hostname.errors.empty");
                } else if ( !ValidationUtils.isValidCharacters(hostname.getText().trim(), ValidationUtils.ALPHA_NUMERIC + "_-.") ) {
                    return resources.getString("settings.hostname.errors.chars");
                }

                return null;
            }
        });
        inputValidator.validateWhenDocumentChanges(hostname);

        // Port must be an integer between 1 and 65535
        port.setModel(new SpinnerNumberModel(EmailServerType.POP3.getDefaultClearPort(), 1, 65535, 1));
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(port, "Port"));

        serverType.setModel(new DefaultComboBoxModel(EmailServerType.values()));
        serverType.setSelectedItem(EmailServerType.POP3);

        // Username field must not be empty and must not be longer than 255 characters
        ((AbstractDocument)username.getDocument()).setDocumentFilter(new DocumentSizeFilter(255));
        inputValidator.constrainTextFieldToBeNonEmpty("Username", username, new InputValidator.ComponentValidationRule(username) {
            @Override
            public String getValidationError() {
                if( username.getText().trim().length()==0 ) {
                    return resources.getString("settings.username.errors.empty");
                } else if ( !ValidationUtils.isValidCharacters(username.getText().trim(), ValidationUtils.ALPHA_NUMERIC + "_-.@") ) {
                    return resources.getString("settings.username.errors.chars");
                }

                return null;
            }
        });
        inputValidator.validateWhenDocumentChanges(username);

        // Password field must not be empty and must not be longer than 32 characters
        ((AbstractDocument)password.getDocument()).setDocumentFilter(new DocumentSizeFilter(32));
        inputValidator.constrainTextFieldToBeNonEmpty("Password", password, new InputValidator.ComponentValidationRule(password) {
            @Override
            public String getValidationError() {
                if( password.getPassword().length==0 ) {
                    return resources.getString("settings.password.errors.empty");
                }

                return null;
            }
        });
        inputValidator.validateWhenDocumentChanges(password);
        PasswordGuiUtils.configureOptionalSecurePasswordField(password, showPasswordCheckBox, plaintextPasswordWarningLabel);

        // Folder field must not be empty and must not be longer than 255 characters
                ((AbstractDocument) folderName.getDocument()).setDocumentFilter(new DocumentSizeFilter(255));
        inputValidator.constrainTextFieldToBeNonEmpty("Folder", folderName, new InputValidator.ComponentValidationRule(folderName) {
            @Override
            public String getValidationError() {
                if( folderName.getText().trim().length()==0 ) {
                    return resources.getString("settings.folder.errors.empty");
                } else if ( !ValidationUtils.isValidCharacters(folderName.getText().trim(), ValidationUtils.ALPHA_NUMERIC + "_-/. ") ) {
                    return resources.getString("settings.folder.errors.chars");
                }

                return null;
            }
        });
        inputValidator.validateWhenDocumentChanges(folderName);

        folderBrowseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onBrowse();
            }
        });


        inputValidator.addRule( new InputValidator.ValidationRule()
        {
            @Override
            public String getValidationError() {
                return byteLimit.validateFields();
            }
        });

        // Interval must be an integer between greater than or equal to 1
        checkInterval.setModel(new SpinnerNumberModel(5, 1, Integer.MAX_VALUE, 1));
        JSpinner.NumberEditor jsne = new JSpinner.NumberEditor(checkInterval,"#");  //number format for parse and display 
        checkInterval.setEditor(jsne);
        ((JSpinner.DefaultEditor) checkInterval.getEditor()).getTextField().setFocusLostBehavior(JFormattedTextField.PERSIST);  //we'll do our own checking

        zoneControl.setEntityType(EntityType.EMAIL_LISTENER);
    }

    private PublishedService getSelectedHardwiredService() {
        PublishedService svc = null;
        ComboItem item = (ComboItem)serviceNameCombo.getSelectedItem();
        if (item == null) return null;
        ServiceAdmin sa = getServiceAdmin();
        try {
            svc = sa.findServiceByID(Long.toString(item.serviceID));
        } catch (FindException e) {
            logger.severe("Can not find service with id " + item.serviceID);
        }
        return svc;
    }

    public ServiceAdmin getServiceAdmin() {
        return Registry.getDefault().getServiceManager();
    }

    @Override
    public void setVisible(boolean b) {
        if (b && !isVisible()) confirmed = false;
        super.setVisible(b);
    }

    private void onBrowse() {
        EmailListenerAdmin emailListenerAdmin = Registry.getDefault().getEmailListenerAdmin();
        if(emailListenerAdmin == null) {
            DialogDisplayer.showMessageDialog(this, "Failed to connect to the Gateway!", "Connection Failure", JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        boolean useSSL = useSSLCheckbox.isSelected();
        String host = hostname.getText().trim();
        int portNum = ((Number)port.getValue()).intValue();
        String user = username.getText().trim();
        String pass = new String(password.getPassword());

        EmailListenerAdmin.IMAPFolder rootFolder = emailListenerAdmin.getIMAPFolderList(host, portNum, user, pass, useSSL);

        if(rootFolder == null) {
            DialogDisplayer.showMessageDialog(this, "Failed to connect to the email server.", "Connection Failure", JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        EmailListenerFolderList folderListDialog = new EmailListenerFolderList(this, rootFolder);
        folderListDialog.setVisible(true);

        if(folderListDialog.isConfirmed()) {
            String path = folderListDialog.getSelectedFolderPath();
            if(path != null) {
                folderName.setText(path);
            }
        }
    }

    private void onOk() {
        try {
            //check if edited value is invalid
             Integer interval = new Integer(((JSpinner.DefaultEditor) checkInterval.getEditor()).getTextField().getText());
            if (interval <= 0) {
                DialogDisplayer.showMessageDialog(this, "Interval value must be between 1 - " + Integer.MAX_VALUE,"Invalid value", JOptionPane.ERROR_MESSAGE, null);
            } else {
                checkInterval.commitEdit(); //commit the edited value into the spinner
                viewToModel();
                confirmed = true;
                dispose();
            }
        } catch (Exception e) {
            DialogDisplayer.showMessageDialog(this, "Interval value must be between 1 - " + Integer.MAX_VALUE,"Invalid value", JOptionPane.ERROR_MESSAGE, null);
        }
    }

    private void onTest() {
        EmailListenerAdmin emailListenerAdmin = Registry.getDefault().getEmailListenerAdmin();
        if(emailListenerAdmin == null) {
            DialogDisplayer.showMessageDialog(this, "Failed to connect to the Gateway!", "Connection Failure", JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        EmailServerType emailServerType = (EmailServerType)serverType.getSelectedItem();
        boolean useSSL = useSSLCheckbox.isSelected();
        String host = hostname.getText().trim();
        int portNum = ((Number)port.getValue()).intValue();
        String user = username.getText().trim();
        String pass = new String(password.getPassword());
        String folder = folderName.getText().trim();

        if(emailListenerAdmin.testEmailAccount(emailServerType, host, portNum, user, pass, useSSL, folder)) {
            DialogDisplayer.showMessageDialog(this, "Connection Succeeded", "Success", JOptionPane.INFORMATION_MESSAGE, null);
        } else {
            DialogDisplayer.showMessageDialog(this, "Connection Failed", "Failure", JOptionPane.ERROR_MESSAGE, null);
        }
    }

    private void updatePortAndFolder() {
        if(serverType.getSelectedItem() == null) {
            return;
        }
        
        switch((EmailServerType)serverType.getSelectedItem()) {
        case POP3:
            port.setValue(useSSLCheckbox.isSelected() ? EmailServerType.POP3.getDefaultSslPort() : EmailServerType.POP3.getDefaultClearPort());
            folderName.setText("INBOX");
            folderName.setEnabled(false);
            folderBrowseButton.setEnabled(false);
            break;
        case IMAP:
            port.setValue(useSSLCheckbox.isSelected() ? EmailServerType.IMAP.getDefaultSslPort() : EmailServerType.IMAP.getDefaultClearPort());
            folderName.setText("");
            folderName.setEnabled(true);
            folderBrowseButton.setEnabled(true);
            break;
        }
    }

    /**
     * Updates the dialog fields to match the values from the backing EmailListener.
     */
    private void modelToView() {
        name.setText(emailListener.getName());
        activeCheckbox.setSelected(emailListener.isActive());
        serverType.setSelectedItem(emailListener.getServerType() == null ? EmailServerType.POP3 : emailListener.getServerType());
        useSSLCheckbox.setSelected(emailListener.isUseSsl());
        deleteOnReceiveCheckbox.setSelected(emailListener.isDeleteOnReceive());
        hostname.setText(emailListener.getHost());
        port.setValue(emailListener.getPort());
        username.setText(emailListener.getUsername());
        password.setText(emailListener.getPassword());
        folderName.setText(emailListener.getFolder() != null && emailListener.getFolder().length() > 0 ? emailListener.getFolder() : "INBOX");
        checkInterval.setValue(emailListener.getPollInterval());
        // populate the service combo
 	 	EntityHeader[] allServices = EMPTY_ENTITY_HEADER;
 	 	try {
 	 	    ServiceAdmin sa = getServiceAdmin();
 	 	    allServices = sa.findAllPublishedServices();
 	 	} catch (Exception e) {
 	 	    logger.log(Level.WARNING, "problem listing services", e);
 	 	}

        boolean isHardWired = false;
 	 	long hardWiredId = 0;

        Properties props = emailListener.properties();
 	 	String tmp = props.getProperty(EmailListener.PROP_IS_HARDWIRED_SERVICE);
 	 	if (tmp != null) {
            if (Boolean.parseBoolean(tmp)) {
                tmp = props.getProperty(EmailListener.PROP_HARDWIRED_SERVICE_ID);
                isHardWired = true;
                hardWiredId = Long.parseLong(tmp);
            }
 	 	}

        String maxResponseSize = props.getProperty(EmailListener.PROP_REQUEST_SIZE_LIMIT);
        byteLimit.setValue(maxResponseSize,Registry.getDefault().getEmailAdmin().getXmlMaxBytes());

        if (allServices != null) {
 	 	    ComboItem[] comboitems = new ComboItem[allServices.length];
 	 	    Object selectMe = null;
            for (int i = 0; i < allServices.length; i++) {
                EntityHeader aService = allServices[i];
                ServiceHeader svcHeader = (ServiceHeader) aService;
                comboitems[i] = new ComboItem(
                    svcHeader.isDisabled()? svcHeader.getDisplayName() + " (This service is currently disabled.)" : svcHeader.getDisplayName(),
                    svcHeader.getOid()
                );
                if (isHardWired && aService.getOid() == hardWiredId) {
                    selectMe = comboitems[i];
                }
            }
            // sort services
            Arrays.sort(comboitems,new Comparator(){
                @Override
                public int compare(Object o1, Object o2) {
                    ComboItem c1 = (ComboItem)o1;
                    ComboItem c2 = (ComboItem)o2;
                    return c1.toString().compareToIgnoreCase(c2.toString());
                }
            });            

            serviceNameCombo.setModel(new DefaultComboBoxModel(comboitems));
            serviceNameCombo.setRenderer(TextListCellRenderer.basicComboBoxRenderer());
 	 	    if (selectMe != null) {
 	 	        serviceNameCombo.setSelectedItem(selectMe);
 	 	        associateWithPublishedService.setSelected(true);
 	 	    } else {
 	 	        associateWithPublishedService.setSelected(false);
 	 	    }
 	 	}
        zoneControl.setSelectedZone(emailListener.getSecurityZone());
    }

    /**
     * Updates the backing EmailListener with the values from this dialog.
     */
    private void viewToModel() {
        emailListener.setName(name.getText().trim());
        emailListener.setActive(activeCheckbox.isSelected());
        emailListener.setServerType((EmailServerType)serverType.getSelectedItem());
        emailListener.setUseSsl(useSSLCheckbox.isSelected());
        emailListener.setDeleteOnReceive(deleteOnReceiveCheckbox.isSelected());
        emailListener.setHost(hostname.getText().trim());
        emailListener.setPort(((Number)port.getValue()).intValue());
        emailListener.setUsername(username.getText().trim());
        emailListener.setPassword(new String(password.getPassword()));
        emailListener.setFolder(folderName.getText().trim());
        emailListener.setPollInterval(((Number)checkInterval.getValue()).intValue());
        emailListener.setSecurityZone(zoneControl.getSelectedZone());

        Properties properties = new Properties();
        if (associateWithPublishedService.isSelected()) {
 	 	    PublishedService svc = getSelectedHardwiredService();
 	 	    properties.setProperty(EmailListener.PROP_IS_HARDWIRED_SERVICE, (Boolean.TRUE).toString());
 	 	    properties.setProperty(EmailListener.PROP_HARDWIRED_SERVICE_ID, (new Long(svc.getOid())).toString());
 	 	} else {
 	 	    properties.setProperty(EmailListener.PROP_IS_HARDWIRED_SERVICE, (Boolean.FALSE).toString());
 	 	}
        if(byteLimit.isSelected()){
            properties.setProperty(EmailListener.PROP_REQUEST_SIZE_LIMIT, byteLimit.getValue());
        }

        emailListener.properties(properties);
    }

    /** @return true if the dialog has been dismissed with the ok button */
    public boolean isConfirmed() {
        return confirmed;
    }

    public static void main(String[] args) {
        Frame f = new JFrame();
        f.setVisible(true);
        EmailListenerPropertiesDialog d = new EmailListenerPropertiesDialog(f, new EmailListener());
        d.setVisible(true);
        d.dispose();
        f.dispose();
    }
}
