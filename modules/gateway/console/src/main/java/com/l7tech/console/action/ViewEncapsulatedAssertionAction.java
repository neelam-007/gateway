package com.l7tech.console.action;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Action for selecting and/or viewing an EncapsulatedAssertionConfig.
 */
public class ViewEncapsulatedAssertionAction extends AbstractSelectableEncapsulatedAssertionAction {
    private static final String NAME = "Encapsulated Assertion Properties";
    private static final String DESC = "View the Encapsulated Assertion properties";

    public ViewEncapsulatedAssertionAction(@NotNull final Collection<EncapsulatedAssertionConfig> configs, @Nullable Runnable callback) {
        super(configs, NAME, DESC, callback);
    }

    @NotNull
    @Override
    protected OperationType getOperationType() {
        return OperationType.READ;
    }

    @Override
    protected boolean autoPopulateParams() {
        return false;
    }
}
