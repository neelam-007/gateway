package com.l7tech.console.action;

import com.l7tech.gateway.common.security.rbac.AttemptedCreateSpecific;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Action which creates an EncapsulatedAssertionConfig.
 */
public class CreateEncapsulatedAssertionAction extends AbstractEncapsulatedAssertionAction {
    private static final String NAME = "Create Encapsulated Assertion";
    private static final String DESC = "Create an Encapsulated Assertion from policy";
    private final EncapsulatedAssertionConfig config;

    public CreateEncapsulatedAssertionAction(@NotNull final EncapsulatedAssertionConfig config, @Nullable Runnable callback) {
        super(new AttemptedCreateSpecific(EntityType.ENCAPSULATED_ASSERTION, config), NAME, DESC, callback);
        Validate.isTrue(config.getGuid() == null, "The EncapsulatedAssertionConfig has already been persisted.");
        this.config = config;
    }

    @Override
    protected void performAction() {
        showConfigDialog(false, config);
    }
}
