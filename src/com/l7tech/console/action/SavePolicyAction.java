package com.l7tech.console.action;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.WindowManager;
import com.l7tech.console.util.Registry;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.service.PublishedService;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Level;


/**
 * The <code>SavePolicyAction</code> action saves the service and it's
 * assertion tree.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class SavePolicyAction extends BaseAction {
    protected ServiceNode node;

    public SavePolicyAction() {
    }

    public SavePolicyAction(ServiceNode node) {
        this.node = node;
    }
    /**
     * @return the action name
     */
    public String getName() {
        return "Save policy";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Save the service policy";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Save16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        if (node == null) {
            throw new IllegalStateException("no node specified");
        }
        try {
            PublishedService svc = node.getPublishedService();
            Registry.getDefault().getServiceManager().save(svc);
        } catch (Exception e) {
            ErrorManager.getDefault().notify(Level.WARNING, e, "Error saving service and policy");
        }
    }
}
