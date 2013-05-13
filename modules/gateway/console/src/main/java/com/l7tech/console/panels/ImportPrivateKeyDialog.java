package com.l7tech.console.panels;

import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.OkCancelPanel;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ImportPrivateKeyDialog extends JDialog {
    private JPanel contentPanel;
    private JTextField aliasTextField;
    private SecurityZoneWidget zoneControl;
    private OkCancelPanel okCancelPanel;
    private boolean confirmed;
    private InputValidator inputValidator;

    public ImportPrivateKeyDialog(@NotNull Window owner) {
        super(owner, "Import Private Key", Dialog.ModalityType.APPLICATION_MODAL);
        Utilities.setEscKeyStrokeDisposes(this);
        setContentPane(contentPanel);
        setModal(true);
        getRootPane().setDefaultButton(okCancelPanel.getOkButton());
        zoneControl.configure(EntityType.SSG_KEY_ENTRY, OperationType.CREATE, null);
        okCancelPanel.getCancelButton().addActionListener(Utilities.createDisposeAction(this));
        inputValidator = new InputValidator(this, "Error");
        inputValidator.constrainTextFieldToBeNonEmpty("alias", aliasTextField, null);
        inputValidator.attachToButton(okCancelPanel.getOkButton(), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                dispose();
            }
        });
    }

    /**
     * @return whether the dialog was OKed.
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * @return the key alias provided by the user.
     */
    @NotNull
    public String getAlias() {
        return aliasTextField.getText().trim();
    }

    /**
     * @return the selected SecurityZone.
     */
    @Nullable
    public SecurityZone getSecurityZone() {
        return zoneControl.getSelectedZone();
    }

}
