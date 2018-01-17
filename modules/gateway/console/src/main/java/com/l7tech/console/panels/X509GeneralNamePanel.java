package com.l7tech.console.panels;

import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.util.NameValuePair;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class X509GeneralNamePanel extends ValidatedPanel<NameValuePair> {
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
            error =  nameLabel.getText() + " must be specified.";
        }
        else if(type.equalsIgnoreCase("rfc822Name")) {
            error = validatePattern(rfc822Pattern, value, nameLabel.getText() + " is not in proper format");
        }
        else if(type.equalsIgnoreCase("dNSName")) {
            error = validatePattern(dnsNamePattern, value, nameLabel.getText() + " is not in proper format");
        }
        else if(type.equalsIgnoreCase("iPAddress")) {
            error = validatePattern(ipAddressPattern, value, nameLabel.getText() + " is not in proper format");
        }
        else if(type.equalsIgnoreCase("directoryName")) {
            error = validatePattern(directoryNamePattern, value, nameLabel.getText() + " is not in proper format");
        }
        else if(type.equalsIgnoreCase("uniformResourceIdentifier")) {
            error = validatePattern(urlPattern, value, nameLabel.getText() + " is not in proper format");
        }
        return error;
    }

    private static final Pattern rfc822Pattern = Pattern.compile("^[a-zA-Z0-9_!#$%&’*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$");
    private static final Pattern dnsNamePattern = Pattern.compile("^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$");
    private static final Pattern ipAddressPattern = Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$");
    private static final Pattern directoryNamePattern = Pattern.compile("(\\w+[=]{1}[a-zA-Z0-9\\-\\$&\\(\\)\\[\\]\\{\\}\\.\\s]+)([,{1}]\\s*\\w+[=]{1}[a-zA-Z0-9\\-\\(\\)\\[\\]\\{\\}\\.\\s]+)*");
    private static final Pattern urlPattern =Pattern.compile("^([a-z0-9+.-]+):(?://(?:((?:[a-z0-9-._~!$&'()*+,;=:]|%[0-9A-F]{2})*)@)?((?:[a-z0-9-._~!$&'()*+,;=]|%[0-9A-F]{2})*)(?::(\\d*))?(/(?:[a-z0-9-._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)?|(/?(?:[a-z0-9-._~!$&'()*+,;=:@]|%[0-9A-F]{2})+(?:[a-z0-9-._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)?)(?:\\?((?:[a-z0-9-._~!$&'()*+,;=:/?@]|%[0-9A-F]{2})*))?(?:#((?:[a-z0-9-._~!$&'()*+,;=:/?@]|%[0-9A-F]{2})*))?$");

    private static String validatePattern(Pattern p, String s, String msg) {
        String error = null;
        if(!p.matcher(s).matches()) {
            error = msg;
        }
        return error;
    }
}
