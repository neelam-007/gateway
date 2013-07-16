package com.l7tech.server.ems.monitoring;

import com.l7tech.objectmodel.*;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Map;

/**
 * This manager managing the system monitoring setup settings.
 * Note: when saving setup settings, the manager stores all settings in a map, then serializes the map into a string and
 * stores it as a cluster property into the DB table, cluster_properties.  The name of the cluster property stored in the
 * DB is "system.monitoring.setup.settings". 
 * 
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Jan 28, 2009
 * @since Enterprise Manager 1.0
 */
@Transactional
public interface SystemMonitoringSetupSettingsManager {
    // This constant used when converting the value of swap usage in KB(MB) to MB(KB),
    public final static long KB_MB_CONVERTOR = 1024;

    // This constant used when converting the value of disk free in KB(GB) to GB(KB),
    public final static long KB_GB_CONVERTOR = KB_MB_CONVERTOR * 1024;

    /**
     * Save or update system monitoring setup settings as a cluster property into the DB table cluster_properties.
     *
     * @param setupSettings: the new setup settings to save or update
     * @return the object id if save/update is successful.
     * @throws FindException if an error occurs when retrieving system monitoring setup settings in the DB.
     * @throws SaveException if an error occurs when saving the new settings in the DB.
     * @throws IOException if an error occurs when converting a map to a serialized string.
     * @throws UpdateException if an error occurs when updating the new settings in the DB.
     */
    Goid saveSetupSettings(Map<String, Object> setupSettings) throws FindException, SaveException, IOException, UpdateException;

    /**
     * Delete system monitoring setup settings as a cluster propperty from the DB table cluster_properties.
     *
     * @throws FindException if an error occurs when retrieving system monitoring setup settings in the DB.
     * @throws DeleteException if an error occurs when deleting the cluster property from the table cluster_properties.
     */
    void deleteSetupSettings() throws FindException, DeleteException;

    /**
     * Get system monitoring setup settings as a cluster property from the DB table cluster_properties.
     *
     * @return a map with all system monitoring setup settings.
     * @throws FindException if an error occurs when retrieving system monitoring setup settings in the DB.
     * @throws InvalidMonitoringSetupSettingException when a setting is assigned as an invalid value.
     */
    Map<String, Object> findSetupSettings() throws FindException, InvalidMonitoringSetupSettingException;
}
