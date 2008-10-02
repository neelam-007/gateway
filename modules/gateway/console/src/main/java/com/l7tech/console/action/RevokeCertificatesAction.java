package com.l7tech.console.action;

import javax.swing.*;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteAll;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.objectmodel.UpdateException;

/**
 * Console action for revocation of user certificate issued by the internal CA.
 *
 * @author Steve Jones
 */
public class RevokeCertificatesAction extends SecureAction {

    //- PUBLIC

    /**
     * create the aciton that disables the service
     */
    public RevokeCertificatesAction() {
        super(new AttemptedDeleteAll(EntityType.USER), CA_FEATURESET_NAME);
    }

    /**
     * Get the name for the action
     *
     * @return the action name
     */
    public String getName() {
        return "Revoke User Certificates";
    }

    /**
     * Get the description for the action
     *
     * @return the aciton description
     */
    public String getDescription() {
        return "Revoke all user certificates";
    }

    //- PROTECTED

    /**
     * Get the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/cert16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        String confirmationDialogTitle = "Confirm Certificate Revocation";
        String confirmationDialogMessage =
            "<html><center>This will irrevocably destroy all user certificates and cannot be undone.</center>" +
                "<center>Really revoke all certificates issued by the SecureSpan Gateway certificate authority?</center></html>";

        DialogDisplayer.showSafeConfirmDialog(
            TopComponents.getInstance().getTopParent(),
            confirmationDialogMessage,
            confirmationDialogTitle,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE,
            new DialogDisplayer.OptionListener() {
                public void reportResult(int option) {
                    if (option == JOptionPane.CANCEL_OPTION) {
                        return;
                    }

                    try {
                        IdentityAdmin identityAdmin = getIdentityAdmin();
                        int revoked = identityAdmin.revokeCertificates();

                        String message;
                        if (revoked == 0) {
                            message = "No client certificates were revoked.";
                        } else {
                            message = "Revoked " + revoked + " client certificate(s).";
                        }

                        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                            message,
                            "Client Certificates Revoked",
                            JOptionPane.INFORMATION_MESSAGE, null);
                    } catch (UpdateException e) {
                        String msg = "Error revoking certificates.\n" + ExceptionUtils.getMessage(e);
                        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), msg,
                            "Cannot Revoke Certificates",
                            JOptionPane.ERROR_MESSAGE, null);
                    }
                }
            }
        );
    }

    //- PRIVATE

    /**
     *
     */
    private IdentityAdmin getIdentityAdmin() {
        IdentityAdmin admin = Registry.getDefault().getIdentityAdmin();

        if (admin == null) {
            throw new RuntimeException("Could not find registered " + IdentityAdmin.class);
        }

        return admin;
    }

}
