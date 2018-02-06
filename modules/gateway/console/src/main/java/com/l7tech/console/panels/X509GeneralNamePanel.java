package com.l7tech.console.panels;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.NameValuePair;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class X509GeneralNamePanel extends ValidatedPanel<NameValuePair> {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.X509GeneralNamePanel");
    private JComboBox typeComboBox;
    private JTextField nameTextField;
    private JPanel contentPane;
    private JLabel nameLabel;
    private JLabel statusLabel;

    private NameValuePair model = new NameValuePair("", "");

    public X509GeneralNamePanel() {
        init();
    }

    public X509GeneralNamePanel(NameValuePair model) {
        this.model = model;
        setStatusLabel(statusLabel);
        init();
    }

    @Override
    protected NameValuePair getModel() {
        return model;
    }

    @Override
    protected void initComponents() {
        typeComboBox.setModel(new DefaultComboBoxModel(new String[]{
                "dNSName",
                "directoryName",
                "iPAddress",
                "rfc822Name",
                "uniformResourceIdentifier"
                }));

        nameTextField.getDocument().addDocumentListener(syntaxListener());
        typeComboBox.addItemListener(syntaxListener());
        if(StringUtils.isNotEmpty(model.getKey())) {
            typeComboBox.setSelectedItem(model.getKey());
        }
        else {
            typeComboBox.setSelectedIndex(0);
        }
        nameTextField.setText(model.getValue());
        setLayout(new BorderLayout());
        add(contentPane, BorderLayout.CENTER);
    }

    @Override
    public void focusFirstComponent() {
        nameTextField.requestFocusInWindow();
    }

    @Override
    protected void doUpdateModel() {
        model.setKey((String) typeComboBox.getSelectedItem());
        model.setValue(nameTextField.getText());
    }

    @Override
    protected String getSyntaxError(final NameValuePair model) {
        String error = null;
        if(typeComboBox.getSelectedItem() == null) return "";
        // must validate value set on TextField, not the NameValuePair
        // because NameValuePair is not updated until OK is clicked
        String type = typeComboBox.getSelectedItem().toString();
        String value = nameTextField.getText();
        if(StringUtils.isBlank(value)){
            error = MessageFormat.format(resources.getString("error.name.isBlank"),nameLabel.getText());
        }
        else if(type.equalsIgnoreCase("rfc822Name")) {
            error = validatePattern(CertUtils.rfc822Pattern, value, MessageFormat.format(resources.getString("error.rfc822Name.invalidFormat"),nameLabel.getText()));
        }
        else if(type.equalsIgnoreCase("dNSName")) {
            error = validatePattern(CertUtils.dnsNamePattern, value, MessageFormat.format(resources.getString("error.dNSName.invalidFormat"),nameLabel.getText()));
        }
        else if(type.equalsIgnoreCase("iPAddress")) {
            if(!InetAddressUtil.looksLikeIpAddressV4OrV6(value))
                error = MessageFormat.format(resources.getString("error.iPAddress.invalidFormat"),nameLabel.getText());
        }
        else if(type.equalsIgnoreCase("directoryName")) {
            error = validatePattern(CertUtils.directoryNamePattern, value, MessageFormat.format(resources.getString("error.directoryName.invalidFormat"),nameLabel.getText()));
        }
        else if(type.equalsIgnoreCase("uniformResourceIdentifier")) {
            error = validatePattern(CertUtils.urlPattern, value, MessageFormat.format(resources.getString("error.uniformResourceIdentifier.invalidFormat"),nameLabel.getText()));
        }
        return error;
    }

    private static String validatePattern(Pattern p, String s, String msg) {
        String error = null;
        if(!p.matcher(s).matches()) {
            error = msg;
        }
        return error;
    }
}
