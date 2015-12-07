package com.l7tech.external.assertions.remotecacheassertion.console;

import com.l7tech.external.assertions.remotecacheassertion.server.MemcachedRemoteCache;
import com.l7tech.gui.util.InputValidator;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 16/11/11
 * Time: 10:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class MemcachedConfigPanel implements RemoteCacheConfigPanel {
    private JCheckBox specifyBucketCheckBox;
    private JTextField bucketNameField;
    private JPasswordField passwordField;
    private JCheckBox showPasswordCheckBox;
    private JPanel mainPanel;
    private JTextField serverUriTextField;
    private JPanel bucketPanel;

    private Dialog parent;

    private InputValidator validator;
    private InputValidator.ValidationRule validationRule;

    public MemcachedConfigPanel(Dialog parent, HashMap<String, String> properties, InputValidator validator) {
        this.parent = parent;
        this.validator = validator;

        initComponents();
        setData(properties);
    }

    private void initComponents() {
        specifyBucketCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableComponents();
            }
        });

        showPasswordCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passwordField.setEchoChar(showPasswordCheckBox.isSelected() ? 0 : '*');
            }
        });
        showPasswordCheckBox.setSelected(false);

        validationRule = new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (specifyBucketCheckBox.isSelected()) {
                    if (bucketNameField.getText().trim().isEmpty()) {
                        return "Bucket name cannot be empty.";
                    }
                    validateServerUriInput();
                } else {
                    validateServerUriInput();
                }

                return null;
            }
        };

        validator.addRule(validationRule);
    }

    private String validateServerUriInput() {
        if (serverUriTextField.getText().trim().isEmpty()) {
            return "Server URI can't be empty.";
        }

        String[] serverList = serverUriTextField.getText().split(",");
        for (String server : serverList) {
            if (server.split(":").length != 2) {
                return "Invalid format. Correct format is servername:port,servername2:port2.";
            }
        }
        return null;
    }

    private void enableDisableComponents() {
        if (specifyBucketCheckBox.isSelected()) {
            bucketPanel.setVisible(true);
        } else {
            bucketPanel.setVisible(false);
        }
        parent.pack();
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void setData(HashMap<String, String> properties) {
        specifyBucketCheckBox.setSelected(Boolean.parseBoolean(properties.get(MemcachedRemoteCache.PROP_BUCKET_SPECIFIED)));

        if (Boolean.parseBoolean(properties.get(MemcachedRemoteCache.PROP_BUCKET_SPECIFIED))) {
            bucketNameField.setText(properties.get(MemcachedRemoteCache.PROP_BUCKETNAME) == null ? "" : properties.get(MemcachedRemoteCache.PROP_BUCKETNAME));
            passwordField.setText(properties.get(MemcachedRemoteCache.PROP_PASSWORD) == null ? "" : properties.get(MemcachedRemoteCache.PROP_PASSWORD));

            if (StringUtils.isNotBlank(properties.get(MemcachedRemoteCache.PROP_SERVERPORTS))) {
                serverUriTextField.setText(properties.get(MemcachedRemoteCache.PROP_SERVERPORTS));
            }
        } else {
            if (StringUtils.isNotBlank(properties.get(MemcachedRemoteCache.PROP_SERVERPORTS))) {
                serverUriTextField.setText(properties.get(MemcachedRemoteCache.PROP_SERVERPORTS));
            }
        }

        enableDisableComponents();
    }

    public HashMap<String, String> getData() {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(MemcachedRemoteCache.PROP_BUCKET_SPECIFIED, Boolean.toString(specifyBucketCheckBox.isSelected()));

        if (specifyBucketCheckBox.isSelected()) {
            properties.put(MemcachedRemoteCache.PROP_BUCKETNAME, bucketNameField.getText());
            properties.put(MemcachedRemoteCache.PROP_PASSWORD, new String(passwordField.getPassword()));
            properties.put(MemcachedRemoteCache.PROP_SERVERPORTS, serverUriTextField.getText());
        } else {
            properties.put(MemcachedRemoteCache.PROP_SERVERPORTS, serverUriTextField.getText());
        }

        return properties;
    }

    public void removeValidators() {
        validator.removeRule(validationRule);
    }
}
