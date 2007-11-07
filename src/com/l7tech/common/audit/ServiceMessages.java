package com.l7tech.common.audit;

import java.util.logging.Level;

/**
 * Message catalog for messages audited by anciliary servlets and services (token service, applet filter, etc).
 * The message range 3500 - 3999 (inclusive) is reserved for these messages.
 */
public class ServiceMessages extends Messages {

    // Admin applet messages
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

    // Backup servlet messages
    public static final M BACKUP_SUCCESS                = m(3600, Level.INFO,    "Backup for node {0} downloaded by {1} to {2}");
    public static final M BACKUP_NOT_SSL                = m(3601, Level.WARNING, "Backup request blocked: request did not arrive over SSL");
    public static final M BACKUP_BAD_CREDENTIALS        = m(3602, Level.WARNING, "Backup request blocked: invalid credentials");
    public static final M BACKUP_NO_CLIENT_CERT         = m(3603, Level.WARNING, "Backup request blocked: no client cert provided");
    public static final M BACKUP_NOT_LICENSED           = m(3604, Level.WARNING, "Backup request blocked: feature not licensed");
    public static final M BACKUP_BAD_CONNECTOR          = m(3605, Level.WARNING, "Backup request blocked: request did not arrive over a connector configured for backup");
    public static final M BACKUP_NO_AUTHENTICATED_USER  = m(3606, Level.WARNING, "Backup request blocked: no authenticated user found in credentials");
    public static final M BACKUP_NO_PERMISSION          = m(3607, Level.WARNING, "Backup request blocked: user does not have Administrator role: {0}");
    public static final M BACKUP_PERMISSION_CHECK_FAILED= m(3608, Level.WARNING, "Backup request permission checked failed");
    public static final M BACKUP_CANT_CREATE_IMAGE      = m(3609, Level.WARNING, "Backup for node {0} failed: cannot create backup image");
    public static final M BACKUP_CANT_READ_IMAGE        = m(3610, Level.WARNING, "Backup for node {0} failed: cannot read backup image");
    public static final M BACKUP_TOO_BIG                = m(3611, Level.WARNING, "Backup for node {0} failed: file size too big: {1} bytes");
    public static final M BACKUP_NO_CLUSTER_INFO        = m(3612, Level.WARNING, "Backup request routing failed: cannot get cluster nodes information");
    public static final M BACKUP_NO_SUCH_NODE           = m(3613, Level.WARNING, "Backup request routing failed: no such node: {0}");
    public static final M BACKUP_ROUTING_IO_ERROR       = m(3614, Level.WARNING, "Backup request routing failed");

    // MAX -                                              m(3999
}
