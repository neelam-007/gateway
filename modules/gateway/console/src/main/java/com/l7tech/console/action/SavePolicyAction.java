/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.PolicyEntityNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.CircularPolicyException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyCheckpointState;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * The <code>SavePolicyAction</code> action saves the policy and it's
 * assertion tree.
 */
public class SavePolicyAction extends EntityWithPolicyNodeAction<PolicyEntityNode> {
    protected AssertionTreeNode node;
    private final boolean activateAsWell;
    private Map<String, String> fragmentNameGuidMap;

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
    @Override
    public String getName() {
        return getLabel();
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return getShortDesc();
    }

    /**
     * subclasses override this method specifying the resource name
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Save16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    @Override
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

    @Override
    protected OperationType getOperation() {
        return OperationType.UPDATE;
    }

    protected void performAction(String xml) {
        performAction(xml, null);
    }

    protected void performAction(String xml, HashMap<String, Policy> fragments) {
        if (node == null) {
            throw new IllegalStateException("no node specified");
        }
        String name = null;
        try {
            EntityWithPolicyNode policyNode = getEntityWithPolicyNode();
            if (policyNode == null) {
                throw new IllegalArgumentException("No edited policy or service specified");
            }
            final Goid policyGoid;
            final int policyVersion;
            if (policyNode instanceof ServiceNode) {
                final PublishedService svc = ((ServiceNode) policyNode).getEntity();
                name = svc.getName();
                policyGoid = svc.getPolicy().getGoid();
                policyVersion = svc.getPolicy().getVersion();
            } else {
                Policy policy = policyNode.getPolicy();
                name = policy.getName();
                policyGoid = policy.getGoid();
                policyVersion = policy.getVersion();
            }

            PolicyAdmin.SavePolicyWithFragmentsResult result = updatePolicyXml(policyGoid, policyVersion, xml, fragments, activateAsWell);
            fragmentNameGuidMap = result.fragmentNameGuidMap;
            PolicyCheckpointState checkpointState = result.policyCheckpointState;
            final long newVersionOrdinal = checkpointState.getPolicyVersionOrdinal();
            final boolean activationState = checkpointState.isPolicyVersionActive();

            policyNode.clearCachedEntities();

            WorkSpacePanel workspace = TopComponents.getInstance().getCurrentWorkspace();
            if (workspace.getComponent() instanceof PolicyEditorPanel) {
                PolicyEditorPanel pep = (PolicyEditorPanel)workspace.getComponent();
                pep.setOverrideVersionNumber(newVersionOrdinal);
                pep.setOverrideVersionActive(activationState);
                pep.updateHeadings();
            }
        } catch ( ObjectModelException ome) {
            if ( ExceptionUtils.causedBy( ome, CircularPolicyException.class )) {
                JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(),
                      "Unable to save the policy '" + name + "'.\n" +
                      "The policy contains an include that causes circularity.",
                      "Policy include circularity",
                      JOptionPane.ERROR_MESSAGE);
            } else if ( ExceptionUtils.causedBy( ome, StaleUpdateException.class )) {
                JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(),
                      "Unable to save the policy '" + name + "'.\n" +
                      "The policy was modified by another user.",
                      "Policy not updated.",
                      JOptionPane.ERROR_MESSAGE);
            } else if (ExceptionUtils.causedBy(ome, FindException.class)) {
                //got deleted somehow
                JOptionPane.showMessageDialog(
                        TopComponents.getInstance().getTopParent(),
                        "The service or policy was deleted, cannot save changes.  \n Please refresh the service and policy tree.",
                        "Failed to save",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                throw new RuntimeException("Error saving service and policy", ome);                
            }
        } catch (Exception e) {
            throw new RuntimeException("Error saving service and policy",e);
        }
    }

    private static PolicyAdmin.SavePolicyWithFragmentsResult updatePolicyXml(final Goid policyGoid,
                                                                             final int version,
                                                                             final String xml,
                                                                             final HashMap<String, Policy> fragments,
                                                                             final boolean activateAsWell)
            throws FindException, UpdateException, SaveException, PolicyAssertionException {
        Policy policy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(policyGoid);
        if (policy == null)
            throw new FindException("Unable to save policy -- this policy no longer exists");
        if (version != policy.getVersion())
            throw new StaleUpdateException("Unable to save policy, the policy was edited by another user.");

        policy.setXml(xml);
        return Registry.getDefault().getPolicyAdmin().savePolicy(policy, activateAsWell, fragments);
    }

    public Map<String, String> getFragmentNameGuidMap() {
        return fragmentNameGuidMap;
    }
}
