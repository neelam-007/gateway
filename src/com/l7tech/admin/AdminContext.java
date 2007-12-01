/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.admin;

import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.log.LogSinkAdmin;
import com.l7tech.common.policy.PolicyAdmin;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.kerberos.KerberosAdmin;
import com.l7tech.common.security.rbac.RbacAdmin;
import com.l7tech.common.transport.TransportAdmin;
import com.l7tech.common.transport.ftp.FtpAdmin;
import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.xml.schema.SchemaAdmin;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.service.ServiceAdmin;

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
     */
    IdentityAdmin getIdentityAdmin() throws SecurityException;

    /**
     * @return the service managerr
     * @throws SecurityException on security error accessing the interface
     */
    ServiceAdmin getServiceAdmin() throws SecurityException;

    /**
     * @return the jms provider manager
     * @throws SecurityException on security error accessing the interface
     */
    JmsAdmin getJmsAdmin() throws SecurityException;

    /**
     * @return the FTP manager
     * @throws SecurityException on security error accessing the interface
     */
    FtpAdmin getFtpAdmin() throws SecurityException;

    /**
     * @return the trusted certificates manager
     * @throws SecurityException on security error accessing the interface
     */
    TrustedCertAdmin getTrustedCertAdmin() throws SecurityException;

    SchemaAdmin getSchemaAdmin() throws SecurityException;

    /**
     * @return the custome assertions registrar
     * @throws SecurityException on security error accessing the interface
     */
    CustomAssertionsRegistrar getCustomAssertionsRegistrar() throws SecurityException;

    /**
     * @return the audit admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     */
    AuditAdmin getAuditAdmin() throws SecurityException;

    /**
     * @return the cluster status admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     */
    ClusterStatusAdmin getClusterStatusAdmin() throws SecurityException;

    /**
     * @return the kerberos admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     */
    KerberosAdmin getKerberosAdmin() throws SecurityException;

    /**
     * @return the RBAC admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     */
    RbacAdmin getRbacAdmin() throws SecurityException;

    /**
     * @return the transport admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     */
    TransportAdmin getTransportAdmin() throws SecurityException;

    /**
     * @return the Policy admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     */
    PolicyAdmin getPolicyAdmin() throws SecurityException;

    /**
     * @return the Log sink admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     */
    LogSinkAdmin getLogSinkAdmin() throws SecurityException;
}
