package com.l7tech.external.assertions.portalbootstrap.console;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.portalbootstrap.PortalBootstrapExtensionInterface;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdateAny;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chean22, 12/23/2015
 */
public class UpgradePortalAction extends SecureAction {
    private static final Logger logger = Logger.getLogger(UpgradePortalAction.class.getName());

    public UpgradePortalAction() {
        super(new AttemptedUpdateAny(EntityType.USER), "Update Portal Integration", "Update Portal Integration on Gateway",
                "com/l7tech/console/resources/ManageUserAccounts16.png");
    }

    @Override
    protected void performAction() {
        PortalBootstrapExtensionInterface portalboot =
                Registry.getDefault().getExtensionInterface(PortalBootstrapExtensionInterface.class, null);

        if (portalboot.isGatewayEnrolled()) {
            DialogDisplayer.showConfirmDialog(TopComponents.getInstance().getTopParent(),
                    new JLabel("<html>Update the Portal integration on this Gateway? This cannot be reversed.</html>"),
                    "Confirm Update",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    new DialogDisplayer.OptionListener() {
                        PortalBootstrapExtensionInterface portalboot =
                                Registry.getDefault().getExtensionInterface(PortalBootstrapExtensionInterface.class, null);
                        final Frame parent = TopComponents.getInstance().getTopParent();

                        @Override
                        public void reportResult(int option) {
                            if (JOptionPane.OK_OPTION == option) {
                                try {
                                    AsyncAdminMethods.JobId<Boolean> upgradeJobId = portalboot.upgradePortal();
                                    Either<String, Boolean> result =
                                            AdminGuiUtils.doAsyncAdmin(portalboot, parent, "Update Portal integration", "Updating... (do not interrupt)", upgradeJobId, false);

                                    if (result.isLeft()) {
                                        if(result.left().contains("latest Portal"))
                                        {
                                            String msg = "Update not necessary. " + result.left();
                                            logger.log(Level.INFO, msg);
                                            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), msg, "Information", JOptionPane.INFORMATION_MESSAGE, null);
                                        } else {
                                            String msg = "Unable to update: " + result.left();
                                            logger.log(Level.WARNING, msg);
                                            showError(msg);
                                        }
                                    } else {
                                        DialogDisplayer.showMessageDialog(parent,
                                                "Gateway Updated Successfully",
                                                "Gateway Updated Successfully",
                                                JOptionPane.INFORMATION_MESSAGE,
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                    }
                                                });
                                        TopComponents.getInstance().refreshPoliciesFolderNode();
                                    }
                                } catch (Exception e) {
                                    String msg = "Unable to update: " + ExceptionUtils.getMessage(e);
                                    logger.log(Level.WARNING, msg, e);
                                    showError(msg);
                                }
                            }
                        }
                    });
        } else {
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), "Cannot update a Gateway that is not enrolled for use with a Portal.", "Error", JOptionPane.PLAIN_MESSAGE, null);
        }

    }

    private void showError(String s) {
        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), "Error", s, null);
    }
}


