package com.l7tech.console.action;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesTree;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The <code>EnableServiceAction</code> action disables the service
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class EnableServiceAction extends SecureAction {
    static final Logger log = Logger.getLogger(EnableServiceAction.class.getName());
    ServiceNode node;

    /**
     * create the aciton that disables the service
     * @param en the node to deleteEntity
     */
    public EnableServiceAction(ServiceNode en) {
        super(null);
        node = en;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Enable";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Enable the Web service";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/enableService.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        try {
            final PublishedService publishedService = node.getPublishedService();
            publishedService.setDisabled(false);
            Registry.getDefault().getServiceManager().savePublishedService(publishedService);
            node.clearServiceHolder();
            JTree tree =
              (JTree)TopComponents.getInstance().getComponent(ServicesTree.NAME);
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            model.nodeChanged(node);
        } catch (Exception e) {
            ErrorManager.
              getDefault().
              notify(Level.WARNING, e, "The system reported an error while saving the service.");
        }

    }
}
