package com.l7tech.console.action;

import com.l7tech.console.panels.*;
import com.l7tech.console.tree.*;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.PublishedService;

import javax.swing.*;

/**
 * The <code>UserPropertiesAction</code> edits the user entity.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ServicePolicyPropertiesAction extends NodeAction {

    public ServicePolicyPropertiesAction(ServiceNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Policy properties";
    }

    /**
     * @return the aciton description
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

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        SwingUtilities.invokeLater(
          new Runnable() {
              public void run() {
                  try {
                      WorkSpacePanel wpanel =
                        Registry.getDefault().
                        getWindowManager().getCurrentWorkspace();
                      PublishedService svc = ((ServiceNode)node).getPublishedService();
                      PolicyTreeModel model = new PolicyTreeModel(svc.rootAssertion());
                      JTree tree = new JTree(model);
                      tree.setCellRenderer(new EntityTreeCellRenderer());
                      tree.setName(svc.getName());
                      wpanel.setComponent(tree);
                  } catch (Exception e) {
                      //todo: ErroManager someday?
                      e.printStackTrace();
                  }
              }
          });
    }
}
