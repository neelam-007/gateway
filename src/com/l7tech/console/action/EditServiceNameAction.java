package com.l7tech.console.action;

import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.panels.EditServiceNameDialog;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesTree;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>EditServiceNameAction</code> invokes the service name
 * edit dialog.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class EditServiceNameAction extends NodeAction {
    static final Logger log = Logger.getLogger(EditServiceNameAction.class.getName());
    private String lastServiceName; // remeber old name fro for rename property event

    public EditServiceNameAction(ServiceNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Rename";
    }

    /**
     * @return the action description
     */
    public String getDescription() {
        return "Edit the service name";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Edit16.gif";
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
                      ComponentRegistry wm =
                        Registry.getDefault().getWindowManager();
                      PublishedService svc = ((ServiceNode)node).getPublishedService();
                      lastServiceName = svc.getName();
                      EditServiceNameDialog d =
                        new EditServiceNameDialog(wm.getMainWindow(), svc, nameChangeListener);
                      d.show();
                  } catch (Exception e) {
                      //todo: ErroManager someday?
                      e.printStackTrace();
                  }
              }
          });
    }

    private EntityListener
      nameChangeListener = new EntityListenerAdapter() {
          /**
           * Fired when an set of children is updated.
           * @param ev event describing the action
           */
          public void entityUpdated(EntityEvent ev) {
              SwingUtilities.invokeLater(new Runnable() {
                  /** */
                  public void run() {
                      ServiceNode n = ((ServiceNode)node);
                      n.clearServiceHolder();
                      JTree tree =
                        (JTree)ComponentRegistry.getInstance().getComponent(ServicesTree.NAME);
                      if (tree != null) {
                          DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                          model.nodeChanged(node);
                          try {
                              PublishedService svc = ((ServiceNode)node).getPublishedService();
                              node.firePropertyChange(this, "service.name", lastServiceName, svc.getName());
                          } catch (FindException e) {
                              e.printStackTrace();
                          }
                      } else {
                          log.log(Level.WARNING, "Unable to reach the service tree.");
                      }
                  }
              });
          }
      };

}
