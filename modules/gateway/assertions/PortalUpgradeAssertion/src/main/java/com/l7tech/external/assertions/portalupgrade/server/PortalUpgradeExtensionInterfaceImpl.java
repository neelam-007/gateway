package com.l7tech.external.assertions.portalupgrade.server;

import com.l7tech.external.assertions.portalupgrade.PortalUpgradeExtensionInterface;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;

/**
 * Implements the Portal Bootstrap Interface.
 */
public class PortalUpgradeExtensionInterfaceImpl extends AsyncAdminMethodsImpl implements PortalUpgradeExtensionInterface {

    @Override
    public JobId<Boolean> upgradePortal() throws IOException {
        final FutureTask<Boolean> upgradeTask = new FutureTask<>(AdminInfo.find(false).wrapCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                PortalUpgradeManager.getInstance().upgradePortal();
                return true;
            }
        }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                upgradeTask.run();
            }
        }, 0L);

        return registerJob(upgradeTask, Boolean.class);
    }

    @Override
    public boolean isGatewayEnrolled() {
        return PortalUpgradeManager.getInstance().isGatewayEnrolled();
    }
}
