package com.l7tech.console.util;

import com.l7tech.gateway.common.admin.AdminContext;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.admin.KerberosAdmin;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.admin.AdminLogin;
import com.l7tech.gateway.common.spring.remoting.http.RemotingContext;
import com.l7tech.gateway.common.spring.remoting.http.ConfigurableHttpInvokerRequestExecutor;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.gateway.common.transport.ftp.FtpAdmin;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gateway.common.transport.email.EmailListenerAdmin;
import com.l7tech.gateway.common.transport.email.EmailAdmin;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.schema.SchemaAdmin;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.CancelableOperationDialog;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import javax.swing.*;

/**
 * Admin context implementation
 */
public class AdminContextImpl extends RemotingContext implements AdminContext {

    //- PUBLIC

    public AdminLogin getAdminLogin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(AdminLogin.class);
    }

    public IdentityAdmin getIdentityAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(IdentityAdmin.class);
    }

    public ServiceAdmin getServiceAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(ServiceAdmin.class);
    }

    public FolderAdmin getFolderAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(FolderAdmin.class);
    }

    public JmsAdmin getJmsAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(JmsAdmin.class);
    }

    public FtpAdmin getFtpAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(FtpAdmin.class);
    }

    public TrustedCertAdmin getTrustedCertAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(TrustedCertAdmin.class);
    }

    public SchemaAdmin getSchemaAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(SchemaAdmin.class);
    }

    public CustomAssertionsRegistrar getCustomAssertionsRegistrar() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(CustomAssertionsRegistrar.class);
    }

    public AuditAdmin getAuditAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(AuditAdmin.class);
    }

    public ClusterStatusAdmin getClusterStatusAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(ClusterStatusAdmin.class);
    }

    public KerberosAdmin getKerberosAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(KerberosAdmin.class);
    }

    public RbacAdmin getRbacAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(RbacAdmin.class);
    }

    public TransportAdmin getTransportAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(TransportAdmin.class);
    }

    public PolicyAdmin getPolicyAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(PolicyAdmin.class);
    }

    public LogSinkAdmin getLogSinkAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(LogSinkAdmin.class);
    }

    public EmailListenerAdmin getEmailListenerAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(EmailListenerAdmin.class);
    }

    public EmailAdmin getEmailAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(EmailAdmin.class);
    }

    //- PACKAGE

    /**
     *
     */
    AdminContextImpl( final String host,
                      final int port,
                      final String sessionId,
                      final Collection<Object> remoteObjects,
                      final ConfigurableHttpInvokerRequestExecutor configurableInvoker ) {
        super( host, port, sessionId, remoteObjects, configurableInvoker );
    }

    //- PROTECTED

    @Override
    protected Object doRemoteInvocation( final Object targetObject,
                                         final Method method,
                                         final Object[] args) throws Throwable {
        if (NO_CANCEL_DIALOGS || !SwingUtilities.isEventDispatchThread() || Utilities.isCurrentThreadDoingWithDelayedCancelDialog())
            return super.doRemoteInvocation(targetObject, method, args);    //To change body of overridden methods use File | Settings | File Templates.

        // Running on Swing event thread, and no delayed cancel dialog already pending
        try {
            return CancelableOperationDialog.doWithDelayedCancelDialog(new Callable<Object>() {
                public Object call() throws Exception {
                    try {
                        return AdminContextImpl.super.doRemoteInvocation(targetObject, method, args);
                    } catch (InterruptedException ie) {
                        throw ie;
                    } catch (Throwable throwable) {
                        throw new ThrowableWrapper(throwable);
                    }
                }
            }, "Waiting for Server", "Waiting for response from Gateway...", MS_BEFORE_DLG);
        } catch (InterruptedException e) {
            String msg = "Remote invocation canceled by user: " + ExceptionUtils.getMessage(e);

            // We log and throw here because the exception is at risk of getting eaten without being
            // logged if the SSM is running as an untrusted applet and the runtime exception makes it all the
            // way back to the Swing event pump without being caught.
            logger.log(Level.WARNING, msg);
            throw new RemoteInvocationCanceledException(msg, e);
        } catch (InvocationTargetException e) {
            Throwable target = e.getCause();
            if (target instanceof ThrowableWrapper) {
                ThrowableWrapper wrapper = (ThrowableWrapper) target;
                throw wrapper.getCause();
            } else if (target != null) {
                throw target;
            }
            throw e;
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(AdminContextImpl.class.getName());

    private static final String PROP_BASE = "com.l7tech.console.";
    private static final boolean NO_CANCEL_DIALOGS = SyspropUtil.getBoolean(PROP_BASE + "suppressRemoteInvocationCancelDialog");
    private static final long MS_BEFORE_DLG = SyspropUtil.getLong(PROP_BASE + "remoteInvocationCancelDialogDelayMillis", 600L);

    private static class ThrowableWrapper extends Exception {
        private ThrowableWrapper(Throwable cause) {
            super(cause);
        }
    }
}
