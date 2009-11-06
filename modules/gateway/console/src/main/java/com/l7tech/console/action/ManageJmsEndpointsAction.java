package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.console.panels.JmsQueuesWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.JmsRoutingAssertion;

import java.util.logging.Logger;


/**
 * The <code>ManageJmsEndpointsAction</code>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ManageJmsEndpointsAction extends SecureAction {
    static final Logger log = Logger.getLogger(ManageJmsEndpointsAction.class.getName());

    /**
     * create the aciton that disables the service
     */
    public ManageJmsEndpointsAction() {
        super(new AttemptedAnyOperation(EntityType.JMS_ENDPOINT), JmsRoutingAssertion.class);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Manage JMS Queues";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View and manage JMS queues";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/enableService.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        JmsQueuesWindow jqw = new JmsQueuesWindow(TopComponents.getInstance().getTopParent());
        jqw.pack();
        Utilities.centerOnScreen(jqw);
        DialogDisplayer.display(jqw);
    }


}
