package com.l7tech.server.ems.ui;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.util.Background;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.ValidationUtils;
import org.apache.commons.collections.map.LRUMap;
import org.apache.wicket.markup.html.WebResource;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.value.ValueMap;
import org.apache.wicket.protocol.http.WebResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.UUID;
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

        EsmSecurityManager securityManager = securityManagerRef.get();
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
    public static void setSecurityManager( final EsmSecurityManager securityManager ) {
        securityManagerRef.compareAndSet( null, securityManager );
    }

    /**
     * Register parameters for a resource access.
     *
     * @param parameters The parameters to register
     * @return The identifier for resource access.
     */
    public static String registerResourceParameters( final SecureResourceParameters parameters ) {
        final String id = UUID.randomUUID().toString();
        synchronized ( resourceLock ) {
            resourceInfo.put( id, parameters );
        }
        return id;
    }

    public static class SecureResourceParameters {
        private final long createdTime = System.currentTimeMillis();
        private final String disposition;

        protected SecureResourceParameters( final String disposition ) {
            this.disposition = disposition;
        }

        public String getDisposition() {
            return disposition;
        }

        private boolean isExpired() {
            return createdTime + resourceParameterExpiryAge < System.currentTimeMillis();
        }
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
        if (isCacheable()) {
            super.setHeaders(webResponse);
        } else { // default cache-control header breaks file downloading in IE when using SSL (bug 10273)
            webResponse.setHeader( HttpConstants.HEADER_CACHE_CONTROL, "max-age=15; s-maxage=0" );
        }

        String filename = processFilename(getFilename());
        if ( !Strings.isEmpty(filename) ) {
            ValueMap parameters = getParameters();

            final SecureResourceParameters resourceParameters = getResourceParameters( parameters.getString( "id" ), SecureResourceParameters.class );

            if ( (resourceParameters!=null && resourceParameters.getDisposition().equals( "attachment" )) ||
                 (resourceParameters==null && "attachment".equals(parameters.getString("disposition")) )) {
                webResponse.setAttachmentHeader( filename );
            } else {
                // Bug 6123: remove the filename in the below header setting.
                // In RFC2183, all examples of "inline" do not have "filename"
                // It seems if filename was there with the "inline" setting, IE interpret the presence of filename to mean download.
                webResponse.setHeader("Content-Disposition", "inline;");
            }
        }
    }

    protected <T extends SecureResourceParameters> T getResourceParameters( final String id,
                                                                            final Class<T> type ) {
        T resourceParameters = null;

        final Object parameters;
        synchronized ( resourceLock ) {
           parameters = resourceInfo.get( id );
        }
        if ( type.isInstance( parameters ) ) {
            resourceParameters = (T) parameters;
        }

        return resourceParameters;
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
        EsmSecurityManager securityManager = securityManagerRef.get();
        return securityManager != null && securityManager.hasPermission( attemptedOperation );        
    }

    protected String getFilename() {
        return null;
    }

    protected EsmSecurityManager getSecurityManager() {
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

    private static final AtomicReference<EsmSecurityManager> securityManagerRef = new AtomicReference<EsmSecurityManager>();
    private static final Map resourceInfo = new LRUMap();
    private static final Object resourceLock = new Object();
    private static final long resourceParameterExpiryAge = SyspropUtil.getLong( "com.l7tech.server.ems.resourceParameterExpiryAge", 60000L );

    private final AttemptedOperation attemptedOperation;

    static {
        Background.scheduleRepeated( new TimerTask(){
            @Override
            public void run() {
                expireResourceParameters();
            }
        }, 23417, 23417 );
    }

    private static void expireResourceParameters() {
        synchronized ( resourceLock ) {
            final Collection<String> expiredIds = new ArrayList<String>();
            for ( final Map.Entry<String,SecureResourceParameters> entry : (Set<Map.Entry<String,SecureResourceParameters>>)resourceInfo.entrySet() ) {
                final SecureResourceParameters parameters = entry.getValue();
                if ( parameters.isExpired() ) {
                    expiredIds.add( entry.getKey() );
                }
            }
            for ( final String id : expiredIds ) resourceInfo.remove( id );
        }
    }
}
