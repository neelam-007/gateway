package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.Cookie;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.service.PublishedService;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;


/**
 * The <code>SavePolicyAction</code> action saves the service and it's
 * assertion tree.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class SavePolicyAction extends SecureAction {
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
            ServiceNode sn = getServiceNodeCookie();
            if (sn == null)
                throw new IllegalArgumentException("No edited service specified");
            PublishedService svc = sn.getPublishedService();

            Assertion rootAssertion = ((AssertionTreeNode)node.getRoot()).asAssertion();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            WspWriter.writePolicy(rootAssertion, bo);
            svc.setPolicyXml(bo.toString());
            Registry.getDefault().getServiceManager().savePublishedService(svc);
            sn.clearServiceHolder();
        } catch (Exception e) {
            throw new RuntimeException("Error saving service and policy",e);
        }
    }

    /**
     * @return the published service cookie or null if not founds
     */
    private ServiceNode getServiceNodeCookie() {
        for (Iterator i = ((AbstractTreeNode)node.getRoot()).cookies(); i.hasNext(); ) {
            Object value = ((Cookie)i.next()).getValue();
            if (value instanceof ServiceNode) return (ServiceNode)value;
        }
        return null;
    }
}
