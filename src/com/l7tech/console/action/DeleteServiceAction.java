package com.l7tech.console.action;

import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesTree;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Logger;


/**
 * The <code>DeleteServiceAction</code> action deletes the service
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class DeleteServiceAction extends BaseAction {
    static final Logger log = Logger.getLogger(DeleteServiceAction.class.getName());
    ServiceNode node;

    /**
     * create the acction that deletes the service
     * 
     * @param en the node to delete
     */
    public DeleteServiceAction(ServiceNode en) {
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
        return "Delete the Web service";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/delete.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        if (!Actions.deleteService(node)) return;

        Runnable runnable = new Runnable() {
            public void run() {
                final TopComponents creg = TopComponents.getInstance();
                JTree tree = (JTree)creg.getComponent(ServicesTree.NAME);
                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                model.removeNodeFromParent(node);

                try {
                    final WorkSpacePanel cws = creg.getCurrentWorkspace();
                    JComponent jc = cws.getComponent();
                    if (jc == null || !(jc instanceof PolicyEditorPanel)) {
                        return;
                    }
                    PolicyEditorPanel pe = (PolicyEditorPanel)jc;
                    PublishedService svc = pe.getServiceNode().getPublishedService();
                    // if currently edited service was deleted
                    if (node.getPublishedService().getOid() == svc.getOid()) {
                        cws.clearWorkspace();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

}
