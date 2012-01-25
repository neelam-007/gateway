package com.l7tech.external.assertions.mqnative.console;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedEntityOperation;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

public class MqNativeCustomAction extends SecureAction {

    public MqNativeCustomAction() {
        super(new AttemptedEntityOperation(EntityType.SSG_ACTIVE_CONNECTOR,
                new SsgActiveConnector() {
                    public String getType() {
                        return ACTIVE_CONNECTOR_TYPE_MQ_NATIVE;
                    }
                }
            ) {
                @Override
                public OperationType getOperation() {
                    return OperationType.READ;
                }
            }
        );
    }

    @Override
    public String getName() {
        return "Manage MQ Native Connections";
    }

    @Override
    public String getDescription() {
        return "Create, edit and remove configurations for MQ Native connections.";
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
