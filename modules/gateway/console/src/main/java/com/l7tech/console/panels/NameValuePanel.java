package com.l7tech.console.panels;

import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.util.NameValuePair;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;

/**
 * A validated panel for editing a simple name-value pair, such as an HTTP header.
 */
public class NameValuePanel extends ValidatedPanel<NameValuePair> {
    private JPanel contentPane;
    private JLabel nameLabel;
    private JLabel valueLabel;
    private JTextField nameField;
    private JTextField valueField;

    private NameValuePair model = new NameValuePair("", "");

    public NameValuePanel() {
        init();
    }

    public NameValuePanel(NameValuePair model) {
        this.model = model;
        init();
    }

    @Override
    protected NameValuePair getModel() {
        return model;
    }

    @Override
    protected void initComponents() {
        nameField.getDocument().addDocumentListener(syntaxListener());
        valueField.getDocument().addDocumentListener(syntaxListener());

        nameField.setText(model.getKey());
        valueField.setText(model.getValue());
        setLayout(new BorderLayout());
        add(contentPane, BorderLayout.CENTER);
    }

    @Override
    public void focusFirstComponent() {
        nameField.requestFocusInWindow();
    }

    @Override
    protected void doUpdateModel() {
        model.setKey(nameField.getText());
        model.setValue(valueField.getText());
    }

    @Override
    protected String getSyntaxError(final NameValuePair model) {
        String error = null;
        // must validate value set on TextField, not the NameValuePair
        // because NameValuePair is not updated until OK is clicked
        if(StringUtils.isBlank(nameField.getText())){
            error = nameLabel.getText() + " must be specified.";
        }
        return error;
    }

    public void setNameLabelText(String label) {
        nameLabel.setText(label);
    }

    public void setValueLabelText(String label) {
        valueLabel.setText(label);
    }
}
