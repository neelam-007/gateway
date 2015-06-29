package com.l7tech.external.assertions.portalbootstrap.server;

import com.l7tech.external.assertions.portalbootstrap.PortalBootstrapExtensionInterface;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.util.Background;

import java.io.IOException;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Implements the Portal Bootstrap Interface.
 */
public class PortalBootstrapExtensionInterfaceImpl extends AsyncAdminMethodsImpl implements PortalBootstrapExtensionInterface {

    @Override
    public JobId<Boolean> enrollWithPortal(final String enrollmentUrl) throws IOException {

        final FutureTask<Boolean> enrollTask = new FutureTask<Boolean>(AdminInfo.find(false).wrapCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                PortalBootstrapManager.getInstance().enrollWithPortal(enrollmentUrl);
                return true;
            }

        }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                enrollTask.run();
            }
        }, 0L);

        return registerJob(enrollTask, Boolean.class);
    }
}
