package com.l7tech.console.action;

import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteAll;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;

/**
 * Action to delete multiple selected nodes, regardless of EntityType.
 *
 * @author jwilliams
 * @version 1.0 2012/08/22
 */
public class DeleteTargetsAction extends SecureAction {

    private static final String DELETE_TARGETS = "Delete Targets";
    private static final String CONFIRM_MESSAGE = "Are you sure you want to delete multiple selected targets?";

    public DeleteTargetsAction() {
        /**
         * null for AttemptedOperation so DeleteTargetsAction always passes authorization checks
         * proper authorization checks will be made for each target when their deletion action is invoked
         */
        super(null);
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return DELETE_TARGETS;
    }

    /**
     * @return the action description
     */
    @Override
    public String getDescription() {
        return DELETE_TARGETS;
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/delete.gif";
    }

    /**
     * Confirm multiple target deletion and call ServicesAndPoliciesTree delete methods
     */
    @Override
    protected void performAction() {
        DialogDisplayer.showSafeConfirmDialog(
                TopComponents.getInstance().getTopParent(),
                CONFIRM_MESSAGE,
                DELETE_TARGETS,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == JOptionPane.YES_OPTION) {
                            final ServicesAndPoliciesTree servicesAndPoliciesTree =
                                    (ServicesAndPoliciesTree)TopComponents.getInstance()
                                            .getComponent(ServicesAndPoliciesTree.NAME);
                            servicesAndPoliciesTree.deleteSelectedEntities();
                        }
                    }
                }
        );
    }
}
