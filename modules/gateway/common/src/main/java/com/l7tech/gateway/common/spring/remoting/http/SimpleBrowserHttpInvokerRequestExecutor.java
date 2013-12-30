package com.l7tech.gateway.common.spring.remoting.http;

import com.l7tech.gateway.common.spring.remoting.ssl.SSLTrustFailureHandler;
import com.l7tech.util.FilterClassLoader;
import com.l7tech.util.Functions;
import com.l7tech.util.Functions.BinaryThrows;
import com.l7tech.util.InetAddressUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.remoting.httpinvoker.HttpInvokerClientConfiguration;
import org.springframework.remoting.httpinvoker.SimpleHttpInvokerRequestExecutor;
import org.springframework.remoting.rmi.CodebaseAwareObjectInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.l7tech.util.CollectionUtils.toSet;

/**
 * Extension of the Spring SimpleHttpInvokerRequestExecutor for setting host/session.
 *
 * @author Steve Jones
 */
public class SimpleBrowserHttpInvokerRequestExecutor extends SimpleHttpInvokerRequestExecutor implements ConfigurableHttpInvokerRequestExecutor {

    //- PUBLIC

    public SimpleBrowserHttpInvokerRequestExecutor( @NotNull  final Set<String> excludedBeanPackages,
                                                    @Nullable final BinaryThrows<Class,String,ClassNotFoundException,ClassNotFoundException> classFinder ) {
        this.hostSubstitutionPattern = Pattern.compile(HOST_REGEX);
        this.sessionInfoHolder = new SessionSupport();
        this.excludedBeanPackages = toSet( excludedBeanPackages );
        this.classFinder = classFinder != null ?
                classFinder :
                new BinaryThrows<Class,String,ClassNotFoundException,ClassNotFoundException>(){
                    @Override
                    public Class call( final String s, final ClassNotFoundException e ) throws ClassNotFoundException {
                        throw e;
                    }
                };
        this.useExcludedPackages.set( !this.excludedBeanPackages.isEmpty() );
    }

    @Override
    public <R, E extends Throwable> R doWithSession(String host, int port, String sessionId, Functions.NullaryThrows<R, E> block) throws E {
        SessionSupport.SessionInfo info = sessionInfoHolder.getSessionInfo();
        final String oldHost = info.host;
        final int oldPort = info.port;
        final String oldSessionId = info.sessionId;

        try {
            info.host = host;
            info.port = port;
            info.sessionId = sessionId;

            return block.call();
        } finally {
            info.host = oldHost;
            info.port = oldPort;
            info.sessionId = oldSessionId;
        }
    }

    @Override
    public <R, E extends Throwable> R doWithTrustFailureHandler(SSLTrustFailureHandler failureHandler, Functions.NullaryThrows<R, E> block) throws E {
        return block.call();
    }

    //- PROTECTED
        
    protected HttpURLConnection openConnection(HttpInvokerClientConfiguration config) throws IOException {
        SessionSupport.SessionInfo info = sessionInfoHolder.getSessionInfo();

        HttpURLConnection connection = super.openConnection(new HttpInvokerClientConfigurationImpl(config, info.host, info.port));
        connection.setRequestProperty("X-Layer7-SessionId", info.sessionId);

        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);

        return connection;
    }

    @Override
    protected ObjectInputStream createObjectInputStream( final InputStream is, final String codebaseUrl ) throws IOException {
        ClassLoader loader = getBeanClassLoader();
        if ( useExcludedPackages.get() ) {
            try {
                loader = AccessController.doPrivileged( new PrivilegedAction<ClassLoader>() {
                    @Override
                    public ClassLoader run() {
                        return new FilterClassLoader(getBeanClassLoader(), null, excludedBeanPackages, false);
                    }
                } );
            } catch ( SecurityException e ) {
                // no filtering
                useExcludedPackages.set( false );
            }
        }
        return new CodebaseAwareObjectInputStream(is, loader, codebaseUrl){
            @Override
            protected Class resolveFallbackIfPossible( final String className, final ClassNotFoundException ex ) throws IOException, ClassNotFoundException {
                try {
                    return classFinder.call( className, ex );
                } catch ( ClassNotFoundException e ) {
                    return super.resolveFallbackIfPossible( className, ex );
                }
            }
        };
    }

    //- PRIVATE

    private static final String HOST_REGEX = "(?<=^http[s]?\\://)[a-zA-Z_\\-0-9\\.\\:]{1,1024}";

    private Pattern hostSubstitutionPattern;
    private final SessionSupport sessionInfoHolder;
    private final Set<String> excludedBeanPackages;
    private final BinaryThrows<Class,String,ClassNotFoundException,ClassNotFoundException> classFinder;
    private final AtomicBoolean useExcludedPackages = new AtomicBoolean(false);

     private class HttpInvokerClientConfigurationImpl implements HttpInvokerClientConfiguration {
        private final HttpInvokerClientConfiguration delegate;
        private final String host;
        private final int port;

        private HttpInvokerClientConfigurationImpl(final HttpInvokerClientConfiguration config,
                                                   final String host,
                                                   final int port) {
            this.delegate = config;
            this.host = host;
            this.port = port;
        }

        public String getCodebaseUrl() {
            return decorate(delegate.getCodebaseUrl());
        }

        public String getServiceUrl() {
            return decorate(delegate.getServiceUrl());
        }

        /**
         * Turn the URL into a relative URL and add the session id if there is one.
         */
        private String decorate(final String url) {
            // switch in the correct host/port
            Matcher matcher = hostSubstitutionPattern.matcher(url);
            return matcher.replaceFirst(InetAddressUtil.getHostForUrl(host) + ":" + port);
        }
    }
}
