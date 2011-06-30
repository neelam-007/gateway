package com.l7tech.external.assertions.ssh.console;

import com.l7tech.console.panels.CustomTransportPropertiesPanel;
import com.l7tech.external.assertions.ssh.SshRouteAssertion;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class SshTransportPropertiesPanel extends CustomTransportPropertiesPanel {
    private JPanel mainPanel;
    private JCheckBox scpCheckBox;
    private JCheckBox sftpCheckBox;

    public SshTransportPropertiesPanel() {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    private boolean getBooleanProp(Map<String, String> map, String key, boolean dflt) {
        String val = map.get(key);
        if (val == null)
            return dflt;
        return Boolean.parseBoolean(val);
    }

    @Override
    public void setData(Map<String, String> props) {
        scpCheckBox.setSelected(getBooleanProp(props, SshRouteAssertion.LISTEN_PROP_ENABLE_SCP, true));
        sftpCheckBox.setSelected(getBooleanProp(props, SshRouteAssertion.LISTEN_PROP_ENABLE_SFTP, true));
    }

    @Override
    public Map<String, String> getData() {
        Map<String, String> data = new HashMap<String, String>();
        data.put(SshRouteAssertion.LISTEN_PROP_ENABLE_SCP, String.valueOf(scpCheckBox.isSelected()));
        data.put(SshRouteAssertion.LISTEN_PROP_ENABLE_SFTP, String.valueOf(sftpCheckBox.isSelected()));
        return data;
    }

    @Override
    public String getValidationError() {
        return null;
    }

    @Override
    public String[] getAdvancedPropertyNamesUsedByGui() {
        return new String[] {
            SshRouteAssertion.LISTEN_PROP_ENABLE_SCP,
            SshRouteAssertion.LISTEN_PROP_ENABLE_SFTP,
        };
    }
}
