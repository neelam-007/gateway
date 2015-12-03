package com.l7tech.external.assertions.remotecacheassertion.console;

import com.l7tech.external.assertions.remotecacheassertion.server.TerracottaRemoteCache;
import com.l7tech.gui.util.InputValidator;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 16/11/11
 * Time: 1:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class TerracottaConfigPanel implements RemoteCacheConfigPanel {
    private JPanel mainPanel;
    private JTextField cacheNameField;
    private JTextField serverUriTextField;

    private Dialog parent;
    private InputValidator validator;

    private InputValidator.ValidationRule cacheNameValidationRule;
    private InputValidator.ValidationRule serverUrisValidationRule;

    public TerracottaConfigPanel(Dialog parent, HashMap<String, String> properties, InputValidator validator) {
        this.parent = parent;
        this.validator = validator;

        initComponents();
        setData(properties);
    }

    private void initComponents() {
        cacheNameValidationRule = validator.constrainTextFieldToBeNonEmpty("cache name", cacheNameField, null);

        serverUrisValidationRule = new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if(! (serverUriTextField.getText().split(":").length >= 2)) {
                    return "Server URI not specified in correct format. Correct format is servername:port,servername2:port2.";
                }
                return null;
            }
        };
        validator.addRule(serverUrisValidationRule);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void setData(HashMap<String, String> properties) {

        cacheNameField.setText(properties.get(TerracottaRemoteCache.PROPERTY_CACHE_NAME) == null ? "" : properties.get(TerracottaRemoteCache.PROPERTY_CACHE_NAME));

        if(properties.get(TerracottaRemoteCache.PROPERTY_URLS) != null) {
            serverUriTextField.setText(properties.get(TerracottaRemoteCache.PROPERTY_URLS));
        }
    }

    public HashMap<String, String> getData() {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(TerracottaRemoteCache.PROPERTY_CACHE_NAME, cacheNameField.getText());
        properties.put(TerracottaRemoteCache.PROPERTY_URLS, serverUriTextField.getText());
        return properties;
    }

    public void removeValidators() {
        validator.removeRule(cacheNameValidationRule);
        validator.removeRule(serverUrisValidationRule);
    }
}
