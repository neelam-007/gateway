package com.l7tech.console.action;

import com.l7tech.console.panels.PolicyEditorPanel;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import java.util.logging.Level;

/**
 * The <code>ServicePolicyPropertiesAction</code> invokes the.
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

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        final ServiceNode serviceNode = (ServiceNode)node;
        SwingUtilities.invokeLater(
          new Runnable() {
              public void run() {
                  try {
                      ComponentRegistry windowManager =
                        Registry.getDefault().getWindowManager();
                      WorkSpacePanel wpanel = windowManager.getCurrentWorkspace();

                      wpanel.setComponent(new PolicyEditorPanel(serviceNode));
                  } catch (Exception e) {
                      ErrorManager.getDefault().
                        notify(Level.WARNING, e, "Unable to retrieve service properties "+serviceNode.getName());
                  }
              }
          });
    }

}
