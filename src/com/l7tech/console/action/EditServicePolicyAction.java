package com.l7tech.console.action;

import com.l7tech.common.security.rbac.AttemptedUpdate;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;

import java.beans.PropertyChangeListener;
import java.io.IOException;
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
     * default constructor. invoke the policy validate if
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
        this(node, true);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Policy Assertions";
    }

    /**
     * @return the action description
     */
    public String getDescription() {
        return "Edit Web service policy assertions";
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
    protected void performAction() {
        final ServiceNode serviceNode = (ServiceNode)node;
        try {
            TopComponents windowManager = TopComponents.getInstance();
            WorkSpacePanel wpanel = windowManager.getCurrentWorkspace();

            // clear work space here will prompt user to save or cancel the changes in the current policy first
            // it makes sure the user will see the updated policy if the policy is saved
            wpanel.clearWorkspace();

            serviceNode.clearServiceHolder();
            TopComponents topComponents = TopComponents.getInstance();
            topComponents.unregisterComponent(PolicyTree.NAME);
            PolicyTree policyTree = (PolicyTree)topComponents.getPolicyTree();

            PolicyEditorPanel.PolicyEditorSubject subject = new PolicyEditorPanel.PolicyEditorSubject() {
                public ServiceNode getServiceNode() {return serviceNode;}
                public Assertion getRootAssertion() {
                    try {
                        return serviceNode.getPublishedService().rootAssertion();
                    } catch (IOException e) {
                        log.log(Level.SEVERE, "cannot get service", e);
                        throw new RuntimeException(e);
                    } catch (FindException e) {
                        log.log(Level.SEVERE, "cannot get service", e);
                        throw new RuntimeException(e);
                    }
                }
                public String getName() {
                    return serviceNode.getName();
                }
                public void addPropertyChangeListener(PropertyChangeListener servicePropertyChangeListener) {
                    serviceNode.addPropertyChangeListener(servicePropertyChangeListener);
                }
                public void removePropertyChangeListener(PropertyChangeListener servicePropertyChangeListener) {
                    serviceNode.removePropertyChangeListener(servicePropertyChangeListener);
                }
                public boolean hasWriteAccess() {
                    try {
                        return Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(EntityType.SERVICE, serviceNode.getPublishedService()));
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Error performing permisison check", e);
                        throw new RuntimeException(e);
                    }
                }
            };
            final PolicyEditorPanel pep = new PolicyEditorPanel(subject, policyTree, validate);
            wpanel.setComponent(pep);
            wpanel.addWorkspaceContainerListener(pep);
            TopComponents.getInstance().firePolicyEdit(pep);
        } catch (ActionVetoException e) {
            // action vetoed
            log.log(Level.WARNING, "vetoed!", e);
        } catch (Exception e) {
            ErrorManager.getDefault().
              notify(Level.SEVERE, e, "Unable to retrieve service properties");
        }
    }

}
