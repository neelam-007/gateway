package com.l7tech.server.boot;

/**
 * The SecureSpan Gateway server SecurityManager.
 * <p/>
 * Currently this behaves exactly the same as the default SecurityManager, delegating all access control
 * decisions to the AccessController, which decides based on the security policy provided on the command line.
 */
public class GatewaySecurityManager extends SecurityManager {
}
