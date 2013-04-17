package com.l7tech.external.assertions.mqnative.console;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

public class MqNativeCustomAction extends SecureAction {

    public MqNativeCustomAction() {
        super(new AttemptedAnyOperation(EntityType.SSG_ACTIVE_CONNECTOR));
    }

    @Override
    public String getName() {
        return "Manage MQ Native Queues";
    }

    @Override
    public String getDescription() {
        return "Create, edit and remove configurations for MQ Native queues.";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/enableService.gif";
    }

    @Override
    protected void performAction() {
        final MqNativeQueuesWindow listWindow = new MqNativeQueuesWindow( TopComponents.getInstance().getTopParent());
        listWindow.pack();
        Utilities.centerOnParentWindow(listWindow);
        DialogDisplayer.display(listWindow);
    }
}
