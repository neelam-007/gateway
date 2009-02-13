package com.l7tech.server.ems.monitoring;

import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.BufferPoolByteArrayOutputStream;
import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.UpdateException;

import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * The implementation of the interface, {@link SystemMonitoringSetupSettingsManager}.
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Jan 28, 2009
 * @since Enterprise Manager 1.0
 */
public class SystemMonitoringSetupSettingsManagerImpl implements SystemMonitoringSetupSettingsManager {
    private static final Logger logger = Logger.getLogger(SystemMonitoringSetupSettingsManagerImpl.class.getName());
    private final ClusterPropertyManager clusterPropertyManager;
    private ServerConfig serverConfig;

    public SystemMonitoringSetupSettingsManagerImpl(ClusterPropertyManager clusterPropertyManager, ServerConfig serverConfig) {
        this.clusterPropertyManager = clusterPropertyManager;
        this.serverConfig = serverConfig;
    }

    @Override
    public long saveSetupSettings(Map<String, Object> setupSettings) throws FindException, SaveException, IOException, UpdateException {
        ClusterProperty setupClusterProperty = clusterPropertyManager.findByUniqueName(ServerConfig.PARAM_SYSTEM_MONITORING_SETUP_SETTINGS);
        long oid;
        if (setupClusterProperty == null) {
            oid = clusterPropertyManager.save(new ClusterProperty(ServerConfig.PARAM_SYSTEM_MONITORING_SETUP_SETTINGS, convertMapToString(setupSettings)));
        } else {
            oid = setupClusterProperty.getOid();
            setupClusterProperty.setValue(convertMapToString(setupSettings));
            clusterPropertyManager.update(setupClusterProperty);
        }

        return oid;
    }

    @Override
    public void deleteSetupSettings() throws FindException, DeleteException {
        ClusterProperty setupSettings = clusterPropertyManager.findByUniqueName(ServerConfig.PARAM_SYSTEM_MONITORING_SETUP_SETTINGS);
        if (setupSettings != null) {
            clusterPropertyManager.delete(setupSettings.getOid());
        }
    }

    @Override
    public Map<String, Object> findSetupSettings() throws FindException, InvalidMonitoringSetupSettingException {
        ClusterProperty setupSettings = clusterPropertyManager.findByUniqueName(ServerConfig.PARAM_SYSTEM_MONITORING_SETUP_SETTINGS);

        if (setupSettings == null) {
            return getInitialSetupSettings();
        } else {
            return convertStringToMap(setupSettings.getValue());
        }
    }

    /**
     * Get the initial setup setting, which are configured in the properties file, emconfig.properties.
     * @return a map with all initial system monitoring setup settings.
     * @throws InvalidMonitoringSetupSettingException if there exist invalid setting values in the properties file.
     */
    private Map<String, Object> getInitialSetupSettings() throws InvalidMonitoringSetupSettingException {
        Map<String, Object> initialSetupSettingsMap = new HashMap<String, Object>();

        try {
            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_TRIGGER_AUDITSIZE,        Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_TRIGGER_AUDITSIZE)));
            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_TRIGGER_LOGSIZE,          Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_TRIGGER_LOGSIZE)));
            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_TRIGGER_DISKUSAGE,        Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_TRIGGER_DISKUSAGE)));
            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_TRIGGER_DISKFREE,         Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_TRIGGER_DISKFREE)) * KB_GB_CONVERTOR);
            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_TRIGGER_CPUTEMPERATURE,   Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_TRIGGER_CPUTEMPERATURE)));
            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_TRIGGER_CPUUSAGE,         Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_TRIGGER_CPUUSAGE)));
            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_TRIGGER_SWAPUSAGE,        Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_TRIGGER_SWAPUSAGE)) * KB_MB_CONVERTOR);

            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_AUDITSIZE,       Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_INTERVAL_AUDITSIZE)));
            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_OPERATINGSTATUS, Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_INTERVAL_OPERATINGSTATUS)));
            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_LOGSIZE,         Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_INTERVAL_LOGSIZE)));
            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_DISKUSAGE,       Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_INTERVAL_DISKUSAGE)));
            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_DISKFREE,        Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_INTERVAL_DISKFREE)));
            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_RAIDSTATUS,      Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_INTERVAL_RAIDSTATUS)));
            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_CPUTEMPERATURE,  Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_INTERVAL_CPUTEMPERATURE)));
            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_CPUUSAGE,        Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_INTERVAL_CPUUSAGE)));
            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_SWAPUSAGE,       Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_INTERVAL_SWAPUSAGE)));
            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_INTERVAL_NTPSTATUS,       Long.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_INTERVAL_NTPSTATUS)));

            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_DISABLEALLNOTIFICATIONS,  Boolean.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_DISABLEALLNOTIFICATIONS)));
            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_AUDITUPONALERTSTATE,      Boolean.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_AUDITUPONALERTSTATE)));
            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_AUDITUPONNORMALSTATE,     Boolean.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_AUDITUPONNORMALSTATE)));
            initialSetupSettingsMap.put(ServerConfig.PARAM_MONITORING_AUDITUPONNOTIFICATION,    Boolean.valueOf(serverConfig.getProperty(ServerConfig.PARAM_MONITORING_INIT_AUDITUPONNOTIFICATION)));
        } catch (NumberFormatException e) {
            throw new InvalidMonitoringSetupSettingException("Invalid system monitoring setup settings in the ESM config properties file.", e);
        }

        return initialSetupSettingsMap;
    }

    /**
     * Deserialize a string to a map.
     * @param serializedSettings: the given string to conert to a map.
     * @return a map with all info extracted from the string.
     */
    private Map<String, Object> convertStringToMap(String serializedSettings) {
        Map<String, Object> settingsMap = new HashMap<String, Object>();

        if (serializedSettings != null && serializedSettings.length() >= 2) {
            ByteArrayInputStream in = new ByteArrayInputStream(HexUtils.encodeUtf8(serializedSettings));
            java.beans.XMLDecoder decoder = new java.beans.XMLDecoder(in);
            settingsMap = (Map<String, Object>) decoder.readObject();
        }

        return settingsMap;
    }

    /**
     * Serialize a map to a string
     * @param settingsMap: the given map
     * @return a string with all info from the map.
     * @throws IOException
     */
    private String convertMapToString(Map<String, Object> settingsMap) throws IOException {
        String settingsPropsXml;

        // if no settings, return empty string
        if (settingsMap.isEmpty()) {
            settingsPropsXml = "";
        } else {
            BufferPoolByteArrayOutputStream output = null;
            java.beans.XMLEncoder encoder = null;
            try {
                output = new BufferPoolByteArrayOutputStream();
                encoder = new java.beans.XMLEncoder(new NonCloseableOutputStream(output));
                encoder.writeObject(settingsMap);
                encoder.close(); // writes closing XML tag
                encoder = null;
                settingsPropsXml = output.toString("UTF-8");
            }
            finally {
                if(encoder!=null) encoder.close();
                ResourceUtils.closeQuietly(output);
            }
        }

        return settingsPropsXml;
    }
}
