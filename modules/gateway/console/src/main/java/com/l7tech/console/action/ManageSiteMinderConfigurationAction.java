package com.l7tech.console.action;

import com.l7tech.console.panels.SiteMinderConfigurationWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

/**
 * Created with IntelliJ IDEA.
 * User: nilic
 * Date: 7/22/13
 * Time: 3:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class ManageSiteMinderConfigurationAction extends SecureAction{
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
        SiteMinderConfigurationWindow configurationWindow = new SiteMinderConfigurationWindow(TopComponents.getInstance().getTopParent());
        configurationWindow.pack();
        Utilities.centerOnScreen(configurationWindow);
        DialogDisplayer.display(configurationWindow);
    }
}
