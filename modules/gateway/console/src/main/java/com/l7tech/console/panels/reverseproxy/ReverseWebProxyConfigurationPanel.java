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
    private JCheckBox cookieCheckBox;
    private JRadioButton allContentRadio;
    private JRadioButton specifiedTagsRadio;
    private JRadioButton noneRadioButton;
    private JTextField specifiedTagsTextField;
    private JComboBox platformComboBox;
    private JCheckBox httpsCheckBox;

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
        allContentRadio.addActionListener(changeListener);
        noneRadioButton.addActionListener(changeListener);
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
                (!specifiedTagsRadio.isSelected() || StringUtils.isNotBlank(specifiedTagsTextField.getText()));
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
            locationCheckBox.setSelected(config.isRewriteLocationHeader());
            cookieCheckBox.setSelected(config.isRewriteCookies());
            allContentRadio.setSelected(config.isRewriteResponseContent() && StringUtils.isBlank(config.getHtmlTagsToRewrite()));
            noneRadioButton.setSelected(!config.isRewriteResponseContent());
            specifiedTagsRadio.setSelected(config.isRewriteResponseContent() && StringUtils.isNotBlank(config.getHtmlTagsToRewrite()));
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
            config.setRewriteLocationHeader(locationCheckBox.isSelected());
            config.setRewriteCookies(cookieCheckBox.isSelected());
            config.setRewriteResponseContent(allContentRadio.isSelected() || specifiedTagsRadio.isSelected());
            config.setHtmlTagsToRewrite(specifiedTagsTextField.getText().trim());
        }
    }

    private void enableDisable() {
        specifiedTagsTextField.setEnabled(specifiedTagsRadio.isSelected());
    }
}