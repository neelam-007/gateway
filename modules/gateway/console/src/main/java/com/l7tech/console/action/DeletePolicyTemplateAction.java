package com.l7tech.console.action;

import com.l7tech.console.tree.AssertionsTree;
import com.l7tech.console.tree.PolicyTemplateNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Logger;
import java.io.File;
import java.awt.*;


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
        super(null);
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
        final Frame topFrame = TopComponents.getInstance().getTopParent();
        DialogDisplayer.showConfirmDialog(topFrame,
                "Are you sure you want to delete template " + node.getName() + "?",
                "Delete Policy Template",
                JOptionPane.YES_NO_OPTION,
                new DialogDisplayer.OptionListener() {
                    public void reportResult(int option) {
                        if (option == JOptionPane.YES_OPTION) {
                            // Delete the node and update the tree
                            File file = node.getFile();
                            if (file.exists()) {
                                if (!file.delete()) {
                                    DialogDisplayer.showMessageDialog(topFrame,
                                            "Failed to delete '" + node.getFile().getPath() + "'.",
                                            "Policy template delete failed",
                                            JOptionPane.WARNING_MESSAGE, null);
                                } else {
                                    //update tree
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run() {
                                            JTree tree = (JTree) TopComponents.getInstance().getComponent(AssertionsTree.NAME);
                                            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                                            model.removeNodeFromParent(node);
                                        }
                                    });
                                }
                            } else {
                                DialogDisplayer.showMessageDialog(topFrame,
                                        "Policy template '" + node.getFile().getPath() + "' was changed.  Please refresh for an updated version.",
                                        "Policy template delete failed",
                                        JOptionPane.WARNING_MESSAGE, null);
                            }
                        } else {
                            //user clicked "no", so just do nothing
                        }
                    }
                });
    }
}
