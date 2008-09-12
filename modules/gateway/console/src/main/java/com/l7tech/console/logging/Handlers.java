package com.l7tech.console.logging;

/**
 * The error handlers package private utility class.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
class Handlers {

    /**
     * this class canoot be instantiated
     */
    private Handlers() {}

    /**
     * Return the list of the default handlers. The handlers are executed
     * in that order.s
     *
     * @return the list of default handlers
     */
    static ErrorHandler[] defaultHandlers() {
        return new ErrorHandler[]{
            new RemoteInvocationCanceledHandler(),
            new RoleErrorHandler(),
            new GatewayFailureErrorHandler(),
            new LicenseErrorHandler(),
            new AdminSessionValidationErrorHandler(),
            new VersionMismatchErrorHandler(),
            new PersistenceErrorHandler(),
            new RmiErrorHandler(),
            new PermissionDeniedErrorHandler(),
            new ModularAssertionErrorHandler(),
            new DefaultErrorHandler()
        };
    }
}
