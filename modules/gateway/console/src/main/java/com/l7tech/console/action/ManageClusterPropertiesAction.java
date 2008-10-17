/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 16, 2005<br/>
 */
package com.l7tech.console.action;

import com.l7tech.console.panels.ClusterPropertyDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.objectmodel.EntityType;

/**
 * Action to manager cluster properties from the ssm.
 *
 * @author flascelles@layer7-tech.com
 */
public class ManageClusterPropertiesAction extends SecureAction {
    public ManageClusterPropertiesAction() {
        super(new AttemptedAnyOperation(EntityType.CLUSTER_PROPERTY));
    }

    public String getName() {
        return "Manage Cluster-Wide Properties";
    }

    public String getDescription() {
        return "View/Edit SecureSpan Gateways Cluster Properties.";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        ClusterPropertyDialog dlg = new ClusterPropertyDialog(TopComponents.getInstance().getTopParent());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg);
    }
}
