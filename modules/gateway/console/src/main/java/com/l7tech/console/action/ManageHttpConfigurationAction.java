package com.l7tech.console.action;

import com.l7tech.console.panels.HttpConfigurationManagerDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import java.awt.*;

/**
 * Action for editing HTTP configurations.
 */
public class ManageHttpConfigurationAction extends SecureAction {

    private final Component component;

    public ManageHttpConfigurationAction() {
        this( null );
    }

    public ManageHttpConfigurationAction( final Component component ) {
        super(new AttemptedAnyOperation(EntityType.HTTP_CONFIGURATION), "service:Admin");
        this.component = component;
    }

    @Override
    public String getName() {
        return "Manage HTTP Options";
    }

    @Override
    public String getDescription() {
        return "View and manage HTTP Options";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    @Override
    protected void performAction() {
        final Window parent = component==null ?
                TopComponents.getInstance().getTopParent() :
                SwingUtilities.getWindowAncestor( component );
        final HttpConfigurationManagerDialog httpConfigurationManagerDialog
                = new HttpConfigurationManagerDialog( parent );
        DialogDisplayer.display(httpConfigurationManagerDialog);
    }
}
