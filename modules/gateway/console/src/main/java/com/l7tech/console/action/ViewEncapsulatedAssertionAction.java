package com.l7tech.console.action;

import com.l7tech.console.panels.encass.EncapsulatedAssertionConfigPropertiesDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedReadSpecific;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import org.jetbrains.annotations.NotNull;

/**
 * Opens a read-only view of an EncapsulatedAssertionConfig.
 */
public class ViewEncapsulatedAssertionAction extends SecureAction {
    private static final String NAME = "Encapsulated Assertion Properties";
    private static final String DESC = "View the Encapsulated Assertion properties";
    private static final String ICON = "com/l7tech/console/resources/star16.gif";
    private final EncapsulatedAssertionConfig config;

    public ViewEncapsulatedAssertionAction(@NotNull final EncapsulatedAssertionConfig config) {
        super(new AttemptedReadSpecific(EntityType.ENCAPSULATED_ASSERTION, config), NAME, DESC, ICON);
        this.config = config;
    }

    @Override
    protected void performAction() {
        final EncapsulatedAssertionConfigPropertiesDialog dlg = new EncapsulatedAssertionConfigPropertiesDialog(TopComponents.getInstance().getTopParent(), config, true);
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, null);
    }
}
