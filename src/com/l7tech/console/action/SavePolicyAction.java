/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.common.policy.Policy;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.PolicyEntityNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.service.PublishedService;

import java.io.ByteArrayOutputStream;

/**
 * The <code>SavePolicyAction</code> action saves the policy and it's
 * assertion tree.
 */
public class SavePolicyAction extends PolicyNodeAction {
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
        return "Save the policy";
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
            PolicyEntityNode policyNode = getPolicyNode();
            if (policyNode == null) {
                throw new IllegalArgumentException("No edited policy or service specified");
            }
            if (policyNode instanceof ServiceNode) {
                final PublishedService svc = ((ServiceNode) policyNode).getPublishedService();
                svc.getPolicy().setXml(xml);
                Registry.getDefault().getServiceManager().savePublishedService(svc);
            } else {
                Policy policy = policyNode.getPolicy();
                policy.setXml(xml);
                Registry.getDefault().getPolicyAdmin().savePolicy(policy);
            }
            policyNode.clearCachedEntities();
        } catch (Exception e) {
            throw new RuntimeException("Error saving service and policy",e);
        }
    }
}
