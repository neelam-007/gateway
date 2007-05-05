package com.l7tech.server.security.keystore;

import java.util.List;
import java.util.Map;

/**
 * Interface implemented by central bean that manages keystores on the Gateway node.
 */
public interface SsgKeyStoreManager {
    /**
     * Finds all SsgKeyFinder instances available on this Gateway node.
     * <p/>
     * Some of these SsgKeyFinder instances may be mutable.
     *
     * @return a List of SsgKeyFinder instances.  Never null.  Guaranteed to contain at least one SsgKeyFinder.
     */
    List<SsgKeyFinder> findAll();
}
