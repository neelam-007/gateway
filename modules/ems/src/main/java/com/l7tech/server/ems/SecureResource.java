package com.l7tech.server.ems;

import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import org.apache.wicket.markup.html.WebResource;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;

import java.util.concurrent.atomic.AtomicReference;

/**
 * WebResource that checks permissions.
 */
public abstract class SecureResource extends WebResource {

    //- PUBLIC

    /**
     * Get the resource stream if permitted.
     *
     * @return The stream or null.
     */
    @Override
    public final IResourceStream getResourceStream() {
        IResourceStream stream;

        EmsSecurityManager securityManager = securityManagerRef.get();
        if ( securityManager != null && securityManager.hasPermission( attemptedOperation ) ) {
            stream = getSecureResourceStream();
        } else {
            stream = new StringResourceStream("Access Denied.");
        }
        
        return stream;
    }

    /**
     * Set the security manager for checking resource accesses.
     *
     * @param securityManager The manager for security checks
     */
    public static void setSecurityManager( final EmsSecurityManager securityManager ) {
        securityManagerRef.compareAndSet( null, securityManager );
    }

    //- PROTECTED

    /**
     * Create a SecureAction which may only be enabled if the user meets the admin requirement,
     * and which does not depend on any licensing feature to be enabled.
     */
    protected SecureResource( final AttemptedOperation attemptedOperation ) {
        this.attemptedOperation = attemptedOperation;
    }

    protected abstract IResourceStream getSecureResourceStream();

    //- PRIVATE

    private static AtomicReference<EmsSecurityManager> securityManagerRef = new AtomicReference<EmsSecurityManager>();
    private final AttemptedOperation attemptedOperation;
}
