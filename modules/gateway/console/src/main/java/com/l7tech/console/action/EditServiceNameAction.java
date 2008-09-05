package com.l7tech.console.action;

import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.panels.EditServiceNameDialog;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>EditServiceNameAction</code> invokes the service name
 * edit dialog.
 */
public class EditServiceNameAction extends ServiceNodeAction {
    static final Logger log = Logger.getLogger(EditServiceNameAction.class.getName());
    private String lastServiceName; // remeber old name fro for rename property event

    public EditServiceNameAction(ServiceNode node) {
        super(node);
    }

    protected OperationType getOperation() {
        return OperationType.UPDATE;
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
                      PublishedService svc = ((ServiceNode)node).getEntity();
                      lastServiceName = svc.getName();
                      EditServiceNameDialog d =
                        new EditServiceNameDialog(wm.getTopParent(), svc, nameChangeListener);
                      DialogDisplayer.display(d);
                  } catch (Exception e) {
                      throw new RuntimeException(e);
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
                      n.clearCachedEntities();
                      JTree tree =
                        (JTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                      if (tree != null) {
                          DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                          model.nodeChanged(node);
                          try {
                              PublishedService svc = ((ServiceNode)node).getEntity();
                              node.firePropertyChange(this, PolicyEditorPanel.POLICYNAME_PROPERTY,
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
