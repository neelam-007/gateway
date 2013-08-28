package com.l7tech.console.panels;

import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.siteminder.SiteMinderFipsMode;
import com.l7tech.gateway.common.siteminder.SiteMinderHost;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.siteminder.SiteMinderAdmin;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Either;
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

import static com.l7tech.console.util.AdminGuiUtils.doAsyncAdmin;

/**
 * @author nilic
 * Date: 7/25/13
 * Time: 2:45 PM
 */
public class SiteMinderRegisterConfigDialog extends JDialog {
    private static final ResourceBundle RESOURCES = Resources.getBundle("com.l7tech.console.panels.resources.SiteMinderRegisterConfigDialog");

    private JTextField addressTextField;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField hostnameTextField;
    private JTextField hostConfigurationTextField;
    private JTextField userNameTextField;
    private JComboBox<SiteMinderFipsMode> fipsModeComboBox;
    private JPanel mainPanel;
    private SecurePasswordComboBox securePasswordComboBox;
    private JButton managePasswordsButton;
    private MutablePair<String, SiteMinderHost> property;
    public static Map<String, String> siteMinderHostParams = Collections.synchronizedMap(new WeakHashMap<String, String>());
    private boolean confirmed;

    public SiteMinderRegisterConfigDialog(Dialog owner, MutablePair<String, SiteMinderHost> property) {
        super(owner, RESOURCES.getString("dialog.title.siteminder.register.properties"));
        initialize(property);
    }

    private void initialize(MutablePair<String, SiteMinderHost> property) {

        // TODO jwilliams: really needs some validation to avoid ugly error messages from smreghost

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

        fipsModeComboBox.setModel(new DefaultComboBoxModel<>(SiteMinderFipsMode.values()));

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

    private void modelToView() {
        if (siteMinderHostParams != null && siteMinderHostParams.size() != 0) {
            //parse policy server
            addressTextField.setText(siteMinderHostParams.get(RESOURCES.getString("property.siteminder.address")));
            hostnameTextField.setText(siteMinderHostParams.get(RESOURCES.getString("property.siteminder.hostName")));
            hostConfigurationTextField.setText(siteMinderHostParams.get(RESOURCES.getString("property.siteminder.host.configuration")));
            userNameTextField.setText(siteMinderHostParams.get(RESOURCES.getString("property.siteminder.username")));

            String fipsModeProperty = siteMinderHostParams.get(RESOURCES.getString("property.siteminder.fipsmode"));
            SiteMinderFipsMode mode = SiteMinderFipsMode.getByCode(Integer.parseInt(fipsModeProperty));

            // any unrecognized fips mode setting will be replaced with UNSET
            fipsModeComboBox.setSelectedItem(mode == null ? SiteMinderFipsMode.UNSET : mode);
        }
        // TODO jwilliams: why are these both setting the fields? can both cases be true? if not, should use an else if for clarity - ask Natalija
        if (property.left.equals("Init SiteMinder Host")) {
            addressTextField.setText(property.right.getPolicyServer());
            hostnameTextField.setText(property.right.getHostname());

            SiteMinderFipsMode mode = SiteMinderFipsMode.getByCode(property.right.getFipsMode());

            // any unrecognized fips mode setting will be replaced with UNSET
            fipsModeComboBox.setSelectedItem(mode == null ? SiteMinderFipsMode.UNSET : mode);

            hostConfigurationTextField.setText(property.right.getHostConfigObject());
            userNameTextField.setText(property.right.getUserName());
            if (property.right.getPasswordGoid() != null){
                securePasswordComboBox.setSelectedSecurePassword(property.right.getPasswordGoid());
            }
        }
    }

    private void register(){

        final SiteMinderAdmin admin = getSiteMinderAdmin();
        if (admin == null) return;

        String address = addressTextField.getText().trim();
        String userName = userNameTextField.getText().trim();
        Goid password = securePasswordComboBox.getSelectedSecurePassword().getGoid();
        String hostName = hostnameTextField.getText().trim();
        String hostConfiguration = hostConfigurationTextField.getText().trim();

        int modeIndex = fipsModeComboBox.getSelectedIndex();
        SiteMinderFipsMode mode = modeIndex > -1 ? fipsModeComboBox.getItemAt(modeIndex) : SiteMinderFipsMode.UNSET;
        Integer fipsMode = mode.getCode();

        try {
            Either<String, SiteMinderHost> either = doAsyncAdmin(admin,
                    SiteMinderRegisterConfigDialog.this,
                    RESOURCES.getString("message.registering.progress"),
                    RESOURCES.getString("message.registering"),
                    admin.registerSiteMinderConfiguration(address, userName, password, hostName, hostConfiguration, fipsMode));

            if (either.isLeft()) {
                DialogDisplayer.showMessageDialog(this, MessageFormat.format(RESOURCES.getString("message.register.siteminder.config.failed"), either.left()),
                        RESOURCES.getString("dialog.title.siteminder.configuration.register"),
                        JOptionPane.WARNING_MESSAGE, null);
                return;
            }

            SiteMinderHost siteMinderHost = either.right();

            String message = siteMinderHost != null ?
                    RESOURCES.getString("message.register.siteminder.config.passed") : MessageFormat.format(RESOURCES.getString("message.register.siteminder.config.failed"), RESOURCES.getString("message.register.siteminder.config.message"));

            DialogDisplayer.showMessageDialog(this, message, RESOURCES.getString("dialog.title.siteminder.configuration.register"),
                    siteMinderHost != null ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE, null);

            if (siteMinderHost != null){
                siteMinderHostParams.put(RESOURCES.getString("property.siteminder.address"),address);
                siteMinderHostParams.put(RESOURCES.getString("property.siteminder.hostName"),hostName);
                siteMinderHostParams.put(RESOURCES.getString("property.siteminder.host.configuration"),hostConfiguration);
                siteMinderHostParams.put(RESOURCES.getString("property.siteminder.fipsmode"),fipsMode.toString());
                siteMinderHostParams.put(RESOURCES.getString("property.siteminder.username"),userName);
            }

            if (property == null || property.left == null || property.right == null) {
                throw new IllegalStateException("An additional property object must be initialized first.");
            }

            siteMinderHost.setUserName(userName);
            siteMinderHost.setPasswordGoid(password);
            property.left = "SiteMinder Host Configuration";
            property.right = siteMinderHost;

            dispose();
        } catch (InterruptedException e) {
            // do nothing, user cancelled
        } catch (InvocationTargetException | RuntimeException e) {
            DialogDisplayer.showMessageDialog(this,
                    MessageFormat.format(RESOURCES.getString("message.register.siteminder.config.failed"), e.getMessage()),
                    RESOURCES.getString("dialog.title.siteminder.configuration.register"),
                    JOptionPane.WARNING_MESSAGE, null);
        }
    }

    private void enableOrDisableOkButton() {  // TODO jwilliams: should remove this behaviour, same as SiteMinderConfigPropertiesDialog

        boolean addressOK = isNonEmptyRequiredTextField(addressTextField.getText().trim());
        boolean hostNameOK = isNonEmptyRequiredTextField(hostnameTextField.getText().trim());
        boolean hostConfigurationOK = isNonEmptyRequiredTextField(hostConfigurationTextField.getText().trim());
        boolean userNameOK = isNonEmptyRequiredTextField(userNameTextField.getText().trim());
        boolean passwordOK = securePasswordComboBox.getSelectedItem() != null;
        boolean fipsModeOK = fipsModeComboBox.getSelectedIndex() > 0;

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
