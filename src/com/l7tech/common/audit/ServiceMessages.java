package com.l7tech.common.audit;

import java.util.logging.Level;

/**
 * Message catalog for messages audited by anciliary servlets and services (token service, applet filter, etc).
 * The message range 3500 - 3999 (inclusive) is reserved for these messages.
 */
public class ServiceMessages extends Messages {
    public static final M APPLET_AUTH_FAILED            = m(3500, Level.WARNING, "Admin applet authentication policy failed: assertion status: {0}");
    public static final M APPLET_AUTH_POLICY_FAILED     = m(3501, Level.INFO,    "Admin applet authentication policy error: {0}"); // INFO as of bzilla 3829
    public static final M APPLET_AUTH_NO_ROLES          = m(3502, Level.WARNING, "Admin applet authorization failed: user not in any admin role: {0}");
    public static final M APPLET_AUTH_DB_ERROR          = m(3503, Level.WARNING, "Admin applet authorization error: could not check admin roles: {0}");
    public static final M APPLET_AUTH_CLASS_DOWNLOAD    = m(3504, Level.FINE,    "Admin applet request to download custom assertion class: {0}");
    public static final M APPLET_AUTH_NO_SSL            = m(3505, Level.WARNING, "Admin applet request rejected: request did not arrive over SSL");
    public static final M APPLET_AUTH_POLICY_SUCCESS    = m(3506, Level.FINE,    "Admin applet request authorized for user {0}");
    public static final M APPLET_AUTH_COOKIE_SUCCESS    = m(3507, Level.FINE,    "Admin applet request authorized for user {0} (using session cookie)");
    public static final M APPLET_AUTH_CHALLENGE         = m(3508, Level.FINE,    "Admin applet request: replying with authentication challenge");
    public static final M APPLET_AUTH_FILTER_PASSED     = m(3509, Level.FINE,    "Admin applet authentication filter passed");
    public static final M APPLET_SESSION_CREATED        = m(3510, Level.INFO,    "Admin applet session created for user {0}");
    public static final M APPLET_AUTH_MODULE_CLASS_DL   = m(3511, Level.FINE,    "Admin applet request to download assertion module class: {0} from module: {1}");
    public static final M APPLET_AUTH_PORT_NOT_ALLOWED  = m(3512, Level.WARNING, "Admin applet requests not permitted on this port");
    // MAX -                                              m(3999
}
