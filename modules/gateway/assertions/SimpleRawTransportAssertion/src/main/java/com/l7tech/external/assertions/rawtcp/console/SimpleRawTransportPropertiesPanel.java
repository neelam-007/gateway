package com.l7tech.external.assertions.rawtcp.console;

import com.l7tech.console.panels.CustomTransportPropertiesPanel;
import com.l7tech.gui.NumberField;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static com.l7tech.external.assertions.rawtcp.SimpleRawTransportAssertion.*;

/**
 *
 */
public class SimpleRawTransportPropertiesPanel extends CustomTransportPropertiesPanel {
    private JPanel mainPanel;
    private JTextField requestSizeField;
    private JTextField socketTimeoutField;

    public SimpleRawTransportPropertiesPanel() {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        requestSizeField.setDocument(new NumberField());
        socketTimeoutField.setDocument(new NumberField());
    }

    private long getLongProp(Map<String, String> map, String key, long dflt) {
        String val = map.get(key);
        if (val == null)
            return dflt;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException nfe) {
            return dflt;
        }
    }

    @Override
    public void setData(Map<String, String> props) {
        requestSizeField.setText(Long.toString(getLongProp(props, LISTEN_PROP_REQUEST_SIZE_LIMIT, DEFAULT_REQUEST_SIZE_LIMIT)));
        socketTimeoutField.setText(Long.toString(getLongProp(props, LISTEN_PROP_READ_TIMEOUT, DEFAULT_READ_TIMEOUT)));
    }

    @Override
    public Map<String, String> getData() {
        Map<String, String> data = new HashMap<String, String>();

        data.put(LISTEN_PROP_REQUEST_SIZE_LIMIT, requestSizeField.getText());
        data.put(LISTEN_PROP_READ_TIMEOUT, socketTimeoutField.getText());

        return data;
    }

    @Override
    public String getValidationError() {
        return null;
    }

    @Override
    public String[] getAdvancedPropertyNamesUsedByGui() {
        return new String[] {
            LISTEN_PROP_READ_TIMEOUT,
            LISTEN_PROP_REQUEST_SIZE_LIMIT,
        };
    }
}
