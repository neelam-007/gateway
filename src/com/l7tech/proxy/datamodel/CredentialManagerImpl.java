/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import org.apache.log4j.Category;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;

/**
 * Default CredentialManager implementation.  This version requires that the credentials already be
 * configured in the Ssg object; it takes no action.
 *
 * User: mike
 * Date: Jun 27, 2003
 * Time: 10:39:29 AM
 */
public class CredentialManagerImpl implements CredentialManager {
    private static final Category log = Category.getInstance(CredentialManagerImpl.class);
    private static CredentialManagerImpl INSTANCE = new CredentialManagerImpl();

    private CredentialManagerImpl() {}

    public static CredentialManagerImpl getInstance() {
        return INSTANCE;
    }

    /**
     * Load credentials for this SSG.  If the SSG already contains credentials they will be
     * overwritten with new ones.  For the headless environment, we take no action here; if the
     * Ssg is not already configured with valid credentials there's nothing we can do about it.
     * @param ssg
     */
    public void getCredentials(Ssg ssg) throws OperationCanceledException {
        log.error("Headless CredentialManager: unable to obtain new credentials");
        throw new OperationCanceledException("Unable to obtain new credentials");
    }

    /**
     * Notify that the credentials for this SSG have been tried and found to be no good.
     * Not much we can do about this in a headless environment except log it.
     */
    public void notifyInvalidCredentials(Ssg ssg) {
        log.error("Credentials are invalid for SSG " + ssg);
    }

    public void notifyLengthyOperationStarting(Ssg ssg, String message) {
        log.info("Starting lengthy operation for Ssg " + ssg + ": " + message);
    }

    public void notifyLengthyOperationFinished(Ssg ssg) {
        log.info("Lengthy operation for Ssg " + ssg + " has completed");
    }
}
