package com.l7tech.console.action;

import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CommentAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;

import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>NewPolicyAction</code> action creates and saves a new {@link com.l7tech.common.policy.Policy}.
 */
public class NewPolicyAction extends SecureAction {
    private static final Logger logger = Logger.getLogger(NewPolicyAction.class.getName());
    private AllAssertion root;

    public NewPolicyAction() {
        super(null);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "New";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Create a new policy";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/New16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        TopComponents windowManager = TopComponents.getInstance();
        WorkSpacePanel wpanel = windowManager.getCurrentWorkspace();
        try {
            // clear work space here will prompt user to save or cancel the changes in the current policy first
            // it makes sure the user will see the updated policy if the policy is saved
            wpanel.clearWorkspace();

            TopComponents topComponents = TopComponents.getInstance();
            topComponents.unregisterComponent(PolicyTree.NAME);
            PolicyTree policyTree = (PolicyTree)topComponents.getPolicyTree();

            root = new AllAssertion();
            root.addChild(new CommentAssertion("Todo, add your policy here."));
            PolicyEditorPanel.PolicyEditorSubject subject = new PolicyEditorPanel.PolicyEditorSubject() {
                public ServiceNode getPolicyNode() {return null;}
                public Assertion getRootAssertion() {
                    return root;
                }
                public String getName() {
                    return "New Policy";
                }
                public void addPropertyChangeListener(PropertyChangeListener servicePropertyChangeListener) {
                    // todo?
                }
                public void removePropertyChangeListener(PropertyChangeListener servicePropertyChangeListener) {
                    // todo
                }
                public boolean hasWriteAccess(){return true;}
            };
            final PolicyEditorPanel pep = new PolicyEditorPanel(subject, policyTree, false, !TopComponents.getInstance().isApplet());
            wpanel.setComponent(pep);
            wpanel.addWorkspaceContainerListener(pep);
            TopComponents.getInstance().firePolicyEdit(pep);
        } catch (ActionVetoException e) {
            logger.log(Level.WARNING, "vetoed!", e);
        } catch (Exception e) {
            // todo
            logger.log(Level.SEVERE, "unhandled exception", e);
        }
    }
}
