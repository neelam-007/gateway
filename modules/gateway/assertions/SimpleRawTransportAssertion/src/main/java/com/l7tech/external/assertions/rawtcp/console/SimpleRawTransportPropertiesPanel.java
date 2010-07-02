package com.l7tech.external.assertions.rawtcp.console;

import com.l7tech.console.panels.CustomTransportPropertiesPanel;
import com.l7tech.gateway.common.transport.SsgConnector;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class SimpleRawTransportPropertiesPanel extends CustomTransportPropertiesPanel {
    private JCheckBox usePrivateThreadPoolCheckBox;
    private JSpinner threadPoolSizeSpinner;
    private JPanel mainPanel;
    private JTextField requestSizeField;
    private JTextField socketTimeoutField;
    private JTextField requestReadTimeoutField;

    public SimpleRawTransportPropertiesPanel() {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    @Override
    public void setData(Map<String, String> advancedProperties) {
        String size = advancedProperties.get(SsgConnector.PROP_THREAD_POOL_SIZE);
        if (size != null) {
            usePrivateThreadPoolCheckBox.setSelected(true);
            threadPoolSizeSpinner.setValue(Integer.parseInt(size));
        } else {
            usePrivateThreadPoolCheckBox.setSelected(false);
        }
    }

    @Override
    public Map<String, String> getData() {
        Map<String, String> data = new HashMap<String, String>();

        if (usePrivateThreadPoolCheckBox.isSelected()) {
            data.put(SsgConnector.PROP_THREAD_POOL_SIZE, String.valueOf(threadPoolSizeSpinner.getValue()));
        } else {
            data.put(SsgConnector.PROP_THREAD_POOL_SIZE, null);
        }

        return data;
    }

    @Override
    public String[] getAdvancedPropertyNamesUsedByGui() {
        return new String[] {
                SsgConnector.PROP_THREAD_POOL_SIZE,
        };
    }
}
