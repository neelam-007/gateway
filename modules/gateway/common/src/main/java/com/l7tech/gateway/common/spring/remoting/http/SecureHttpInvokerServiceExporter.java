package com.l7tech.gateway.common.spring.remoting.http;

import com.l7tech.gateway.common.admin.security.DeserializeClassFilter;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    public void setBypassDeserializationClassFilter(@Nullable final Boolean bypassDeserializationClassFilter) {
        this.bypassDeserializationClassFilter = bypassDeserializationClassFilter;
    }

    public boolean isDeserializationClassFilterDisabled() {
        return bypassDeserializationClassFilter != null && bypassDeserializationClassFilter;
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
        } catch (ClassNotPermittedException ex) {
            final RemoteInvocationResult fakeResult = new RemoteInvocationResult( ex );
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

            if (isDeserializationClassFilterDisabled()) {
                return super.createObjectInputStream( is );
            }
            return new ClassFilterCodebaseAwareObjectInputStream(is, getBeanClassLoader(), getDeserializationClassFilter());
        } else if ( restrictClasses ) {
            return new RestrictedCodebaseAwareObjectInputStream(is, getBeanClassLoader(), permittedClassNames);
        } else {
            return super.createObjectInputStream( is );
        }
    }

    //- PACKAGE

    @NotNull
    ClassFilter getDeserializationClassFilter() {
        return DeserializeClassFilter.getInstance();
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( SecureHttpInvokerServiceExporter.class.getName() );
    private static final boolean restrictMethods = ConfigFactory.getBooleanProperty( "com.l7tech.gateway.common.spring.remoting.http.restrictMethods", true );
    private static final String[] restrictedMethods = { "getClass", "hashCode", "toString" };
    private static final boolean restrictClasses = ConfigFactory.getBooleanProperty( "com.l7tech.gateway.common.spring.remoting.http.restrictClasses", true );

    private Set<String> permittedClassNames;
    private SecurityCallback securityCallback;
    private ClassLoader moduleClassLoader;
    @Nullable
    private Boolean bypassDeserializationClassFilter;

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

    static private class ClassFilterCodebaseAwareObjectInputStream extends CodebaseAwareObjectInputStream {
        @NotNull
        private final ClassFilter classFilter;

        private ClassFilterCodebaseAwareObjectInputStream(
                final InputStream inputStream,
                final ClassLoader classLoader,
                @NotNull final ClassFilter classFilter
        ) throws IOException {
            super(inputStream, classLoader, null);
            this.classFilter = classFilter;
        }

        @Override
        protected Class resolveClass(final ObjectStreamClass classDesc) throws IOException, ClassNotFoundException {
            final String clsName = classDesc.getName();
            if (classFilter.permitClass(clsName)) {
                return super.resolveClass(classDesc);
            }

            logger.warning("Attempt to deserialize non-whitelisted class '" + clsName + "'.");
            // log developers note only if debug state is set and log level is set to FINE
            if (JdkLoggerConfigurator.debugState()) {
                final String devNote = "NOTE TO DEVELOPERS:" + System.lineSeparator() +
                        "All serialized classes must be whitelisted before they can be deserialized by the Gateway." + System.lineSeparator() +
                        "Class '" + clsName + "' is not whitelisted." + System.lineSeparator() +
                        "If this is refactored or newly added class for one of the admin interfaces, than make sure the class is " +
                        "properly whitelisted, by either annotating it with @DeserializeSafe (preferred) or adding the class name inside " +
                        "DeserializeClassFilter#ALLOWED_CLASSES collection." + System.lineSeparator() +
                        "Optionally, when developing a new admin interface, deserialization ClassFilter can be temporary " +
                        "disabled by setting 'bypassDeserializationClassFilter' property to 'true' inside admin-servlet.xml." + System.lineSeparator() +
                        "Once the admin interface is complete, remove bypassDeserializationClassFilter property and " +
                        "properly whitelist used classes." + System.lineSeparator() +
                        "WARNING:" + System.lineSeparator() +
                        "Developer must ensure that whitelisted classes ARE NOT from untrusted sources and they CANNOT be used to execute" +
                        "arbitrary Java code e.g. java.lang.System and java.lang.Runtime MUST NOT be permitted." + System.lineSeparator() +
                        "In addition Apache InvokerTransformer is known to have remote code execution vulnerability, and MUST NOT be permitted." + System.lineSeparator() +
                        "For more details refer to https://wiki.l7tech.com/mediawiki/index.php/Deserialization_ClassFilter." + System.lineSeparator();
                logger.info(devNote);
            }

            throw new ClassNotPermittedException("Attempt to deserialize non-whitelisted class '" + clsName + "'", clsName);
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
            final String clsName = classDesc.getName();
            if ( !permittedClassNames.contains( clsName ) ) {
                logger.warning( "Attempt to load restricted class '" + clsName + "'." );
                throw new ClassNotPermittedException("Attempt to deserialize non-whitelisted class '" + clsName + "'", clsName);
            }
            return super.resolveClass(classDesc);
        }
    }
}
