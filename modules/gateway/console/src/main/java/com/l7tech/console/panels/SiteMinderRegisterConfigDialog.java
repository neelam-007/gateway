package com.l7tech.console.panels;

import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.siteminder.SiteMinderHost;
import com.l7tech.console.util.PasswordGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.siteminder.SiteMinderAdmin;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.MutablePair;
import sun.security.util.Resources;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import static com.l7tech.console.util.AdminGuiUtils.doAsyncAdmin;

/**
 * Created with IntelliJ IDEA.
 * User: nilic
 * Date: 7/25/13
 * Time: 2:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class SiteMinderRegisterConfigDialog extends JDialog {

    private static final Logger logger = Logger.getLogger(SiteMinderRegisterConfigDialog.class.getName());
    private static final ResourceBundle resources = Resources.getBundle("com.l7tech.console.panels.resources.SiteMinderRegisterConfigDialog");

    private JTextField addressTextField;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField hostnameTextField;
    private JTextField hostConfigurationTextField;
    private JTextField userNameTextField;
    private JComboBox fipsModeComboBox;
    private JPanel mainPanel;
    private SecurePasswordComboBox securePasswordComboBox;
    private JButton managePasswordsButton;
    private MutablePair<String, SiteMinderHost> property;
    public static Map<String, String> siteMinderHostParams = Collections.synchronizedMap(new WeakHashMap<String, String>());
    private boolean confirmed;

    public SiteMinderRegisterConfigDialog(Dialog owner, MutablePair<String, SiteMinderHost> property) {
        super(owner, resources.getString("dialog.title.siteminder.register.properties"));
        initialize(property);
    }

    private void initialize(MutablePair<String, SiteMinderHost> property){

        this.property = property;

        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        Utilities.setEscKeyStrokeDisposes(this);

        DocumentListener docListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableOkButton();
            }
        });

        addressTextField.getDocument().addDocumentListener(docListener);
        hostnameTextField.getDocument().addDocumentListener(docListener);
        hostConfigurationTextField.getDocument().addDocumentListener(docListener);
        userNameTextField.getDocument().addDocumentListener(docListener);

        securePasswordComboBox.setRenderer(TextListCellRenderer.<SecurePasswordComboBox>basicComboBoxRenderer());
        ((JTextField)securePasswordComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(docListener);

        managePasswordsButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {

                final SecurePasswordManagerWindow dialog = new SecurePasswordManagerWindow(TopComponents.getInstance().getTopParent());
                dialog.pack();
                Utilities.centerOnScreen(dialog);
                DialogDisplayer.display(dialog, new Runnable() {
                    @Override
                    public void run() {
                        securePasswordComboBox.reloadPasswordList();
                        enableOrDisableOkButton();
                        DialogDisplayer.pack(SiteMinderRegisterConfigDialog.this);
                    }
                });
            }
        });

        ((JTextField)fipsModeComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(docListener);

        fipsModeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableOkButton();
            }
        });

        fipsModeComboBox.setModel(new DefaultComboBoxModel(new Object[] {
                SiteMinderConfigPropertiesDialog.UNSET_MODE,
                SiteMinderConfigPropertiesDialog.COMPAT_MODE,
                SiteMinderConfigPropertiesDialog.MIGRATE_MODE,
                SiteMinderConfigPropertiesDialog.ONLY_MODE
        }));

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = true;

                //call to Register utility
                register();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        modelToView();

        enableOrDisableOkButton();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void modelToView(){
        if (siteMinderHostParams != null && siteMinderHostParams.size() != 0) {
            //parse policy server
            addressTextField.setText(siteMinderHostParams.get(resources.getString("property.siteminder.address")));
            hostnameTextField.setText(siteMinderHostParams.get(resources.getString("property.siteminder.hostName")));
            hostConfigurationTextField.setText(siteMinderHostParams.get(resources.getString("property.siteminder.host.configuration")));
            userNameTextField.setText(siteMinderHostParams.get(resources.getString("property.siteminder.username")));
            switch (siteMinderHostParams.get(resources.getString("property.siteminder.fipsmode"))){
                case "0":
                    fipsModeComboBox.setSelectedIndex(SiteMinderConfigPropertiesDialog.FIPS140_UNSET);
                    break;
                case "1":
                    fipsModeComboBox.setSelectedIndex(SiteMinderConfigPropertiesDialog.FIPS140_COMPAT);
                    break;
                case "2":
                    fipsModeComboBox.setSelectedIndex(SiteMinderConfigPropertiesDialog.FIPS140_MIGRATE);
                    break;
                case "3":
                    fipsModeComboBox.setSelectedIndex(SiteMinderConfigPropertiesDialog.FIPS140_ONLY);
                    break;
                default:
                    fipsModeComboBox.setSelectedIndex(SiteMinderConfigPropertiesDialog.FIPS140_UNSET);
                    break;
            }
        }

        if (property.left.equals("Init SiteMinder Host")){
            addressTextField.setText(property.right.getPolicyServer());
            hostnameTextField.setText(property.right.getHostname());
            fipsModeComboBox.setSelectedIndex(property.right.getFipsMode());
            hostConfigurationTextField.setText(property.right.getHostConfigObject());
            userNameTextField.setText(property.right.getUserName());
            if (property.right.getPasswordOid() != null){
                securePasswordComboBox.setSelectedSecurePassword(property.right.getPasswordOid());
            }
        }
    }

    private void register(){

        final SiteMinderAdmin admin = getSiteMinderAdmin();
        if (admin == null) return;

        String address = addressTextField.getText().trim();
        String userName = userNameTextField.getText().trim();
        long password = securePasswordComboBox.getSelectedSecurePassword().getOid();
        String hostName = hostnameTextField.getText().trim();
        String hostConfiguration = hostConfigurationTextField.getText().trim();
        Integer fipsMode = null;

        switch (fipsModeComboBox.getSelectedItem().toString()) {
            case SiteMinderConfigPropertiesDialog.COMPAT_FIPS_MODE:
                fipsMode = SiteMinderConfigPropertiesDialog.FIPS140_COMPAT;
                break;
            case SiteMinderConfigPropertiesDialog.MIGRATE_FIPS_MODE:
                fipsMode = SiteMinderConfigPropertiesDialog.FIPS140_MIGRATE;
                break;
            case SiteMinderConfigPropertiesDialog.ONLY_FIPS_MODE:
                fipsMode = SiteMinderConfigPropertiesDialog.FIPS140_ONLY;
                break;
        }

        try {
            SiteMinderHost siteMinderHost = doAsyncAdmin(admin,
                                          SiteMinderRegisterConfigDialog.this,
                                          resources.getString("message.registering.progress"),
                                          resources.getString("message.registering"),
                                          admin.registerSiteMinderConfiguration(address, userName, password, hostName, hostConfiguration, fipsMode)).right();

            String message = siteMinderHost != null ?
                    resources.getString("message.register.siteminder.config.passed") : MessageFormat.format(resources.getString("message.register.siteminder.config.failed"), "message.register.siteminder.config.message");

            DialogDisplayer.showMessageDialog(this, message, resources.getString("dialog.title.siteminder.configuration.register"),
                    siteMinderHost != null ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE, null);

            if (siteMinderHost != null){
                siteMinderHostParams.put(resources.getString("property.siteminder.address"),address);
                siteMinderHostParams.put(resources.getString("property.siteminder.hostName"),hostName);
                siteMinderHostParams.put(resources.getString("property.siteminder.host.configuration"),hostConfiguration);
                siteMinderHostParams.put(resources.getString("property.siteminder.fipsmode"),fipsMode.toString());
                siteMinderHostParams.put(resources.getString("property.siteminder.username"),userName);
            }

            if (property == null || property.left == null || property.right == null) {
                throw new IllegalStateException("An additional property object must be initialized first.");
            }

            siteMinderHost.setUserName(userName);
            siteMinderHost.setPasswordOid(password);
            property.left = "SiteMinder Host Configuration";
            property.right = siteMinderHost;

            dispose();
        } catch (InterruptedException e) {
            // do nothing, user cancelled
            dispose();

        } catch (InvocationTargetException e) {
            DialogDisplayer.showMessageDialog(this, MessageFormat.format(resources.getString("message.register.siteminder.config.failed"), e.getMessage()),
                    resources.getString("dialog.title.siteminder.configuration.register"),
                    JOptionPane.WARNING_MESSAGE, null);
        } catch (RuntimeException e) {
            DialogDisplayer.showMessageDialog(this, MessageFormat.format(resources.getString("message.register.siteminder.config.failed"), e.getMessage()),
                    resources.getString("dialog.title.siteminder.configuration.register"),
                    JOptionPane.WARNING_MESSAGE, null);
        }
    }

    private void enableOrDisableOkButton(){

        boolean addressOK = isNonEmptyRequiredTextField(addressTextField.getText().trim());
        boolean hostNameOK = isNonEmptyRequiredTextField(hostnameTextField.getText().trim());
        boolean hostConfigurationOK = isNonEmptyRequiredTextField(hostConfigurationTextField.getText().trim());
        boolean userNameOK = isNonEmptyRequiredTextField(userNameTextField.getText().trim());
        boolean passwordOK = securePasswordComboBox.getSelectedItem() != null;

        boolean fipsModeOK = fipsModeComboBox.getSelectedIndex() <= 0  ?  false : true;

        boolean enabled =  addressOK && hostNameOK && hostConfigurationOK && userNameOK && passwordOK && fipsModeOK;

        okButton.setEnabled(enabled);
    }

    private boolean isNonEmptyRequiredTextField(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private SiteMinderAdmin getSiteMinderAdmin(){
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent())
            return null;
        return reg.getSiteMinderConfigurationAdmin();
    }
}
