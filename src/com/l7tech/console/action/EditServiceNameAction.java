package com.l7tech.console.action;

import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.panels.EditServiceNameDialog;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.poleditor.PolicyEditorPanel;
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
        return "Rename the Web service";
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
    protected void performAction() {
        SwingUtilities.invokeLater(
          new Runnable() {
              public void run() {
                  try {
                      TopComponents wm =
                        TopComponents.getInstance();
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
                        (JTree)TopComponents.getInstance().getComponent(ServicesTree.NAME);
                      if (tree != null) {
                          DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                          model.nodeChanged(node);
                          try {
                              PublishedService svc = ((ServiceNode)node).getPublishedService();
                              node.firePropertyChange(this, PolicyEditorPanel.SERVICENAME_PROPERTY,
                                                      lastServiceName, svc.getName());
                          } catch (Exception e) {
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
