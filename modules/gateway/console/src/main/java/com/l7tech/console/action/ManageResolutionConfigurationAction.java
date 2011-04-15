package com.l7tech.console.action;

import com.l7tech.console.panels.ServiceResolutionDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Secure action for editing Service Resolution settings.
 */
public class ManageResolutionConfigurationAction extends SecureAction {

    private final Window parent;

    public ManageResolutionConfigurationAction() {
        this( null );
    }

    public ManageResolutionConfigurationAction( final Window parent ) {
        super(new AttemptedAnyOperation( EntityType.RESOLUTION_CONFIGURATION), "service:Admin");
        this.parent = parent==null ? TopComponents.getInstance().getTopParent() : parent;
        putValue( Action.MNEMONIC_KEY, KeyEvent.VK_S );
    }

    @Override
    public String getName() {
        return "Service Resolution";
    }

    @Override
    protected void performAction() {
        DialogDisplayer.display( new ServiceResolutionDialog( parent ) );
    }

}
