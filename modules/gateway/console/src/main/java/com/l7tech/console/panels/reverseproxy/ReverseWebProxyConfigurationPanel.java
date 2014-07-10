package com.l7tech.console.panels.reverseproxy;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.RunOnChangeListener;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Configuration panel for publishing a reverse web proxy policy.
 */
public class ReverseWebProxyConfigurationPanel extends WizardStepPanel {
    private JPanel contentPanel;
    private JTextField nameTextField;
    private JTextField webAppHostTextField;
    private JTextField routingUriTextField;
    private JLabel gatewayUrlLabel;
    private JCheckBox locationCheckBox;
    private JCheckBox cookiesCheckBox;
    private JRadioButton allRespContentRadio;
    private JRadioButton specifiedTagsRadio;
    private JTextField specifiedTagsTextField;
    private JComboBox platformComboBox;
    private JCheckBox httpsCheckBox;
    private JCheckBox hostCheckBox;
    private JCheckBox requestPortCheckBox;
    private JCheckBox queryCheckBox;
    private JCheckBox requestBodyCheckBox;
    private JCheckBox responseBodyCheckBox;

    public ReverseWebProxyConfigurationPanel() {
        super(null);
        setLayout(new BorderLayout());
        final KeyListener keyListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                notifyListeners();
            }
        };
        nameTextField.addKeyListener(keyListener);
        webAppHostTextField.addKeyListener(keyListener);
        routingUriTextField.addKeyListener(keyListener);
        specifiedTagsTextField.addKeyListener(keyListener);
        final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableDisable();
                notifyListeners();
            }
        });
        responseBodyCheckBox.addActionListener(changeListener);
        allRespContentRadio.addActionListener(changeListener);
        specifiedTagsRadio.addActionListener(changeListener);
        platformComboBox.setModel(new DefaultComboBoxModel(new String[]{ReverseWebProxyConfig.WebApplicationType.SHAREPOINT.getName(), ReverseWebProxyConfig.WebApplicationType.GENERIC.getName()}));
        add(contentPanel);
    }

    @Override
    public String getStepLabel() {
        return "Configure Reverse Web Proxy";
    }

    @Override
    public boolean canFinish() {
        return !nameTextField.getText().isEmpty() &&
                !webAppHostTextField.getText().isEmpty() &&
                !routingUriTextField.getText().isEmpty() &&
                (!responseBodyCheckBox.isSelected() || !specifiedTagsRadio.isSelected() || StringUtils.isNotBlank(specifiedTagsTextField.getText()));
    }

    @Override
    public boolean canAdvance() {
        return canFinish();
    }

    @Override
    public void readSettings(final Object settings) throws IllegalArgumentException {
        if (settings instanceof ReverseWebProxyConfig) {
            final ReverseWebProxyConfig config = (ReverseWebProxyConfig) settings;
            nameTextField.setText(config.getName());
            platformComboBox.setSelectedItem(config.getWebAppType().getName());
            webAppHostTextField.setText(config.getWebAppHost());
            routingUriTextField.setText(config.getRoutingUri());
            gatewayUrlLabel.setText("http(s)://" + TopComponents.getInstance().ssgURL().getHost() + ":[port]/");
            httpsCheckBox.setSelected(config.isUseHttps());
            queryCheckBox.setSelected(config.isRewriteQuery());
            locationCheckBox.setSelected(config.isRewriteLocationHeader());
            hostCheckBox.setSelected(config.isRewriteHostHeader());
            cookiesCheckBox.setSelected(config.isRewriteCookies());
            requestBodyCheckBox.setSelected(config.isRewriteRequestContent());
            responseBodyCheckBox.setSelected(config.isRewriteResponseContent());
            allRespContentRadio.setSelected(config.isRewriteResponseContent() && StringUtils.isBlank(config.getHtmlTagsToRewrite()));
            specifiedTagsRadio.setSelected(config.isRewriteResponseContent() && StringUtils.isNotBlank(config.getHtmlTagsToRewrite()));
            requestPortCheckBox.setSelected(config.isIncludeRequestPort());
            enableDisable();
        }
    }

    @Override
    public void storeSettings(final Object settings) throws IllegalArgumentException {
        if (settings instanceof ReverseWebProxyConfig) {
            final ReverseWebProxyConfig config = (ReverseWebProxyConfig) settings;
            config.setName(nameTextField.getText().trim());
            config.setWebAppType(ReverseWebProxyConfig.WebApplicationType.valueOf(platformComboBox.getSelectedItem().toString().toUpperCase()));
            config.setRoutingUri(routingUriTextField.getText().trim());
            config.setWebAppHost(webAppHostTextField.getText().trim());
            config.setUseHttps(httpsCheckBox.isSelected());
            config.setRewriteQuery(queryCheckBox.isSelected());
            config.setRewriteLocationHeader(locationCheckBox.isSelected());
            config.setRewriteHostHeader(hostCheckBox.isSelected());
            config.setRewriteCookies(cookiesCheckBox.isSelected());
            config.setRewriteRequestContent(requestBodyCheckBox.isSelected());
            config.setRewriteResponseContent(responseBodyCheckBox.isSelected());
            config.setHtmlTagsToRewrite(responseBodyCheckBox.isSelected() && specifiedTagsRadio.isSelected() ? specifiedTagsTextField.getText().trim() : null);
            config.setIncludeRequestPort(requestPortCheckBox.isSelected());
        }
    }

    private void enableDisable() {
        final boolean rewriteResponseBody = responseBodyCheckBox.isSelected();
        allRespContentRadio.setEnabled(rewriteResponseBody);
        specifiedTagsRadio.setEnabled(rewriteResponseBody);
        specifiedTagsTextField.setEnabled(rewriteResponseBody && specifiedTagsRadio.isSelected());
    }
}