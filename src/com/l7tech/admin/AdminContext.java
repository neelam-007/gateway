/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.admin;

import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.logging.LogAdmin;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.cluster.ClusterStatusAdmin;

import java.rmi.RemoteException;

/**
 * @author emil
 * @version Dec 2, 2004
 */
public interface AdminContext {
    /**
     * Retrieve the server admin protocol version string.
     *
     * @return the server admin protocol version string, ie "20040603".  Never null
     */
    String getVersion();

    /**
     * @return the {@link com.l7tech.identity.IdentityAdmin} implementation
     * @throws SecurityException on security error accessing the interface
     * @throws RemoteException   on remote communication error
     */
    IdentityAdmin getIdentityAdmin() throws RemoteException, SecurityException;

    /**
     * @return the {@link com.l7tech.identity.IdentityProviderConfig} object for the internal identity provider
     * @throws SecurityException on security error accessing the interface
     * @throws RemoteException   on remote communication error
     */
    IdentityProviderConfig getInternalProviderConfig() throws RemoteException, SecurityException, FindException;

    /**
     * @return the service managerr
     * @throws SecurityException on security error accessing the interface
     * @throws RemoteException   on remote communication error
     */
    ServiceAdmin getServiceAdmin() throws RemoteException, SecurityException;

    /**
     * @return the jms provider manager
     * @throws SecurityException on security error accessing the interface
     * @throws RemoteException   on remote communication error
     */
    JmsAdmin getJmsAdmin() throws RemoteException, SecurityException;;

    /**
     * @return the trusted certificates manager
     * @throws SecurityException on security error accessing the interface
     * @throws RemoteException   on remote communication error
     */
    TrustedCertAdmin getTrustedCertAdmin() throws RemoteException, SecurityException;;
    /**
     * @return the custome assertions registrar
     * @throws SecurityException on security error accessing the interface
     * @throws RemoteException   on remote communication error
     */
    CustomAssertionsRegistrar getCustomAssertionsRegistrar() throws RemoteException, SecurityException;;

    /**
     * @return the audit admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     * @throws RemoteException   on remote communication error
     */
    AuditAdmin getAuditAdmin() throws RemoteException, SecurityException;;

    /**
     * @return the log admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     * @throws RemoteException   on remote communication error
     */
    LogAdmin getLogAdmin() throws RemoteException, SecurityException;

    /**
     * @return the cluster status admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     * @throws RemoteException   on remote communication error
     */
    ClusterStatusAdmin getClusterStatusAdmin() throws RemoteException, SecurityException;

    /**
     * Invoke admin actions with admin privileges associated with this <code>AdminCntext</code>
     * reference.
     *
     * @param actions the set of actions to execute
     * @return the action results corresponding to the actions. <b>null</b> is returned
     */
    Object[] invoke(AdminAction[] actions) throws RemoteException, SecurityException;
}