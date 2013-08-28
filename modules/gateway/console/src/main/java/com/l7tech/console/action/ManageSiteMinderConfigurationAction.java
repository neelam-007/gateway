package com.l7tech.console.action;

import com.l7tech.console.panels.SiteMinderConfigurationWindow;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

import javax.swing.JOptionPane;

/**
 * @author nilic
 * Date: 7/22/13
 * Time: 3:37 PM
 */
public class ManageSiteMinderConfigurationAction extends SecureAction {
    public ManageSiteMinderConfigurationAction(){
        super(new AttemptedAnyOperation(EntityType.SITEMINDER_CONFIGURATION), "service:Admin");
    }

    @Override
    public String getName() {
        return "Manage SiteMinder Configurations";
    }

    @Override
    public String getDescription() {
        return "Create, edit, and remove SiteMinder Configurations";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    @Override
    protected void performAction() {
        if(checkSiteMinderEnabled()){
            SiteMinderConfigurationWindow configurationWindow = new SiteMinderConfigurationWindow(TopComponents.getInstance().getTopParent());
            configurationWindow.pack();
            Utilities.centerOnScreen(configurationWindow);
            DialogDisplayer.display(configurationWindow);
        }
        else {
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), "Cannot find SiteMinder SDK!", "SiteMinder Error", JOptionPane.ERROR_MESSAGE, null);
        }
    }

    private boolean checkSiteMinderEnabled() {
        boolean enabled = false;
        ClusterStatusAdmin clusterStatusAdmin = Registry.getDefault().getClusterStatusAdmin();

        if(clusterStatusAdmin != null) {
            enabled =  Boolean.parseBoolean(clusterStatusAdmin.getHardwareCapability(ClusterStatusAdmin.CAPABILITY_SITEMINDER));
        }

        return enabled;
    }
}
