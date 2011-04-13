package com.l7tech.console.action;

import com.l7tech.console.panels.IdProviderPasswordPolicyDialog;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.IdentityProviderNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.IdentityProviderPasswordPolicy;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

/**
 * The <code>IdentityProviderManagePasswordPolicyAction</code> edits the
 * password policy.
 *
 * @author wlui
 */
public class IdentityProviderManagePasswordPolicyAction extends NodeAction {
    private AttemptedUpdate attemptedUpdatePasswordPolicy;

    public IdentityProviderManagePasswordPolicyAction(IdentityProviderNode nodeIdentity) {
        super(nodeIdentity, LIC_AUTH_ASSERTIONS, null);
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Manage Password Policy";
    }

    /**
     * @return the action description
     */
    @Override
    public String getDescription() {
        return "Manage the password policy associated with the internal identity provider";
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
        if (attemptedUpdatePasswordPolicy == null) {
            attemptedUpdatePasswordPolicy = new AttemptedUpdate(EntityType.PASSWORD_POLICY, new IdentityProviderPasswordPolicy(-2L));
        }
        return canAttemptOperation(attemptedUpdatePasswordPolicy);
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
        final IdentityProviderPasswordPolicy passwordPolicy;
        Frame f = TopComponents.getInstance().getTopParent();
        if (header.getOid() != -1L) {
            final long oid = header.getOid();
            try {
                passwordPolicy =
                  getIdentityAdmin().getPasswordPolicyForIdentityProvider(oid);
            } catch (FindException e) {
                logger.log(Level.WARNING, "Failed to find password policy: " + ExceptionUtils.getMessage(e), e);
                DialogDisplayer.showMessageDialog(f, "Failed to find password policy: " + ExceptionUtils.getMessage(e), "Find Failed", JOptionPane.ERROR_MESSAGE, null);
                return;
            }
            final IdentityAdmin.AccountMinimums accountMinimums = getIdentityAdmin().getAccountMinimums();
            final Map<String,IdentityProviderPasswordPolicy> policyMinimums = getIdentityAdmin().getPasswordPolicyMinimums();
            final IdProviderPasswordPolicyDialog dlg = new IdProviderPasswordPolicyDialog(
                    f,
                    passwordPolicy,
                    accountMinimums==null?null:accountMinimums.getName(),
                    policyMinimums==null?Collections.<String, IdentityProviderPasswordPolicy>emptyMap():policyMinimums,
                    false);
            dlg.pack();
            Utilities.centerOnScreen(dlg);
            DialogDisplayer.display(dlg, new Runnable() {
               @Override
               public void run() {
                   if (dlg.isConfirmed()) {
                       try {
                           getIdentityAdmin().updatePasswordPolicy(oid, passwordPolicy);
                       } catch (ObjectModelException e) {
                           logger.log(Level.WARNING, "Failed to save password policy: " + ExceptionUtils.getMessage(e), e);
                           DialogDisplayer.showMessageDialog(dlg, "Failed to save password policy: " + ExceptionUtils.getMessage(e), "Save Failed", JOptionPane.ERROR_MESSAGE, null);
                       }
                   }
               }
            });

        }
    }

    private IdentityAdmin getIdentityAdmin() {
        return Registry.getDefault().getIdentityAdmin();
    }
}
