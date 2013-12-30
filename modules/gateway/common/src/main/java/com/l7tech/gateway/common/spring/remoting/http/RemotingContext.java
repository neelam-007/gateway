package com.l7tech.gateway.common.spring.remoting.http;

import com.l7tech.util.Functions;

import java.util.Collection;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;

/**
 * Context support for HTTP remoting.
 *
 * <p>The context supplies information that cannot be application wide, such
 * as the host and port.</p>
 */
public class RemotingContext {

    //- PROTECTED

    /**
     * Create a remoting context for the given interfaces and beans.
     *
     * @param host The host to connect to
     * @param port The port to connect to
     * @param remoteObjects The client side invokers
     */
    protected RemotingContext( final String host,
                               final int port,
                               final String sessionId,
                               final Collection<Object> remoteObjects,
                               final ConfigurableHttpInvokerRequestExecutor configurableInvoker ) {
        if ( host == null ) throw new NullPointerException("null host");

        this.host = host;
        this.port = port;
        this.sessionId = sessionId;
        this.remoteObjects = remoteObjects;
        this.configurableInvoker = configurableInvoker;
    }

    /**
     * Get a remote inteface that invokes the method on this contexts endpoint.
     *
     * @param remoteInterfaceClass The desired interface.
     * @return The object to use
     */
    protected <RI> RI getRemoteInterfaceForEndpoint( Class<RI> remoteInterfaceClass ) {
        Object targetObject = null;

        for ( Object remoteObject : remoteObjects  ) {
            if ( remoteInterfaceClass.isInstance(remoteObject) ) {
                targetObject = remoteObject;
                break;
            }

        }

        if ( targetObject == null ) {
            throw new IllegalStateException( "Instance not found for remote interface '"+remoteInterfaceClass.getName()+"'." );
        }

        return createInstance( remoteInterfaceClass, targetObject );
    }

    protected Object doRemoteInvocation( final Object targetObject, final Method method, final Object[] args ) throws Throwable {
        try {
            return configurableInvoker.doWithSession(host, port, sessionId,  new Functions.NullaryThrows<Object, Throwable>() {
                @Override
                public Object call() throws Throwable {
                    return method.invoke( targetObject, args );
                }
            });
        } catch ( InvocationTargetException ite ) {
            throw ite.getCause();
        }
    }

    //- PRIVATE

    private final String host;
    private final int port;
    private final String sessionId;
    private final Collection<Object> remoteObjects;
    private final ConfigurableHttpInvokerRequestExecutor configurableInvoker;

    /**
     * Create a new instance on each invocation, supports both singletons
     * and templates.
     */
    @SuppressWarnings({"unchecked"})
    private <T> T createInstance( final Class<T> targetInterface,
                                  final Object targetObject ) {
        return (T) Proxy.newProxyInstance(
                RemotingContext.class.getClassLoader(),
                new Class[]{ targetInterface },
                new RemoteEndpointHandler(targetObject));
    }


    /**
     * InvocationHandler
     */
    private final class RemoteEndpointHandler implements InvocationHandler {
        private final Object targetObject;

        private RemoteEndpointHandler( final Object targetObject ) {
            this.targetObject = targetObject;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return doRemoteInvocation( targetObject, method, args );
        }
    }
}
