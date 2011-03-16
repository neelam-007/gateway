package com.l7tech.console.action;

import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.panels.IdProviderPasswordPolicyDialog;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.IdentityProviderNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderPasswordPolicy;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TextUtils;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.EventListener;
import java.util.logging.Level;

/**
 * The <code>ForceAdminPasswordResetAction</code> forces all admin users in the
 * identity provider to reset password
 *
 * @author wlui
 */
public class ForceAdminPasswordResetAction extends NodeAction {
    private EventListenerList listenerList = new EventListenerList();
    private AttemptedUpdate attemptedForcePasswordChange;

    public ForceAdminPasswordResetAction(IdentityProviderNode nodeIdentity) {
        super(nodeIdentity, LIC_AUTH_ASSERTIONS, null);
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Force Administrator Password Reset";
    }

    /**
     * @return the action description
     */
    @Override
    public String getDescription() {
        return "Force all administrative users in the identity provider to reset their passwords";
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif"; 
    }

    @Override
    public boolean isAuthorized() {
        if (attemptedForcePasswordChange == null) {
            attemptedForcePasswordChange = new AttemptedUpdate(EntityType.ID_PROVIDER_CONFIG, new IdentityProviderConfig());
        }
        return canAttemptOperation(attemptedForcePasswordChange);
    }
    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    @Override
    protected void performAction() {

        EntityHeader header = ((EntityHeaderNode)node).getEntityHeader();
        Frame f = TopComponents.getInstance().getTopParent();
        int result =JOptionPane.showConfirmDialog(
            f, "Are you sure you want to force all administrative users in the identity provider to reset their passwords ", getName(), JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION){
            if (header.getOid() != -1) {
                final long oid = header.getOid();
                try {
                    getIdentityAdmin().forceAdminUsersResetPassword(oid);
                } catch (FindException e) {
                    logger.log(Level.WARNING, "Failed to force password change: " + ExceptionUtils.getMessage(e), e);
                    DialogDisplayer.showMessageDialog(f, "Failed to force password change: " + ExceptionUtils.getMessage(e), "Edit Failed", JOptionPane.ERROR_MESSAGE, null);
                } catch (SaveException e) {
                    logger.log(Level.WARNING, "Failed to force password change: " + ExceptionUtils.getMessage(e), e);
                    DialogDisplayer.showMessageDialog(f, "Failed to force password change: " + ExceptionUtils.getMessage(e), "Edit Failed", JOptionPane.ERROR_MESSAGE, null);
                } catch (UpdateException e) {
                    logger.log(Level.WARNING, "Failed to force password change: " + ExceptionUtils.getMessage(e), e);
                    DialogDisplayer.showMessageDialog(f, "Failed to force password change: " + ExceptionUtils.getMessage(e), "Edit Failed", JOptionPane.ERROR_MESSAGE, null);
                }  catch (InvalidPasswordException e) {
                    logger.log(Level.WARNING, "Failed to force password change: " + ExceptionUtils.getMessage(e), e);  // should not happen
                    DialogDisplayer.showMessageDialog(f, "Failed to force password change: " + ExceptionUtils.getMessage(e), "Edit Failed", JOptionPane.ERROR_MESSAGE, null);
                }
            }
        }
    }

    /**
     * notify the listeners that the entity has been removed
     */
    private void fireEventProviderRemoved(EntityHeader header) {
        final EntityEvent event = new EntityEvent(this, header);
        EventListener[] listeners = listenerList.getListeners(EntityListener.class);
        for (EventListener listener : listeners) {
            ((EntityListener) listener).entityRemoved(event);
        }
    }
    /**
     * add the EntityListener
     *
     * @param listener the EntityListener
     */
    public void addEntityListener(EntityListener listener) {
        listenerList.add(EntityListener.class, listener);
    }

    /**
     * remove the the EntityListener
     *
     * @param listener the EntityListener
     */
    public void removeEntityListener(EntityListener listener) {
        listenerList.remove(EntityListener.class, listener);
    }

    private IdentityAdmin getIdentityAdmin() {
        return Registry.getDefault().getIdentityAdmin();
    }
}
