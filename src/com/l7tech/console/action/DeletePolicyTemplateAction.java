package com.l7tech.console.action;

import com.l7tech.console.tree.AssertionsTree;
import com.l7tech.console.tree.PolicyTemplateNode;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Logger;


/**
 * The <code>DeletePolicyTemplateAction</code> action deletes the
 * policy template assertion palette element
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class DeletePolicyTemplateAction extends SecureAction {
    static final Logger log = Logger.getLogger(DeletePolicyTemplateAction.class.getName());
    PolicyTemplateNode node;

    /**
     * create the acciton that deletes
     * @param en the node to deleteEntity
     */
    public DeletePolicyTemplateAction(PolicyTemplateNode en) {
        node = en;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Delete";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Delete policy template";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/delete.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        boolean deleted = Actions.deletePolicyTemplate(node);
        if (deleted) {
            JTree tree =
              (JTree)TopComponents.getInstance().getComponent(AssertionsTree.NAME);
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            model.removeNodeFromParent(node);
        }
    }
}
