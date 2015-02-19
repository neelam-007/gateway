package com.l7tech.console.action;

import com.l7tech.console.panels.encass.EncapsulatedAssertionConfigPropertiesDialog;
import com.l7tech.console.policy.EncapsulatedAssertionRegistry;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.VersionException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Parent for all actions that operate on an EncapsulatedAssertionConfig.
 */
public abstract class AbstractEncapsulatedAssertionAction extends SecureAction {
    protected static final String ICON = "com/l7tech/console/resources/star16.gif";
    protected static final String ERROR_MSG = "Unable to save encapsulated assertion configuration";
    private final Runnable callback;

    /**
     * @param attemptedOperation the operation which will be attempted on the EncapsulatedAssertionConfig.
     * @param name               the name of the action.
     * @param desc               the description of the action.
     * @param callback           callback to execute after successful operation.
     */
    protected AbstractEncapsulatedAssertionAction(@Nullable final AttemptedOperation attemptedOperation, @NotNull final String name, @NotNull final String desc, @Nullable Runnable callback) {
        super(attemptedOperation, name, desc, ICON);
        this.callback = callback;
    }

    /**
     * Displays the EncapsulatedAssertionConfigPropertiesDialog and saves the EncapsulatedAssertionConfig on dialog confirmation if not readOnly.
     *
     * @param readOnly           whether the dialog should be opened in readOnly mode (no modifications allowed).
     * @param config             the EncapsulatedAssertionConfig to display.
     * @param autoPopulateParams whether the EncapsulatedAssertionConfigPropertiesDialog should detect and auto-populate the input and output parameters for new EncapsulatedAssertionConfigs.
     */
    protected void showConfigDialog(final boolean readOnly, @NotNull final EncapsulatedAssertionConfig config, final boolean autoPopulateParams) {
        final Goid oldEncassGoid = config.getGoid();
        final String oldPolicyGuid = config.getProperty( EncapsulatedAssertionConfig.PROP_POLICY_GUID );
        final Collection<EncapsulatedAssertionConfig> existingConfigs = TopComponents.getInstance().getEncapsulatedAssertionRegistry().getRegisteredEncapsulatedAssertionConfigurations();
        final Set<String> usedConfigNames = new HashSet<String>(existingConfigs.size());
        for (final EncapsulatedAssertionConfig existingConfig : existingConfigs) {
            usedConfigNames.add(existingConfig.getName());
        }
        final EncapsulatedAssertionConfigPropertiesDialog dlg = new EncapsulatedAssertionConfigPropertiesDialog(TopComponents.getInstance().getTopParent(), config, readOnly, usedConfigNames, autoPopulateParams);
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed() && !readOnly) {
                    try {
                        Registry.getDefault().getEncapsulatedAssertionAdmin().saveEncapsulatedAssertionConfig(config);
                        final EncapsulatedAssertionRegistry encapsulatedAssertionRegistry = TopComponents.getInstance().getEncapsulatedAssertionRegistry();
                        encapsulatedAssertionRegistry.updateEncapsulatedAssertions();

                        // Ensure policy nodes get updated if we created a new encass config or changed the policy of an existing one
                        if ( policiesFolderNodeRefreshRequired( config, oldEncassGoid, oldPolicyGuid ) ) {
                            SwingUtilities.invokeLater( new Runnable() {
                                @Override
                                public void run() {
                                    TopComponents.getInstance().refreshPoliciesFolderNode();
                                    TopComponents.getInstance().getTopParent().repaint();
                                }
                            } );
                        }

                        executeCallback();
                    } catch (final ObjectModelException e) {
                        handleException(e);
                    } catch (final VersionException e) {
                        handleException(e);
                    }
                } else if (dlg.isConfirmed()) {
                    executeCallback();
                }
            }
        });
    }

    /**
     * Called after the properties dialog is confirmed and an entity is saved in order
     * to check if the policies folder node needs to be refreshed after an encass config change.
     * <p/>
     * This method guesses that a refresh is required if the old Goid was unsaved or if the policy GUID was changed.
     *
     * @param savedConfig the (possibly newly-created) encass config, as it was just saved to the server.
     * @param oldConfigGoid the Goid of this encass config before it was created or edited
     * @param oldPolicyGuid the policy GUID property of this encass config before it was created or edited
     * @return true if a refresh of the services and policies tree may be required in order to update policy icons
     */
    protected boolean policiesFolderNodeRefreshRequired( @NotNull EncapsulatedAssertionConfig savedConfig, @Nullable Goid oldConfigGoid, @Nullable String oldPolicyGuid ) {
        return Goid.isDefault( oldConfigGoid ) || ( oldPolicyGuid != null && !oldPolicyGuid.equals( savedConfig.getProperty( EncapsulatedAssertionConfig.PROP_POLICY_GUID ) ) );
    }

    private void executeCallback() {
        if (callback != null) {
            callback.run();
        }
    }

    private void handleException(final Exception e) {
        logger.log(Level.WARNING, ERROR_MSG + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), ERROR_MSG, "Error", JOptionPane.ERROR_MESSAGE, null);
    }
}
