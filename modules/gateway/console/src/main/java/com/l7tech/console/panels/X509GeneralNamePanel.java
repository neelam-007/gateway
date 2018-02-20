package com.l7tech.console.panels;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.X509GeneralName;
import com.l7tech.gui.util.PauseListener;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.NameValuePair;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        TextComponentPauseListenerManager.registerPauseListener(nameTextField, new PauseListener() {
            public void textEntryPaused(JTextComponent component, long msecs) {
                checkSyntax();
                checkSemantic();
            }

            public void textEntryResumed(JTextComponent component) {
                syntaxOk = false;
                statusLabel.setText(null);
            }
        }, 500);
        initComponents();
    }

    @Override
    protected NameValuePair getModel() {
        return model;
    }

    @Override
    protected void initComponents() {
        typeComboBox.setModel(new DefaultComboBoxModel(
                Arrays.stream(X509GeneralName.Type.values())
                        .filter(new Predicate<X509GeneralName.Type>() {
                            @Override
                            public boolean test(X509GeneralName.Type type) {
                                return CertUtils.isSubjectAlternativeNameTypeSupported(type);
                            }
                        })
                        .map(new Function<X509GeneralName.Type, String>() {
                            @Override
                            public String apply(X509GeneralName.Type type) {
                                return type.getUserFriendlyName();
                            }
                        })
                        .collect(Collectors.toList())
                        .toArray(new String[0])
        ));

        nameTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { checkSyntax(); }
            public void removeUpdate(DocumentEvent e) { checkSyntax(); }
            public void changedUpdate(DocumentEvent e) { checkSyntax(); }
        });
        typeComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                checkSyntax();
            }
        });
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
        typeComboBox.requestFocusInWindow();
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
        if (StringUtils.isBlank(value)) {
            error = MessageFormat.format(resources.getString("error.name.isBlank"), nameLabel.getText());
        } else if (type.equalsIgnoreCase(X509GeneralName.Type.rfc822Name.getUserFriendlyName())) {
            error = validatePattern(CertUtils.rfc822Pattern, value, MessageFormat.format(resources.getString("error.rfc822Name.invalidFormat"), nameLabel.getText()));
        } else if (type.equalsIgnoreCase(X509GeneralName.Type.dNSName.getUserFriendlyName())) {
            error = validatePattern(CertUtils.dnsNamePattern, value, MessageFormat.format(resources.getString("error.dNSName.invalidFormat"), nameLabel.getText()));
        } else if (type.equalsIgnoreCase(X509GeneralName.Type.iPAddress.getUserFriendlyName())) {
            if (!InetAddressUtil.looksLikeIpAddressV4OrV6(value))
                error = MessageFormat.format(resources.getString("error.iPAddress.invalidFormat"), nameLabel.getText());
        } else if (type.equalsIgnoreCase(X509GeneralName.Type.directoryName.getUserFriendlyName())) {
            error = validatePattern(CertUtils.directoryNamePattern, value, MessageFormat.format(resources.getString("error.directoryName.invalidFormat"), nameLabel.getText()));
        } else if (type.equalsIgnoreCase(X509GeneralName.Type.uniformResourceIdentifier.getUserFriendlyName())) {
            error = validatePattern(CertUtils.urlPattern, value, MessageFormat.format(resources.getString("error.uniformResourceIdentifier.invalidFormat"), nameLabel.getText()));
        }
        return error;
    }

    protected void goodSyntax() {
        ((SquigglyTextField)nameTextField).setNone();
    }

    protected void badSyntax() {
        ((SquigglyTextField)nameTextField).setColor(Color.RED);
        ((SquigglyTextField)nameTextField).setSquiggly();
        ((SquigglyTextField)nameTextField).setAll();
    }

    private static String validatePattern(Pattern p, String s, String msg) {
        String error = null;
        if(!p.matcher(s).matches()) {
            error = msg;
        }
        return error;
    }

    private void createUIComponents() {
        nameTextField = new SquigglyTextField();
    }
}
