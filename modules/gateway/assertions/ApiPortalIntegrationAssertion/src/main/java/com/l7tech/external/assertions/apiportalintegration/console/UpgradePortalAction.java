package com.l7tech.external.assertions.apiportalintegration.console;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.panels.CancelableOperationDialog;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.apiportalintegration.server.upgrade.*;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdateAny;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action which executes an API Portal upgrade.
 * <p/>
 * Requires RBAC permissions.
 */
public class UpgradePortalAction extends SecureAction {
    public UpgradePortalAction() {
        super(new AttemptedUpdateAny(EntityType.POLICY), "Upgrade Portal", "Upgrade Portal", ImageCache.getInstance().getIcon("com/l7tech/console/resources/Bean16.gif"));
        this.admin = Registry.getDefault().getExtensionInterface(UpgradePortalAdmin.class, null);
    }

    @Override
    protected void performAction() {
        DialogDisplayer.showConfirmDialog(TopComponents.getInstance().getTopParent(), "Upgrade Portal?", "Upgrade Portal",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, new DialogDisplayer.OptionListener() {
            @Override
            public void reportResult(final int option) {
                if (JOptionPane.YES_OPTION == option) {
                    try {
                        final Pair<List<UpgradedEntity>, String> result = CancelableOperationDialog.doWithDelayedCancelDialog(new Callable<Pair<List<UpgradedEntity>, String>>() {
                            @Override
                            public Pair<List<UpgradedEntity>, String> call() throws Exception {
                                return doUpgrade();
                            }
                        }, TopComponents.getInstance().getTopParent(), "Upgrading Portal", "Upgrading Portal ...", 500);
                        DialogDisplayer.display(new UpgradePortalResultDialog(TopComponents.getInstance().getTopParent(), result.getKey(), result.getValue()), null);
                    } catch (final InterruptedException e) {
                        LOGGER.log(Level.WARNING, "Upgrade interrupted: " + e.getMessage(), ExceptionUtils.getDebugException(e));
                    } catch (final InvocationTargetException e) {
                        LOGGER.log(Level.WARNING, "Unexpected error occurred during upgrade: " + e.getMessage(), ExceptionUtils.getDebugException(e));
                        DialogDisplayer.display(new UpgradePortalResultDialog(TopComponents.getInstance().getTopParent(), Collections.<UpgradedEntity>emptyList(), "An unexpected error occurred"), null);
                    }

                }
            }
        });
    }

    Pair<List<UpgradedEntity>, String> doUpgrade() {
        return doUpgrade(admin);
    }

    /**
     * Protected static method for unit tests which has no Dialog behaviour (our build environment doesn't support Dialog tests).
     */
    static final Pair<List<UpgradedEntity>, String> doUpgrade(final UpgradePortalAdmin admin) {
        final List<UpgradedEntity> upgradedEntities = new ArrayList<UpgradedEntity>();
        String error = null;
        try {
            upgradedEntities.addAll(admin.upgradeServicesTo2_1());
            if (!upgradedEntities.isEmpty()) {
                sleep();
            }
            upgradedEntities.addAll(admin.upgradeKeysTo2_1());
            admin.deleteUnusedClusterProperties();
        } catch (final UpgradeServiceException e) {
            error = API_ERROR;
            LOGGER.log(Level.WARNING, error + ": " + e.getMessage(), ExceptionUtils.getDebugException(e));
        } catch (final UpgradeKeyException e) {
            error = KEY_ERROR;
            LOGGER.log(Level.WARNING, error + ": " + e.getMessage(), ExceptionUtils.getDebugException(e));
        } catch (final UpgradeClusterPropertyException e) {
            // don't pass this error to the dialog as it doesn't really affect anything
            LOGGER.log(Level.WARNING, error + ": " + e.getMessage(), ExceptionUtils.getDebugException(e));
        }
        return new Pair(upgradedEntities, error);
    }

    static final void sleep() {
        try {
            final Integer wait = ConfigFactory.getIntProperty("com.l7tech.apiportal.UpgradePortalAction", DEFAULT_WAIT);
            Thread.sleep(wait);
        } catch (final InterruptedException e) {
            LOGGER.log(Level.WARNING, "Sleep interrupted: " + e.getMessage(), ExceptionUtils.getDebugException(e));
        }
    }

    static int DEFAULT_WAIT = 3000;
    static final String API_ERROR = "Error upgrading APIs";
    static final String KEY_ERROR = "Error upgrading API keys";
    private static final Logger LOGGER = Logger.getLogger(UpgradePortalAction.class.getName());
    private final UpgradePortalAdmin admin;
}
