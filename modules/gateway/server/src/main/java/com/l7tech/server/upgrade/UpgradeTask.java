/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.upgrade;

import org.springframework.context.ApplicationContext;

/**
 * Interface implemented by tasks to run when a cluster property has flagged additional database upgrade
 * work to be performed by the first Gateway cluster node that notices it.
 */
public interface UpgradeTask {
    /**
     * Perform any configuration upgrade tasks required on this Gateway.  This will always be called after the
     * application context is created, but before any servlet or jms input is possible (including both message traffic
     * and administrative calls), when the "Starting" event has been fired.
     * <p/>
     * Each UpgradeTask will be invoked in its own transaction.
     * <p/>
     * To signal a fatal error that should prevent the Gateway from trying to continue starting up, implementors
     * of this method should throw FatalUpgradeException.  The Gateway will roll back the upgrade transaction
     * and halt.
     * <p/>
     * To signal a warning, but allow the Gateway to continue startup anyway, implementors of this method should
     * throw NonfatalUpgradeException.  The Gateway will roll back the upgrade transaction and continue.
     * <p/>
     * If this method returns, the Gateway will commit the transaction.
     * <p/>
     * <b>Auditing note:</b> if this method throws, the result rollback will include audit messages as well
     * (except for any logging component, of course).  If you want any information to make it into the audit log
     * and stay there you'll need to wrap it up in whichever UpgradeException you throw to signal failure.
     *
     * @param applicationContext Spring application context from which to get whatever admin beans are needed to
     *                           perform the configuration upgrade.  Never null.
     * @throws NonfatalUpgradeException if this upgrade task failed and should be retried on next bootup,
     *                                  other upgrade tasks should still be attempted, and SSG startup should
     *                                  still attempt to continue.
     * @throws FatalUpgradeException if this upgrade task failed and should be retried on next bootup,
     *                               and the SSG should immediately halt without attempting any further
     *                               upgrade tasks or continuing to start up.
     */
    void upgrade(ApplicationContext applicationContext) throws NonfatalUpgradeException, FatalUpgradeException;
}
