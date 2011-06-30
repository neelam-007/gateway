package com.l7tech.gateway.common.spring.remoting.http;

import com.l7tech.util.ArrayUtils;
import com.l7tech.util.SyspropUtil;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;
import org.springframework.remoting.rmi.CodebaseAwareObjectInputStream;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Secure extension of the spring HttpInvokerServiceExporter.
 *
 * <p>This exporter is secure as it requires that either the connection is
 * "secured" or a configured class whitelist. This prevents malicious class
 * loading / reading of unexpected Serializable classes.</p>
 */
public class SecureHttpInvokerServiceExporter extends HttpInvokerServiceExporter {

    //- PUBLIC

    public Set<String> getPermittedClassNames() {
        return permittedClassNames;
    }

    public void setPermittedClassNames( final Set<String> permittedClassNames ) {
        this.permittedClassNames = permittedClassNames;
    }

    public SecurityCallback getSecurityCallback() {
        return securityCallback;
    }

    public void setSecurityCallback( final SecurityCallback securityCallback ) {
        this.securityCallback = securityCallback;
    }

    public interface SecurityCallback {
        void checkSecured() throws IOException;
    }

    public void setModuleClassLoader(ClassLoader moduleClassLoader) {
        this.moduleClassLoader = moduleClassLoader;
    }

    //- PROTECTED

    @Override
    public void handleRequest( final HttpServletRequest request,
                               final HttpServletResponse response ) throws ServletException, IOException {
        try {
            super.handleRequest(request, response);
        } catch ( SecurityIOException ioe ) {
            final RemoteInvocationResult fakeResult = new RemoteInvocationResult( ioe.getCause() );
            this.writeRemoteInvocationResult( request, response, fakeResult );
        }
    }

    @Override
    protected RemoteInvocation doReadRemoteInvocation( final ObjectInputStream ois ) throws IOException, ClassNotFoundException {
        final RemoteInvocation remoteInvocation = super.doReadRemoteInvocation(ois);
        validate( remoteInvocation );
        return remoteInvocation;
    }

    @Override
    protected ClassLoader getBeanClassLoader() {
        return moduleClassLoader != null ? moduleClassLoader : super.getBeanClassLoader();
    }

    @Override
    protected ObjectInputStream createObjectInputStream( final InputStream is ) throws IOException {
        if ( permittedClassNames == null || permittedClassNames.isEmpty() ) {
            // Ensure caller is permitted to use an unrestricted object input stream
            if ( securityCallback == null ) {
                throw new IOException( "Unable to check secured, access denied." );
            }

            try {
                securityCallback.checkSecured();
            } catch ( IOException ioe ) {
                throw new SecurityIOException( ioe );
            }

            return super.createObjectInputStream( is );
        } else if ( restrictClasses ) {
            return new RestrictedCodebaseAwareObjectInputStream(is, getBeanClassLoader(), permittedClassNames);
        } else {
            return super.createObjectInputStream( is );
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( SecureHttpInvokerServiceExporter.class.getName() );
    private static final boolean restrictMethods = SyspropUtil.getBoolean("com.l7tech.gateway.common.spring.remoting.http.restrictMethods", true);
    private static final String[] restrictedMethods = { "getClass", "hashCode", "toString" };
    private static final boolean restrictClasses = SyspropUtil.getBoolean("com.l7tech.gateway.common.spring.remoting.http.restrictClasses", true);

    private Set<String> permittedClassNames;
    private SecurityCallback securityCallback;
    private ClassLoader moduleClassLoader;

    private void validate( final RemoteInvocation remoteInvocation ) throws IOException {
        if ( remoteInvocation != null && restrictMethods ) {
            final String methodName = remoteInvocation.getMethodName();
            if ( ArrayUtils.contains( restrictedMethods, methodName ) ) {
                logger.warning( "Attempt to invoke restricted method '"+methodName+"'." );
                throw new IOException( "Method forbidden '"+methodName+"'." );     
            }
        }
    }

    private static class SecurityIOException extends IOException {
        private SecurityIOException( final IOException cause ) {
            super( cause );
        }
    }

    private static class RestrictedCodebaseAwareObjectInputStream extends CodebaseAwareObjectInputStream {
        private final Set<String> permittedClassNames;

        private RestrictedCodebaseAwareObjectInputStream( final InputStream inputStream,
                                                          final ClassLoader classLoader,
                                                          final Set<String> permittedClassNames ) throws IOException {
            super( inputStream, classLoader, null );
            this.permittedClassNames = permittedClassNames;
        }

        @Override
        protected Class resolveClass( final ObjectStreamClass classDesc ) throws IOException, ClassNotFoundException {
            if ( !permittedClassNames.contains( classDesc.getName() ) ) {
                logger.warning( "Attempt to load restricted class '"+classDesc.getName()+"'." );
                throw new ClassNotFoundException( classDesc.getName() );
            }
            return super.resolveClass(classDesc);
        }
    }
}
