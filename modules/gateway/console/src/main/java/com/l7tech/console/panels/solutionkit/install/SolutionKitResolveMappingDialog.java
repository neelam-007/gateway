package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.panels.PrivateKeysComboBox;
import com.l7tech.console.panels.SecurePasswordComboBox;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class SolutionKitResolveMappingDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(SolutionKitResolveMappingDialog.class.getName());

    private JPanel mainPanel;
    private JLabel nameFieldLabel;
    private JLabel idFieldLabel;
    private JLabel entityTypeFieldLabel;
    private JLabel errorTypeFieldLabel;
    private JComboBox entityComboBox;
    private JButton createEntityButton;
    private JButton okButton;
    private JButton cancelButton;

    private final Mapping mapping;
    private final Item item;
    private boolean isConfirmed = false;

    // Casted ComboBox of entityComboBox. Only one of these is not null.
    //
    private SecurePasswordComboBox securePasswordComboBox;
    private PrivateKeysComboBox privateKeysComboBox;

    public SolutionKitResolveMappingDialog(@NotNull Dialog owner, @NotNull Mapping mapping, @NotNull Item item) {
        super(owner, "Resolve Entity Conflict", true);
        this.mapping = mapping;
        this.item = item;

        initialize();
        populateFields();
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public String getResolvedId() {
        if (entityComboBox.getSelectedIndex() == -1) {
            return null;
        }

        if (securePasswordComboBox != null) {
            return securePasswordComboBox.getSelectedSecurePassword().getGoid().toString();
        } else if (privateKeysComboBox != null) {
            return privateKeysComboBox.getSelectedKeyAlias();
        } else {
            return entityComboBox.getSelectedItem().toString();
        }
    }

    private void initialize() {
        createEntityButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCreateEntity();
            }
        });

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(mainPanel);
    }

    private void populateFields() {
        nameFieldLabel.setText(item.getName());
        idFieldLabel.setText(mapping.getSrcId());
        entityTypeFieldLabel.setText(mapping.getType());
        errorTypeFieldLabel.setText(mapping.getErrorType().toString());

        if (securePasswordComboBox != null) {
            securePasswordComboBox.reloadPasswordList();
        } else if (privateKeysComboBox != null) {
            privateKeysComboBox.repopulate();
        }
        // else, unsupported entity type. Do nothing.
    }

    private void onCreateEntity() {
        if (securePasswordComboBox != null) {
            createSecurePassword();
        } else if (privateKeysComboBox != null) {
            createPrivateKey();
        }
        // else, unsupported entity type. Do nothing.
    }

    private void createSecurePassword() {
        // todo (kpak) - implement.
    }

    private void createPrivateKey() {
        // todo (kpak) - implement.
    }

    private void onOk() {
        if (entityComboBox.getSelectedIndex() == -1) {
            JOptionPane.showMessageDialog(this,
                "An entity must be selected.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        } else {
            isConfirmed = true;
            dispose();
        }
    }

    private void onCancel() {
        dispose();
    }

    private void createUIComponents() {
        EntityType entityType = EntityType.valueOf(mapping.getType());
        switch (entityType) {
            case SECURE_PASSWORD:
                entityComboBox = new SecurePasswordComboBox();
                securePasswordComboBox = (SecurePasswordComboBox) entityComboBox;
                break;

            case SSG_KEY_ENTRY:
                entityComboBox = new PrivateKeysComboBox();
                privateKeysComboBox = (PrivateKeysComboBox) entityComboBox;
                break;

            // todo (kpak) - JDBC Connection.

            default:
                logger.log(Level.WARNING, "Unsupported entity type: " + mapping.getType());
                // create default JComboBox.
                entityComboBox = new JComboBox();
                break;
        }
    }
}