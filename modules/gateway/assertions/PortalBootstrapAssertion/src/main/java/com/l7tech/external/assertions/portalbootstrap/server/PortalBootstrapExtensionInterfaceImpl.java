package com.l7tech.external.assertions.portalbootstrap.server;

import com.l7tech.external.assertions.portalbootstrap.PortalBootstrapExtensionInterface;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.util.Background;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Implements the Portal Bootstrap Interface.
 */
public class PortalBootstrapExtensionInterfaceImpl extends AsyncAdminMethodsImpl implements PortalBootstrapExtensionInterface {

    @Override
    public JobId<Boolean> enrollWithPortal(final String enrollmentUrl, @NotNull final JdbcConnection otkConnection) throws IOException {
        if (StringUtils.isBlank(otkConnection.getName())) {
            throw new IllegalArgumentException("OTK connection name must not be null or empty");
        }
        final FutureTask<Boolean> enrollTask = new FutureTask<Boolean>(AdminInfo.find(false).wrapCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                PortalBootstrapManager.getInstance().enrollWithPortal(enrollmentUrl, otkConnection.getGoid().toString(), otkConnection.getName());
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
