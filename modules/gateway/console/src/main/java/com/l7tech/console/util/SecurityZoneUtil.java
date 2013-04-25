package com.l7tech.console.util;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for working with security zones within the SSM.
 */
public class SecurityZoneUtil {
    private static final Logger logger = Logger.getLogger(SecurityZoneUtil.class.getName());

    private static final AtomicReference<Map<Long, SecurityZone>> securityZones = new AtomicReference<>();

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
    @NotNull
    public static Set<SecurityZone> getSecurityZones() {
        final Map<Long, SecurityZone> ret = loadMap();
        return ret != null ? new HashSet<>(ret.values()) : Collections.<SecurityZone>emptySet();
    }

    @Nullable
    public static SecurityZone getSecurityZoneByOid(final long oid) {
        return loadMap().get(oid);
    }

    private static Map<Long, SecurityZone> loadMap() {
        Map<Long, SecurityZone> ret = securityZones.get();
        if (ret == null) {
            try {
                if (Registry.getDefault().isAdminContextPresent()) {
                    final Collection<SecurityZone> zones = Registry.getDefault().getRbacAdmin().findAllSecurityZones();
                    ret = new HashMap<>(zones.size());
                    for (final SecurityZone zone : zones) {
                        ret.put(zone.getOid(), zone);
                    }
                    securityZones.set(ret);
                }
            } catch (final FindException e) {
                logger.log(Level.WARNING, "Unable to load security zones: " + ExceptionUtils.getMessage(e), e);
            }
        }
        return ret;
    }
}
