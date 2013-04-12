package com.l7tech.console.util;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.util.ExceptionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for working with security zones within the SSM.
 */
public class SecurityZoneUtil {
    private static final Logger logger = Logger.getLogger(SecurityZoneUtil.class.getName());

    private static final AtomicReference<Set<SecurityZone>> securityZones = new AtomicReference<>();

    /**
     * Check if any security zones exist and are visible to the current admin user.
     *
     * @return true iff. we can see at least one security zone.
     */
    public static boolean isAnySecurityZonesPresent() {
        return !getSecurityZones().isEmpty();
    }

    public static void flushCachedSecurityZones() {
        securityZones.set(null);
    }

    /**
     * @return all security zones visible to the current admin.  Never null.
     */
    public static Set<SecurityZone> getSecurityZones() {
        Set<SecurityZone> ret = securityZones.get();

        if (ret == null) {
            try {
                if (Registry.getDefault().isAdminContextPresent()) {
                    Collection<SecurityZone> zones = Registry.getDefault().getRbacAdmin().findAllSecurityZones();
                    ret = new HashSet<>(zones);
                    securityZones.set(ret);
                }
            } catch (FindException e) {
                logger.log(Level.WARNING, "Unable to load security zones: " + ExceptionUtils.getMessage(e), e);
            }
        }

        return ret != null ? ret : Collections.<SecurityZone>emptySet();
    }
}
