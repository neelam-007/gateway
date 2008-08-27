/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.util.Functions;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.panels.HomePagePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.PolicyEntityNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.PolicyHeader;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Logger;
import java.util.Set;

/**
 * The <code>DeletePolicyAction</code> action deletes a {@link com.l7tech.policy.Policy}.
 */
public class DeletePolicyAction extends PolicyNodeAction {
    static final Logger log = Logger.getLogger(DeletePolicyAction.class.getName());

    /**
     * create the action that deletes the policy
     *
     * @param en the node to delete
     */
    public DeletePolicyAction(PolicyEntityNode en) {
        super(en);
    }

    protected OperationType getOperation() {
        return OperationType.DELETE;
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Delete";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "Delete the Policy";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/delete.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        Actions.deletePolicy((PolicyEntityNode)policyNode, new Functions.UnaryVoid<Boolean>() {
            public void call(Boolean confirmed) {
                if (!confirmed) return;

                Registry.getDefault().getSecurityProvider().refreshPermissionCache();

                Runnable runnable = new Runnable() {
                    public void run() {
                        final TopComponents creg = TopComponents.getInstance();
                        ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree)creg.getComponent(ServicesAndPoliciesTree.NAME);
                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                        model.removeNodeFromParent(node);

                        //Remove an aliases if they exist
                        PolicyHeader pH = (PolicyHeader) node.getUserObject();
                        long oldPolicyOid = pH.getOid();
                        if(!pH.isAlias()){
                            Object root = model.getRoot();
                            RootNode rootNode = (RootNode) root;
                            Set<AbstractTreeNode> foundNodes = rootNode.getAliasesForEntity(oldPolicyOid);
                            for(AbstractTreeNode atn: foundNodes){
                                model.removeNodeFromParent(atn);
                            }
                            tree.removeTrackedEntity(oldPolicyOid);
                        }else{
                            tree.removeTrackedAlias(oldPolicyOid, node);
                        }
                        
                        try {
                            final WorkSpacePanel cws = creg.getCurrentWorkspace();
                            JComponent jc = cws.getComponent();
                            if (jc == null || !(jc instanceof PolicyEditorPanel)) {
                                return;
                            }
                            PolicyEditorPanel pe = (PolicyEditorPanel)jc;
                            // if currently edited service was deleted
                            if (policyNode.getPolicy().getOid() == pe.getPolicyNode().getPolicy().getOid()) {
                                cws.setComponent(new HomePagePanel());
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
                SwingUtilities.invokeLater(runnable);
            }
        });
    }

}
