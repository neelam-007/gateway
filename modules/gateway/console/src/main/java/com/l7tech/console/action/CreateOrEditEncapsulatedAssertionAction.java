package com.l7tech.console.action;

import com.l7tech.console.panels.encass.EncapsulatedAssertionConfigPropertiesDialog;
import com.l7tech.console.policy.EncapsulatedAssertionRegistry;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreateSpecific;
import com.l7tech.gateway.common.security.rbac.AttemptedReadSpecific;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.VersionException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.logging.Level;

/**
 * Action which can create/edit an EncapsulatedAssertionConfig.
 */
public class CreateOrEditEncapsulatedAssertionAction extends SecureAction {
    private static final String CREATE = "Create Encapsulated Assertion";
    private static final String EDIT = "Encapsulated Assertion Properties";
    private static final String CREATE_DESC = "Create an Encapsulated Assertion from policy";
    private static final String EDIT_DESC = "Edit the Encapsulated Assertion properties";
    private static final String ICON = "com/l7tech/console/resources/star16.gif";
    private static final String ERROR_MSG = "Unable to save encapsulated assertion configuration";
    private final EncapsulatedAssertionConfig config;
    private final Runnable callback;

    /**
     * @param config the EncapsulatedAssertionConfig to edit or create
     * @param callback optional callback to execute after successful create/edit
     */
    public CreateOrEditEncapsulatedAssertionAction(@NotNull final EncapsulatedAssertionConfig config, @Nullable Runnable callback) {
        super(config.getGuid() == null ? new AttemptedCreateSpecific(EntityType.ENCAPSULATED_ASSERTION, config) : new AttemptedUpdate(EntityType.ENCAPSULATED_ASSERTION, config),
                config.getGuid() == null ? CREATE : EDIT, config.getGuid() == null ? CREATE_DESC : EDIT_DESC, ICON);
        this.config = config;
        this.callback = callback;
    }

    @Override
    protected void performAction() {
        final EncapsulatedAssertionConfigPropertiesDialog dlg = new EncapsulatedAssertionConfigPropertiesDialog(TopComponents.getInstance().getTopParent(), config, false);
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    try {
                        Registry.getDefault().getEncapsulatedAssertionAdmin().saveEncapsulatedAssertionConfig(config);
                        final EncapsulatedAssertionRegistry encapsulatedAssertionRegistry = TopComponents.getInstance().getEncapsulatedAssertionRegistry();
                        encapsulatedAssertionRegistry.updateEncapsulatedAssertions();
                        if (callback != null) {
                            callback.run();
                        }
                    } catch (final ObjectModelException e) {
                        handleException(e);
                    } catch (final VersionException e) {
                        handleException(e);
                    }
                }
            }
        });
    }

    private void handleException(final Exception e) {
        logger.log(Level.WARNING, ERROR_MSG + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), ERROR_MSG, "Error", JOptionPane.ERROR_MESSAGE, null);
    }
}
