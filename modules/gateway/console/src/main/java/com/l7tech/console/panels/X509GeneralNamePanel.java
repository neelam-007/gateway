package com.l7tech.console.panels;

import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.util.NameValuePair;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;

public class X509GeneralNamePanel extends ValidatedPanel<NameValuePair> {
    private JComboBox typeComboBox;
    private JTextField nameTextField;
    private JPanel contentPane;
    private JLabel nameLabel;

    private NameValuePair model = new NameValuePair("", "");

    public X509GeneralNamePanel() {
        init();
    }

    public X509GeneralNamePanel(NameValuePair model) {
        this.model = model;
        init();
    }

    @Override
    protected NameValuePair getModel() {
        return model;
    }

    @Override
    protected void initComponents() {
        nameTextField.getDocument().addDocumentListener(syntaxListener());

        //typeComboBox.setModel(ComboBoxModel).setText(model.getKey());
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
        model.setKey((String)typeComboBox.getSelectedItem());
        model.setValue(nameTextField.getText());
    }

    @Override
    protected String getSyntaxError(final NameValuePair model) {
        String error = null;
        // must validate value set on TextField, not the NameValuePair
        // because NameValuePair is not updated until OK is clicked
        if(StringUtils.isBlank(nameTextField.getText())){
            error = nameLabel.getText() + " must be specified.";
        }
        return error;
    }
}
