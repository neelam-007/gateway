package com.l7tech.console.action;

import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.console.GatewayAuditWindow;
import com.l7tech.console.util.Registry;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EnumSet;
import java.util.Date;


/**
 * The <code>ViewGatewayAuditsAction</code> invokes the audit viewer.
 *
 * @author mike
 */
public class ViewGatewayAuditsAction extends SecureAction {
    private static GatewayAuditWindow gatewayAuditWindow;
    private final long auditTime;

    /**
     * @see #isAuthorized()
     */
    public ViewGatewayAuditsAction() {
        super(null, UI_AUDIT_WINDOW);
        this.auditTime = 0;
    }

    /**
     * @see #isAuthorized()
     */
    public ViewGatewayAuditsAction(final long auditTime) {
        super(null, UI_AUDIT_WINDOW);
        this.auditTime = auditTime;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Gateway Audit Events";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View Gateway audit events";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/AnalyzeGatewayLog16x16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        GatewayAuditWindow gaw = getGatewayAuditWindow();
        if (!gaw.isVisible() && auditTime!=0) {
            gaw.displayAudits(new Date(auditTime));
        }
        gaw.setVisible(true);
        gaw.setState(Frame.NORMAL);
        gaw.toFront();
    }

    private static final EnumSet<EntityType> ANY_AUDIT_RECORD = EnumSet.of(EntityType.ANY, EntityType.AUDIT_RECORD, EntityType.AUDIT_MESSAGE, EntityType.AUDIT_ADMIN, EntityType.AUDIT_SYSTEM);

    @Override
    public synchronized boolean isAuthorized() {
        if (!Registry.getDefault().isAdminContextPresent()) return false;
        
        for (Permission perm : getSecurityProvider().getUserPermissions()) {
            EntityType etype = perm.getEntityType();
            OperationType op = perm.getOperation();
            if (op == OperationType.READ && ANY_AUDIT_RECORD.contains(etype)) return true;
        }
        return false;
    }

    private GatewayAuditWindow getGatewayAuditWindow() {
        if (gatewayAuditWindow != null) return gatewayAuditWindow;


        gatewayAuditWindow = new GatewayAuditWindow();


        gatewayAuditWindow.addWindowListener(new WindowAdapter() {
            public void windowClosing(final WindowEvent e) {
                destroyGatewayLogWindow();
            }

            public void windowClosed(final WindowEvent e) {
                destroyGatewayLogWindow();
            }
        });

        gatewayAuditWindow.pack();
        return gatewayAuditWindow;
    }

    private void destroyGatewayLogWindow() {
        if (gatewayAuditWindow == null)
            return;
        gatewayAuditWindow.dispose();
        gatewayAuditWindow = null;
    }

    public void onLogon(LogonEvent e) {
        super.onLogon(e);
        if (gatewayAuditWindow == null)
              return;
          gatewayAuditWindow.onLogon(e);
    }

    public void onLogoff(LogonEvent e) {
        super.onLogoff(e);
        if (gatewayAuditWindow == null)
              return;
          gatewayAuditWindow.onLogoff(e);
    }
}
