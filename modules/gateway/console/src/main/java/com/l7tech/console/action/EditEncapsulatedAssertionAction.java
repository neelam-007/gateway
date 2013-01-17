package com.l7tech.console.action;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Action for selecting and/or editing an EncapsulatedAssertionConfig.
 */
public class EditEncapsulatedAssertionAction extends AbstractSelectableEncapsulatedAssertionAction {
    private static final String NAME = "Encapsulated Assertion Properties";
    private static final String DESC = "Edit the Encapsulated Assertion properties";

    /**
     * @param configs the EncapsulatedAssertionConfigs that the user can select from to edit.
     */
    public EditEncapsulatedAssertionAction(@NotNull final Collection<EncapsulatedAssertionConfig> configs, @Nullable final Runnable callback) {
        super(configs, NAME, DESC, callback);
    }


    @NotNull
    @Override
    protected OperationType getOperationType() {
        return OperationType.UPDATE;
    }

    @Override
    protected boolean autoPopulateParams() {
        return false;
    }
}
