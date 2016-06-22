package com.l7tech.external.assertions.amqpassertion.console;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

/**
 * Created by IntelliJ IDEA.
 * User: ashah
 * Date: 06/03/12
 * Time: 10:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class AmqpDestinationsAction extends SecureAction {

    public AmqpDestinationsAction() {
        super(new AttemptedAnyOperation(EntityType.GENERIC));
    }

    @Override
    protected void performAction() {
        AMQPDestinationsDialog dialog = new AMQPDestinationsDialog(TopComponents.getInstance().getTopParent());
        dialog.pack();
        Utilities.centerOnScreen(dialog);
        DialogDisplayer.display(dialog);
    }

    @Override
    public String getName() {
        return "Manage AMQP Destinations";
    }

    @Override
    public String getDescription() {
        return "Create, edit and remove configurations for AMQP Destinations.";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Bean16.gif";
    }
}
