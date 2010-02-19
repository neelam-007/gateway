package com.l7tech.external.assertions.cache.console;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.external.assertions.cache.CacheLookupAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class CacheLookupPropertiesDialog extends AssertionPropertiesEditorSupport<CacheLookupAssertion> {
    public static final String TITLE = "Cache Lookup Properties";
    
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JRadioButton useResponseRadioButton;
    private JRadioButton useRequestRadioButton;
    private JRadioButton useVariableRadioButton;
    private SquigglyTextField cacheIdField;
    private SquigglyTextField cacheKeyField;
    private SquigglyTextField maxAgeField;
    private SquigglyTextField variableNameField;
    private JTextField contentTypeOverride;

    /** @noinspection ThisEscapedInObjectConstruction*/
    private final InputValidator validator = new InputValidator(this, TITLE);
    private boolean confirmed = false;

    public CacheLookupPropertiesDialog(Window owner) {
        super(owner, TITLE);
        init();
    }

    private void init() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        validator.attachToButton(buttonOK, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        buttonCancel.addActionListener(new AbstractAction("cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        validator.constrainTextFieldToBeNonEmpty("Cache ID", cacheIdField, null);
        validator.constrainTextFieldToBeNonEmpty("Cache Entry Key", cacheKeyField, null);
        validator.constrainTextFieldToBeNonEmpty("Context Variable Name", variableNameField, null);
        validator.constrainTextFieldToNumberRange("Maximum age", maxAgeField, 0, 1000000L);
        validator.constrainTextField(contentTypeOverride, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                try {
                    String contentType = contentTypeOverride.getText();
                    if (contentType == null || contentType.isEmpty())
                        return null;
                    ContentTypeHeader.parseValue(contentType);
                    return null;
                } catch (IOException e) {
                    return "Invalid content type configured: " + ExceptionUtils.getMessage(e);
                }
            }
        });
        maxAgeField.setText("86400");

        ActionListener updateAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateEnableState();
            }
        };
        useRequestRadioButton.addActionListener(updateAction);
        useResponseRadioButton.addActionListener(updateAction);
        useVariableRadioButton.addActionListener(updateAction);

        Utilities.enableGrayOnDisabled(variableNameField);

        Utilities.setEscKeyStrokeDisposes(this);
        
        updateEnableState();
    }

    private void updateEnableState() {
        variableNameField.setEnabled(useVariableRadioButton.isSelected());
    }

    private void onOk() {
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    public static void main(String[] args) {
        CacheLookupPropertiesDialog dialog = new CacheLookupPropertiesDialog(null);
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(CacheLookupAssertion ass) {
        cacheIdField.setText(ass.getCacheId());
        cacheKeyField.setText(ass.getCacheEntryKey());
        maxAgeField.setText(Long.toString(ass.getMaxEntryAgeMillis() / 1000L));
        contentTypeOverride.setText(ass.getContentTypeOverride());

        String vn = ass.getTargetVariableName();
        variableNameField.setEnabled(vn != null);
        if (vn == null) {
            useRequestRadioButton.setSelected(ass.isUseRequest());
            useResponseRadioButton.setSelected(!ass.isUseRequest());
        } else {
            variableNameField.setText(vn);
            useVariableRadioButton.setSelected(true);
        }
    }

    @Override
    public CacheLookupAssertion getData(CacheLookupAssertion ass) {
        ass.setCacheId(cacheIdField.getText());
        ass.setCacheEntryKey(cacheKeyField.getText());
        ass.setMaxEntryAgeMillis(Long.parseLong(maxAgeField.getText()) * 1000L);
        ass.setTargetVariableName(useVariableRadioButton.isSelected() ? variableNameField.getText() : null);
        ass.setUseRequest(useRequestRadioButton.isSelected());
        ass.setContentTypeOverride(contentTypeOverride.getText());
        return ass;
    }
}
