package com.l7tech.console.action;

import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.service.PublishedService;

import java.io.ByteArrayOutputStream;


/**
 * The <code>SavePolicyAction</code> action saves the service and it's
 * assertion tree.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class SavePolicyAction extends ServiceNodeAction {
    protected AssertionTreeNode node;

    public SavePolicyAction() {
        super(null);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Save";
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
    protected void performAction() {
        if (node == null) {
            throw new IllegalStateException("no node specified");
        }
        try {
            Assertion rootAssertion = ((AssertionTreeNode)node.getRoot()).asAssertion();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            WspWriter.writePolicy(rootAssertion, bo);
            performAction(bo.toString());
        } catch (Exception e) {
            throw new RuntimeException("Error saving service and policy",e);
        }
    }

    protected OperationType getOperation() {
        return OperationType.UPDATE;
    }

    protected void performAction(String xml) {
        if (node == null) {
            throw new IllegalStateException("no node specified");
        }
        try {
            ServiceNode serviceNode = getServiceNode();
            if (serviceNode == null) {
                throw new IllegalArgumentException("No edited service specified");
            }
            PublishedService svc = serviceNode.getPublishedService();
            svc.setPolicyXml(xml);
            Registry.getDefault().getServiceManager().savePublishedService(svc);
            serviceNode.clearServiceHolder();
        } catch (Exception e) {
            throw new RuntimeException("Error saving service and policy",e);
        }
    }
}
