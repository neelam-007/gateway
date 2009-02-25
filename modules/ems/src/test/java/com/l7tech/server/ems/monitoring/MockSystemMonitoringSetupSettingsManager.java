package com.l7tech.server.ems.monitoring;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.Pair;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class MockSystemMonitoringSetupSettingsManager implements SystemMonitoringSetupSettingsManager {
    Map<String, Object> settings = new HashMap<String, Object>();

    public MockSystemMonitoringSetupSettingsManager() {
    }

    public MockSystemMonitoringSetupSettingsManager(Pair<String, Object>... settings) {
        for (Pair<String, Object> setting : settings)
            this.settings.put(setting.left, setting.right);
    }

    public MockSystemMonitoringSetupSettingsManager(Map<String, Object> settings) {
        if (settings == null) throw new NullPointerException();
        this.settings = settings;
    }

    public long saveSetupSettings(Map<String, Object> setupSettings) throws FindException, SaveException, IOException, UpdateException {
        if (setupSettings == null) throw new NullPointerException();
        settings = setupSettings;
        return 1;
    }

    public void deleteSetupSettings() throws FindException, DeleteException {
        settings.clear();
    }

    public Map<String, Object> findSetupSettings() throws FindException, InvalidMonitoringSetupSettingException {
        return settings;
    }
}
