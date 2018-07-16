package com.l7tech.external.assertions.remotecacheassertion.console;

import com.l7tech.external.assertions.remotecacheassertion.server.CoherenceRemoteCache;
import com.l7tech.gui.util.InputValidator;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: kpak
 * Date: 6/14/12
 * Time: 12:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class CoherenceConfigPanel implements RemoteCacheConfigPanel {

    private InputValidator validator;
    private JPanel mainPanel;
    private JTextField cacheNameTextField;
    private JTextField serversTextField;

    private InputValidator.ValidationRule cacheNameValidationRule;
    private InputValidator.ValidationRule serversValidationRule;

    public CoherenceConfigPanel (Dialog parent, HashMap<String, String> properties, InputValidator validator) {
        this.validator = validator;
        this.initComponents();
        this.setData(properties);
    }

    @Override
    public HashMap<String, String> getData () {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(CoherenceRemoteCache.PROP_CACHE_NAME, cacheNameTextField.getText().trim());
        properties.put(CoherenceRemoteCache.PROP_SERVERS, serversTextField.getText().trim());

        return properties;
    }

    @Override
    public JPanel getMainPanel () {
        return mainPanel;
    }

    @Override
    public void removeValidators () {
        validator.removeRule(cacheNameValidationRule);
        validator.removeRule(serversValidationRule);
    }

    private void initComponents () {
        // Cache name text field.
        //
        cacheNameValidationRule = validator.constrainTextFieldToBeNonEmpty("cache name", cacheNameTextField, null);

        // Servers text field.
        //
        serversValidationRule = new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                String servers = serversTextField.getText().trim();
                if (servers.length() == 0) {
                    return "The server URI(s) field must not be empty.";
                }

                String[] serverList = servers.split(",");
                for (String server : serverList) {
                    String[] addressPort = server.split(":");
                    if (addressPort.length == 2 &&
                        addressPort[0].trim().length() > 0 &&
                        addressPort[1].trim().length() > 0) {
                        try {
                            Integer.parseInt(addressPort[1].trim());
                            // The server URI is valid.
                        } catch (NumberFormatException e) {
                            return "Server URI(s) field is not specified in correct format. The correct format is servername:port,servername2:port2.";
                        }
                    } else {
                        return "Server URI(s) field is not specified in correct format. The correct format is servername:port,servername2:port2.";
                    }
                }

                return null;
            }
        };
        validator.addRule(serversValidationRule);
    }

    private void setData (HashMap<String, String> properties) {
        cacheNameTextField.setText(properties.get(CoherenceRemoteCache.PROP_CACHE_NAME));
        serversTextField.setText(properties.get(CoherenceRemoteCache.PROP_SERVERS));

    }
}