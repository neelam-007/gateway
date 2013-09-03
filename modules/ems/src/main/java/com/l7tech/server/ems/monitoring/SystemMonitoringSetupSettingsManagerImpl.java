package com.l7tech.server.ems.monitoring;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.*;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.ems.EsmConfigParams;
import com.l7tech.util.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The implementation of the interface, {@link SystemMonitoringSetupSettingsManager}.
 */
public class SystemMonitoringSetupSettingsManagerImpl implements SystemMonitoringSetupSettingsManager {
    private final ClusterPropertyManager clusterPropertyManager;
    private Config config;

    public SystemMonitoringSetupSettingsManagerImpl(ClusterPropertyManager clusterPropertyManager, Config config ) {
        this.clusterPropertyManager = clusterPropertyManager;
        this.config = config;
    }

    @Override
    public Goid saveSetupSettings(Map<String, Object> setupSettings) throws FindException, SaveException, IOException, UpdateException {
        ClusterProperty setupClusterProperty = clusterPropertyManager.findByUniqueName(EsmConfigParams.PARAM_SYSTEM_MONITORING_SETUP_SETTINGS);
        Goid goid;
        if (setupClusterProperty == null) {
            goid = clusterPropertyManager.save(new ClusterProperty(EsmConfigParams.PARAM_SYSTEM_MONITORING_SETUP_SETTINGS, convertMapToString(setupSettings)));
        } else {
            goid = setupClusterProperty.getGoid();
            setupClusterProperty.setValue(convertMapToString(setupSettings));
            clusterPropertyManager.update(setupClusterProperty);
        }

        return goid;
    }

    @Override
    public void deleteSetupSettings() throws FindException, DeleteException {
        ClusterProperty setupSettings = clusterPropertyManager.findByUniqueName(EsmConfigParams.PARAM_SYSTEM_MONITORING_SETUP_SETTINGS);
        if (setupSettings != null) {
            clusterPropertyManager.delete(setupSettings.getGoid());
        }
    }

    @Override
    public Map<String, Object> findSetupSettings() throws FindException, InvalidMonitoringSetupSettingException {
        ClusterProperty setupSettings = clusterPropertyManager.findByUniqueName(EsmConfigParams.PARAM_SYSTEM_MONITORING_SETUP_SETTINGS);

        Map<String, Object> defaults = getInitialSetupSettings();
        if (setupSettings == null) {
            return defaults;
        } else {
            return convertStringToMap(setupSettings.getValue(), defaults);
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
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_TRIGGER_AUDITSIZE,        Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_TRIGGER_AUDITSIZE ) ));
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_TRIGGER_DATABASEREPLICATIONDELAY, Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_TRIGGER_DATABASEREPLICATIONDELAY ) ));
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_TRIGGER_LOGSIZE,          Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_TRIGGER_LOGSIZE ) ) * KB_MB_CONVERTOR);
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_TRIGGER_DISKUSAGE,        Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_TRIGGER_DISKUSAGE ) ));
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_TRIGGER_DISKFREE,         Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_TRIGGER_DISKFREE ) ) * KB_GB_CONVERTOR);
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_TRIGGER_CPUTEMPERATURE,   Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_TRIGGER_CPUTEMPERATURE ) ));
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_TRIGGER_CPUUSAGE,         Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_TRIGGER_CPUUSAGE ) ));
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_TRIGGER_SWAPUSAGE,        Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_TRIGGER_SWAPUSAGE ) ) * KB_MB_CONVERTOR);

            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_AUDITSIZE,       Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_INTERVAL_AUDITSIZE ) ));
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_DATABASEREPLICATIONDELAY, Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_INTERVAL_DATABASEREPLICATIONDELAY ) ));
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_OPERATINGSTATUS, Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_INTERVAL_OPERATINGSTATUS ) ));
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_LOGSIZE,         Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_INTERVAL_LOGSIZE ) ));
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_DISKUSAGE,       Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_INTERVAL_DISKUSAGE ) ));
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_DISKFREE,        Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_INTERVAL_DISKFREE ) ));
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_RAIDSTATUS,      Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_INTERVAL_RAIDSTATUS ) ));
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_CPUTEMPERATURE,  Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_INTERVAL_CPUTEMPERATURE ) ));
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_CPUUSAGE,        Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_INTERVAL_CPUUSAGE ) ));
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_SWAPUSAGE,       Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_INTERVAL_SWAPUSAGE ) ));
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_INTERVAL_NTPSTATUS,       Long.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_INTERVAL_NTPSTATUS ) ));

            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_DISABLEALLNOTIFICATIONS,  Boolean.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_DISABLEALLNOTIFICATIONS ) ));
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_AUDITUPONALERTSTATE,      Boolean.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_AUDITUPONALERTSTATE ) ));
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_AUDITUPONNORMALSTATE,     Boolean.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_AUDITUPONNORMALSTATE ) ));
            initialSetupSettingsMap.put(EsmConfigParams.PARAM_MONITORING_AUDITUPONNOTIFICATION,    Boolean.valueOf( config.getProperty( EsmConfigParams.PARAM_MONITORING_INIT_AUDITUPONNOTIFICATION ) ));
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
    private Map<String, Object> convertStringToMap(final String serializedSettings,
                                                   final Map<String, Object> defaults ) {
        Map<String, Object> settingsMap = new HashMap<String, Object>();

        if (serializedSettings != null && serializedSettings.length() >= 2) {
            ByteArrayInputStream in = new ByteArrayInputStream(HexUtils.encodeUtf8(serializedSettings));
            SafeXMLDecoder decoder = new SafeXMLDecoderBuilder(in).build();
            settingsMap = (Map<String, Object>) decoder.readObject();
        }

        for ( final String key : defaults.keySet() ) {
            if ( !settingsMap.containsKey( key ) ) {
                settingsMap.put( key, defaults.get( key ) );
            }
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
            PoolByteArrayOutputStream output = null;
            java.beans.XMLEncoder encoder = null;
            try {
                output = new PoolByteArrayOutputStream();
                encoder = new java.beans.XMLEncoder(new NonCloseableOutputStream(output));
                encoder.writeObject(settingsMap);
                encoder.close(); // writes closing XML tag
                encoder = null;
                settingsPropsXml = output.toString(Charsets.UTF8);
            }
            finally {
                if(encoder!=null) encoder.close();
                ResourceUtils.closeQuietly(output);
            }
        }

        return settingsPropsXml;
    }
}
