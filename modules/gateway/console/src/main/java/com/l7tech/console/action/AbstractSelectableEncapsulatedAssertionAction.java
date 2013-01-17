package com.l7tech.console.action;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.gateway.common.security.rbac.AttemptedReadSpecific;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.sun.istack.Nullable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.*;

/**
 * Parent action which allows the user to select an EncapsulatedAssertionConfig from a collection and then perform an operation on the selected EncapsulatedAssertionConfig.
 */
public abstract class AbstractSelectableEncapsulatedAssertionAction extends AbstractEncapsulatedAssertionAction {
    /**
     * Key = EncapsulatedAssertionConfig name (name is unique for each EncapsulatedAssertionConfig).
     * Value = EncapsulatedAssertionConfig
     */
    private final Map<String, EncapsulatedAssertionConfig> configs;

    /**
     * @return the OperationType to perform on the selected EncapsulatedAssertionConfig.
     */
    @NotNull
    protected abstract OperationType getOperationType();

    /**
     * @return whether this action supports auto-population of input and output parameters for new EncapsulatedAssertionConfigs.
     */
    protected abstract boolean autoPopulateParams();

    /**
     * @param configs  the EncapsulatedAssertionConfigs that the user can choose from. Each one should already exist in the database.
     * @param name     the name of the action.
     * @param desc     the description of the action.
     * @param callback callback to execute after the operation has succeeded.
     */
    protected AbstractSelectableEncapsulatedAssertionAction(@NotNull final Collection<EncapsulatedAssertionConfig> configs, @NotNull final String name, @NotNull final String desc, @Nullable final Runnable callback) {
        // attemptedOperation is null because we override the isAuthorized() method
        super(null, name, desc, callback);
        this.configs = new HashMap<String, EncapsulatedAssertionConfig>(configs.size());
        for (final EncapsulatedAssertionConfig config : configs) {
            // filter any null configs or ones that have not been persisted yet
            if (config != null && config.getGuid() != null) {
                this.configs.put(config.getName(), config);
            }
        }
    }

    /**
     * @return true if the user is able to perform the operation on at least one of the given EncapsulatedAssertionConfigs.
     */
    @Override
    public boolean isAuthorized() {
        if (!Registry.getDefault().isAdminContextPresent()) {
            return false;
        }
        final List<String> invalid = new ArrayList<String>();
        for (final EncapsulatedAssertionConfig config : configs.values()) {
            if (config.getGuid() == null || !getSecurityProvider().hasPermission(createAttemptedOperation(config))) {
                invalid.add(config.getName());
            }
        }
        for (final String toRemove : invalid) {
            configs.remove(toRemove);
        }
        return !configs.isEmpty();
    }

    /**
     * Displays a dialog for the user to select the EncapsulatedAssertionConfig that they want to operate on and then
     * opens an EncapsulatedAssertionConfigPropertiesDialog relevant for the operation.
     * <p/>
     * If there is only one EncapsulatedAssertionConfig to choose from, the selection dialog is not shown.
     */
    @Override
    protected void performAction() {
        final boolean readOnly = getOperationType().equals(OperationType.READ);
        if (configs.size() == 1) {
            showConfigDialog(readOnly, configs.values().iterator().next(), autoPopulateParams());
        } else {
            DialogDisplayer.showInputDialog(TopComponents.getInstance().getTopParent(),
                    "Please select an Encapsulated Assertion", "Select Encapsulated Assertion",
                    JOptionPane.QUESTION_MESSAGE, null, configs.keySet().toArray(), configs.keySet().iterator().next(),
                    new DialogDisplayer.InputListener() {
                        @Override
                        public void reportResult(final Object option) {
                            if (option != null) {
                                final String configName = option.toString();
                                showConfigDialog(readOnly, configs.get(configName), autoPopulateParams());
                            }
                        }
                    });
        }
    }

    /**
     * @param config the EncapsulatedAssertionConfig which will be operated on.
     * @return an AttemptedOperation relevant to the OperationType returned by getOperationType().
     * @throws UnsupportedOperationException if the OperationType is not supported.
     */
    private AttemptedOperation createAttemptedOperation(final EncapsulatedAssertionConfig config) {
        switch (getOperationType()) {
            case UPDATE:
                return new AttemptedUpdate(EntityType.ENCAPSULATED_ASSERTION, config);
            case READ:
                return new AttemptedReadSpecific(EntityType.ENCAPSULATED_ASSERTION, config);
            default:
                throw new UnsupportedOperationException("Operation type " + getOperationType() + " is not supported");
        }
    }
}
