/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.identity.cert;

import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;

import java.security.cert.Certificate;

/**
 * @author emil
 * @version Feb 14, 2005
 */
public class ClientCertManagerStub implements ClientCertManager {
    /**
     * record the fact that the a user cert was used successfully in an authentication operation
     * this will prevent the user to regen his cert until the administrator revokes the cert
     *
     * @param user owner of the cert
     * @throws com.l7tech.objectmodel.UpdateException
     *
     */
    public void forbidCertReset(User user) throws UpdateException {
        throw new RuntimeException("Not implemented");
    }

    /**
     * retrieves existing cert for this user
     */
    public Certificate getUserCert(User user) throws FindException {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Records new cert for the user (if user is allowed)
     *
     * @param cert the cert to record
     * @throws com.l7tech.objectmodel.UpdateException
     *          if user was not in a state that allowes the creation
     *          of a cert or if an internal error occurs
     */
    public void recordNewUserCert(User user, Certificate cert) throws UpdateException {
        throw new RuntimeException("Not implemented");
    }

    /**
     * revokes the cert (if applicable) for this user
     */
    public void revokeUserCert(User user) throws UpdateException, ObjectNotFoundException {
        throw new RuntimeException("Not implemented");
    }

    /**
     * checks whether the user passwd is authorized to generate a new cert.
     * if the user already has a cert and it has been consumed, this will return false;
     * if the user has a non-consumed cert and that cert has been regenerated 10 times,
     * it will also return false.
     * <p/>
     * if the user has no current cert or a cert that has not been used, this will
     * return true
     */
    public boolean userCanGenCert(User user) {
        throw new RuntimeException("Not implemented");
    }
}