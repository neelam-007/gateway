package com.l7tech.external.assertions.simplegatewaymetricextractor.console;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

@SuppressWarnings("unused")
public class SimpleGatewayMetricExtractorAction extends SecureAction {

    public SimpleGatewayMetricExtractorAction() {
        // modified rbac check as required
        super(new AttemptedAnyOperation(EntityType.GENERIC));
    }

    @Override
    public String getName() {
        return "Configure Simple Gateway Metric Extractor Assertion";
    }

    @Override
    public String getDescription() {
        return "Configure the Simple Gateway Metric Extractor assertion.";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/services16.png";
    }

    @Override
    protected void performAction() {
        SimpleGatewayMetricExtractorDialog dlg = new SimpleGatewayMetricExtractorDialog(TopComponents.getInstance().getTopParent());
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg);
    }
}
