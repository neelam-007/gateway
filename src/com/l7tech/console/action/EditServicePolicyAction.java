package com.l7tech.console.action;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.PolicyEditorPanel;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import java.util.logging.Level;

/**
 * The <code>ServicePolicyPropertiesAction</code> invokes the service
 * policy editor.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class EditServicePolicyAction extends NodeAction {
    private final boolean validate;

    /**
     * default constructor. invoke the policy vaslidate if
     * specified.
     * 
     * @param node the service node
     * @param b    true validate the policy, false
     */
    public EditServicePolicyAction(ServiceNode node, boolean b) {
        super(node);
        validate = b;
    }

    /**
     * default constructor. invoke the node validate
     * 
     * @param node the service node
     */
    public EditServicePolicyAction(ServiceNode node) {
        this(node, false);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Service policy";
    }

    /**
     * @return the action description
     */
    public String getDescription() {
        return "View/edit service policy assertions";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/policy16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        final ServiceNode serviceNode = (ServiceNode)node;
        try {
            ComponentRegistry windowManager =
              Registry.getDefault().getComponentRegistry();
            WorkSpacePanel wpanel = windowManager.getCurrentWorkspace();

            // clear work space here will prompt user to save or cancel the changes in the current policy first
            // it makes sure the user will see the updated policy if the policy is saved
            wpanel.clearWorkspace();

            serviceNode.clearServiceHolder();
            final PolicyEditorPanel pep = new PolicyEditorPanel(serviceNode);
            if (validate) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        pep.validatePolicy();
                    }
                });
            }
            wpanel.setComponent(pep);
            wpanel.addWorkspaceContainerListener(pep);
        } catch (ActionVetoException e) {
            // action vetoed
        } catch (Exception e) {
            ErrorManager.getDefault().
              notify(Level.SEVERE, e, "Unable to retrieve service properties " + serviceNode.getName());
        }
    }

}
