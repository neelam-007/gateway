package com.l7tech.console.util;

import com.l7tech.gateway.common.cassandra.CassandraConnectionManagerAdmin;
import com.l7tech.gateway.common.siteminder.SiteMinderAdmin;
import com.l7tech.console.panels.CancelableOperationDialog;
import com.l7tech.gateway.common.admin.*;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.spring.remoting.http.ConfigurableHttpInvokerRequestExecutor;
import com.l7tech.gateway.common.spring.remoting.http.RemotingContext;
import com.l7tech.gateway.common.task.ScheduledTaskAdmin;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gateway.common.transport.email.EmailAdmin;
import com.l7tech.gateway.common.transport.email.EmailListenerAdmin;
import com.l7tech.gateway.common.transport.ftp.FtpAdmin;
import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.gateway.common.workqueue.WorkQueueManagerAdmin;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.Utilities.getBlockerOrSelf;

/**
 * Admin context implementation
 */
public class AdminContextImpl extends RemotingContext implements AdminContext {

    //- PUBLIC

    @Override
    public AdminLogin getAdminLogin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(AdminLogin.class);
    }

    @Override
    public IdentityAdmin getIdentityAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(IdentityAdmin.class);
    }

    @Override
    public ServiceAdmin getServiceAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(ServiceAdmin.class);
    }

    @Override
    public FolderAdmin getFolderAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(FolderAdmin.class);
    }

    @Override
    public JmsAdmin getJmsAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(JmsAdmin.class);
    }

    @Override
    public JdbcAdmin getJdbcConnectionAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(JdbcAdmin.class);
    }

    @Override
    public CassandraConnectionManagerAdmin getCassandraConnecitonAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(CassandraConnectionManagerAdmin.class);
    }

    @Override
    public SiteMinderAdmin getSiteMinderConfigurationAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(SiteMinderAdmin.class);
    }

    @Override
    public FtpAdmin getFtpAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(FtpAdmin.class);
    }

    @Override
    public TrustedCertAdmin getTrustedCertAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(TrustedCertAdmin.class);
    }

    @Override
    public ResourceAdmin getResourceAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(ResourceAdmin.class);
    }

    @Override
    public CustomAssertionsRegistrar getCustomAssertionsRegistrar() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(CustomAssertionsRegistrar.class);
    }

    @Override
    public AuditAdmin getAuditAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(AuditAdmin.class);
    }

    @Override
    public ClusterStatusAdmin getClusterStatusAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(ClusterStatusAdmin.class);
    }

    @Override
    public KerberosAdmin getKerberosAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(KerberosAdmin.class);
    }

    @Override
    public RbacAdmin getRbacAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(RbacAdmin.class);
    }

    @Override
    public TransportAdmin getTransportAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(TransportAdmin.class);
    }

    @Override
    public PolicyAdmin getPolicyAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(PolicyAdmin.class);
    }

    @Override
    public LogSinkAdmin getLogSinkAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(LogSinkAdmin.class);
    }

    @Override
    public EmailListenerAdmin getEmailListenerAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(EmailListenerAdmin.class);
    }

    @Override
    public EmailAdmin getEmailAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(EmailAdmin.class);
    }

    @Override
    public ScheduledTaskAdmin getScheduledTaskAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(ScheduledTaskAdmin.class);
    }

    @Override
    public UDDIRegistryAdmin getUDDIRegistryAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(UDDIRegistryAdmin.class);
    }

    @Override
    public WorkQueueManagerAdmin getWorkQueueAdmin() throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(WorkQueueManagerAdmin.class);
    }

    @Override
    public <AI> AI getAdminInterface( final Class<AI> adminInterfaceClass ) throws SecurityException {
        return this.getRemoteInterfaceForEndpoint(adminInterfaceClass);
    }

    //- PACKAGE

    /**
     *
     */
    AdminContextImpl( final String host,
                      final int port,
                      final String sessionId,
                      final Collection<Object> remoteObjects,
                      final ConfigurableHttpInvokerRequestExecutor configurableInvoker,
                      final Functions.Nullary<Void> activityCallback ) {
        super( host, port, sessionId, remoteObjects, configurableInvoker );
        this.activityCallback = activityCallback;
    }

    //- PROTECTED

    @Override
    protected Object doRemoteInvocation( final Object targetObject,
                                         final Method method,
                                         final Object[] args) throws Throwable
    {
        long start = TRACE ? System.currentTimeMillis() : 0;
        try {
            return reallyDoRemoteInvocation(targetObject, method, args);
        } finally {
            if (TRACE) {
                long end = System.currentTimeMillis();
                double total = ((double)end - (double)start) / 1000;
                System.out.printf("%6.2f sec for: %s\n", total, method.toString());
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(AdminContextImpl.class.getName());

    private static final String PROP_BASE = "com.l7tech.console.";
    private static final boolean NO_CANCEL_DIALOGS = ConfigFactory.getBooleanProperty( PROP_BASE + "suppressRemoteInvocationCancelDialog", false );
    private static final long MS_BEFORE_DLG = ConfigFactory.getLongProperty( PROP_BASE + "remoteInvocationCancelDialogDelayMillis", 10000L );
    private static final boolean TRACE = ConfigFactory.getBooleanProperty( PROP_BASE + "remoteInvocationTracing", false );

    private final Functions.Nullary<Void> activityCallback;

    private Object reallyDoRemoteInvocation( final Object targetObject,
                                         final Method method,
                                         final Object[] args) throws Throwable {
        if (NO_CANCEL_DIALOGS || !SwingUtilities.isEventDispatchThread() || Utilities.isAnyThreadDoingWithDelayedCancelDialog())
            return super.doRemoteInvocation(targetObject, method, args);    //To change body of overridden methods use File | Settings | File Templates.

        // Track activity that will keep the session alive on the Gateway
        final Administrative administrative = method.getAnnotation( Administrative.class );
        final boolean background = administrative!=null && administrative.background();

        // Running on Swing event thread, and no delayed cancel dialog already pending
        try {
            final Object result = CancelableOperationDialog.doWithDelayedCancelDialog(
                    new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            try {
                                return AdminContextImpl.super.doRemoteInvocation(targetObject, method, args);
                            } catch (InterruptedException ie) {
                                throw ie;
                            } catch (Throwable throwable) {
                                throw new ThrowableWrapper(throwable);
                            }
                        }
                    },
                    getBlockerOrSelf( TopComponents.getInstance().getTopParent() ),
                    "Waiting for Server",
                    "Waiting for response from Gateway...",
                    MS_BEFORE_DLG );
            if ( !background && activityCallback != null ) {
                activityCallback.call();
            }
            return result;
        } catch (InterruptedException e) {
            String msg = "Remote invocation canceled by user: " + ExceptionUtils.getMessage(e);

            // We log and throw here because the exception is at risk of getting eaten without being
            // logged if the SSM is running as an untrusted applet and the runtime exception makes it all the
            // way back to the Swing event pump without being caught.
            logger.log(Level.WARNING, msg);
            throw new RemoteInvocationCanceledException(msg, e);
        } catch (InvocationTargetException e) {
            //noinspection ThrowableInstanceNeverThrown
            ConnectException causedBy = ExceptionUtils.getCauseIfCausedBy(e, ConnectException.class);
            if(causedBy!=null){
                final String logMsg = causedBy.getMessage() != null? causedBy.getMessage(): "Error during remote API call";
                logger.log(Level.WARNING, logMsg, ExceptionUtils.getDebugException(e));

            } else{
                logger.log(Level.WARNING, "Exception during remote API call: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(new Throwable(e)));
            }
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

    private static class ThrowableWrapper extends Exception {
        private ThrowableWrapper(Throwable cause) {
            super(cause);
        }
    }
}
