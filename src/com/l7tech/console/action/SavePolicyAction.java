package com.l7tech.console.action;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.WindowManager;
import com.l7tech.console.MainWindow;
import com.l7tech.service.PublishedService;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;
import java.util.logging.Level;
import java.io.ByteArrayOutputStream;


/**
 * The <code>SavePolicyAction</code> action saves the service and it's
 * assertion tree.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class SavePolicyAction extends BaseAction {
    protected AssertionTreeNode node;

    public SavePolicyAction() {
    }

    public SavePolicyAction(AssertionTreeNode node) {
        if (node == null) {
            throw new IllegalArgumentException();
        }
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
            JTree tree = WindowManager.getInstance().getPolicyTree();
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            PublishedService svc = model.getService();
            Assertion rootAssertion = ((AssertionTreeNode)node.getRoot()).asAssertion();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            WspWriter.writePolicy(rootAssertion, bo);
            svc.setPolicyXml(bo.toString());
            Registry.getDefault().getServiceManager().update(svc);
        } catch (Exception e) {
            ErrorManager.getDefault().notify(Level.WARNING, e, "Error saving service and policy");
        }
    }
}
