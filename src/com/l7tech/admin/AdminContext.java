/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.admin;

import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.kerberos.KerberosAdmin;
import com.l7tech.common.security.rbac.RbacAdmin;
import com.l7tech.common.transport.ftp.FtpAdmin;
import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.transport.TransportAdmin;
import com.l7tech.common.xml.schema.SchemaAdmin;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.service.ServiceAdmin;

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
     * Retrieve the server software product version string.
     *
     * @return the server admin protocol version string, ie "4.0".  Never null
     */
    String getSoftwareVersion();

    /**
     * @return the {@link com.l7tech.identity.IdentityAdmin} implementation
     * @throws SecurityException on security error accessing the interface
     * @throws RemoteException   on remote communication error
     */
    IdentityAdmin getIdentityAdmin() throws RemoteException, SecurityException;

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
     * @return the FTP manager
     * @throws SecurityException on security error accessing the interface
     * @throws RemoteException   on remote communication error
     */
    FtpAdmin getFtpAdmin() throws RemoteException, SecurityException;;

    /**
     * @return the trusted certificates manager
     * @throws SecurityException on security error accessing the interface
     * @throws RemoteException   on remote communication error
     */
    TrustedCertAdmin getTrustedCertAdmin() throws RemoteException, SecurityException;;

    SchemaAdmin getSchemaAdmin() throws RemoteException, SecurityException;

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
     * @return the cluster status admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     * @throws RemoteException   on remote communication error
     */
    ClusterStatusAdmin getClusterStatusAdmin() throws RemoteException, SecurityException;

    /**
     * @return the kerberos admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     * @throws RemoteException   on remote communication error
     */
    KerberosAdmin getKerberosAdmin() throws RemoteException, SecurityException;

    /**
     * @return the RBAC admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     * @throws RemoteException   on remote communication error
     */
    RbacAdmin getRbacAdmin() throws RemoteException, SecurityException;

    /**
     * @return the transport admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     * @throws RemoteException   on remote communication error
     */
    TransportAdmin getTransportAdmin() throws RemoteException, SecurityException;
}