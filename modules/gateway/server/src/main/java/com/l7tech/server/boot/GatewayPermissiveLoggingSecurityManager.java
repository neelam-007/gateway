package com.l7tech.server.boot;

import com.l7tech.util.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.SocketPermission;
import java.net.URL;
import java.security.*;
import java.util.HashSet;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A security manager that collects a list of all permissions required by various code bases in order
 * to allow the Gateway to run normally.
 */
public class GatewayPermissiveLoggingSecurityManager extends SecurityManager {

    public GatewayPermissiveLoggingSecurityManager() {
    }

    public GatewayPermissiveLoggingSecurityManager(boolean flag) {
    }

    /**
     * Begin periodically logging captured permissions to the specified file.
     *
     * @param logfile the file in which to log the permissions dump.  Required.  Because the FileUtils#saveFileSafely protocol
     *                is used, the directory (not just the file) must be writable by the current process.
     * @param millis number of milliseconds between each log.
     */
    public void periodicLog(@NotNull final File logfile, final long millis) {
        final Thread permsDump = new Thread("SM Permissions Dump") {
            @Override
            public void run() {
                for (;;) {
                    try {
                        Thread.sleep(millis);
                        dumpPerms(getGrantedPermissions(), logfile);
                    } catch (InterruptedException e) {
                        System.err.println("SM Permissions Dump: interrupted");
                        return;
                    } catch (Exception e) {
                        System.err.println("SM Permissions Dump: error: " + e.getMessage());
                        e.printStackTrace(System.err);
                    }
                }
            }
        };
        permsDump.setDaemon(true);
        permsDump.start();
    }

    private static void dumpPerms(final Set<String> grantedPermissions, final File dumpfile) throws IOException {
        FileUtils.saveFileSafely(dumpfile.getCanonicalPath(), new FileUtils.Saver() {
            @Override
            public void doSave(FileOutputStream fos) throws IOException {
                XMLEncoder encoder = null;
                try {
                    encoder = new XMLEncoder(fos);
                    encoder.writeObject(grantedPermissions);
                    encoder.close();
                    encoder = null;
                } finally {
                    if (encoder != null)
                        encoder.close();
                }
            }
        });
    }

    @Override
    public void checkPermission(Permission perm) {
        if (recursing.get()) return;
        logAccessControlCheck(perm, AccessController.getContext());
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
        if (recursing.get()) return;
        logAccessControlCheck(perm, (AccessControlContext)context);
    }

    private void logAccessControlCheck(Permission perm, AccessControlContext context) {
        recursing.set(true);
        try {
            doLogAccessControlCheck(perm, context);
        } finally {
            recursing.set(false);
        }
    }

    private void doLogAccessControlCheck(Permission perm, AccessControlContext context) {
        try {
            ProtectionDomain[] domains = (ProtectionDomain[]) accessControlContextDomainField.get(context);
            if (domains != null) for (ProtectionDomain domain : domains) {
                recordRequiredGrant(perm, domain);
            }
        } catch (IllegalAccessException e) {
            throw new SecurityException("IllegalAccessException reading context field of AccessControlContext class: " + e.getMessage(), e);
        }
    }


    private void recordRequiredGrant(Permission perm, ProtectionDomain domain) {
        if (domain == null) {
            logger.log(Level.WARNING, "Missing ProtectionDomain", new Throwable());
            return;
        }
        CodeSource cs = domain.getCodeSource();
        if (cs == null) {
            //logger.log(Level.WARNING, "Missing CodeSource", new Throwable());
            return;
        }
        URL url = cs.getLocation();
        if (url == null) {
            //logger.log(Level.WARNING, "Missing CodeSource location", new Throwable());
            return;
        }

        final String permissionName;
        if (useWildcards) {
            // Use wildcard permissions where possible, to reduce the size of the needed permissions list
            if (perm instanceof SocketPermission) {
                permissionName = "*";
            } else if (perm instanceof FilePermission) {
                permissionName = "<<ALL FILES>>";
            } else if (perm instanceof PropertyPermission) {
                permissionName = "*";
            } else {
                permissionName = perm.getName();
            }
        } else {
            permissionName = perm.getName();
        }

        String permString = url + " " + perm.getClass().getName() + " " + permissionName;
        permsMap.putIfAbsent(permString, true);
    }

    /**
     * Peek at the currently-granted permissions collected by all instances of this security manager in the current process.
     *
     * @return a set of strings in the form "codeBaseUrl permissionClass permissionName".  Never null.
     *         permissionName may have always been widened to a wildcard permission if useWildcards is enabled.
     */
    public Set<String> getGrantedPermissions() {
        return new HashSet<String>(permsMap.keySet());
    }

    private final ConcurrentMap<String,Object> permsMap = new ConcurrentHashMap<String,Object>();

    private static final Field accessControlContextDomainField;
    static {
        try {
            accessControlContextDomainField = AccessControlContext.class.getDeclaredField("context");
            accessControlContextDomainField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new SecurityException("Unable to read context field of AccessControlContext class -- incompatible JVM implementation?: " + e.getMessage(), e);
        }
    }

    private static final ThreadLocal<Boolean> recursing = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    private static final boolean useWildcards = !Boolean.getBoolean("com.l7tech.server.sm.noWildcards");
    private static final Logger logger = Logger.getLogger(GatewayPermissiveLoggingSecurityManager.class.getName());
}
