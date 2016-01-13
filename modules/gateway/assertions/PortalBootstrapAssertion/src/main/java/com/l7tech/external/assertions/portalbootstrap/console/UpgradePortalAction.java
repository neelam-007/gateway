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
        super(new AttemptedUpdateAny(EntityType.USER), "Upgrade Portal Integration", "Upgrade Portal Integration on Gateway",
                "com/l7tech/console/resources/ManageUserAccounts16.png");
    }

    @Override
    protected void performAction() {
        PortalBootstrapExtensionInterface portalboot =
                Registry.getDefault().getExtensionInterface(PortalBootstrapExtensionInterface.class, null);

        if (portalboot.isGatewayEnrolled()) {
            DialogDisplayer.showConfirmDialog(TopComponents.getInstance().getTopParent(),
                    new JLabel("<html>Portal upgrade cannot be reversed. Please do not cancel or exit this process.<br/>Allow API Portal to upgrade portal-specific software on this Gateway?</html>"),
                    "Confirm Upgrade",
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
                                            AdminGuiUtils.doAsyncAdmin(portalboot, parent, "Upgrade SaaS Portal", "Upgrading...", upgradeJobId, false);

                                    if (result.isLeft()) {
                                        String msg = "Unable to upgrade: " + result.left();
                                        logger.log(Level.WARNING, msg);
                                        showError(msg);
                                    } else {
                                        DialogDisplayer.showMessageDialog(parent,
                                                "Gateway Upgraded Successfully",
                                                "Gateway Upgraded Successfully",
                                                JOptionPane.INFORMATION_MESSAGE,
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                    }
                                                });
                                        TopComponents.getInstance().refreshPoliciesFolderNode();
                                    }
                                } catch (Exception e) {
                                    String msg = "Unable to upgrade: " + ExceptionUtils.getMessage(e);
                                    logger.log(Level.WARNING, msg, e);
                                    showError(msg);
                                }
                            }
                        }
                    });
        } else {
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                    "Upgrade failed. Gateway must be enrolled to upgrade.",
                    "Error", JOptionPane.PLAIN_MESSAGE, null);
        }

    }

    private void showError(String s) {
        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), "Error", s, null);
    }
}


