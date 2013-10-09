package com.l7tech.console.panels;

import com.l7tech.console.util.EntityUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.exporter.StoredPasswordReference;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.console.util.AdminGuiUtils.doAsyncAdmin;

/**
 * The resolver panel for stored passwords
 */
public class ResolveStoredPasswordPanel extends WizardStepPanel {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.ResolveStoredPasswordPanel");
    private static final Logger logger = Logger.getLogger(ResolveStoredPasswordPanel.class.getName());

    private JPanel mainPanel;
    private JTextField nameTextField;
    private JTextField typeTextField;
    private JRadioButton changeRadioButton;
    private JRadioButton removeRadioButton;
    private JRadioButton ignoreRadioButton;
    private JButton createStoredPasswordButton;
    private SecurePasswordComboBox securePasswordComboBox;
    private JTextField idTextField;

    private StoredPasswordReference storedPasswordReference;

    public ResolveStoredPasswordPanel(WizardStepPanel next, StoredPasswordReference storedPasswordReference) {
        super(next);
        this.storedPasswordReference = storedPasswordReference;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        nameTextField.setText(storedPasswordReference.getName());
        nameTextField.setCaretPosition(0);
        idTextField.setText(storedPasswordReference.getId().toString());
        idTextField.setCaretPosition(0);
        typeTextField.setText(storedPasswordReference.getType());
        typeTextField.setCaretPosition(0);

        securePasswordComboBox.reloadPasswordList(SecurePassword.SecurePasswordType.valueOf(storedPasswordReference.getType()));

        // default is delete
        removeRadioButton.setSelected(true);
        securePasswordComboBox.setEnabled(false);

        // enable/disable provider selector as per action type selected
        changeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                securePasswordComboBox.setEnabled(true);
            }
        });
        removeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                securePasswordComboBox.setEnabled(false);
            }
        });
        ignoreRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                securePasswordComboBox.setEnabled(false);
            }
        });

        createStoredPasswordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                doCreatePassword();
            }
        });

        enableAndDisableComponents();
    }

    @Override
    public String getDescription() {
        return getStepLabel();
    }

    @Override
    public boolean canFinish() {
        return !hasNextPanel();
    }

    @Override
    public String getStepLabel() {
        return MessageFormat.format(resources.getString("label.unresolved.stored.password"), storedPasswordReference.getName() != null ? storedPasswordReference.getName() : storedPasswordReference.getId());
    }

    @Override
    public boolean onNextButton() {
        if (changeRadioButton.isSelected()) {
            if (securePasswordComboBox.getSelectedIndex() < 0) return false;

            SecurePassword securePassword = securePasswordComboBox.getSelectedSecurePassword();
            storedPasswordReference.setLocalizeReplace(securePassword.getGoid());
        } else if (removeRadioButton.isSelected()) {
            storedPasswordReference.setLocalizeDelete();
        } else if (ignoreRadioButton.isSelected()) {
            storedPasswordReference.setLocalizeIgnore();
        }
        return true;
    }

    @Override
    public void notifyActive() {
        securePasswordComboBox.reloadPasswordList(SecurePassword.SecurePasswordType.valueOf(storedPasswordReference.getType()));
    }

    private void enableAndDisableComponents() {
        final boolean enableSelection = securePasswordComboBox.getModel().getSize() > 0;
        changeRadioButton.setEnabled(enableSelection);

        if (!changeRadioButton.isEnabled() && changeRadioButton.isSelected()) {
            removeRadioButton.setSelected(true);
        }
    }

    private void doCreatePassword() {
        final SecurePassword newSecurePassword = new SecurePassword();
        newSecurePassword.setName(storedPasswordReference.getName());
        newSecurePassword.setType(SecurePassword.SecurePasswordType.valueOf(storedPasswordReference.getType()));

        EntityUtils.resetIdentity(newSecurePassword);
        editAndSave(newSecurePassword);
    }

    private void editAndSave(final SecurePassword securePassword) {
        final SecurePasswordPropertiesDialog dlg = new SecurePasswordPropertiesDialog(TopComponents.getInstance().getTopParent(), securePassword, false, true);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    Runnable reedit = new Runnable() {
                        public void run() {
                            editAndSave(securePassword);
                        }
                    };

                    // Save the connection
                    TrustedCertAdmin admin = getTrustedCertAdmin();
                    final Goid savedId;
                    if (admin == null) return;
                    try {
                        savedId = admin.saveSecurePassword(securePassword);

                        // Update password field, if necessary
                        char[] newpass = dlg.getEnteredPassword();
                        if (newpass != null)
                            admin.setSecurePassword(savedId, newpass);

                        int keybits = dlg.getGenerateKeybits();
                        if (keybits > 0) {
                            doAsyncAdmin(
                                    admin,
                                    TopComponents.getInstance().getTopParent(),
                                    "Generating Key",
                                    "Generating PEM private key ...",
                                    admin.setGeneratedSecurePassword(savedId, keybits)
                            );
                        }
                    } catch (SaveException | UpdateException | FindException | InvocationTargetException | InterruptedException e) {
                        showErrorMessage(resources.getString("errors.saveFailed.title"),
                                resources.getString("errors.saveFailed.message") + " " + ExceptionUtils.getMessage(e),
                                e,
                                reedit);
                        return;
                    }

                    // refresh controls
                    securePasswordComboBox.reloadPasswordList(SecurePassword.SecurePasswordType.valueOf(storedPasswordReference.getType()));
                    securePasswordComboBox.setSelectedSecurePassword(savedId);
                    securePasswordComboBox.setEnabled(true);
                    changeRadioButton.setSelected(true);
                    enableAndDisableComponents();
                }
            }
        });
    }

    private void showErrorMessage(String title, String msg, Throwable e, Runnable continuation) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, continuation);
    }

    private TrustedCertAdmin getTrustedCertAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent()) {
            logger.warning("Cannot get Trusted Cert Admin due to no Admin Context present.");
            return null;
        }
        return reg.getTrustedCertManager();
    }
}
