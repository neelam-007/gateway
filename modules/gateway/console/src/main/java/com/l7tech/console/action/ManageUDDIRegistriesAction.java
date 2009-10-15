/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 13, 2009
 * Time: 5:02:03 PM
 */
package com.l7tech.console.action;

import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.console.panels.UddiRegistryManagerWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;

import java.util.logging.Logger;

public class ManageUDDIRegistriesAction extends SecureAction{

    static final Logger log = Logger.getLogger(ManageUDDIRegistriesAction.class.getName());

    public ManageUDDIRegistriesAction() {
        super(new AttemptedAnyOperation(EntityType.UDDI_REGISTRY), "service:Admin");
    }

    @Override
    public String getName() {
        return "Manage UDDI Registries";
    }

    @Override
    public String getDescription() {
        return "View and manage UDDI Registries";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/interface.gif";
    }

    @Override
    protected void performAction() {
        UddiRegistryManagerWindow pkmw = new UddiRegistryManagerWindow(TopComponents.getInstance().getTopParent());
        pkmw.pack();
        Utilities.centerOnScreen(pkmw);
        DialogDisplayer.display(pkmw);
    }
}

