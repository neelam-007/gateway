package com.l7tech.remote.jini.lookup;

import net.jini.core.constraint.InvocationConstraints;
import net.jini.core.constraint.MethodConstraints;
import net.jini.io.context.ClientSubject;
import net.jini.jeri.*;

import javax.security.auth.Subject;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.server.ExportException;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.Principal;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import com.l7tech.identity.User;

/**
 * A basic invocation layer factory, used by Jini(TM) extensible remote invocation
 * (Jini ERI), that is based on{@link BasicILFactory} and provides
 * {@link InvocationDispatcher} and {@link  InvocationHandler} with access control.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class AccessILFactory extends BasicILFactory {
    private static final Logger logger = Logger.getLogger(AccessILFactory.class.getName());

    /**
     * Creates an <code>AccessILFactory</code>instance with no server
     * constraints, no permission class, and a <code>null</code> class
     * loader.
     */
    public AccessILFactory() {
    }

    /**
     * Creates a <code>BasicILFactory</code> with the specified server
     * constraints, permission class, and a <code>null</code> class
     * loader.
     *
     * @param	serverConstraints the server constraints, or <code>null</code>
     * @param	permissionClass the permission class, or <code>null</code>
     * @throws	IllegalArgumentException if the permission class is
     * abstract, is not a subclass of {@link java.security.Permission}, or does
     * not have a public constructor that has either one
     * <code>String</code> parameter or one {@link Method}
     * parameter and has no declared exceptions
     */
    public AccessILFactory(MethodConstraints serverConstraints, Class permissionClass) {
        super(serverConstraints, permissionClass);
    }

    /**
     * Creates an <code>AccessILFactory</code>instance with no server
     * constraints, no permission class, and the specified class loader.
     * The specified class loader is used by the {@link #createInstances
     * createInstances} method.
     *
     * @param loader the class loader, or <code>null</code>
     */
    public AccessILFactory(ClassLoader loader) {
        super(null, null, loader);
    }

    /**
     * Returns an {@link AccessDispatcher} instance constructed with the
     * specified methods, the specified server capabilities, and the class
     * loader specified at construction.
     *
     * @return the {@link AccessDispatcher} instance
     * @throws NullPointerException {@inheritDoc}
     */
    protected InvocationDispatcher
      createInvocationDispatcher(Collection methods,
                                 Remote impl,
                                 ServerCapabilities caps)
      throws ExportException {
        if (impl == null) {
            throw new NullPointerException("impl is null");
        }
        logger.finest("the access dispatcher created");
        return new AccessDispatcher(methods, caps, getClassLoader());
    }

    /**
     * Returns an invocation handler to use with a {@link java.lang.reflect.Proxy} instance
     * implementing the specified interfaces, communicating with the
     * specified remote object using the specified object endpoint.
     * <p/>
     * <p><code>BasicILFactory</code> implements this method to
     * return a {@link BasicInvocationHandler} constructed with the
     * specified object endpoint and this factory's server constraints.
     *
     * @throws	NullPointerException {@inheritDoc}
     */
    protected InvocationHandler createInvocationHandler(Class[] interfaces, Remote impl, ObjectEndpoint oe) throws ExportException {
        for (int i = interfaces.length; --i >= 0;) {
            if (interfaces[i] == null) {
                throw new NullPointerException();
            }
        }
        if (impl == null) {
            throw new NullPointerException();
        }
        logger.finest("the invocation handler created");
        return new AccessInvocationHandler(oe, getServerConstraints());
    }

    /**
     * A subclass of {@link BasicInvocationHandler} that handles the subject
     * passing from the client to the server.
     */

    public static class AccessInvocationHandler extends BasicInvocationHandler {

        /**
         * Creates a new <code>BasicInvocationHandler</code> with the
         * specified <code>ObjectEndpoint</code> and server constraints.
         * <p/>
         * <p>The client constraints of the created
         * <code>BasicInvocationHandler</code> will be <code>null</code>.
         *
         * @param	oe the <code>ObjectEndpoint</code> for this invocation handler
         * @param	serverConstraints the server constraints, or <code>null</code>
         * @throws	NullPointerException if <code>oe</code> is <code>null</code>
         */
        public AccessInvocationHandler(ObjectEndpoint oe, MethodConstraints serverConstraints) {
            super(oe, serverConstraints);
        }

        /**
         * Creates a new <code>BasicInvocationHandler</code> with the
         * specified client constraints and with the same
         * <code>ObjectEndpoint</code> and server constraints as the given
         * other <code>BasicInvocationHandler</code>.
         * <p/>
         * <p>This constructor is intended for use by the
         * <code>BasicInvocationHandler</code> implementation of the
         * {@link #setClientConstraints} method.  To create a copy of a
         * given <code>BasicInvocationHandler</code> with new client
         * constraints, use the {@link net.jini.core.constraint.RemoteMethodControl#setConstraints
         * RemoteMethodControl.setConstraints} method on the containing
         * proxy.
         *
         * @param	other the <code>BasicInvocationHandler</code> to obtain the
         * <code>ObjectEndpoint</code> and server constraints from
         * @param	clientConstraints the client constraints, or
         * <code>null</code>
         * @throws	NullPointerException if <code>other</code> is
         * <code>null</code>
         */
        public AccessInvocationHandler(AccessInvocationHandler other, MethodConstraints clientConstraints) {
            super(other, clientConstraints);
        }

        /**
         * Marshals a representation of the given <code>method</code> to
         * the outgoing request stream, <code>out</code>.  For each remote
         * call, the <code>invoke</code> method calls this method to
         * marshal a representation of the method.
         * <p/>
         * <p><code>BasicInvocationHandler</code> implements this method
         * to write the JRMP method hash (defined in section 8.3 of the
         * RMI specification) for the given method to the output stream
         * using the {@link java.io.ObjectOutputStream#writeLong writeLong}
         * method.
         * <p/>
         * <p>A subclass can override this method to control how the remote
         * method is marshalled.
         *
         * @param method the <code>Method</code> instance corresponding
         *               to the interface method invoked on the proxy
         *               instance.  The declaring class of the
         *               <code>Method</code> object will be the interface that
         *               the method was declared in, which may be a
         *               superinterface of the proxy interface that the proxy
         *               class inherits the method through.
         * @param	proxy the proxy instance that the method was invoked on
         * @param	out outgoing request stream for the remote call
         * @param	context the client context
         * @throws	java.io.IOException if an I/O exception occurs
         * @throws	NullPointerException if any argument is <code>null</code>
         */
        protected void marshalMethod(Object proxy,
                                     Method method,
                                     ObjectOutputStream out,
                                     Collection context)
          throws IOException {
            if (proxy == null || method == null || context == null) {
                throw new NullPointerException();
            }
            Subject subject = Subject.getSubject(AccessController.getContext());
            if (subject == null || subject.getPrincipals().isEmpty()) {
                throw new
                  AccessControlException("Cannot dispatch the request. No principal set.");
            }
            logger.finest("Invoke: '" + proxy.getClass().getName() + "#" + method.getName() + "' principal : '" + extractPrincipalName(subject) + "'");
            out.writeObject(subject);
            super.marshalMethod(proxy, method, out, context);
        }

    }

    /**
     * A subclass of {@link BasicInvocationDispatcher} that only accepts
     * lookup calls from the local host.
     */
    public static class AccessDispatcher extends BasicInvocationDispatcher {
        /**
         * Constructs an invocation dispatcher for the specified methods.
         *
         * @param	methods a collection of {@link Method} instances
         * for the	remote methods
         * @param	caps the transport capabilities of the server
         * @param	loader the class loader, or <code>null</code>
         * @throws	NullPointerException if <code>methods</code> is
         * <code>null</code> or if <code>methods</code> contains a
         * <code>null</code> elememt
         * @throws	IllegalArgumentException if <code>methods</code>
         * contains an element that is not a <code>Method</code>
         * instance
         */
        public AccessDispatcher(Collection methods,
                                ServerCapabilities caps,
                                ClassLoader loader)
          throws ExportException {
            super(methods, caps, null, null, loader);
        }

        /**
         * Checks that the client is calling from the local host.
         * //todo: rework this with the local host principal - em
         *
         * @throws java.security.AccessControlException
         *          if the client is not
         *          calling from the local host
         */
        protected void checkAccess(Remote impl,
                                   Method method,
                                   InvocationConstraints constraints,
                                   Collection context) {
//            if (!LocalAccess.isLocal()) {
//                try {
//                    ServerContext.getServerContextElement(Subject.class);
//                } catch (ServerNotActiveException e) {
//                    IllegalStateException ie = new IllegalStateException();
//                    ie.initCause(e);
//                    throw ie;
//                }
//            }
        }


        protected Method unmarshalMethod(Remote impl, ObjectInputStream in, Collection context)
          throws IOException, NoSuchMethodException, ClassNotFoundException {
            Subject subject = (Subject)in.readObject();
            if (subject == null) {
                throw new AccessControlException("No subject presented.");
            }
            // if there is a null subject remove from context, if there is exisitng subject
            // combine principals
            Subject exisitingSubject = null;
            Iterator iter = context.iterator();
            while (iter.hasNext()) {
                Object elem = iter.next();
                if (elem != null && ClientSubject.class.isAssignableFrom(elem.getClass())) {
                    ClientSubject clientSubject = (ClientSubject)elem;
                    if (clientSubject.getClientSubject() != null) {
                        exisitingSubject = clientSubject.getClientSubject();
                    } else {
                        context.remove(elem);
                    }
                    break;
                }
            }
            if (exisitingSubject == null) {
                ClientSubject cs = new ClientSubjectImpl(subject);
                context.add(cs);
            } else {
                exisitingSubject.getPrincipals().addAll(subject.getPrincipals());
                exisitingSubject.getPrivateCredentials().addAll(subject.getPrivateCredentials());
                exisitingSubject.getPublicCredentials().addAll(subject.getPublicCredentials());
            }
            Method m = super.unmarshalMethod(impl, in, context);
            logger.finest("Invoke: '" + impl.getClass().getName() + "#" + m.getName() + "' principal : '" + extractPrincipalName(subject) + "'");
            return m;
        }

        private static class ClientSubjectImpl implements ClientSubject {
            Subject subject;

            private ClientSubjectImpl(Subject subject) {
                this.subject = subject;
            }

            public Subject getClientSubject() {
                return subject;
            }
        }
    }


    private static String extractPrincipalName(Subject subject) {
        Set principals = subject.getPrincipals();
        for (Iterator iterator = principals.iterator(); iterator.hasNext();) {
            Object o = (Object)iterator.next();
            if (o instanceof User) {
                return ((User)o).getLogin();
            } else if (o instanceof Principal) {
                return ((Principal)o).getName();
            } else {
                return o.toString();
            }
        }
        return "no principal set";
    }


}
