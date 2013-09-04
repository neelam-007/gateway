package com.l7tech.console.panels;

import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.gateway.common.siteminder.SiteMinderFipsModeOption;
import com.l7tech.gateway.common.siteminder.SiteMinderHost;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.siteminder.SiteMinderAdmin;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Either;
import sun.security.util.Resources;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static com.l7tech.console.util.AdminGuiUtils.doAsyncAdmin;

/**
 * @author nilic
 * Date: 7/25/13
 * Time: 2:45 PM
 */
public class SiteMinderRegisterConfigDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(SiteMinderRegisterConfigDialog.class.getName());

    private static final ResourceBundle RESOURCES =
            Resources.getBundle("com.l7tech.console.panels.resources.SiteMinderRegisterConfigDialog");

    private JTextField addressTextField;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField hostnameTextField;
    private JTextField hostConfigurationTextField;
    private JTextField userNameTextField;
    private JComboBox<SiteMinderFipsModeOption> fipsModeComboBox;
    private JPanel mainPanel;
    private SecurePasswordComboBox securePasswordComboBox;
    private JButton managePasswordsButton;
    private SiteMinderHost siteMinderHost;
    private boolean confirmed;

    public SiteMinderRegisterConfigDialog(Dialog owner, SiteMinderHost siteMinderHost) {
        super(owner, RESOURCES.getString("dialog.title.siteminder.register.properties"));
        initialize(siteMinderHost);
    }

    private void initialize(SiteMinderHost siteMinderHost) {
        this.siteMinderHost = siteMinderHost;

        confirmed = false;

        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);

        InputValidator validator =
                new InputValidator(this, RESOURCES.getString("dialog.title.siteminder.register.properties"));

        validator.constrainTextFieldToBeNonEmpty(RESOURCES.getString("property.siteminder.address"), addressTextField, null);
        validator.constrainTextFieldToBeNonEmpty(RESOURCES.getString("property.siteminder.hostName"), hostnameTextField, null);
        validator.constrainTextFieldToBeNonEmpty(RESOURCES.getString("property.siteminder.hostConfiguration"), hostConfigurationTextField, null);
        validator.constrainTextFieldToBeNonEmpty(RESOURCES.getString("property.siteminder.username"), userNameTextField, null);
        validator.ensureComboBoxSelection(RESOURCES.getString("property.siteminder.password"), securePasswordComboBox);
        validator.ensureComboBoxSelection(RESOURCES.getString("property.siteminder.fipsMode"), fipsModeComboBox);

        securePasswordComboBox.setRenderer(TextListCellRenderer.basicComboBoxRenderer());

        managePasswordsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final SecurePasswordManagerWindow dialog =
                        new SecurePasswordManagerWindow(TopComponents.getInstance().getTopParent());
                dialog.pack();

                Utilities.centerOnScreen(dialog);

                DialogDisplayer.display(dialog, new Runnable() {
                    @Override
                    public void run() {
                        securePasswordComboBox.reloadPasswordList();
                        DialogDisplayer.pack(SiteMinderRegisterConfigDialog.this);
                    }
                });
            }
        });

        fipsModeComboBox.setModel(new DefaultComboBoxModel<>(SiteMinderFipsModeOption.values()));

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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

        Utilities.setEscKeyStrokeDisposes(this);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public SiteMinderHost getSiteMinderHost() {
        return siteMinderHost;
    }

    private void modelToView() {
        addressTextField.setText(siteMinderHost.getPolicyServer());
        hostnameTextField.setText(siteMinderHost.getHostname());

        SiteMinderFipsModeOption mode = siteMinderHost.getFipsMode();

        // any unrecognized fips mode setting will be replaced with COMPAT
        fipsModeComboBox.setSelectedItem(mode == null ? SiteMinderFipsModeOption.COMPAT : mode);

        hostConfigurationTextField.setText(siteMinderHost.getHostConfigObject());
        userNameTextField.setText(siteMinderHost.getUserName());

        if (siteMinderHost.getPasswordGoid() != null) {
            securePasswordComboBox.setSelectedSecurePassword(siteMinderHost.getPasswordGoid());
        }
    }

    private void register() {
        final SiteMinderAdmin admin = getSiteMinderAdmin();

        if (admin == null) return;

        String address = addressTextField.getText().trim();
        String userName = userNameTextField.getText().trim();
        Goid password = securePasswordComboBox.getSelectedSecurePassword().getGoid();
        String hostName = hostnameTextField.getText().trim();
        String hostConfiguration = hostConfigurationTextField.getText().trim();

        int modeIndex = fipsModeComboBox.getSelectedIndex();
        SiteMinderFipsModeOption fipsMode = fipsModeComboBox.getItemAt(modeIndex);

        SiteMinderConfiguration c = new SiteMinderConfiguration();
        c.setAddress(address);
        c.setUserName(userName);
        c.setPasswordGoid(password);
        c.setHostname(hostName);
        c.setHostConfiguration(hostConfiguration);
        c.setFipsmode(fipsMode.getCode());
        c.setSecurityZone(siteMinderHost.getSecurityZone());

        Either<String, SiteMinderHost> either;

        try {
            either = doAsyncAdmin(admin,
                    SiteMinderRegisterConfigDialog.this,
                    RESOURCES.getString("message.registering.progress"),
                    RESOURCES.getString("message.registering"),
                    admin.registerSiteMinderConfiguration(c));
        } catch (InterruptedException e) {
            // do nothing, user cancelled
            return;
        } catch (InvocationTargetException | RuntimeException e) {
            DialogDisplayer.showMessageDialog(this,
                    MessageFormat.format(RESOURCES.getString("message.register.siteminder.config.failed"),
                            e.getMessage()),
                    RESOURCES.getString("dialog.title.siteminder.configuration.register"),
                    JOptionPane.WARNING_MESSAGE, null);
            return;
        }

        // if the expected result is null, show error message and return
        if (!either.isRight()) {
            String failureMessage = either.isLeft()
                    ? either.left()
                    : RESOURCES.getString("message.register.siteminder.config.message");

            DialogDisplayer.showMessageDialog(this,
                    MessageFormat.format(RESOURCES.getString("message.register.siteminder.config.failed"),
                            failureMessage),
                    RESOURCES.getString("dialog.title.siteminder.configuration.register"),
                    JOptionPane.WARNING_MESSAGE, null);

            return;
        }

        DialogDisplayer.showMessageDialog(this,
                RESOURCES.getString("message.register.siteminder.config.passed"),
                RESOURCES.getString("dialog.title.siteminder.configuration.register"),
                JOptionPane.INFORMATION_MESSAGE, null);

        siteMinderHost = either.right();
        siteMinderHost.setUserName(userName);
        siteMinderHost.setPasswordGoid(password);

        confirmed = true;

        dispose();
    }

    private SiteMinderAdmin getSiteMinderAdmin() {
        Registry reg = Registry.getDefault();

        if (!reg.isAdminContextPresent()) {
            logger.warning("Cannot get SiteMinder Configuration Admin due to no Admin Context present.");
            return null;
        }

        return reg.getSiteMinderConfigurationAdmin();
    }
}
