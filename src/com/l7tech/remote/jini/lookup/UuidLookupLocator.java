package com.l7tech.remote.jini.lookup;

import net.jini.core.constraint.MethodConstraints;
import net.jini.core.constraint.RemoteMethodControl;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.jeri.ssl.SslEndpoint;
import net.jini.jeri.Endpoint;
import net.jini.jeri.ObjectEndpoint;
import net.jini.jeri.BasicObjectEndpoint;
import net.jini.jeri.BasicInvocationHandler;
import net.jini.id.UuidFactory;
import net.jini.id.Uuid;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * A <code>LookupLocator</code> implementation that performs unicast registrar discovery
 * based on the <code>Uuid<.code> on a given host.
 *
 * @see LookupLocator
 * @see Uuid
 * @see RegistrarLookup#REGISTRAR_UUID
 * @author emil
 * @version 17-May-2004
 */
public final class UuidLookupLocator extends LookupLocator implements RemoteMethodControl {
    private MethodConstraints methodConstraints;
    private String uuid;
    private static final Class[] proxyInterfaces = new Class[]{RegistrarLookup.class};

    public UuidLookupLocator(String host, int port, String uuid, MethodConstraints methodConstraints) {
        super(host, port);
        this.uuid = uuid;
        this.methodConstraints = methodConstraints;
    }

    public RemoteMethodControl setConstraints(MethodConstraints methodConstraints) {
        return new UuidLookupLocator(host, port, uuid, methodConstraints);
    }

    public MethodConstraints getConstraints() {
        return methodConstraints;
    }

    public ServiceRegistrar getRegistrar(int i)
      throws IOException, ClassNotFoundException {
        return getProxy().getRegistrar();
    }

    private final RegistrarLookup getProxy() {
        Endpoint e = SslEndpoint.getInstance("localhost", 2124);  //todo: from configuration! - em
        Uuid uuid = UuidFactory.create(RegistrarLookup.REGISTRAR_UUID);

        ObjectEndpoint oe = new BasicObjectEndpoint(e, uuid, false);
        InvocationHandler ih = new BasicInvocationHandler(oe, methodConstraints);
        ClassLoader cl = getClass().getClassLoader();

        return (RegistrarLookup)Proxy.newProxyInstance(cl, proxyInterfaces, ih);
    }
}
