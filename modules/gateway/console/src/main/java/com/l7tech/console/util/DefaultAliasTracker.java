package com.l7tech.console.util;

import com.l7tech.gateway.common.security.SpecialKeyType;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.ExceptionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client-side utility class for keeping track of the currently-designated special-purpose keys.
 */
public class DefaultAliasTracker {
    private static final Logger logger = Logger.getLogger(DefaultAliasTracker.class.getName());

    private static final class SpecialKeyInfo {
        final SpecialKeyType type;
        final SsgKeyEntry entry;
        final boolean mutable;

        private SpecialKeyInfo(SpecialKeyType type, SsgKeyEntry entry, boolean mutable) {
            this.type = type;
            this.entry = entry;
            this.mutable = mutable;
        }
    }

    private static final class AllSpecialKeys {
        final List<SpecialKeyInfo> infos;
        final Map<SpecialKeyType, SpecialKeyInfo> infoByType;

        private AllSpecialKeys(List<SpecialKeyInfo> infos, Map<SpecialKeyType, SpecialKeyInfo> infoByType) {
            this.infos = infos;
            this.infoByType = infoByType;
        }

        static AllSpecialKeys loadUpdatedInfo() {
            if (!Registry.getDefault().isAdminContextPresent()) {
                logger.log(Level.INFO, "Unable to refresh special key types -- not connected to Gateway");
                return new AllSpecialKeys(Collections.<SpecialKeyInfo>emptyList(), Collections.<SpecialKeyType, SpecialKeyInfo>emptyMap());
            }

            final TrustedCertAdmin trustedCertAdmin = Registry.getDefault().getTrustedCertManager();

            List<SpecialKeyInfo> infos = new ArrayList<SpecialKeyInfo>();
            SpecialKeyType[] types = SpecialKeyType.values();
            for (SpecialKeyType type : types) {
                try {
                    SsgKeyEntry entry = trustedCertAdmin.findDefaultKey(type);
                    boolean mutable = trustedCertAdmin.isDefaultKeyMutable(type);
                    SpecialKeyInfo info = new SpecialKeyInfo(type, entry, mutable);
                    infos.add(info);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Unable to determine current special key type '" + type + "' using findDefaultKey: " + ExceptionUtils.getMessage(e), e);
                }
            }

            final Map<SpecialKeyType, SpecialKeyInfo> infoByType = new HashMap<SpecialKeyType, SpecialKeyInfo>();
            for (SpecialKeyInfo info : infos) {
                infoByType.put(info.type,  info);
            }
            return new AllSpecialKeys(infos, infoByType);
        }
    }

    private final AtomicReference<AllSpecialKeys> info = new AtomicReference<AllSpecialKeys>();

    public SsgKeyEntry getSpecialKey(SpecialKeyType type) {
        SpecialKeyInfo info = info().infoByType.get(type);
        return info == null ? null : info.entry;
    }

    public EnumSet<SpecialKeyType> getSpecialKeyTypes(SsgKeyEntry entry) {
        if (entry == null) throw new NullPointerException("A key entry must be provided to retrieve its special key types");
        if (entry.getAlias() == null) throw new IllegalArgumentException("Key entry must have an alias assigned in order retrieve its special key types");
        if (Goid.isDefault(entry.getKeystoreId())) throw new IllegalArgumentException("Key entry must have specific keystore ID assigned in order to retrieve its special key types");

        EnumSet<SpecialKeyType> ret = EnumSet.noneOf(SpecialKeyType.class);
        for (SpecialKeyType type : SpecialKeyType.values()) {
            if (isSpecialKey(entry, type))
                ret.add(type);
        }
        return ret;
    }

    public boolean isSpecialKey(SsgKeyEntry entry, SpecialKeyType type) {
        if (type == null) throw new NullPointerException("A special key type must be provided");
        if (entry == null) throw new NullPointerException("A key entry must be provided to check whether it is a special key type");
        if (entry.getAlias() == null) throw new IllegalArgumentException("Key entry must have an alias assigned in order to check whether it is a special key type");
        if (Goid.isDefault(entry.getKeystoreId())) throw new IllegalArgumentException("Key entry must have specific keystore ID assigned in order to check whether it is a special key type");

        SpecialKeyInfo info = info().infoByType.get(type);
        return info != null && info.entry != null &&
                entry.getAlias().equalsIgnoreCase(info.entry.getAlias()) &&
                entry.getKeystoreId() == info.entry.getKeystoreId();
    }

    public boolean isSpecialKey(Goid keystoreId, String alias, SpecialKeyType type) {
        if (type == null) throw new NullPointerException("A special key type must be provided");
        if (alias == null) throw new IllegalArgumentException("An alias must be provided");

        SpecialKeyInfo info = info().infoByType.get(type);
        return info != null && info.entry != null &&
                (Goid.isDefault(keystoreId) || Goid.equals(keystoreId, info.entry.getKeystoreId())) &&
                alias.equalsIgnoreCase(info.entry.getAlias());
    }

    /**
     * @param type a special key type.  required
     * @return true if the designation for this key type can be changed on this Gateway.
     */
    public boolean isSpecialKeyMutable(SpecialKeyType type) {
        if (type == null) throw new NullPointerException("A special key type must be provided");

        SpecialKeyInfo info = info().infoByType.get(type);
        return info != null && info.mutable;
    }

    private AllSpecialKeys info() {
        AllSpecialKeys ret = info.get();
        if (ret == null) {
            synchronized (this) {
                ret = info.get();
                if (ret == null) {
                    ret = AllSpecialKeys.loadUpdatedInfo();
                    info.set(ret);
                }
            }
        }
        return ret;
    }

    public void assignSpecialKeyRole(SsgKeyEntry keyEntry, SpecialKeyType type) throws UpdateException {
        Registry.getDefault().getTrustedCertManager().setDefaultKey(type, keyEntry.getKeystoreId(), keyEntry.getAlias());
        invalidate();
    }

    /**
     * Forget any cached information about the default SSL and CA certs.
     */
    public void invalidate() {
        info.set(null);
    }
}
