package com.l7tech.console.action;

import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gateway.common.security.rbac.AttemptedCreateSpecific;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Action which creates an EncapsulatedAssertionConfig.
 */
public class CreateEncapsulatedAssertionAction extends AbstractEncapsulatedAssertionAction {
    private static final String NAME = "Create Encapsulated Assertion";
    private static final String DESC = "Create an Encapsulated Assertion from policy";
    private final EncapsulatedAssertionConfig config;
    private final boolean promptForAutoPopulate;

    /**
     * @param promptForAutoPopulate whether the user should be asked if they want to auto-populate the inputs and outputs for the EncapsulatedAssertionConfig.
     */
    public CreateEncapsulatedAssertionAction(@NotNull final EncapsulatedAssertionConfig config, @Nullable Runnable callback, final boolean promptForAutoPopulate) {
        super(new AttemptedCreate(EntityType.ENCAPSULATED_ASSERTION), NAME, DESC, callback);
        Validate.isTrue(config.getGuid() == null, "The EncapsulatedAssertionConfig has already been persisted.");
        this.config = config;
        this.promptForAutoPopulate = promptForAutoPopulate;
    }

    @Override
    protected void performAction() {
        if (config.getPolicy() != null && promptForAutoPopulate) {
            DialogDisplayer.showConfirmDialog(TopComponents.getInstance().getTopParent(), "Auto-populate inputs and outputs for the encapsulated assertion?",
                    "Confirm Auto-Population of Inputs and Outputs",
                    JOptionPane.YES_NO_OPTION,
                    new DialogDisplayer.OptionListener() {
                        @Override
                        public void reportResult(final int option) {
                            showConfigDialog(false, config, option == JOptionPane.YES_OPTION);
                        }
                    });
        } else {
            showConfigDialog(false, config, false);
        }
    }
}
