package com.l7tech.console.action;

import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesTree;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.service.PublishedService;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Logger;


/**
 * The <code>DeleteServiceAction</code> action deletes the service
 * such as .
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class DeleteServiceAction extends BaseAction {
    static final Logger log = Logger.getLogger(DeleteServiceAction.class.getName());
    ServiceNode node;

    /**
     * create the acciton that deletes
     * @param en the node to deleteEntity
     */
    public DeleteServiceAction(ServiceNode en) {
        node = en;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Delete";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Delete";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/delete.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        boolean deleted = Actions.deleteService(node);
        if (deleted) {
            JTree tree =
              (JTree)ComponentRegistry.getInstance().getComponent(ServicesTree.NAME);
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            model.removeNodeFromParent(node);
            tree = (JTree)ComponentRegistry.
              getInstance().getComponent(PolicyTree.NAME);
            ServiceNode sn =  (ServiceNode)tree.getClientProperty("service.node");
            if (sn == null) return;

            try {
                PublishedService svc = sn.getPublishedService();
                // if currently edited service was deleted
                if (node.getPublishedService().getOid() == svc.getOid()) {
                    ComponentRegistry.getInstance().getCurrentWorkspace().clearWorskpace();
                }
            } catch (FindException e) {
                e.printStackTrace();
            }
        }
    }
}
