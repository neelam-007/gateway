package com.l7tech.server.ems;

import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.util.ValidationUtils;
import org.apache.wicket.markup.html.WebResource;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.value.ValueMap;
import org.apache.wicket.protocol.http.WebResponse;

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
        if ( attemptedOperation == null || (securityManager != null && securityManager.hasPermission( attemptedOperation )) ) {
            stream = getSecureResourceStream();
        } else {
            stream = getAccessDeniedStream();
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

    @Override
    protected void setHeaders( final WebResponse webResponse) {
        super.setHeaders(webResponse);

        String filename = processFilename(getFilename());
        if ( !Strings.isEmpty(filename) ) {
            ValueMap parameters = getParameters();
            if ( "attachment".equals(parameters.getString("disposition") )) {
                webResponse.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            } else {
                webResponse.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
            }
        }
    }

    /**
     * Get the access denied stream.
     *
     * @return The access denied resource.
     */
    protected IResourceStream getAccessDeniedStream() {
        return new StringResourceStream("Access Denied.");
    }

    /**
     * Test if the current user has permission to perform the given operation.
     *
     * @param attemptedOperation The attempted operation.
     * @return true if permission is permitted.
     */
    protected boolean hasPermission( final AttemptedOperation attemptedOperation ) {
        EmsSecurityManager securityManager = securityManagerRef.get();
        return securityManager != null && securityManager.hasPermission( attemptedOperation );        
    }

    protected String getFilename() {
        return null;
    }

    protected EmsSecurityManager getSecurityManager() {
        return securityManagerRef.get();
    }

    protected String processFilename( final String filename ) {
        String name = null;

        if ( filename != null ) {
            StringBuilder builder = new StringBuilder();
            for ( char character : filename.toCharArray() ) {
                if ( ValidationUtils.ALPHA_NUMERIC.indexOf(character) >= 0 ||
                     character == '.' ||
                     character == '-') {
                    builder.append( character );
                } else {
                    builder.append( '_' );
                }
            }
            name = builder.toString();
        }

        return name;
    }

    //- PRIVATE

    private static AtomicReference<EmsSecurityManager> securityManagerRef = new AtomicReference<EmsSecurityManager>();
    private final AttemptedOperation attemptedOperation;
}
