/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.common.policy.Policy;
import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.console.tree.PolicyEntityNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import java.io.ByteArrayOutputStream;

/**
 * The <code>SavePolicyAction</code> action saves the policy and it's
 * assertion tree.
 */
public class SavePolicyAction extends PolicyNodeAction {
    protected AssertionTreeNode node;
    private final boolean activateAsWell;

    public SavePolicyAction(boolean activateAsWell) {
        super(null);
        this.activateAsWell = activateAsWell;
        putValue(Action.NAME, getLabel());
        putValue(Action.SHORT_DESCRIPTION, getShortDesc());
    }

    private String getLabel() {
        return activateAsWell ? "Save and Activate" : "Save";
    }

    private String getShortDesc() {
        return activateAsWell
               ? "Save the policy and make this version active"
               : "Save the policy but do not activate this version yet";
    }

    public boolean isActivateAsWell() {
        return activateAsWell;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return getLabel();
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return getShortDesc();
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
                if (activateAsWell) {
                    svc.getPolicy().setXml(xml);
                    Registry.getDefault().getServiceManager().savePublishedService(svc);
                } else {
                    updatePolicyXml(svc.getPolicy().getOid(), xml, activateAsWell);
                }
            } else {
                Policy policy = policyNode.getPolicy();
                updatePolicyXml(policy.getOid(), xml, activateAsWell);
            }
            policyNode.clearCachedEntities();
        } catch (Exception e) {
            throw new RuntimeException("Error saving service and policy",e);
        }
    }

    private static void updatePolicyXml(long policyOid, String xml, boolean activateAsWell) throws FindException, SaveException, PolicyAssertionException {
        Policy policy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(policyOid);
        if (policy == null) throw new SaveException("Unable to save policy -- this policy no longer exists");
        policy.setXml(xml);
        Registry.getDefault().getPolicyAdmin().savePolicy(policy, activateAsWell);
    }
}
