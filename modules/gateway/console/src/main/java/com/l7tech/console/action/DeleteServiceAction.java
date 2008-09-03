package com.l7tech.console.action;

import com.l7tech.console.panels.HomePagePanel;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Logger;
import java.util.Set;


/**
 * The <code>DeleteServiceAction</code> action deletes the service
 */
public class DeleteServiceAction extends ServiceNodeAction {
    static final Logger log = Logger.getLogger(DeleteServiceAction.class.getName());

    /**
     * create the acction that deletes the service
     *
     * @param en the node to delete
     */
    public DeleteServiceAction(ServiceNode en) {
        super(en);
    }

    protected OperationType getOperation() {
        return OperationType.DELETE;
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
    protected void performAction() {
        Actions.deleteService(serviceNode, new Functions.UnaryVoid<Boolean>() {
            public void call(Boolean confirmed) {
                if (!confirmed) return;

                Registry.getDefault().getSecurityProvider().refreshPermissionCache();

                Runnable runnable = new Runnable() {
                    public void run() {
                        final TopComponents creg = TopComponents.getInstance();
                        ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree)creg.getComponent(ServicesAndPoliciesTree.NAME);
                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                        model.removeNodeFromParent(node);

                        //Remove an aliases if they exist
                        ServiceHeader sH = (ServiceHeader) node.getUserObject();
                        long oldServiceOid = sH.getOid();
                        Object root = model.getRoot();
                        RootNode rootNode = (RootNode) root;
                        
                        if(!sH.isAlias()){
                            Set<AbstractTreeNode> foundNodes = rootNode.getAliasesForEntity(oldServiceOid);
                            if(!foundNodes.isEmpty()){
                                for(AbstractTreeNode atn: foundNodes){
                                    model.removeNodeFromParent(atn);
                                }
                                rootNode.removeEntity(oldServiceOid);                                
                            }
                        }else{
                            rootNode.removeAlias(oldServiceOid, node);
                        }

                        try {
                            final WorkSpacePanel cws = creg.getCurrentWorkspace();
                            JComponent jc = cws.getComponent();
                            if (jc == null || !(jc instanceof PolicyEditorPanel)) {
                                return;
                            }
                            PolicyEditorPanel pe = (PolicyEditorPanel)jc;
                            EntityWithPolicyNode pn = pe.getPolicyNode();
                            if (pn instanceof ServiceNode) {
                                PublishedService svc = ((ServiceNode) pn).getPublishedService();
                                // if currently edited service was deleted
                                if (serviceNode.getPublishedService().getOid() == svc.getOid()) {
                                cws.setComponent(new HomePagePanel());
                                }
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
                SwingUtilities.invokeLater(runnable);
            }
        });
    }

}
