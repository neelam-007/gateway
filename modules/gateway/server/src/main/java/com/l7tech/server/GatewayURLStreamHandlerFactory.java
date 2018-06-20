package com.l7tech.server;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static java.util.logging.Level.FINEST;

/**
 * Implementation of {@link java.net.URLStreamHandlerFactory} that holds concrete factories for {@link java.net.URLStreamHandler} implementations.
 * Delegates operations to the correct implementation according to the protocol.
 *
 * An {@link URLStreamHandlerFactory} is responsible to create instances of {@link URLStreamHandler}s that handle access to class loader resources
 * like jar-files, manifests, properties files and also external resources like http/s or s/ftp/s addresses.
 *
 * Java enable that applications can register one URLStreamHandlerFactory per JVM, using URL.setURLStreamHandlerFactory.
 * But if someone try to register a second one, an exception is thrown and application fail to start.
 *
 * So, this is needed because Gateway needs to handle multiple factories (tomcat has one internal, and we register one for modular assertions) and need to be
 * installed early in the boot process to ensure that our implementation is the one that is used.
 *
 * @see java.net.URLStreamHandlerFactory
 * @see java.net.URLStreamHandler
 * @see java.net.URL
 * @see org.apache.naming.resources.DirContextURLStreamHandlerFactory
 * @see com.l7tech.server.policy.module.ModularAssertionURLStreamHandler (see here for the description of the original problem)
 * @see com.l7tech.server.policy.ServerAssertionRegistry
 */
public class GatewayURLStreamHandlerFactory implements URLStreamHandlerFactory {

    private static final Logger LOGGER = Logger.getLogger(GatewayURLStreamHandlerFactory.class.getName());
    private static final Map<String, URLStreamHandlerFactory> HANDLER_FACTORIES = new ConcurrentHashMap<>();
    private static GatewayURLStreamHandlerFactory instance = null;

    private GatewayURLStreamHandlerFactory() {
        // no external instantiation
    }

    @Override
    public URLStreamHandler createURLStreamHandler(final String protocol) {
        if (HANDLER_FACTORIES.containsKey(protocol)) {
            LOGGER.log(FINEST, "Creating handler for protocol {0}", protocol);
            return HANDLER_FACTORIES.get(protocol).createURLStreamHandler(protocol);
        }
        LOGGER.log(FINEST, "No handler factory available for {0}", protocol);
        return null;
    }

    /**
     * Register a new handler factory for a specified protocol
     *
     * @param protocol protocol name
     * @param factory factory instance
     */
    public static void registerHandlerFactory(String protocol, URLStreamHandlerFactory factory) {
        HANDLER_FACTORIES.put(protocol, factory);
    }

    /**
     * Install an instance of {@link URLStreamHandlerFactory}. Any error will abort the installation.
     */
    @SuppressWarnings("squid:S1181") // to suppress sonar about throwable
    public static void install() throws LifecycleException {
        if (instance != null) {
            // factory already installed
            return;
        }

        try {
            GatewayURLStreamHandlerFactory factory = new GatewayURLStreamHandlerFactory();
            URL.setURLStreamHandlerFactory(factory);
            instance = factory;
        } catch (Throwable e) {
            throw new LifecycleException("Error installing GatewayURLStreamHandlerFactory", e);
        }
    }

}
